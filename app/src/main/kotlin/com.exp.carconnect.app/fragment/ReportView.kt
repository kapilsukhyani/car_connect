package com.exp.carconnect.app.fragment

import android.app.Application
import android.arch.lifecycle.*
import android.content.Context
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.support.v4.app.Fragment
import android.text.Html.fromHtml
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.GridLayout
import android.widget.TextView
import com.exp.carconnect.app.R
import com.exp.carconnect.app.state.MonitorStatusTest
import com.exp.carconnect.app.state.ReportData
import com.exp.carconnect.base.BaseAppContract
import com.exp.carconnect.base.LoadableState
import com.exp.carconnect.base.UnAvailableAvailableData
import com.exp.carconnect.base.asCustomObservable
import com.exp.carconnect.base.state.*
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.BehaviorSubject
import kotlinx.android.synthetic.main.report_view.*
import java.util.concurrent.TimeUnit

internal class ReportView : Fragment() {
    lateinit var reportVM: ReportViewModel

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        return inflater.inflate(R.layout.report_view, null)
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        reportVM = ViewModelProviders.of(this).get(ReportViewModel::class.java)
        reportVM.getReportLiveData()
                .observe(this, Observer {
                    onNewSpanShot(it!!)
                })
        report_toolbar.setNavigationOnClickListener { reportVM.onBackPressed() }
        refresh_report_button.setOnClickListener { reportVM.fetchReport() }
    }


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
        ft_st_bank1.text = fromHtml(getString(R.string.fuel_trim_short_term_bank1_value, reportViewModel.fuelTrimShortTermBank1))
        ft_st_bank2.text = fromHtml(getString(R.string.fuel_trim_short_term_bank2_value, reportViewModel.fuelTrimShortTermBank2))
        ft_lt_bank1.text = fromHtml(getString(R.string.fuel_trim_long_term_bank1_value, reportViewModel.fuelTrimLongTermBank1))
        ft_lt_bank2.text = fromHtml(getString(R.string.fuel_trim_long_term_bank2_value, reportViewModel.fuelTrimLongTermBank2))
        ethanol_percentage.text = fromHtml(getString(R.string.ethanol_fuel_percent_value, reportViewModel.ethanolFuelPercent))
        fuel_injection_timing.text = fromHtml(getString(R.string.fuel_injection_timing_value, reportViewModel.fuelInjectionTiming))
        abs_evap_pressure.text = fromHtml(getString(R.string.abs_evap_system_vapor_pressure_value, reportViewModel.absoluteEvapSystempVaporPressure))
        evap_pressure.text = fromHtml(getString(R.string.evap_system_vapor_pressure_value, reportViewModel.evapSystemVaporPressure))

        monitor_status_grid.removeAllViews()
        addNewRowMonitorStatusGrid(getString(R.string.test), getString(R.string.available), getString(R.string.complete), 0)
        var index = 1
        reportViewModel.monitorStatusTests.forEach {
            addNewRowMonitorStatusGrid(it, index)
            index++
        }


    }

    private fun addNewRowMonitorStatusGrid(it: MonitorStatusTest, rowNumber: Int) {
        addNewRowMonitorStatusGrid(it.testName.toLowerCase(), it.available.toString(), it.complete.toString(), rowNumber)
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

    private val reportLiveData: MutableLiveData<ReportData> = MutableLiveData()
    private val store = (app as BaseAppContract).store
    private val fetchReportCommandObservable = BehaviorSubject.createDefault<Boolean>(true)
    private val baseApp = (app as BaseAppContract)

    private val storeSubscription: CompositeDisposable = CompositeDisposable()


    init {
        storeSubscription.add(store
                .asCustomObservable()
                .filter {
                    it.isAvailablePIDsLoaded() &&
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
                        val monitoStatus = (liveVehicleData.monitorStatus as UnAvailableAvailableData.Available).data
                        Array<MonitorStatusTest>(monitoStatus.size, { index ->
                            MonitorStatusTest(monitoStatus[index].testType.name, monitoStatus[index].available, monitoStatus[index].complete)
                        })
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
                    //todo send an action to modify view state and add a reducer instead of directly setting to livedata


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
                            barometricPressure = report.barometricPressure.toString(),
                            intakeManifoldPressure = report.intakeManifoldPressure.toString(),
                            timingAdvance = report.timingAdvance.toString(),
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
                            fuelTrimShortTermBank1 = report.fuelTrimShortTermBank1.toString(),
                            fuelTrimShortTermBank2 = report.fuelTrimShortTermBank2.toString(),
                            fuelTrimLongTermBank1 = report.fuelTrimLongTermBank1.toString(),
                            fuelTrimLongTermBank2 = report.fuelTrimLongTermBank2.toString(),
                            ethanolFuelPercent = report.ethanolFuelPercent.toString(),
                            fuelInjectionTiming = report.fuelInjectionTiming.toString(),
                            absoluteEvapSystempVaporPressure = report.absoluteEvapSystempVaporPressure.toString(),
                            evapSystemVaporPressure = report.evapSystemVaporPressure.toString(),
                            monitorStatusTests = tests)

                }
                .distinctUntilChanged()
                .startWith(ReportData())
                .subscribe {
                    reportLiveData.value = it
                })

        storeSubscription.add(fetchReportCommandObservable
                .debounce(1, TimeUnit.SECONDS, baseApp.computationScheduler)
                .observeOn(baseApp.mainScheduler)
                .subscribe {
                    store.dispatch(BaseAppAction.FetchReport)
                })
    }

    fun fetchReport() {
        fetchReportCommandObservable.onNext(true)
    }

    fun getReportLiveData(): LiveData<ReportData> {
        return reportLiveData
    }

    override fun onCleared() {
        storeSubscription.dispose()
        storeSubscription.clear()
    }

    fun onBackPressed() {
        store.dispatch(CommonAppAction.BackPressed)
    }
}