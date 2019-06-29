package com.exp.carconnect.report.fragment

import android.app.AlertDialog
import android.app.Application
import android.app.ProgressDialog
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.content.FileProvider
import android.text.Html.fromHtml
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.GridLayout
import android.widget.TextView
import com.crashlytics.android.answers.CustomEvent
import com.crashlytics.android.answers.ShareEvent
import com.exp.carconnect.app.state.*
import com.exp.carconnect.base.*
import com.exp.carconnect.base.state.*
import com.exp.carconnect.report.R
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.BehaviorSubject
import kotlinx.android.synthetic.main.report_view.*
import redux.api.Reducer
import java.io.File
import java.util.*
import java.util.concurrent.TimeUnit


class ReportView : Fragment() {

    companion object {
        private const val SHARE_REPORT_REQUEST_ID = 1112
    }

    private lateinit var reportVM: ReportViewModel
    private var progressDialog: ProgressDialog? = null

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        activity!!.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        return inflater.inflate(R.layout.report_view, null)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        reportVM = ViewModelProviders.of(this).get(ReportViewModel::class.java)
        reportVM.getReportLiveData()
                .observe(this, Observer {
                    onNewState(it!!)

                })
        report_toolbar.setNavigationOnClickListener { reportVM.onBackPressed() }
        refresh_report_button.setOnClickListener { reportVM.fetchReport() }
        capture_report.setOnClickListener {
            reportVM.captureReport(report_container)
        }
    }

    private fun onNewState(state: ReportScreenState) {
        when (state) {
            is ReportScreenState.ShowNewSnapshot -> {
                onNewSpanShot(state.report)
            }
            is ReportScreenState.ShowError -> {
                hideReportCapturingDialogIfAny()
                showError(state.error)
            }

            is ReportScreenState.ShowReportCapturingProgressDialog -> {
                showReportCapturingProgressDialog()
            }

            is ReportScreenState.InitReportShare -> {
                hideReportCapturingDialogIfAny()
                initReportShare(state.fileUrl)
            }
        }
    }

    private fun initReportShare(fileUrl: String) {
        //sample url
        //storage/emulated/0/Android/data/com.exp.carconnect/files/WP0AA2A79BL017244_SatJun2908:55:56PDT2019.pdf
        val uri = FileProvider.getUriForFile(activity!!,
                "com.exp.carconnect.report.file_provider",
                File(fileUrl))
        val intentShareFile = Intent(Intent.ACTION_SEND)
        intentShareFile.type = "application/pdf"
        intentShareFile.putExtra(Intent.EXTRA_STREAM, uri)
        intentShareFile.putExtra(Intent.EXTRA_SUBJECT,
                "OBD Report captured by CarConnect")
        intentShareFile.putExtra(Intent.EXTRA_TEXT, "OBD Report captured by CarConnect")
        startActivityForResult(Intent.createChooser(intentShareFile, "Share File"), SHARE_REPORT_REQUEST_ID)
    }

    private fun hideReportCapturingDialogIfAny() {
        progressDialog?.dismiss()
        progressDialog = null
    }

    private fun showReportCapturingProgressDialog() {
        progressDialog = ProgressDialog.show(activity, getString(R.string.capturing_report), getString(com.exp.carconnect.base.R.string.please_wait))
    }


    private fun showError(error: String) {
        AlertDialog
                .Builder(activity)
                .setTitle(getString(com.exp.carconnect.base.R.string.data_loading_error))
                .setMessage(error)
                .setCancelable(false)
                .setPositiveButton(android.R.string.ok) { dialog, which ->
                    dialog.dismiss()
                    reportVM.onDataErrorAcknowledged()
                }
                .create()
                .show()
    }

    @Suppress("DEPRECATION") //TODO visit this again
    private fun onNewSpanShot(reportViewModel: ReportData) {
        vehicle_info.text = getString(R.string.vehicle_info_value, reportViewModel.name, reportViewModel.model + " " + reportViewModel.year, reportViewModel.vin, reportViewModel.obdStandard)
        mil.text = fromHtml(getString(R.string.mil_value, reportViewModel.milStatus, reportViewModel.pendingDTCs))
        speed.text = fromHtml(getString(R.string.speed_value, reportViewModel.speed))
        rpm.text = fromHtml(getString(R.string.rpm_value, reportViewModel.rpm))
        throttle_position.text = fromHtml(getString(R.string.throttle_value, reportViewModel.throttlePosition))
        fuel.text = fromHtml(getString(R.string.fuel_value, reportViewModel.fuelLevel, reportViewModel.fuelConsumptionRate))
        temperature.text = fromHtml(getString(R.string.temperature_value, reportViewModel.airIntakeTemperature, reportViewModel.ambientTemperature))
        engine_load.text = fromHtml(getString(R.string.engine_load_value, reportViewModel.engineLoad))
        fuel_pressure.text = fromHtml(getString(R.string.fuel_pressure_value, reportViewModel.fuelPressure))
        fuel_rail_pressure.text = fromHtml(getString(R.string.fuel_rail_pressure_value, reportViewModel.fuelRailPressure))
        relative_fuel_rail_pressure.text = fromHtml(getString(R.string.relative_fuel_rail_pressure_value, reportViewModel.relativeFuelRailPressure))
        absolute_fuel_rail_pressure.text = fromHtml(getString(R.string.absolute_fuel_rail_pressure_value, reportViewModel.absoluteFuelRailPressure))
        barometric_pressure.text = fromHtml(getString(R.string.barometric_pressure_value, reportViewModel.barometricPressure))
        intake_manifold_pressure.text = fromHtml(getString(R.string.intake_manifold_pressure_value, reportViewModel.intakeManifoldPressure))
        timing_advance.text = fromHtml(getString(R.string.timing_advance_value, reportViewModel.timingAdvance))
        mass_air_flow.text = fromHtml(getString(R.string.mass_air_flow_value, reportViewModel.massAirFlow))
        runtime_since_engine_start.text = fromHtml(getString(R.string.runtime_since_engine_start_value, reportViewModel.runtimeSinceEngineStart))
        runtime_with_mil_on.text = fromHtml(getString(R.string.runtime_with_mil_on_value, reportViewModel.runtimeWithMIlOn))
        runtime_since_dtc_cleared.text = fromHtml(getString(R.string.runtime_since_dtc_cleared_value, reportViewModel.runtimeSinceDTCCleared))
        distance_since_mil_on.text = fromHtml(getString(R.string.distance_since_mil_on_value, reportViewModel.distanceSinceMILOn))
        distance_since_cc_cleared.text = fromHtml(getString(R.string.distance_since_cc_cleared_value, reportViewModel.distanceSinceDTCCleared))
        wide_band_air_fuel_ratio.text = fromHtml(getString(R.string.wide_band_fuel_ratio_value, reportViewModel.wideBandAirFuelRatio))
        module_voltage.text = fromHtml(getString(R.string.module_voltage_value, reportViewModel.moduleVoltage))
        absolute_load.text = fromHtml(getString(R.string.absolute_load_value, reportViewModel.absoluteLoad))
        fuel_air_commanded_equivalence_ratio.text = fromHtml(getString(R.string.fuel_air_commanded_equivalence_ratio_value, reportViewModel.fuelAirCommandedEquivalenceRatio))
        oil_temperature.text = fromHtml(getString(R.string.oil_temperature_value, reportViewModel.oilTemperature))
        engine_coolant_temperature.text = fromHtml(getString(R.string.engine_load_value, reportViewModel.engineCoolantTemperature))
        relative_throttle_position.text = fromHtml(getString(R.string.relative_throttle_position_value, reportViewModel.relativeThrottlePosition))
        abs_throttle_b.text = fromHtml(getString(R.string.absolute_throttle_position_b_value, reportViewModel.absoluteThrottlePositionB))
        abs_throttle_c.text = fromHtml(getString(R.string.absolute_throttle_position_c_value, reportViewModel.absoluteThrottlePositionC))
        accel_pedal_d.text = fromHtml(getString(R.string.accel_pedal_position_d_value, reportViewModel.accelPedalPositionD))
        accel_pedal_e.text = fromHtml(getString(R.string.accel_pedal_position_e_value, reportViewModel.accelPedalPositionE))
        accel_pedal_f.text = fromHtml(getString(R.string.accel_pedal_position_f_value, reportViewModel.accelPedalPositionF))
        relative_accel_position.text = fromHtml(getString(R.string.relative_accel_pedal_position_value, reportViewModel.relativeAccelPedalPosition))
        commanded_throttle_actuator.text = fromHtml(getString(R.string.commanded_throttle_actuator_value, reportViewModel.commandedThrottleActuator))
        commanded_egr.text = fromHtml(getString(R.string.commanded_egr_value, reportViewModel.commandedEGR))
        commanded_egr_error.text = fromHtml(getString(R.string.commanded_egr_error_value, reportViewModel.commandedEGRError))
        commanded_evaporative_purge.text = fromHtml(getString(R.string.commanded_evaporative_purge_value, reportViewModel.commandedEvaporativePurge))
        ft_st_bank1.text = fromHtml(getString(R.string.fuel_trim_short_term_bank1_value, reportViewModel.fuelTrimShortTermBank1))
        ft_st_bank2.text = fromHtml(getString(R.string.fuel_trim_short_term_bank2_value, reportViewModel.fuelTrimShortTermBank2))
        ft_lt_bank1.text = fromHtml(getString(R.string.fuel_trim_long_term_bank1_value, reportViewModel.fuelTrimLongTermBank1))
        ft_lt_bank2.text = fromHtml(getString(R.string.fuel_trim_long_term_bank2_value, reportViewModel.fuelTrimLongTermBank2))
        ethanol_percentage.text = fromHtml(getString(R.string.ethanol_fuel_percent_value, reportViewModel.ethanolFuelPercent))
        fuel_injection_timing.text = fromHtml(getString(R.string.fuel_injection_timing_value, reportViewModel.fuelInjectionTiming))
        abs_evap_pressure.text = fromHtml(getString(R.string.abs_evap_system_vapor_pressure_value, reportViewModel.absoluteEvapSystempVaporPressure))
        evap_pressure.text = fromHtml(getString(R.string.evap_system_vapor_pressure_value, reportViewModel.evapSystemVaporPressure))
        warmups.text = fromHtml(getString(R.string.warmups_value, reportViewModel.warmupsSinceCodeCleared))

        monitor_status_grid.removeAllViews()
        addNewRowMonitorStatusGrid(getString(R.string.test), getString(R.string.available), getString(R.string.complete), 0)
        var index = 1
        reportViewModel.monitorStatusTests.forEach {
            addNewRowMonitorStatusGrid(it, index)
            index++
        }


    }

    private fun addNewRowMonitorStatusGrid(it: MonitorStatusTest, rowNumber: Int) {
        addNewRowMonitorStatusGrid(it.testName.toLowerCase(Locale.US),
                it.available.toString(),
                it.complete.toString(),
                rowNumber)
    }


    private fun addNewRowMonitorStatusGrid(col1: String, col2: String, col3: String, rowNumber: Int) {
        val testName = layoutInflater.inflate(R.layout.monitor_status_grid_col, null) as TextView
        val available = layoutInflater.inflate(R.layout.monitor_status_grid_col, null) as TextView
        val complete = layoutInflater.inflate(R.layout.monitor_status_grid_col, null) as TextView

        testName.layoutParams = GridLayout.LayoutParams(GridLayout.spec(rowNumber), GridLayout.spec(0, .6f))
        available.layoutParams = GridLayout.LayoutParams(GridLayout.spec(rowNumber), GridLayout.spec(1, .2f))
        complete.layoutParams = GridLayout.LayoutParams(GridLayout.spec(rowNumber), GridLayout.spec(2, .2f))

        testName.text = col1
        available.text = col2
        complete.text = col3

        monitor_status_grid.addView(testName)
        monitor_status_grid.addView(available)
        monitor_status_grid.addView(complete)
    }
}


