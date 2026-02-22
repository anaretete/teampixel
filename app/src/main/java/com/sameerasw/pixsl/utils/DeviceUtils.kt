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
    val availableRam: Long
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
            availableRam = memoryInfo.availMem
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
}
