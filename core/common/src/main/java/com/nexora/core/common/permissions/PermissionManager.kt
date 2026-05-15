package com.nexora.core.common.permissions

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import javax.inject.Inject

interface PermissionManager {
    fun getMediaPermissionState(activity: Activity): PermissionState
    fun openAppSettings(context: Context)
}

class AndroidPermissionManager @Inject constructor() : PermissionManager {
    override fun getMediaPermissionState(activity: Activity): PermissionState {
        val permissions = PermissionSpec.mediaPermissions()
        val granted = permissions.all { permission ->
            ContextCompat.checkSelfPermission(activity, permission) == PackageManager.PERMISSION_GRANTED
        }
        val shouldShowRationale = permissions.any { permission ->
            ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
        }
        val permanentlyDenied = !granted && !shouldShowRationale
        return PermissionState(
            requiredPermissions = permissions,
            isGranted = granted,
            shouldShowRationale = shouldShowRationale,
            isPermanentlyDenied = permanentlyDenied
        )
    }

    override fun openAppSettings(context: Context) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
