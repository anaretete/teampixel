package com.sameerasw.pixsl.utils

import android.content.Context
import com.sameerasw.pixsl.data.model.DeviceSpecCategory
import com.sameerasw.pixsl.data.model.DeviceSpecItem
import com.sameerasw.pixsl.data.model.DeviceSpecs
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

object LocalSpecService {

    fun getLocalSpecs(context: Context, model: String): DeviceSpecs? {
        return try {
            val jsonString = context.assets.open("pixel_devices.json").bufferedReader().use { it.readText() }
            val jsonArray = Json.parseToJsonElement(jsonString).jsonArray
            
            // Find the best match for the model
            val deviceJson = findBestMatch(jsonArray, model) ?: return null
            
            mapJsonToSpecs(deviceJson)
        } catch (e: Exception) {
            android.util.Log.e("LocalSpecService", "Error loading local specs", e)
            null
        }
    }

    private fun findBestMatch(jsonArray: JsonArray, model: String): JsonObject? {
        val modelLower = model.lowercase()
        val devices = jsonArray.map { it.jsonObject }
        
        // Try exact match first
        devices.firstOrNull { it["name"]?.jsonPrimitive?.content?.lowercase() == modelLower }?.let { return it }
        
        // Fallback to containment, but prioritize the longest name to avoid "Pixel 6" matching "Pixel 6 Pro"
        return devices.filter { 
            val name = it["name"]?.jsonPrimitive?.content?.lowercase() ?: ""
            modelLower.contains(name) || name.contains(modelLower)
        }.maxByOrNull { it["name"]?.jsonPrimitive?.content?.length ?: 0 }
    }

    private fun mapJsonToSpecs(json: JsonObject): DeviceSpecs {
        val name = json["name"]?.jsonPrimitive?.content ?: "Unknown Device"
        val categories = mutableListOf<DeviceSpecCategory>()

        // Map OS
        json["os_at_launch"]?.jsonPrimitive?.content?.let {
            categories.add(DeviceSpecCategory("Platform", listOf(DeviceSpecItem("OS at launch", it))))
        }

        // Map Display
        json["display"]?.jsonObject?.let { d ->
            val items = mutableListOf<DeviceSpecItem>()
            d["size_inches"]?.jsonPrimitive?.content?.let { items.add(DeviceSpecItem("Size", "$it inches")) }
            d["type"]?.jsonPrimitive?.content?.let { items.add(DeviceSpecItem("Type", it)) }
            d["panel_type"]?.jsonPrimitive?.content?.let { items.add(DeviceSpecItem("Panel", it)) }
            d["resolution"]?.jsonPrimitive?.content?.let { items.add(DeviceSpecItem("Resolution", it)) }
            d["ppi"]?.jsonPrimitive?.content?.let { items.add(DeviceSpecItem("PPI", it)) }
            d["refresh_rate"]?.jsonPrimitive?.content?.let { items.add(DeviceSpecItem("Refresh Rate", it)) }
            if (items.isNotEmpty()) categories.add(DeviceSpecCategory("Display", items))
        }

        // Map Dimensions & weight
        val dimItems = mutableListOf<DeviceSpecItem>()
        json["dimensions"]?.jsonObject?.let { d ->
            if (d.containsKey("height_mm")) {
                val h = d["height_mm"]?.jsonPrimitive?.content
                val w = d["width_mm"]?.jsonPrimitive?.content
                val th = d["depth_mm"]?.jsonPrimitive?.content
                dimItems.add(DeviceSpecItem("Size", "${h} x ${w} x ${th} mm"))
            }
        }
        json["weight_g"]?.jsonPrimitive?.content?.let { dimItems.add(DeviceSpecItem("Weight", "${it}g")) }
        if (dimItems.isNotEmpty()) categories.add(DeviceSpecCategory("Body", dimItems))

        // Map Hardware
        json["hardware"]?.jsonObject?.let { h ->
            val items = mutableListOf<DeviceSpecItem>()
            h["processor"]?.jsonPrimitive?.content?.let { items.add(DeviceSpecItem("Processor", it)) }
            h["ram"]?.jsonPrimitive?.content?.let { items.add(DeviceSpecItem("RAM", it)) }
            h["security_chip"]?.jsonPrimitive?.content?.let { items.add(DeviceSpecItem("Security", it)) }
            val options = h["storage_options"]?.jsonArray?.map { it.jsonPrimitive.content }?.joinToString(", ")
            if (!options.isNullOrEmpty()) items.add(DeviceSpecItem("Storage", options))
            if (items.isNotEmpty()) categories.add(DeviceSpecCategory("Hardware", items))
        }

        // Map Durability
        json["durability"]?.jsonObject?.let { d ->
            val items = mutableListOf<DeviceSpecItem>()
            d["ip_rating"]?.jsonPrimitive?.content?.let { items.add(DeviceSpecItem("IP Rating", it)) }
            d["glass"]?.jsonPrimitive?.content?.let { items.add(DeviceSpecItem("Materials", it)) }
            if (items.isNotEmpty()) categories.add(DeviceSpecCategory("Durability", items))
        }

        // Map Battery
        json["battery"]?.jsonObject?.let { b ->
            val items = mutableListOf<DeviceSpecItem>()
            b["capacity_typical_mah"]?.jsonPrimitive?.content?.let { items.add(DeviceSpecItem("Typical", "${it} mAh")) }
            b["capacity_min_mah"]?.jsonPrimitive?.content?.let { items.add(DeviceSpecItem("Minimum", "${it} mAh")) }
            if (items.isNotEmpty()) categories.add(DeviceSpecCategory("Battery", items))
        }

        return DeviceSpecs(
            deviceName = name,
            detailSpec = categories,
            imageUrls = emptyList() // Local image mapping handled by DeviceImageMapper if needed, or left empty
        )
    }
}
