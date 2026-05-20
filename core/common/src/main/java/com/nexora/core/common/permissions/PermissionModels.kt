package com.nexora.core.common.permissions

enum class PermissionGroup {
    MEDIA
}

data class PermissionState(
    val requiredPermissions: List<String>,
    val isGranted: Boolean,
    val shouldShowRationale: Boolean,
    val isPermanentlyDenied: Boolean
)

object PermissionSpec {
    fun mediaPermissions(): List<String> = emptyList()
}
