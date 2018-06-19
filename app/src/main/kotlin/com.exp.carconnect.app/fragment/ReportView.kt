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
        report_toolbar.setNavigationOnClickListener { reportVM.onBackPressed() }
        refresh_report_button.setOnClickListener { reportVM.fetchReport() }
    }

    private fun onNewSpanShot(reportViewModel: ReportData) {
        vehicle_info.text = getString(R.string.vehicle_info_value, reportViewModel.name, reportViewModel.model + " " + reportViewModel.year, reportViewModel.vin)
        mil.text = getString(R.string.mil_value, reportViewModel.milStatus, reportViewModel.pendingDTCs)
        speed.text = getString(R.string.speed_value, reportViewModel.speed)
        rpm.text = getString(R.string.rpm_value, reportViewModel.rpm)
        throttle_position.text = getString(R.string.throttle_value, reportViewModel.throttlePosition)
        fuel.text = getString(R.string.fuel_value, reportViewModel.fuelLevel, reportViewModel.fuelConsumptionRate)
        temperature.text = getString(R.string.temperature_value, reportViewModel.airIntakeTemperature, reportViewModel.ambientTemperature)
        engine_load.text = getString(R.string.engine_load_value, reportViewModel.engineLoad)
        fuel_pressure.text = getString(R.string.fuel_pressure_value, reportViewModel.fuelPressure)
        fuel_rail_pressure.text = getString(R.string.fuel_rail_pressure_value, reportViewModel.fuelRailPressure)
        barometric_pressure.text = getString(R.string.barometric_pressure_value, reportViewModel.barometricPressure)
        intake_manifold_pressure.text = getString(R.string.intake_manifold_pressure_value, reportViewModel.intakeManifoldPressure)
        timing_advance.text = getString(R.string.timing_advance_value, reportViewModel.timingAdvance)
        mass_air_flow.text = getString(R.string.mass_air_flow_value, reportViewModel.massAirFlow)
        runtime_since_engine_start.text = getString(R.string.runtime_since_engine_start_value, reportViewModel.runtimeSinceEngineStart)
        distance_since_mil_on.text = getString(R.string.distance_since_mil_on_value, reportViewModel.distanceSinceMILOn)
        distance_since_cc_cleared.text = getString(R.string.distance_since_cc_cleared_value, reportViewModel.distanceSinceDTCCleared)
        wide_band_air_fuel_ratio.text = getString(R.string.wide_band_fuel_ratio_value, reportViewModel.wideBandAirFuelRatio)
        module_voltage.text = getString(R.string.module_voltage_value, reportViewModel.moduleVoltage)
        absolute_load.text = getString(R.string.absolute_load_value, reportViewModel.absoluteLoad)
        fuel_air_commanded_equivalence_ratio.text = getString(R.string.fuel_air_commanded_equivalence_ratio_value, reportViewModel.fuelAirCommandedEquivalenceRatio)
        oil_temperature.text = getString(R.string.oil_temperature_value, reportViewModel.oilTemperature)
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
                            vin = vehicle.vin, fuelType = vehicle.fuelType.toString(),
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

    fun onBackPressed() {
        store.dispatch(CommonAppAction.BackPressed)
    }
}