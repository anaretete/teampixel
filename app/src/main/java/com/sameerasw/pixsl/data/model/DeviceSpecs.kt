package com.sameerasw.pixsl.data.model

import kotlinx.serialization.Serializable

@Serializable
data class DeviceSpecItem(
    val name: String,
    val value: String
)

@Serializable
data class DeviceSpecCategory(
    val category: String,
    val specifications: List<DeviceSpecItem>
)

@Serializable
data class DeviceSpecs(
    val deviceName: String,
    val detailSpec: List<DeviceSpecCategory>,
    val imageUrls: List<String> = emptyList()
)
