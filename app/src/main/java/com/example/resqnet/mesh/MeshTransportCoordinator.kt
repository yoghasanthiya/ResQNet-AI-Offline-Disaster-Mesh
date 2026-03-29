package com.example.resqnet.mesh

import android.Manifest
import android.annotation.SuppressLint
import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.resqnet.data.model.DiscoveredPeer
import com.example.resqnet.data.repository.MeshRepository
import com.example.resqnet.service.MeshNotificationHelper
import com.example.resqnet.util.MeshEncryption
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MeshTransportCoordinator(
    private val application: Application,
    private val repository: MeshRepository
) {
    private companion object {
        private const val TAG = "MeshTransport"
        private const val CONNECT_RETRY_DELAY_MS = 2_000L
        private const val MAX_CONNECT_ATTEMPTS = 3
        private const val AUTO_CONNECT_THROTTLE_MS = 12_000L
    }

    private val adapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val serviceUuid: UUID = UUID.fromString("d81d8274-60b1-4f42-89d7-569c87c8ff95")
    private val connections = ConcurrentHashMap<String, PeerConnection>()
    private val connectionAttempts = ConcurrentHashMap<String, Int>()
    private val lastAutoConnectAttemptAt = ConcurrentHashMap<String, Long>()
    private var acceptThread: Thread? = null
    private var discoveryJob: Job? = null
    private var retryJob: Job? = null
    private var serverSocket: BluetoothServerSocket? = null
    private var receiverRegistered = false
    private var started = false

    private val _peers = MutableStateFlow<List<DiscoveredPeer>>(emptyList())
    val peers: StateFlow<List<DiscoveredPeer>> = _peers

    private val _status = MutableStateFlow("Mesh offline")
    val status: StateFlow<String> = _status

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    }
                    val rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE)
                        .takeUnless { it == Short.MIN_VALUE }
                        ?.toInt()
                    device?.let {
                        rememberPeer(it, connected = false, rssi = rssi)
                        maybeAutoConnect(it)
                    }
                }

                BluetoothAdapter.ACTION_DISCOVERY_STARTED -> {
                    Log.d(TAG, "Bluetooth discovery started")
                    _status.value = "Scanning nearby peers"
                }

                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    Log.d(TAG, "Bluetooth discovery finished with ${_peers.value.size} known peers")
                    connectToBestAvailablePeer()
                    _status.value = if (connections.isEmpty()) {
                        "Store-carry-forward active. Waiting for peers."
                    } else {
                        "Connected to ${connections.size} relay peers"
                    }
                }

                BluetoothAdapter.ACTION_STATE_CHANGED -> {
                    val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                    Log.d(TAG, "Bluetooth adapter state changed: $state")
                    if (state == BluetoothAdapter.STATE_ON && started) {
                        startDiscovery()
                    }
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun start() {
        if (started) return
        if (adapter == null) {
            _status.value = "Bluetooth unsupported on this device"
            return
        }
        if (!hasBluetoothRuntimePermissions()) {
            _status.value = "Bluetooth permissions required for mesh mode"
            return
        }
        if (!adapter.isEnabled) {
            _status.value = "Bluetooth is off. Turn it on to scan nearby devices."
            return
        }
        started = true
        registerReceiverIfNeeded()
        loadBondedDevices()
        startServerSocket()
        startDiscovery()
        discoveryJob = scope.launch {
            while (started) {
                flushQueue()
                delay(25_000)
                startDiscovery()
            }
        }
        retryJob = scope.launch {
            while (started) {
                repository.registerRetryForPendingAcks()
                flushQueue()
                delay(15_000)
            }
        }
    }

    fun stop() {
        if (!started) return
        started = false
        Log.i(TAG, "Stopping mesh transport")
        discoveryJob?.cancel()
        retryJob?.cancel()
        discoveryJob = null
        retryJob = null
        runCatching { application.unregisterReceiver(receiver) }
        receiverRegistered = false
        runCatching { adapter?.cancelDiscovery() }
        runCatching { serverSocket?.close() }
        serverSocket = null
        acceptThread?.interrupt()
        acceptThread = null
        connections.values.forEach { it.close() }
        connections.clear()
        connectionAttempts.clear()
        lastAutoConnectAttemptAt.clear()
        _peers.value = _peers.value.map { it.copy(isConnected = false, isConnecting = false) }
        _status.value = "Mesh offline"
    }

    fun flushQueue() {
        scope.launch {
            val pending = repository.pendingMeshPackets()
            val retries = repository.getRetryPackets()
            val packets = (pending + retries).distinctBy { it.id }
            if (packets.isEmpty()) return@launch
            if (connections.isEmpty()) {
                Log.d(TAG, "Flush skipped because there are no active Bluetooth connections")
                return@launch
            }
            packets.forEach { packet ->
                if (packet.packetKind == PacketKind.MESSAGE && packet.hopCount >= packet.ttl) return@forEach
                val outbound = if (packet.packetKind == PacketKind.MESSAGE) {
                    packet.copy(hopCount = packet.hopCount + 1)
                } else {
                    packet
                }
                connections.forEach { (address, connection) ->
                    val sent = connection.send(outbound)
                    if (sent && outbound.packetKind == PacketKind.MESSAGE) {
                        Log.d(
                            TAG,
                            "Relaying message ${outbound.id} with hop ${outbound.hopCount}/${outbound.ttl} to $address"
                        )
                        repository.markSending(outbound.id, address)
                        repository.markForwarded(outbound.id, outbound.hopCount)
                    }
                }
            }
        }
    }

    fun scanNow() {
        if (!started) {
            start()
        }
        loadBondedDevices()
        startDiscovery()
    }

    fun isBluetoothSupported(): Boolean = adapter != null

    @SuppressLint("MissingPermission")
    fun isBluetoothEnabled(): Boolean = adapter?.isEnabled == true

    fun isRunning(): Boolean = started

    fun updateStatus(message: String) {
        _status.value = message
    }

    @SuppressLint("MissingPermission")
    fun connectToPeer(address: String) {
        val bluetoothAdapter = adapter ?: return
        if (!hasBluetoothRuntimePermissions()) {
            _status.value = "Bluetooth permissions required for mesh mode"
            return
        }
        if (!isBluetoothEnabled()) {
            _status.value = "Bluetooth is off. Turn it on to connect."
            return
        }
        if (!started) {
            start()
        }
        if (!started) return
        val peer = _peers.value.firstOrNull { it.address == address }
        if (connections.containsKey(address)) {
            _status.value = "Already connected to ${peer?.name ?: address}"
            return
        }
        if (connectionAttempts.containsKey(address)) {
            _status.value = "Already attempting connection to ${peer?.name ?: address}"
            return
        }
        updatePeerConnectionState(address, isConnecting = true, isConnected = false)
        _status.value = "Connecting to ${peer?.name ?: address}"
        val device = bluetoothAdapter.getRemoteDevice(address)
        connectToDevice(device)
    }

    @SuppressLint("MissingPermission")
    private fun startDiscovery() {
        val bluetoothAdapter = adapter ?: return
        if (!started) return
        if (!hasBluetoothRuntimePermissions()) {
            _status.value = "Bluetooth permissions required for mesh mode"
            return
        }
        if (!bluetoothAdapter.isEnabled) {
            _status.value = "Bluetooth is off. Turn it on to scan nearby devices."
            return
        }
        if (bluetoothAdapter.isDiscovering) {
            bluetoothAdapter.cancelDiscovery()
        }
        val startedDiscovery = bluetoothAdapter.startDiscovery()
        if (startedDiscovery) {
            Log.d(TAG, "startDiscovery() succeeded")
            return
        }

        Log.w(TAG, "startDiscovery() returned false. Retrying once. state=${bluetoothAdapter.state}")
        scope.launch {
            delay(1_000L)
            if (!started || !bluetoothAdapter.isEnabled) return@launch
            runCatching { if (bluetoothAdapter.isDiscovering) bluetoothAdapter.cancelDiscovery() }
            val retryStarted = bluetoothAdapter.startDiscovery()
            if (retryStarted) {
                Log.d(TAG, "Bluetooth discovery retry succeeded")
                _status.value = "Scanning nearby peers"
            } else {
                Log.e(TAG, "Bluetooth discovery retry failed. state=${bluetoothAdapter.state}")
                loadBondedDevices()
                _status.value = if (_peers.value.isNotEmpty()) {
                    "Live scan unavailable. Showing paired Bluetooth devices instead."
                } else {
                    "Unable to start Bluetooth scan. Check Bluetooth permissions or pair a device first."
                }
            }
        }
    }

    private fun startServerSocket() {
        acceptThread = Thread {
            try {
                serverSocket = createServerSocket()
                Log.i(TAG, "Bluetooth server socket started")
                while (started) {
                    val socket = serverSocket?.accept() ?: break
                    Log.i(TAG, "Accepted incoming Bluetooth socket from ${socket.remoteDevice.address}")
                    handleSocket(socket)
                }
            } catch (exception: Exception) {
                Log.w(TAG, "Bluetooth server socket stopped", exception)
                _status.value = if (started) "Mesh listener interrupted" else "Mesh offline"
            }
        }.apply { start() }
    }

    @SuppressLint("MissingPermission")
    private fun connectToDevice(device: BluetoothDevice) {
        if (!started || !hasBluetoothRuntimePermissions()) return
        if (connections.containsKey(device.address)) return
        if (connectionAttempts.containsKey(device.address)) return
        scope.launch {
            val peerName = device.safeName()
            var lastError: Throwable? = null
            repeat(MAX_CONNECT_ATTEMPTS) { attempt ->
                val attemptNumber = attempt + 1
                connectionAttempts[device.address] = attemptNumber
                Log.i(TAG, "Connecting to $peerName (${device.address}), attempt $attemptNumber/$MAX_CONNECT_ATTEMPTS")
                val result = runCatching {
                    adapter?.cancelDiscovery()
                    val socket = createClientSocket(device, attemptNumber)
                    socket.connect()
                    socket
                }
                result.onSuccess { socket ->
                    Log.i(TAG, "Bluetooth socket connected to $peerName (${device.address})")
                    handleSocket(socket)
                    connectionAttempts.remove(device.address)
                    flushQueue()
                    return@launch
                }.onFailure { error ->
                    lastError = error
                    Log.w(TAG, "Connection attempt $attemptNumber failed for $peerName (${device.address})", error)
                    runCatching { adapter?.cancelDiscovery() }
                    updatePeerConnectionState(device.address, isConnecting = true, isConnected = false)
                    delay(CONNECT_RETRY_DELAY_MS)
                }
            }
            connectionAttempts.remove(device.address)
            updatePeerConnectionState(device.address, isConnecting = false, isConnected = false)
            _status.value = "Connection failed for ${device.safeName()}"
            Log.e(TAG, "All connection attempts failed for $peerName (${device.address})", lastError)
        }
    }

    private fun handleSocket(socket: BluetoothSocket) {
        connections[socket.remoteDevice.address]?.close()
        val connection = PeerConnection(socket)
        connections[socket.remoteDevice.address] = connection
        connectionAttempts.remove(socket.remoteDevice.address)
        rememberPeer(socket.remoteDevice, connected = true, rssi = currentPeer(socket.remoteDevice.address)?.rssi)
        _status.value = "Connected to ${connections.size} relay peers"
        Log.i(TAG, "Connection ready for ${socket.remoteDevice.safeName()} (${socket.remoteDevice.address})")
        connection.start()
    }

    private fun registerReceiverIfNeeded() {
        if (receiverRegistered) return
        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
        }
        application.registerReceiver(receiver, filter)
        receiverRegistered = true
    }

    private fun rememberPeer(device: BluetoothDevice, connected: Boolean, rssi: Int?) {
        val current = _peers.value.associateBy { it.address }.toMutableMap()
        val existing = current[device.address]
        current[device.address] = DiscoveredPeer(
            name = device.safeName(),
            address = device.address,
            isConnected = connected || existing?.isConnected == true,
            isConnecting = false,
            rssi = rssi ?: existing?.rssi,
            lastSeenAt = System.currentTimeMillis()
        )
        _peers.value = current.values.sortedWith(
            compareByDescending<DiscoveredPeer> { it.isConnected }
                .thenByDescending { it.rssi ?: Int.MIN_VALUE }
                .thenBy { it.name.lowercase() }
        )
        Log.d(TAG, "Discovered peer ${device.safeName()} (${device.address}), connected=$connected, rssi=${rssi ?: existing?.rssi}")
        scope.launch { repository.rememberKnownDevice(device.address, device.safeName(), connected) }
    }

    @SuppressLint("MissingPermission")
    private fun loadBondedDevices() {
        val bluetoothAdapter = adapter ?: return
        if (!hasBluetoothRuntimePermissions()) return
        runCatching {
            bluetoothAdapter.bondedDevices.orEmpty().forEach { device ->
                rememberPeer(
                    device = device,
                    connected = connections.containsKey(device.address),
                    rssi = currentPeer(device.address)?.rssi
                )
            }
            if (bluetoothAdapter.bondedDevices.isNotEmpty()) {
                Log.d(TAG, "Loaded ${bluetoothAdapter.bondedDevices.size} bonded Bluetooth devices")
            }
        }.onFailure { error ->
            Log.w(TAG, "Unable to load bonded Bluetooth devices", error)
        }
    }

    private fun hasBluetoothRuntimePermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(application, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(application, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    @SuppressLint("MissingPermission")
    private fun BluetoothDevice.safeName(): String {
        return runCatching { name }.getOrNull().orEmpty().ifBlank { address }
    }

    private fun currentPeer(address: String): DiscoveredPeer? {
        return _peers.value.firstOrNull { it.address == address }
    }

    private fun updatePeerConnectionState(
        address: String,
        isConnecting: Boolean,
        isConnected: Boolean
    ) {
        _peers.value = _peers.value.map { peer ->
            if (peer.address == address) {
                peer.copy(isConnecting = isConnecting, isConnected = isConnected)
            } else {
                peer
            }
        }
        Log.d(TAG, "Peer state updated for $address: connecting=$isConnecting connected=$isConnected")
    }

    @SuppressLint("MissingPermission")
    private fun maybeAutoConnect(device: BluetoothDevice) {
        if (!started || connections.isNotEmpty()) return
        if (connectionAttempts.containsKey(device.address)) return
        val lastAttempt = lastAutoConnectAttemptAt[device.address] ?: 0L
        val now = System.currentTimeMillis()
        if (now - lastAttempt < AUTO_CONNECT_THROTTLE_MS) return
        lastAutoConnectAttemptAt[device.address] = now
        Log.d(TAG, "Auto-connecting to discovered peer ${device.safeName()} (${device.address})")
        connectToDevice(device)
    }

    @SuppressLint("MissingPermission")
    private fun connectToBestAvailablePeer() {
        if (!started || connections.isNotEmpty()) return
        val bluetoothAdapter = adapter ?: return
        val peer = _peers.value
            .filterNot { it.isConnected || it.isConnecting }
            .maxByOrNull { it.rssi ?: Int.MIN_VALUE }
            ?: return
        if (connectionAttempts.containsKey(peer.address)) return
        Log.d(TAG, "Discovery finished. Trying best available peer ${peer.name} (${peer.address})")
        runCatching { bluetoothAdapter.getRemoteDevice(peer.address) }
            .onSuccess { device ->
                lastAutoConnectAttemptAt[peer.address] = System.currentTimeMillis()
                connectToDevice(device)
            }
            .onFailure { error ->
                Log.w(TAG, "Unable to resolve best available peer ${peer.address}", error)
            }
    }

    @SuppressLint("MissingPermission")
    private fun createServerSocket(): BluetoothServerSocket? {
        val bluetoothAdapter = adapter ?: return null
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD_MR1) {
            bluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord("ResQNetMesh", serviceUuid)
        } else {
            bluetoothAdapter.listenUsingRfcommWithServiceRecord("ResQNetMesh", serviceUuid)
        }
    }

    @SuppressLint("MissingPermission")
    private fun createClientSocket(device: BluetoothDevice, attemptNumber: Int): BluetoothSocket {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD_MR1) {
            device.createInsecureRfcommSocketToServiceRecord(serviceUuid)
        } else {
            device.createRfcommSocketToServiceRecord(serviceUuid)
        }
    }

    private inner class PeerConnection(
        private val socket: BluetoothSocket
    ) {
        private val writer = BufferedWriter(OutputStreamWriter(socket.outputStream))
        private val reader = BufferedReader(InputStreamReader(socket.inputStream))

        fun start() {
            scope.launch {
                runCatching {
                    while (started) {
                        val line = reader.readLine() ?: break
                        Log.d(TAG, "Received packet from ${socket.remoteDevice.safeName()} (${socket.remoteDevice.address})")
                        val packet = MeshPacket.fromJson(line)
                        val result = repository.ingestIncomingPacket(packet)
                        result.ackPacket?.let { send(it) }
                        if (result.processed) {
                            MeshNotificationHelper.showIncomingMessage(
                                context = application,
                                senderName = packet.senderName.ifBlank { socket.remoteDevice.safeName() },
                                content = MeshEncryption.decrypt(packet.encryptedPayload),
                                type = packet.type
                            )
                            Log.i(TAG, "Processed incoming ${packet.packetKind.name} ${packet.id} from ${packet.senderName}")
                            Log.i(
                                TAG,
                                "Auto-relay queued for ${packet.id}; forwarding to ${connections.size} connected peers"
                            )
                            flushQueue()
                        }
                    }
                    Log.i(TAG, "Input stream ended for ${socket.remoteDevice.safeName()} (${socket.remoteDevice.address})")
                }
                .onFailure { error ->
                    Log.w(TAG, "Receive loop failed for ${socket.remoteDevice.safeName()} (${socket.remoteDevice.address})", error)
                }
                close()
            }
        }

        fun send(packet: MeshPacket): Boolean {
            return runCatching {
                writer.write(packet.toJson())
                writer.newLine()
                writer.flush()
                Log.d(TAG, "Sent ${packet.packetKind.name} ${packet.id} to ${socket.remoteDevice.safeName()} (${socket.remoteDevice.address})")
                true
            }
                .onFailure { error ->
                    Log.w(TAG, "Send failed for ${socket.remoteDevice.safeName()} (${socket.remoteDevice.address})", error)
                    close()
                }
                .getOrDefault(false)
        }

        fun close() {
            connections.remove(socket.remoteDevice.address)
            runCatching { reader.close() }
            runCatching { writer.close() }
            runCatching { socket.close() }
            Log.i(TAG, "Closed connection for ${socket.remoteDevice.safeName()} (${socket.remoteDevice.address})")
            _peers.value = _peers.value.map {
                if (it.address == socket.remoteDevice.address) {
                    it.copy(isConnected = false, isConnecting = false)
                } else {
                    it
                }
            }
        }
    }
}
