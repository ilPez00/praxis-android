package com.praxis.app.tiles

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi
import com.praxis.app.integrations.IntegrationManager
import com.praxis.app.integrations.IntegrationType
import com.praxis.app.integrations.worker.IntegrationSyncScheduler
import com.praxis.app.WebAppActivity

/**
 * Quick Settings Tile for triggering integration sync
 * 
 * Users can add this tile to their Quick Settings panel
 * to quickly sync all connected integrations
 */
@RequiresApi(Build.VERSION_CODES.N)
class SyncIntegrationTile : TileService() {

    companion object {
        const val TAG = "SyncIntegrationTile"
    }

    override fun onStartListening() {
        super.onStartListening()
        updateTileState()
    }

    override fun onClick() {
        super.onClick()
        
        // Trigger immediate sync
        IntegrationSyncScheduler.triggerImmediateSync(this)
        
        // Update tile to show syncing state
        qsTile?.state = Tile.STATE_ACTIVE
        qsTile?.updateTile()
        
        // Open app after sync
        val intent = Intent(this, WebAppActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivityAndCollapse(intent)
    }

    private fun updateTileState() {
        qsTile?.apply {
            state = if (hasConnectedIntegrations()) {
                Tile.STATE_ACTIVE
            } else {
                Tile.STATE_INACTIVE
            }
            label = "Praxis Sync"
            updateTile()
        }
    }

    private fun hasConnectedIntegrations(): Boolean {
        return IntegrationManager.isConnected(IntegrationType.HEALTH_CONNECT) ||
               IntegrationManager.isConnected(IntegrationType.STRAVA) ||
               IntegrationManager.isConnected(IntegrationType.FITBIT) ||
               IntegrationManager.isConnected(IntegrationType.GOOGLE_CALENDAR) ||
               IntegrationManager.isConnected(IntegrationType.YAZIO)
    }
}

/**
 * Quick Settings Tile for quick logging a tracker entry
 * 
 * Allows users to quickly log common activities
 */
@RequiresApi(Build.VERSION_CODES.N)
class QuickLogTile : TileService() {

    companion object {
        const val TAG = "QuickLogTile"
    }

    override fun onStartListening() {
        super.onStartListening()
        qsTile?.apply {
            state = Tile.STATE_INACTIVE
            label = "Quick Log"
            updateTile()
        }
    }

    override fun onClick() {
        super.onClick()
        
        // Open the app to the quick log screen
        val intent = Intent(this, WebAppActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.putExtra("open_quick_log", true)
        startActivityAndCollapse(intent)
    }
}

/**
 * Helper to add quick settings tiles to the manifest
 * 
 * Add these to AndroidManifest.xml inside <application>:
 * 
 * <service
 *     android:name=".tiles.SyncIntegrationTile"
 *     android:icon="@drawable/ic_sync"
 *     android:label="Praxis Sync"
 *     android:permission="android.permission.BIND_QUICK_SETTINGS_TILE"
 *     android:exported="true">
 *     <intent-filter>
 *         <action android:name="android.service.quicksettings.action.QS_TILE" />
 *     </intent-filter>
 * </service>
 * 
 * <service
 *     android:name=".tiles.QuickLogTile"
 *     android:icon="@drawable/ic_add"
 *     android:label="Quick Log"
 *     android:permission="android.permission.BIND_QUICK_SETTINGS_TILE"
 *     android:exported="true">
 *     <intent-filter>
 *         <action android:name="android.service.quicksettings.action.QS_TILE" />
 *     </intent-filter>
 * </service>
 */
