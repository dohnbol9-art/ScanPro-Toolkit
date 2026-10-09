package com.example

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.example.model.BarcodeFormatType
import com.example.model.ParsedContentType
import com.example.model.ScanRecord
import com.example.ui.components.RecentScanCard
import com.example.ui.components.ScanProBottomBar
import com.example.ui.navigation.Screen
import com.example.ui.theme.ScanProTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [34])
class GreetingScreenshotTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun recentScanCard_screenshot() {
        val sampleRecord = ScanRecord(
            id = 1L,
            content = "https://github.com/android/camera-samples",
            format = BarcodeFormatType.QR_CODE,
            contentType = ParsedContentType.URL,
            title = "github.com",
            domain = "github.com",
            metadata = "42 Bytes • 18ms",
            createdAt = System.currentTimeMillis(),
            isFavorite = true
        )

        composeTestRule.setContent {
            ScanProTheme {
                Box(modifier = Modifier.padding(16.dp)) {
                    RecentScanCard(
                        record = sampleRecord,
                        onClick = {},
                        onFavoriteToggle = {}
                    )
                }
            }
        }

        composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/recent_scan_card.png")
    }

    @Test
    fun bottomBar_screenshot() {
        composeTestRule.setContent {
            ScanProTheme {
                ScanProBottomBar(
                    currentRoute = Screen.Home.route,
                    onNavigate = {}
                )
            }
        }

        composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/bottom_bar.png")
    }
}