internal class ReportViewModel(app: Application) : AndroidViewModel(app) {

    private val reportLiveData: MutableLiveData<ReportScreenState> = MutableLiveData()
    private val store = (app as BaseAppContract).store
    private val fetchReportCommandObservable = BehaviorSubject.createDefault<Boolean>(true)
    private val baseApp = (app as BaseAppContract)

    private val storeSubscription: CompositeDisposable = CompositeDisposable()


    init {
        app.logContentViewEvent("ReportView")
        storeSubscription.add(store
                .asCustomObservable()
                .filter {
                    it.isAvailablePIDsLoaded()
                }
                .map {
                    val session = it.getActiveSession()
                    if (session.report is LoadableState.LoadingError) {
                        throw (session.report as LoadableState.LoadingError<Throwable>).error
                    }
                    if (session.liveVehicleData is LoadableState.LoadingError) {
                        throw (session.liveVehicleData as LoadableState.LoadingError<Throwable>).error
                    }

                    it
                }
                //todo not sure why debounce isn't working
                //debouncing events, as it is not needed to refresh report ui as fast as fast data changing refresh frequency
//                .debounce(500, TimeUnit.MILLISECONDS, baseApp.computationScheduler)
//                .observeOn(baseApp.mainScheduler)
                .filter {
                    it.isLiveVehicleDataLoaded() &&
                            it.isReportLoaded()
                }
                .distinctUntilChanged { first, second ->
                    first.getCurrentVehicleData() != second.getCurrentVehicleData() ||
                            first.getReport() != second.getReport()
                }
                .map {
                    val activeSession = it.getActiveSession()
                    val report = (activeSession.report as LoadableState.Loaded).savedState
                    val vehicle = (activeSession.vehicle as LoadableState.Loaded).savedState
                    val liveVehicleData = (activeSession.liveVehicleData as LoadableState.Loaded).savedState
                    var milStatus = "N/A"
                    var pendingDTCs = "N/A"
                    var name = "N/A"
                    var manufacturer = "N/A"
                    var model = "N/A"
                    var year = "N/A"
                    val tests = if (liveVehicleData.monitorStatus is UnAvailableAvailableData.Available) {
                        val monitorStatus = (liveVehicleData.monitorStatus as UnAvailableAvailableData.Available).data
                        Array(monitorStatus.size) { index ->
                            MonitorStatusTest(monitorStatus[index].testType.name, monitorStatus[index].available, monitorStatus[index].complete)
                        }
                    } else {
                        emptyArray()
                    }

                    if (vehicle.attributes is UnAvailableAvailableData.Available) {
                        name = (vehicle.attributes as UnAvailableAvailableData.Available).data.make
                        model = (vehicle.attributes as UnAvailableAvailableData.Available).data.model
                        year = (vehicle.attributes as UnAvailableAvailableData.Available).data.modelYear
                        manufacturer = (vehicle.attributes as UnAvailableAvailableData.Available).data.manufacturer
                    }

                    if ((liveVehicleData.milStatus is UnAvailableAvailableData.Available)) {

                        if ((liveVehicleData.milStatus as UnAvailableAvailableData.Available).data is MILStatus.On) {
                            milStatus = "ON"
                            pendingDTCs = ((liveVehicleData.milStatus as UnAvailableAvailableData.Available).data as MILStatus.On).dtcs.toString()
                        } else {
                            milStatus = "OFF"
                        }
                    }


                    ReportData(name = name, model = model, year = year, manufacturer = manufacturer,
                            vin = vehicle.vin, fuelType = vehicle.fuelType.toString(), obdStandard = vehicle.obdStandard.toString(),
                            supportedPIDs = vehicle.supportedPIDs.toString(),
                            speed = "${liveVehicleData.speed} km/hr",
                            rpm = liveVehicleData.rpm.toString(),
                            throttlePosition = liveVehicleData.throttlePosition.toString(),
                            fuelLevel = liveVehicleData.fuel.toString(),
                            fuelConsumptionRate = liveVehicleData.fuelConsumptionRate.toString(),
                            airIntakeTemperature = liveVehicleData.currentAirIntakeTemp.toString(),
                            ambientTemperature = liveVehicleData.currentAmbientTemp.toString(),
                            milStatus = milStatus,
                            pendingDTCs = pendingDTCs,
                            engineLoad = report.engineLoad.toString(),
                            fuelPressure = report.fuelPressure.toString(),
                            fuelRailPressure = report.fuelRailPressure.toString(),
                            relativeFuelRailPressure = report.relativeFuelRailPressure.toString(),
                            absoluteFuelRailPressure = report.absoluteFuelRailPressure.toString(),
                            barometricPressure = report.barometricPressure.toString(),
                            intakeManifoldPressure = report.intakeManifoldPressure.toString(),
                            timingAdvance = report.timingAdvance.toString(),
                            //todo mass air flow response does not match with what is being set in ECUISM, check again
                            massAirFlow = report.massAirFlow.toString(),
                            runtimeSinceEngineStart = report.runTimeSinceEngineStart.toString(),
                            runtimeSinceDTCCleared = report.runTimeSinceDTCCleared.toString(),
                            runtimeWithMIlOn = report.runTimeWithMilOn.toString(),
                            distanceSinceMILOn = "${report.distanceTravelledSinceMILOn} km",
                            distanceSinceDTCCleared = "${report.distanceSinceDTCCleared} km",
                            wideBandAirFuelRatio = report.wideBandAirFuelRatio.toString(),
                            moduleVoltage = report.moduleVoltage.toString(),
                            absoluteLoad = report.absoluteLoad.toString(),
                            fuelAirCommandedEquivalenceRatio = report.fuelAirCommandedEquivalenceRatio.toString(),
                            oilTemperature = report.oilTemperature.toString(),
                            engineCoolantTemperature = report.engineCoolantTemperature.toString(),
                            relativeThrottlePosition = report.relativeThrottlePosition.toString(),
                            absoluteThrottlePositionB = report.absoluteThrottlePositionB.toString(),
                            absoluteThrottlePositionC = report.absoluteThrottlePositionC.toString(),
                            accelPedalPositionD = report.accelPedalPositionD.toString(),
                            accelPedalPositionE = report.accelPedalPositionE.toString(),
                            accelPedalPositionF = report.accelPedalPositionF.toString(),
                            relativeAccelPedalPosition = report.relativeAccelPedalPosition.toString(),
                            commandedThrottleActuator = report.commandedThrottleActuator.toString(),
                            commandedEGR = report.commandedEGR.toString(),
                            commandedEGRError = report.egrError.toString(),
                            commandedEvaporativePurge = report.commandedEvaporativePurge.toString(),
                            fuelTrimShortTermBank1 = report.fuelTrimShortTermBank1.toString(),
                            fuelTrimShortTermBank2 = report.fuelTrimShortTermBank2.toString(),
                            fuelTrimLongTermBank1 = report.fuelTrimLongTermBank1.toString(),
                            fuelTrimLongTermBank2 = report.fuelTrimLongTermBank2.toString(),
                            ethanolFuelPercent = report.ethanolFuelPercent.toString(),
                            fuelInjectionTiming = report.fuelInjectionTiming.toString(),
                            absoluteEvapSystempVaporPressure = report.absoluteEvapSystempVaporPressure.toString(),
                            evapSystemVaporPressure = report.evapSystemVaporPressure.toString(),
                            warmupsSinceCodeCleared = report.warmupsSinceCodeCleared.toString(),
                            monitorStatusTests = tests)

                }
                .distinctUntilChanged()
                .startWith(ReportData())
                .subscribe({
                    store.dispatch(ReportAction.AddNewReportState(ReportScreenState.ShowNewSnapshot(it)))
                }, {
                    store.dispatch(ReportAction.AddNewReportState(ReportScreenState.ShowError(getApplication<Application>()
                            .getString(com.exp.carconnect.base.R.string.data_load_error,
                                    "Unknown"))))
                }))


        storeSubscription.add(store
                .asCustomObservable()
                .filter { it.uiState.currentView is ReportScreen }
                .map { (it.uiState.currentView as ReportScreen).screenState }
                .distinctUntilChanged()
                .subscribe {
                    reportLiveData.value = it
                    if (it is ReportScreenState.ShowError) {
                        app.logContentViewEvent("ReportErrorDialog")
                    } else if (it is ReportScreenState.InitReportShare) {
                        app.logEvent(ShareEvent().putContentId("share_report"))
                    }
                })

        storeSubscription.add(fetchReportCommandObservable
                .debounce(1, TimeUnit.SECONDS, baseApp.computationScheduler)
                .observeOn(baseApp.mainScheduler)
                .subscribe {
                    store.dispatch(BaseAppAction.FetchReport)
                })

        storeSubscription.add(store
                .asCustomObservable()
                .filter { it.isAnActiveSessionAvailable() }
                .map { it.getActiveSession().captureReportOperationState }
                .distinctUntilChanged()
                .subscribe {
                    when (it) {
                        CaptureReportOperationState.Capturing -> {
                            store.dispatch(ReportAction.AddNewReportState(ReportScreenState.ShowReportCapturingProgressDialog))
                        }
                        is CaptureReportOperationState.Error -> {
                            store.dispatch(ReportAction.AddNewReportState(ReportScreenState.ShowError(getApplication<Application>()
                                    .getString(R.string.cature_report_error))))
                        }
                        is CaptureReportOperationState.Successful -> {
                            store.dispatch(ReportAction.AddNewReportState(ReportScreenState.InitReportShare(it.fileUrl)))
                        }
                    }
                })
    }

