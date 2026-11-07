package id.my.nanclouder.nanhistory.feature.eventdetail.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import id.my.nanclouder.nanhistory.ui.MentionModal as CoreMentionModal
import id.my.nanclouder.nanhistory.ui.MentionModalHandler as CoreMentionModalHandler
import id.my.nanclouder.nanhistory.ui.MentionModalState

@Composable
fun MentionModal(
    mention: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    CoreMentionModal(
        mention = mention,
        onDismiss = onDismiss,
        modifier = modifier
    )
}

@Composable
fun MentionModalHandler(
    modalState: MentionModalState
) {
    CoreMentionModalHandler(
        modalState = modalState
    )
}
