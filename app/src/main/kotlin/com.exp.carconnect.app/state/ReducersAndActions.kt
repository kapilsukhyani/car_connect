package com.exp.carconnect.app.state

sealed class ReportAction {
    data class AddNewReportState(val state: ReportScreenState) : ReportAction()
}