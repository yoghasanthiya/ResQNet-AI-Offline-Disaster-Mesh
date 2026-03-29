package com.example.resqnet.util

import android.content.Context
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.views.MapView
import java.io.File

object MapViewConfigurator {
    fun configure(mapView: MapView, context: Context) {
        val appContext = context.applicationContext
        val configuration = Configuration.getInstance()
        val osmdroidBase = File(appContext.cacheDir, "osmdroid").apply { mkdirs() }
        val osmdroidTiles = File(osmdroidBase, "tiles").apply { mkdirs() }
        configuration.load(
            appContext,
            appContext.getSharedPreferences("osmdroid", Context.MODE_PRIVATE)
        )
        configuration.osmdroidBasePath = osmdroidBase
        configuration.osmdroidTileCache = osmdroidTiles
        configuration.userAgentValue = appContext.packageName

        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setMultiTouchControls(true)
        mapView.setBuiltInZoomControls(false)
        mapView.setUseDataConnection(true)
        mapView.isClickable = true
        mapView.isFocusable = true
        mapView.isTilesScaledToDpi = true
        mapView.isHorizontalMapRepetitionEnabled = false
        mapView.isVerticalMapRepetitionEnabled = false
        mapView.setScrollableAreaLimitLatitude(
            MapView.getTileSystem().maxLatitude,
            MapView.getTileSystem().minLatitude,
            0
        )
        mapView.minZoomLevel = 4.0
        mapView.maxZoomLevel = 19.0
        mapView.invalidate()
    }
}
