package com.exp.carconnect.app.state

import com.exp.carconnect.base.CarConnectIndividualViewState
import com.exp.carconnect.base.CarConnectView

data class ReportData(val name: String = "N/A", val model: String = "N/A", val manufacturer: String = "N/A", val year: String = "N/A",
                      val vin: String = "N/A", val fuelType: String = "N/A", val supportedPIDs: String = "N/A",
                      val speed: String = "N/A", val rpm: String = "N/A", val throttlePosition: String = "N/A", val fuelLevel: String = "N/A",
                      val fuelConsumptionRate: String = "N/A", val airIntakeTemperature: String = "N/A", val ambientTemperature: String = "N/A",
                      val milStatus: String = "N/A", val pendingDTCs: String = "N/A",
                      val engineLoad: String = "N/A", val fuelPressure: String = "N/A",
                      val fuelRailPressure: String = "N/A", val barometricPressure: String = "N/A",
                      val intakeManifoldPressure: String = "N/A", val timingAdvance: String = "N/A",
                      val massAirFlow: String = "N/A", val runtimeSinceEngineStart: String = "N/A",
                      val distanceSinceMILOn: String = "N/A", val distanceSinceDTCCleared: String = "N/A",
                      val wideBandAirFuelRatio: String = "N/A", val moduleVoltage: String = "N/A",
                      val absoluteLoad: String = "N/A", val fuelAirCommandedEquivalenceRatio: String = "N/A",
                      val oilTemperature: String = "N/A")

data class ReportScreen(override val screenState: ReportScreenState) : CarConnectView

sealed class ReportScreenState : CarConnectIndividualViewState {
    data class ShowNewSnapshot(val report: ReportData) : ReportScreenState()
    data class ShowError(val error: String) : ReportScreenState()
}