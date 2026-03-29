package com.example.resqnet

import android.app.Application
import com.example.resqnet.data.db.ResQNetDatabase
import com.example.resqnet.data.repository.MeshRepository
import com.example.resqnet.data.repository.UserRepository
import com.example.resqnet.mesh.MeshTransportCoordinator
import com.example.resqnet.util.LiveLocationTracker
import org.osmdroid.config.Configuration
import java.io.File

class ResQNetApp : Application() {
    override fun onCreate() {
        super.onCreate()
        val configuration = Configuration.getInstance()
        val osmdroidBase = File(cacheDir, "osmdroid").apply { mkdirs() }
        val osmdroidTiles = File(osmdroidBase, "tiles").apply { mkdirs() }
        configuration.load(this, getSharedPreferences("osmdroid", MODE_PRIVATE))
        configuration.osmdroidBasePath = osmdroidBase
        configuration.osmdroidTileCache = osmdroidTiles
        configuration.userAgentValue = packageName
        configuration.isDebugMapView = false
        configuration.tileDownloadThreads = 2
        configuration.tileFileSystemThreads = 2
    }

    val database: ResQNetDatabase by lazy { ResQNetDatabase.create(this) }
    val userRepository: UserRepository by lazy { UserRepository(database.resQNetDao()) }
    val meshRepository: MeshRepository by lazy {
        MeshRepository(
            dao = database.resQNetDao(),
            application = this,
            userRepository = userRepository
        )
    }
    val meshCoordinator: MeshTransportCoordinator by lazy {
        MeshTransportCoordinator(
            application = this,
            repository = meshRepository
        )
    }
    val liveLocationTracker: LiveLocationTracker by lazy { LiveLocationTracker(this) }
}