    fun fetchReport() {
        getApplication<Application>().logEvent(CustomEvent("refresh_report_clicked"))
        fetchReportCommandObservable.onNext(true)
    }

    fun captureReport(view: View) {
        val value = reportLiveData.value
        if (value is ReportScreenState.ShowNewSnapshot) {
            store.dispatch(ReportAction.CaptureReport(value.report, view))
        }

    }

    fun getReportLiveData(): LiveData<ReportScreenState> {
        return reportLiveData
    }

    override fun onCleared() {
        storeSubscription.dispose()
        storeSubscription.clear()
    }

    fun onBackPressed() {
        store.dispatch(CommonAppAction.BackPressed)
    }

    fun onDataErrorAcknowledged() {
        getApplication<Application>().logDataErrorAcknowledgedEvent("ReportView")
        store.dispatch(CommonAppAction.FinishCurrentView)
    }
}

class ReportScreenStateReducer : Reducer<AppState> {

    override fun reduce(state: AppState, action: Any): AppState {
        return when (action) {
            is ReportAction.AddNewReportState -> {
                updateState(state, action.state)
            }
            else -> {
                state
            }
        }
    }


    private fun updateState(state: AppState, reportScreenState: ReportScreenState): AppState {
        return state.copy(uiState = state
                .uiState
                .copy(backStack = state
                        .uiState
                        .backStack
                        .subList(0, state.uiState.backStack.size - 1) +
                        ReportScreen(reportScreenState)))
    }

}