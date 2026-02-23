package com.sameerasw.pixsl.utils

import com.sameerasw.pixsl.data.model.DeviceSpecCategory
import com.sameerasw.pixsl.data.model.DeviceSpecItem
import com.sameerasw.pixsl.data.model.DeviceSpecs
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

object GSMArenaService {
    private const val BASE_URL = "https://www.gsmarena.com"

    fun fetchSpecs(brand: String, model: String): DeviceSpecs? {
        return try {
            val query = "$brand $model".replace(" ", "+")
            val searchUrl = "$BASE_URL/results.php3?sQuickSearch=yes&sName=$query"
            
            val searchDoc: Document = Jsoup.connect(searchUrl)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                .get()
            
            val firstDevicePath = searchDoc.select(".makers li a").firstOrNull()?.attr("href") ?: return null
            val deviceUrl = if (firstDevicePath.startsWith("/")) "$BASE_URL$firstDevicePath" else "$BASE_URL/$firstDevicePath"
            
            val deviceDoc: Document = Jsoup.connect(deviceUrl)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                .get()
            
            val name = deviceDoc.select(".specs-phone-name-title").text()
            val tables = deviceDoc.select("table")
            val detailSpecs = mutableListOf<DeviceSpecCategory>()
            
            tables.forEach { table ->
                val categoryName = table.select("th").firstOrNull()?.text() ?: ""
                val rows = table.select("tr")
                val specs = mutableListOf<DeviceSpecItem>()
                
                rows.forEach { row ->
                    val label = row.select("td.ttl").text()
                    val value = row.select("td.nfo").text()
                    if (label.isNotBlank() && value.isNotBlank()) {
                        specs.add(DeviceSpecItem(label, value))
                    }
                }
                
                if (categoryName.isNotBlank() && specs.isNotEmpty()) {
                    detailSpecs.add(DeviceSpecCategory(categoryName, specs))
                }
            }
            
            DeviceSpecs(name, detailSpecs)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
