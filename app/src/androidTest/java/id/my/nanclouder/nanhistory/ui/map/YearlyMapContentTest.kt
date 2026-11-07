package id.my.nanclouder.nanhistory.ui.map

import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import id.my.nanclouder.nanhistory.domain.model.EventRange
import id.my.nanclouder.nanhistory.domain.model.TransportationType
import id.my.nanclouder.nanhistory.feature.yearlymap.TimeRangeSelection
import id.my.nanclouder.nanhistory.feature.yearlymap.YearlyMapContent
import id.my.nanclouder.nanhistory.feature.yearlymap.YearlyMapUiState
import id.my.nanclouder.nanhistory.ui.theme.NanHistoryTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.ZonedDateTime

@RunWith(AndroidJUnit4::class)
class YearlyMapContentTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun transportStats_displaysTopTransportModes() {
        val stats = mapOf(
            TransportationType.Walk to 0.60f,
            TransportationType.Motorcycle to 0.30f,
            TransportationType.Car to 0.10f
        )
        val state = YearlyMapUiState(transportStats = stats)

        composeTestRule.setContent {
            NanHistoryTheme {
                YearlyMapContent(
                    uiState = state,
                    onTimeRangeSelected = {},
                    onCustomRangeClicked = {},
                    onDismissBottomSheet = {},
                    onEventClicked = {},
                    onBackClicked = {},
                    mapContent = { Box(Modifier) }
                )
            }
        }

        composeTestRule.onNodeWithText("Walk").assertIsDisplayed()
        composeTestRule.onNodeWithText("60%").assertIsDisplayed()
        composeTestRule.onNodeWithText("Motorcycle").assertIsDisplayed()
        composeTestRule.onNodeWithText("30%").assertIsDisplayed()
    }

    @Test
    fun bottomSheet_showsEventCountWhenVisible() {
        val dummyEvent = EventRange(
            id = "event-123",
            title = "Test Walk",
            description = "A test walk",
            time = ZonedDateTime.now(),
            end = ZonedDateTime.now().plusHours(1)
        )
        val state = YearlyMapUiState(
            selectedEdge = "edge_0",
            isBottomSheetVisible = true,
            selectedSegmentEvents = listOf(dummyEvent)
        )

        composeTestRule.setContent {
            NanHistoryTheme {
                YearlyMapContent(
                    uiState = state,
                    onTimeRangeSelected = {},
                    onCustomRangeClicked = {},
                    onDismissBottomSheet = {},
                    onEventClicked = {},
                    onBackClicked = {},
                    mapContent = { Box(Modifier) }
                )
            }
        }

        composeTestRule.onNodeWithText("1 events on this path").assertIsDisplayed()
        composeTestRule.onNodeWithText("Test Walk").assertIsDisplayed()
    }
}
