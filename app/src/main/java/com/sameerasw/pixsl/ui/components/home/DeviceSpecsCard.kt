package com.sameerasw.pixsl.ui.components.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sameerasw.pixsl.ui.components.containers.RoundedCardContainer
import com.sameerasw.pixsl.ui.theme.Shapes

@Composable
fun DeviceSpecsCard(
    modifier: Modifier = Modifier
) {
    RoundedCardContainer(
        modifier = modifier.fillMaxWidth(),
    ) {
        // Section Header
        SpecHeader("Device Specifications")

        // Network
        SpecSection(
            title = "Network",
            specs = listOf(
                "Technology" to "GSM / HSPA / LTE / 5G",
                "Speed" to "HSPA, LTE, 5G"
            )
        )

        // Display
        SpecSection(
            title = "Display",
            specs = listOf(
                "Type" to "LTPO OLED, 120Hz, HDR10+, 2000 nits",
                "Size" to "6.7 inches, 110.6 cm²",
                "Resolution" to "1344 x 2992 pixels, 20:9 ratio"
            )
        )

        // Platform
        SpecSection(
            title = "Platform",
            specs = listOf(
                "OS" to "Android 15",
                "Chipset" to "Google Tensor G4 (4 nm)",
                "CPU" to "Octa-core",
                "GPU" to "Mali-G715 MC7"
            )
        )

        // Memory
        SpecSection(
            title = "Memory",
            specs = listOf(
                "Internal" to "128GB 12GB RAM, 256GB 12GB RAM, 512GB 12GB RAM",
                "Card slot" to "No"
            )
        )

        // Main Camera
        SpecSection(
            title = "Main Camera",
            specs = listOf(
                "Triple" to "50 MP, f/1.7, 25mm (wide)\n48 MP, f/2.8, 113mm (telephoto)\n48 MP, f/2.2, 123˚ (ultrawide)",
                "Features" to "Dual-LED flash, Pixel Shift, Ultra-HDR, panorama",
                "Video" to "4K@30/60fps, 1080p@24/30/60/120/240fps"
            )
        )

        // Battery
        SpecSection(
            title = "Battery",
            specs = listOf(
                "Type" to "Li-Ion 5060 mAh, non-removable",
                "Charging" to "30W wired, PD3.0, 50% in 30 min"
            )
        )
    }
}

@Composable
private fun SpecHeader(title: String) {
    Column(
        modifier = Modifier
            .background(
                MaterialTheme.colorScheme.surfaceContainerHighest,
                shape = Shapes.extraSmall
            )
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun SpecSection(
    title: String,
    specs: List<Pair<String, String>>
) {
    Column(
        modifier = Modifier
            .background(
                MaterialTheme.colorScheme.surfaceContainerHighest,
                shape = Shapes.extraSmall
            )
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.secondary
        )

        Spacer(modifier = Modifier.height(8.dp))

        specs.forEach { (label, value) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Text(
                    text = label,
                    modifier = Modifier.width(100.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = value,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
