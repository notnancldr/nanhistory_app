package id.my.nanclouder.nanhistory.receiver

import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.DetectedActivity
import id.my.nanclouder.nanhistory.service.RecordService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ActivityRecognitionEngineTest {

    @Test
    fun buildTransitionRequest_containsRequiredActivitiesAndTransitions() {
        val transitions = ActivityRecognitionEngine.buildTransitionList()

        assertEquals(6, transitions.size)

        val vehicleTransition = transitions.firstOrNull {
            it.activityType == DetectedActivity.IN_VEHICLE &&
                it.transitionType == ActivityTransition.ACTIVITY_TRANSITION_ENTER
        }
        val bikeTransition = transitions.firstOrNull {
            it.activityType == DetectedActivity.ON_BICYCLE &&
                it.transitionType == ActivityTransition.ACTIVITY_TRANSITION_ENTER
        }
        val runningTransition = transitions.firstOrNull {
            it.activityType == DetectedActivity.RUNNING &&
                it.transitionType == ActivityTransition.ACTIVITY_TRANSITION_ENTER
        }
        val walkingTransition = transitions.firstOrNull {
            it.activityType == DetectedActivity.WALKING &&
                it.transitionType == ActivityTransition.ACTIVITY_TRANSITION_ENTER
        }
        val stillEnterTransition = transitions.firstOrNull {
            it.activityType == DetectedActivity.STILL &&
                it.transitionType == ActivityTransition.ACTIVITY_TRANSITION_ENTER
        }
        val stillExitTransition = transitions.firstOrNull {
            it.activityType == DetectedActivity.STILL &&
                it.transitionType == ActivityTransition.ACTIVITY_TRANSITION_EXIT
        }

        assertTrue("IN_VEHICLE enter transition missing", vehicleTransition != null)
        assertTrue("ON_BICYCLE enter transition missing", bikeTransition != null)
        assertTrue("RUNNING enter transition missing", runningTransition != null)
        assertTrue("WALKING enter transition missing", walkingTransition != null)
        assertTrue("STILL enter transition missing", stillEnterTransition != null)
        assertTrue("STILL exit transition missing", stillExitTransition != null)
    }

    @Test
    fun automationActions_matchServiceActionExpectations() {
        assertEquals("RECORD_START", RecordService.ACTION_RECORD_START)
        assertEquals("RECORD_STOP", RecordService.ACTION_RECORD_STOP)
        assertEquals("RECORD_TOGGLE", RecordService.ACTION_RECORD_TOGGLE)
        assertEquals("STILL_ENTER", RecordService.ACTION_STILL_ENTER)
        assertEquals("STILL_EXIT", RecordService.ACTION_STILL_EXIT)

        assertEquals(
            "id.my.nanclouder.nanhistory.action.START_RECORDING",
            RecordAutomationReceiver.ACTION_START_RECORDING
        )
        assertEquals(
            "id.my.nanclouder.nanhistory.action.STOP_RECORDING",
            RecordAutomationReceiver.ACTION_STOP_RECORDING
        )
        assertEquals(
            "id.my.nanclouder.nanhistory.action.TOGGLE_RECORDING",
            RecordAutomationReceiver.ACTION_TOGGLE_RECORDING
        )
    }
}
