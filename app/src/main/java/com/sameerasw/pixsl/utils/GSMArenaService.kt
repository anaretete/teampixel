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

            val firstDeviceElement = searchDoc.select(".makers li").firstOrNull() ?: return null
            val firstDevicePath = firstDeviceElement.select("a").attr("href")
            val searchThumbnail = firstDeviceElement.select("img").attr("src")

            val deviceUrl =
                if (firstDevicePath.startsWith("/")) "$BASE_URL$firstDevicePath" else "$BASE_URL/$firstDevicePath"

            val deviceDoc: Document = Jsoup.connect(deviceUrl)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                .get()

            val name = deviceDoc.select(".specs-phone-name-title").text()
            val tables = deviceDoc.select("table")
            val detailSpecs = mutableListOf<DeviceSpecCategory>()

            // Scrape images
            val imageUrls = mutableListOf<String>()

            // Fix protocol helper
            fun String.fixUrl(): String {
                return when {
                    startsWith("//") -> "https:$this"
                    startsWith("/") -> "$BASE_URL$this"
                    else -> this
                }
            }

            // Fallback to search thumbnail
            if (searchThumbnail.isNotBlank()) {
                imageUrls.add(searchThumbnail.fixUrl())
            }

            // Get main image on device page (often higher quality)
            deviceDoc.select(".specs-photo-main a img").firstOrNull()?.attr("src")?.let {
                val url = it.fixUrl()
                if (!imageUrls.contains(url)) imageUrls.add(0, url)
            }

            // Get more images from gallery if available
            val picturesLink =
                deviceDoc.select(".specs-links a:contains(Pictures)").firstOrNull()?.attr("href")
            if (picturesLink != null) {
                val picturesUrl = picturesLink.fixUrl()
                val picturesDoc = Jsoup.connect(picturesUrl)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                    .get()

                picturesDoc.select("#pictures-list img").forEach { img ->
                    val src = img.attr("src").ifBlank { img.attr("data-src") }
                    if (src.isNotBlank()) {
                        val url = src.fixUrl()
                        if (!imageUrls.contains(url)) {
                            imageUrls.add(url)
                        }
                    }
                }
            }

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

            DeviceSpecs(name, detailSpecs, imageUrls)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
