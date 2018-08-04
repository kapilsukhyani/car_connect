package com.exp.carconnect.base

import android.app.Application
import com.crashlytics.android.answers.AnswersEvent
import com.crashlytics.android.answers.ContentViewEvent
import com.crashlytics.android.answers.CustomEvent
import com.exp.carconnect.base.state.Vehicle
import com.exp.carconnect.base.store.BaseStore
import io.reactivex.Scheduler
import redux.api.Store


interface BaseAppContract {
    fun onDataLoadingStartedFor(info: Vehicle)
    fun onReportRequested()
    fun enableReporting()
    fun logEvent(event: AnswersEvent<*>)
    fun killSession()

    val store: Store<AppState>
    val persistenceStore: BaseStore

    val mainScheduler: Scheduler
    val ioScheduler: Scheduler
    val computationScheduler: Scheduler

}

fun Application.logEvent(event: AnswersEvent<*>) {
    (this as BaseAppContract).logEvent(event)
}

fun Application.onDataLoadingStartedFor(info: Vehicle) {
    (this as BaseAppContract).onDataLoadingStartedFor(info)
}

fun Application.onReportRequested() {
    (this as BaseAppContract).onReportRequested()
}

fun Application.enableReporting() {
    (this as BaseAppContract).enableReporting()
}

fun Application.killSession() {
    (this as BaseAppContract).killSession()
}

fun Application.logContentViewEvent(viewId: String) {
    logEvent(ContentViewEvent()
            .putContentId(viewId))
}

fun Application.logSettingsClickedEvent(location: String) {
    logEvent(CustomEvent("settings_icon_clicked")
            .putCustomAttribute("location", location))
}

fun Application.logDataErrorAcknowledgedEvent(location: String) {
    logEvent(CustomEvent("data_error_acknowledged")
            .putCustomAttribute("location", location))
}

fun Application.logBackPressedEvent(location: String) {
    logEvent(CustomEvent("device_back_pressed")
            .putCustomAttribute("location", location))
}
