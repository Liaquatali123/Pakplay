package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.data.local.entity.VideoEntity
import com.example.ui.components.VideoCard
import com.example.ui.screens.SplashScreen
import com.example.ui.theme.PakPlayTheme
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

    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun splash_screen_screenshot() {
        composeTestRule.setContent {
            PakPlayTheme {
                SplashScreen(onSplashComplete = {})
            }
        }
        composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/splash_screen.png")
    }

    @Test
    fun video_card_screenshot() {
        val sampleVideo = VideoEntity(
            id = 1L,
            title = "Big Buck Bunny 4K HDR",
            path = "/storage/emulated/0/Movies/sample.mp4",
            duration = 596000L,
            size = 125000000L,
            width = 3840,
            height = 2160,
            folder = "Movies",
            mimeType = "video/mp4",
            dateAdded = System.currentTimeMillis(),
            resumePosition = 120000L
        )

        composeTestRule.setContent {
            PakPlayTheme {
                VideoCard(
                    video = sampleVideo,
                    isGrid = false,
                    isSelected = false,
                    isSelectionMode = false,
                    onClick = {},
                    onLongClick = {},
                    onFavoriteToggle = {},
                    onShare = {},
                    onRename = {},
                    onDelete = {},
                    onAddToPlaylist = {},
                    onShowInfo = {}
                )
            }
        }
        composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/video_card.png")
    }
}
