package com.exp.carconnect.app.state

import com.exp.carconnect.base.CarConnectIndividualViewState
import com.exp.carconnect.base.CarConnectView
import java.util.*

data class MonitorStatusTest(val testName: String, val available: Boolean, val complete: Boolean)

data class ReportData(val name: String = "N/A", val model: String = "N/A", val manufacturer: String = "N/A", val year: String = "N/A",
                      val vin: String = "N/A", val fuelType: String = "N/A", val obdStandard: String = "N/A", val supportedPIDs: String = "N/A",
                      val speed: String = "N/A", val rpm: String = "N/A", val throttlePosition: String = "N/A", val fuelLevel: String = "N/A",
                      val fuelConsumptionRate: String = "N/A", val airIntakeTemperature: String = "N/A", val ambientTemperature: String = "N/A",
                      val milStatus: String = "N/A", val pendingDTCs: String = "N/A",
                      val engineLoad: String = "N/A", val fuelPressure: String = "N/A",
                      val fuelRailPressure: String = "N/A", val relativeFuelRailPressure: String = "N/A",
                      val absoluteFuelRailPressure: String = "N/A", val barometricPressure: String = "N/A",
                      val intakeManifoldPressure: String = "N/A", val timingAdvance: String = "N/A",
                      val massAirFlow: String = "N/A", val runtimeSinceEngineStart: String = "N/A",
                      val runtimeSinceDTCCleared: String = "N/A", val runtimeWithMIlOn: String = "N/A",
                      val distanceSinceMILOn: String = "N/A", val distanceSinceDTCCleared: String = "N/A",
                      val wideBandAirFuelRatio: String = "N/A", val moduleVoltage: String = "N/A",
                      val absoluteLoad: String = "N/A", val fuelAirCommandedEquivalenceRatio: String = "N/A",
                      val oilTemperature: String = "N/A", val engineCoolantTemperature: String = "N/A",
                      val relativeThrottlePosition: String = "N/A", val absoluteThrottlePositionB: String = "N/A",
                      val absoluteThrottlePositionC: String = "N/A", val accelPedalPositionD: String = "N/A",
                      val accelPedalPositionE: String = "N/A", val accelPedalPositionF: String = "N/A",
                      val relativeAccelPedalPosition: String = "N/A", val commandedThrottleActuator: String = "N/A",
                      val commandedEGR: String = "N/A", val commandedEGRError: String = "N/A", val commandedEvaporativePurge: String = "N/A",
                      val fuelTrimShortTermBank1: String = "N/A", val fuelTrimShortTermBank2: String = "N/A",
                      val fuelTrimLongTermBank1: String = "N/A", val fuelTrimLongTermBank2: String = "N/A",
                      val ethanolFuelPercent: String = "N/A", val fuelInjectionTiming: String = "N/A",
                      val absoluteEvapSystempVaporPressure: String = "N/A", val evapSystemVaporPressure: String = "N/A",
                      val warmupsSinceCodeCleared: String = "N/A", val monitorStatusTests: Array<MonitorStatusTest> = emptyArray()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ReportData

        if (name != other.name) return false
        if (model != other.model) return false
        if (manufacturer != other.manufacturer) return false
        if (year != other.year) return false
        if (vin != other.vin) return false
        if (fuelType != other.fuelType) return false
        if (obdStandard != other.obdStandard) return false
        if (supportedPIDs != other.supportedPIDs) return false
        if (speed != other.speed) return false
        if (rpm != other.rpm) return false
        if (throttlePosition != other.throttlePosition) return false
        if (fuelLevel != other.fuelLevel) return false
        if (fuelConsumptionRate != other.fuelConsumptionRate) return false
        if (airIntakeTemperature != other.airIntakeTemperature) return false
        if (ambientTemperature != other.ambientTemperature) return false
        if (milStatus != other.milStatus) return false
        if (pendingDTCs != other.pendingDTCs) return false
        if (engineLoad != other.engineLoad) return false
        if (fuelPressure != other.fuelPressure) return false
        if (fuelRailPressure != other.fuelRailPressure) return false
        if (relativeFuelRailPressure != other.relativeFuelRailPressure) return false
        if (absoluteFuelRailPressure != other.absoluteFuelRailPressure) return false
        if (barometricPressure != other.barometricPressure) return false
        if (intakeManifoldPressure != other.intakeManifoldPressure) return false
        if (timingAdvance != other.timingAdvance) return false
        if (massAirFlow != other.massAirFlow) return false
        if (runtimeSinceEngineStart != other.runtimeSinceEngineStart) return false
        if (runtimeSinceDTCCleared != other.runtimeSinceDTCCleared) return false
        if (runtimeWithMIlOn != other.runtimeWithMIlOn) return false
        if (distanceSinceMILOn != other.distanceSinceMILOn) return false
        if (distanceSinceDTCCleared != other.distanceSinceDTCCleared) return false
        if (wideBandAirFuelRatio != other.wideBandAirFuelRatio) return false
        if (moduleVoltage != other.moduleVoltage) return false
        if (absoluteLoad != other.absoluteLoad) return false
        if (fuelAirCommandedEquivalenceRatio != other.fuelAirCommandedEquivalenceRatio) return false
        if (oilTemperature != other.oilTemperature) return false
        if (engineCoolantTemperature != other.engineCoolantTemperature) return false
        if (relativeThrottlePosition != other.relativeThrottlePosition) return false
        if (absoluteThrottlePositionB != other.absoluteThrottlePositionB) return false
        if (absoluteThrottlePositionC != other.absoluteThrottlePositionC) return false
        if (accelPedalPositionD != other.accelPedalPositionD) return false
        if (accelPedalPositionE != other.accelPedalPositionE) return false
        if (accelPedalPositionF != other.accelPedalPositionF) return false
        if (relativeAccelPedalPosition != other.relativeAccelPedalPosition) return false
        if (commandedThrottleActuator != other.commandedThrottleActuator) return false
        if (commandedEGR != other.commandedEGR) return false
        if (commandedEGRError != other.commandedEGRError) return false
        if (commandedEvaporativePurge != other.commandedEvaporativePurge) return false
        if (fuelTrimShortTermBank1 != other.fuelTrimShortTermBank1) return false
        if (fuelTrimShortTermBank2 != other.fuelTrimShortTermBank2) return false
        if (fuelTrimLongTermBank1 != other.fuelTrimLongTermBank1) return false
        if (fuelTrimLongTermBank2 != other.fuelTrimLongTermBank2) return false
        if (ethanolFuelPercent != other.ethanolFuelPercent) return false
        if (fuelInjectionTiming != other.fuelInjectionTiming) return false
        if (absoluteEvapSystempVaporPressure != other.absoluteEvapSystempVaporPressure) return false
        if (evapSystemVaporPressure != other.evapSystemVaporPressure) return false
        if (warmupsSinceCodeCleared != other.warmupsSinceCodeCleared) return false
        if (!Arrays.equals(monitorStatusTests, other.monitorStatusTests)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + model.hashCode()
        result = 31 * result + manufacturer.hashCode()
        result = 31 * result + year.hashCode()
        result = 31 * result + vin.hashCode()
        result = 31 * result + fuelType.hashCode()
        result = 31 * result + obdStandard.hashCode()
        result = 31 * result + supportedPIDs.hashCode()
        result = 31 * result + speed.hashCode()
        result = 31 * result + rpm.hashCode()
        result = 31 * result + throttlePosition.hashCode()
        result = 31 * result + fuelLevel.hashCode()
        result = 31 * result + fuelConsumptionRate.hashCode()
        result = 31 * result + airIntakeTemperature.hashCode()
        result = 31 * result + ambientTemperature.hashCode()
        result = 31 * result + milStatus.hashCode()
        result = 31 * result + pendingDTCs.hashCode()
        result = 31 * result + engineLoad.hashCode()
        result = 31 * result + fuelPressure.hashCode()
        result = 31 * result + fuelRailPressure.hashCode()
        result = 31 * result + relativeFuelRailPressure.hashCode()
        result = 31 * result + absoluteFuelRailPressure.hashCode()
        result = 31 * result + barometricPressure.hashCode()
        result = 31 * result + intakeManifoldPressure.hashCode()
        result = 31 * result + timingAdvance.hashCode()
        result = 31 * result + massAirFlow.hashCode()
        result = 31 * result + runtimeSinceEngineStart.hashCode()
        result = 31 * result + runtimeSinceDTCCleared.hashCode()
        result = 31 * result + runtimeWithMIlOn.hashCode()
        result = 31 * result + distanceSinceMILOn.hashCode()
        result = 31 * result + distanceSinceDTCCleared.hashCode()
        result = 31 * result + wideBandAirFuelRatio.hashCode()
        result = 31 * result + moduleVoltage.hashCode()
        result = 31 * result + absoluteLoad.hashCode()
        result = 31 * result + fuelAirCommandedEquivalenceRatio.hashCode()
        result = 31 * result + oilTemperature.hashCode()
        result = 31 * result + engineCoolantTemperature.hashCode()
        result = 31 * result + relativeThrottlePosition.hashCode()
        result = 31 * result + absoluteThrottlePositionB.hashCode()
        result = 31 * result + absoluteThrottlePositionC.hashCode()
        result = 31 * result + accelPedalPositionD.hashCode()
        result = 31 * result + accelPedalPositionE.hashCode()
        result = 31 * result + accelPedalPositionF.hashCode()
        result = 31 * result + relativeAccelPedalPosition.hashCode()
        result = 31 * result + commandedThrottleActuator.hashCode()
        result = 31 * result + commandedEGR.hashCode()
        result = 31 * result + commandedEGRError.hashCode()
        result = 31 * result + commandedEvaporativePurge.hashCode()
        result = 31 * result + fuelTrimShortTermBank1.hashCode()
        result = 31 * result + fuelTrimShortTermBank2.hashCode()
        result = 31 * result + fuelTrimLongTermBank1.hashCode()
        result = 31 * result + fuelTrimLongTermBank2.hashCode()
        result = 31 * result + ethanolFuelPercent.hashCode()
        result = 31 * result + fuelInjectionTiming.hashCode()
        result = 31 * result + absoluteEvapSystempVaporPressure.hashCode()
        result = 31 * result + evapSystemVaporPressure.hashCode()
        result = 31 * result + warmupsSinceCodeCleared.hashCode()
        result = 31 * result + Arrays.hashCode(monitorStatusTests)
        return result
    }
}

data class ReportScreen(override val screenState: ReportScreenState) : CarConnectView

sealed class ReportScreenState : CarConnectIndividualViewState {
    data class ShowNewSnapshot(val report: ReportData) : ReportScreenState()
    data class ShowError(val error: String) : ReportScreenState()
}