package com.exp.carconnect.app.state

import com.exp.carconnect.app.pdf.ReportPDFGenerator
import com.exp.carconnect.base.AppState
import com.exp.carconnect.base.state.CaptureReportOperationState
import com.exp.carconnect.base.state.getActiveSession
import com.exp.carconnect.base.state.isAnActiveSessionAvailable
import io.reactivex.Observable
import io.reactivex.Scheduler
import redux.api.Store
import redux.observable.Epic

internal class CaptureReportEpic(private val ioScheduler: Scheduler,
                                 private val mainThreadScheduler: Scheduler,
                                 private val reportGenerator: ReportPDFGenerator) : Epic<AppState> {

    override fun map(actions: Observable<out Any>, store: Store<AppState>): Observable<out Any> {
        return actions
                .filter {
                    it is ReportAction.CaptureReport
                }
                .flatMap {
                    if (!store.state.isAnActiveSessionAvailable() ||
                            store.state.getActiveSession().captureReportOperationState == CaptureReportOperationState.Capturing) {
                        Observable.empty()
                    } else {
                        reportGenerator
                                .generateReportPDF((it as ReportAction.CaptureReport).data, it.view)
                                .map { ReportAction.UpdateCaptureReportOperationStateToSuccessful(it) as ReportAction }
                                .onErrorReturn { ReportAction.UpdateCaptureReportOperationStateToFailed(it) }
                                .toObservable()
                                .startWith(ReportAction.UpdateCaptureReportOperationStateToCapturing)
                                .subscribeOn(ioScheduler)
                                .observeOn(mainThreadScheduler)
                    }
                }
    }
}