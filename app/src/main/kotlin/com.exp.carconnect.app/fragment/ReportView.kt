package com.exp.carconnect.app.fragment

import android.app.Application
import android.arch.lifecycle.*
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.exp.carconnect.app.R
import com.exp.carconnect.app.state.ReportData
import com.exp.carconnect.base.BaseAppContract
import com.exp.carconnect.base.LoadableState
import com.exp.carconnect.base.UnAvailableAvailableData
import com.exp.carconnect.base.asCustomObservable
import com.exp.carconnect.base.state.*
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.BehaviorSubject
import kotlinx.android.synthetic.main.report_view.*
import kotlinx.android.synthetic.main.report_view.view.*
import java.util.concurrent.TimeUnit

internal class ReportView : Fragment() {
    lateinit var reportVM: ReportViewModel
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
        refresh_report_button.setOnClickListener { reportVM.fetchReport() }
    }

    private fun onNewSpanShot(reportViewModel: ReportData) {
        vehicle_info.text = getString(R.string.vehicle_info_value, reportViewModel.name, reportViewModel.model + " " + reportViewModel.year, reportViewModel.vin)
        vehicle_info.mil.text = getString(R.string.mil_value, reportViewModel.milStatus, reportViewModel.pendingDTCs)
        vehicle_info.speed.text = getString(R.string.speed_value, reportViewModel.speed)
        vehicle_info.rpm.text = getString(R.string.rpm_value, reportViewModel.rpm)
        vehicle_info.throttle_position.text = getString(R.string.throttle_value, reportViewModel.throttlePosition)
        vehicle_info.fuel.text = getString(R.string.fuel_value, reportViewModel.fuelLevel, reportViewModel.fuelConsumptionRate)
        vehicle_info.temperature.text = getString(R.string.temperature_value, reportViewModel.airIntakeTemperature, reportViewModel.ambientTemperature)
        vehicle_info.engine_load.text = getString(R.string.engine_load_value, reportViewModel.engineLoad)
        vehicle_info.fuel_pressure.text = getString(R.string.fuel_pressure_value, reportViewModel.fuelPressure)
        vehicle_info.fuel_rail_pressure.text = getString(R.string.fuel_rail_pressure_value, reportViewModel.fuelRailPressure)
        vehicle_info.barometric_pressure.text = getString(R.string.barometric_pressure_value, reportViewModel.barometricPressure)
        vehicle_info.intake_manifold_pressure.text = getString(R.string.intake_manifold_pressure_value, reportViewModel.intakeManifoldPressure)
        vehicle_info.timing_advance.text = getString(R.string.timing_advance_value, reportViewModel.timingAdvance)
        vehicle_info.mass_air_flow.text = getString(R.string.mass_air_flow_value, reportViewModel.massAirFlow)
        vehicle_info.runtime_since_engine_start.text = getString(R.string.runtime_since_engine_start_value, reportViewModel.runtimeSinceEngineStart)
        vehicle_info.distance_since_mil_on.text = getString(R.string.distance_since_mil_on_value, reportViewModel.distanceSinceMILOn)
        vehicle_info.distance_since_cc_cleared.text = getString(R.string.distance_since_cc_cleared_value, reportViewModel.distanceSinceDTCCleared)
        vehicle_info.wide_band_air_fuel_ratio.text = getString(R.string.wide_band_fuel_ratio_value, reportViewModel.wideBandAirFuelRatio)
        vehicle_info.module_voltage.text = getString(R.string.module_voltage_value, reportViewModel.moduleVoltage)
        vehicle_info.absolute_load.text = getString(R.string.absolute_load_value, reportViewModel.absoluteLoad)
        vehicle_info.fuel_air_commanded_equivalence_ratio.text = getString(R.string.fuel_air_commanded_equivalence_ratio_value, reportViewModel.fuelAirCommandedEquivalenceRatio)
        vehicle_info.oil_temperature.text = getString(R.string.oil_temperature_value, reportViewModel.oilTemperature)
    }
}


internal class ReportViewModel(app: Application) : AndroidViewModel(app) {

    private val reportLiveData: MutableLiveData<ReportData> = MutableLiveData()
    private val store = (app as BaseAppContract).store
    private val fetchReportCommandObservable = BehaviorSubject.create<Boolean>()
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
                .map {
                    val activeSession = it.getActiveSession()
                    val report = (activeSession.report as LoadableState.Loaded).savedState
                    val vehicle = (activeSession.vehicle as LoadableState.Loaded).savedState
                    val liveVehicleData = (activeSession.liveVehicleData as LoadableState.Loaded).savedState
                    var milStatus = "N/A"
                    var pendingDTCs = "N/A"
                    if ((liveVehicleData.milStatus is UnAvailableAvailableData.Available)) {

                        if ((liveVehicleData.milStatus as UnAvailableAvailableData.Available).data is MILStatus.On) {
                            milStatus = "ON"
                            pendingDTCs = ((liveVehicleData.milStatus as UnAvailableAvailableData.Available).data as MILStatus.On).dtcs.toString()
                        } else {
                            milStatus = "OFF"
                        }
                    }
                    //todo send an action to modify view state and add a reducer instead of directly setting to livedata


                    ReportData(vin = vehicle.vin, fuelType = vehicle.fuelType.toString(),
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
                            distanceSinceMILOn = "${report.distanceTravelledSinceMILOn} km",
                            distanceSinceDTCCleared = "${report.distanceSinceDTCCleared} km",
                            wideBandAirFuelRatio = report.wideBandAirFuelRatio.toString(),
                            moduleVoltage = report.moduleVoltage.toString(),
                            absoluteLoad = report.absoluteLoad.toString(),
                            fuelAirCommandedEquivalenceRatio = report.fuelAirCommandedEquivalenceRatio.toString(),
                            oilTemperature = report.oilTemperature.toString())

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
//                    store.dispatch(BaseAppAction.FetchReport)
                })
        store.dispatch(BaseAppAction.FetchReport)
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
}