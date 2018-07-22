package com.exp.carconnect.app.state

import android.view.View
import com.exp.carconnect.base.AppState
import com.exp.carconnect.base.state.CaptureReportOperationState
import com.exp.carconnect.base.state.copyAndReplaceActiveSession
import com.exp.carconnect.base.state.getActiveSession
import com.exp.carconnect.base.state.isAnActiveSessionAvailable
import redux.api.Reducer

sealed class ReportAction {
    data class AddNewReportState(val state: ReportScreenState) : ReportAction()
    data class CaptureReport(val data: ReportData, val view: View) : ReportAction()
    object UpdateCaptureReportOperationStateToCapturing : ReportAction()
    data class UpdateCaptureReportOperationStateToSuccessful(val fileUrl: String) : ReportAction()
    object UpdateCaptureReportOperationStateToToNone : ReportAction()
    data class UpdateCaptureReportOperationStateToFailed(val error: Throwable) : ReportAction()
}

internal class ReportModuleReducer : Reducer<AppState> {

    override fun reduce(state: AppState, action: Any): AppState {
        return when (action) {
            ReportAction.UpdateCaptureReportOperationStateToCapturing -> {
                updateCaptureReportOperationState(state, CaptureReportOperationState.Capturing)
            }
            is ReportAction.UpdateCaptureReportOperationStateToSuccessful -> {
                updateCaptureReportOperationState(state, CaptureReportOperationState.Successful(action.fileUrl))
            }
            ReportAction.UpdateCaptureReportOperationStateToToNone -> {
                updateCaptureReportOperationState(state, CaptureReportOperationState.None)
            }

            is ReportAction.UpdateCaptureReportOperationStateToFailed -> {
                updateCaptureReportOperationState(state, CaptureReportOperationState.Error(action.error))
            }
            else -> {
                state
            }
        }
    }

    private fun updateCaptureReportOperationState(appState: AppState, state: CaptureReportOperationState): AppState {
        return if (appState.isAnActiveSessionAvailable()) {
            appState.copyAndReplaceActiveSession(appState.getActiveSession().copy(captureReportOperationState = state))
        } else {
            appState
        }
    }

}

sealed class DonationAction {
    data class AddNewDonationState(val state: DonationScreenState) : DonationAction()
}