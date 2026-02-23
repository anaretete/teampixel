package com.sameerasw.pixsl.utils

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.provider.Settings

data class DeviceInfo(
    val deviceName: String,
    val brand: String = Build.BRAND,
    val model: String = Build.MODEL,
    val device: String = Build.DEVICE,
    val hardware: String = Build.HARDWARE,
    val product: String = Build.PRODUCT,
    val androidVersion: String = Build.VERSION.RELEASE,
    val sdkInt: Int = Build.VERSION.SDK_INT,
    val manufacturer: String = Build.MANUFACTURER,
    val board: String = Build.BOARD,
    val display: String = Build.DISPLAY,
    val fingerprint: String = Build.FINGERPRINT,
    val totalStorage: Long,
    val availableStorage: Long,
    val totalRam: Long,
    val availableRam: Long,
    val securityPatch: String = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) Build.VERSION.SECURITY_PATCH else "Unknown",
    val osCodename: String = Build.VERSION.CODENAME
)

object DeviceUtils {
    fun getDeviceInfo(context: Context): DeviceInfo {
        val deviceName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            Settings.Global.getString(context.contentResolver, Settings.Global.DEVICE_NAME)
                ?: Settings.Secure.getString(context.contentResolver, "bluetooth_name")
                ?: Build.MODEL
        } else {
            Settings.Secure.getString(context.contentResolver, "bluetooth_name") ?: Build.MODEL
        }

        val stat = StatFs(Environment.getDataDirectory().path)
        val blockSize = stat.blockSizeLong
        val totalStorage = stat.blockCountLong * blockSize
        val availableStorage = stat.availableBlocksLong * blockSize

        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)

        return DeviceInfo(
            deviceName = deviceName,
            totalStorage = totalStorage,
            availableStorage = availableStorage,
            totalRam = memoryInfo.totalMem,
            availableRam = memoryInfo.availMem,
            securityPatch = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) Build.VERSION.SECURITY_PATCH else "Unknown",
            osCodename = Build.VERSION.CODENAME
        )
    }

    fun formatSize(size: Long): String {
        if (size <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()
        return String.format(
            "%.1f %s",
            size / Math.pow(1024.0, digitGroups.toDouble()),
            units[digitGroups]
        )
    }

    fun getOSName(sdkInt: Int, defaultCodename: String): String {
        return when (sdkInt) {
            37 -> "CinnamonBun"
            36 -> "Baklava"
            35 -> "Vanilla Ice Cream"
            34 -> "Upside Down Cake"
            33 -> "Tiramisu"
            32 -> "Snow Cone v2"
            31 -> "Snow Cone"
            30 -> "R"
            29 -> "Q"
            28 -> "Pie"
            27 -> "Oreo"
            26 -> "Oreo"
            else -> defaultCodename.takeIf { it != "REL" } ?: "Android"
        }
    }

    fun formatHardwareSize(sizeBytes: Long): String {
        if (sizeBytes <= 0) return "Unknown"
        val rawGb = sizeBytes / (1024.0 * 1024.0 * 1024.0)
        
        // Known standard hardware sizes in GB
        val standardSizes = listOf(1, 2, 3, 4, 6, 8, 10, 12, 16, 18, 24, 32, 64, 128, 256, 512, 1024, 2048)
        
        // Find the closest standard size that is greater than or equal to our raw GB (allowing a 10% delta for OS reservations)
        val roundedGb = standardSizes.firstOrNull { it >= rawGb * 0.9 } ?: Math.ceil(rawGb).toInt()
        
        return if (roundedGb >= 1024 && roundedGb % 1024 == 0) {
            "${roundedGb / 1024} TB"
        } else {
            "$roundedGb GB"
        }
    }

    fun formatSecurityPatch(patchString: String): String {
        if (patchString == "Unknown" || patchString.isBlank()) return patchString
        return try {
            val parser = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            val formatter = java.text.SimpleDateFormat("d MMMM, yyyy", java.util.Locale.getDefault())
            val date = parser.parse(patchString)
            if (date != null) formatter.format(date) else patchString
        } catch (e: Exception) {
            patchString
        }
    }
}
