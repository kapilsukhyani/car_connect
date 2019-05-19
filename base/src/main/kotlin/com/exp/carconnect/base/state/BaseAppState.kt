package com.exp.carconnect.base.state

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import com.exp.carconnect.base.*
import com.exp.carconnect.base.store.PersistedAppState
import com.exp.carconnect.obdlib.OBDEngine
import com.exp.carconnect.obdlib.obdmessage.FuelType
import com.exp.carconnect.obdlib.obdmessage.MonitorTest
import com.exp.carconnect.obdlib.obdmessage.OBDStandard
import java.util.concurrent.TimeUnit


fun AppState.getBaseAppState(): BaseAppState {
    return (moduleStateMap[BaseAppState.STATE_KEY] as LoadableState.Loaded<BaseAppState>).savedState
}

fun AppState.isBaseStateLoaded(): Boolean {
    return (moduleStateMap[BaseAppState.STATE_KEY] is LoadableState.Loaded)
}

fun AppState.copyAndReplaceBaseAppState(state: LoadableState<BaseAppState, BaseAppStateLoadingError>): AppState {
    return this.copy(moduleStateMap = moduleStateMap + Pair(BaseAppState.STATE_KEY, state))
}

fun AppState.isAnActiveSessionAvailable(): Boolean {
    return this.isBaseStateLoaded() &&
            this.getBaseAppState().activeSession is UnAvailableAvailableData.Available
}

fun AppState.isLiveVehicleDataLoaded(): Boolean {
    return this.isAnActiveSessionAvailable() &&
            (this.getBaseAppState().activeSession as UnAvailableAvailableData.Available<ActiveSession>).data.liveVehicleData is LoadableState.Loaded
}

fun AppState.isReportLoaded(): Boolean {
    return this.isAnActiveSessionAvailable() &&
            (this.getBaseAppState().activeSession as UnAvailableAvailableData.Available<ActiveSession>).data.report is LoadableState.Loaded
}

fun AppState.isVehicleInfoLoaded(): Boolean {
    return this.isAnActiveSessionAvailable() &&
            (this.getBaseAppState().activeSession as UnAvailableAvailableData.Available<ActiveSession>).data.vehicle is LoadableState.Loaded
}

fun AppState.isAvailablePIDsLoaded(): Boolean {
    return isVehicleInfoLoaded() && getCurrentVehicleInfo().supportedPIDs is UnAvailableAvailableData.Available
}

fun AppState.getCurrentVehicleData(): LiveVehicleData {
    return ((this.getBaseAppState().activeSession as UnAvailableAvailableData.Available<ActiveSession>).data.liveVehicleData as LoadableState.Loaded).savedState
}

fun AppState.getReport(): Report {
    return ((this.getBaseAppState().activeSession as UnAvailableAvailableData.Available<ActiveSession>).data.report as LoadableState.Loaded).savedState
}

fun AppState.getCurrentVehicleInfo(): Vehicle {
    return ((this.getBaseAppState().activeSession as UnAvailableAvailableData.Available<ActiveSession>).data.vehicle as LoadableState.Loaded).savedState
}

fun AppState.getAvailablePIDs(): Set<String> {
    return (getCurrentVehicleInfo().supportedPIDs as UnAvailableAvailableData.Available).data
}

fun AppState.isActiveVehicleSpeedLoaded(): Boolean {
    return this.isLiveVehicleDataLoaded() && ((this.getBaseAppState().activeSession as UnAvailableAvailableData.Available<ActiveSession>)
            .data.liveVehicleData as LoadableState.Loaded).savedState.speed is UnAvailableAvailableData.Available
}

fun AppState.isActiveVehcilesMILOn(): Boolean {
    return this.isLiveVehicleDataLoaded() && this.isActiveVehicleMilStatusLoaded() && this.getMilStatus() is MILStatus.On
}

fun AppState.isActiveVehicleMilStatusLoaded(): Boolean {
    return this.isLiveVehicleDataLoaded() && ((this.getBaseAppState().activeSession as UnAvailableAvailableData.Available<ActiveSession>)
            .data.liveVehicleData as LoadableState.Loaded).savedState.milStatus is UnAvailableAvailableData.Available
}


fun AppState.isActiveVehicleFuelLoaded(): Boolean {
    return this.isLiveVehicleDataLoaded() && ((this.getBaseAppState().activeSession as UnAvailableAvailableData.Available<ActiveSession>)
            .data.liveVehicleData as LoadableState.Loaded).savedState.fuel is UnAvailableAvailableData.Available
}

fun AppState.isSpeedNotificationOn(): Boolean {
    return this.getBaseAppState().baseAppPersistedState.appSettings.notificationSettings.speedNotificationSettings is SpeedNotificationSettings.On
}

fun AppState.getAppSettings(): AppSettings {
    return this.getBaseAppState().baseAppPersistedState.appSettings
}

fun AppState.getMilStatus(): MILStatus {
    return (((this.getBaseAppState().activeSession as UnAvailableAvailableData.Available<ActiveSession>)
            .data.liveVehicleData as LoadableState.Loaded).savedState.milStatus as UnAvailableAvailableData.Available).data
}

fun AppState.isFuelNotificationOn(): Boolean {
    return this.getBaseAppState().baseAppPersistedState.appSettings.notificationSettings.fuelNotificationSettings is FuelNotificationSettings.On
}

fun AppState.copyAndReplaceActiveSession(activeSession: ActiveSession): AppState {
    return this.copyAndReplaceBaseAppState(LoadableState.Loaded(this.getBaseAppState()
            .copy(activeSession = UnAvailableAvailableData.Available(activeSession))))
}

fun AppState.clearActiveSessionState(): AppState {
    return this.copyAndReplaceBaseAppState(LoadableState.Loaded(this.getBaseAppState()
            .copy(activeSession = UnAvailableAvailableData.UnAvailable)))
}

fun AppState.getActiveVehicleSpeed(): Float {
    return (((this.getBaseAppState().activeSession as UnAvailableAvailableData.Available<ActiveSession>)
            .data.liveVehicleData as LoadableState.Loaded).savedState.speed as UnAvailableAvailableData.Available).data
}

fun AppState.getActiveVehicleFuelLevel(): Float {
    return (((this.getBaseAppState().activeSession as UnAvailableAvailableData.Available<ActiveSession>)
            .data.liveVehicleData as LoadableState.Loaded).savedState.fuel as UnAvailableAvailableData.Available).data
}

fun AppState.getMaxSpeedThresholdFromSettings(): Int {
    return (this.getBaseAppState().baseAppPersistedState
            .appSettings.notificationSettings.speedNotificationSettings as SpeedNotificationSettings.On).maxSpeedThreshold
}

fun AppState.getDashboardTheme(): DashboardTheme {
    return this.getBaseAppState().baseAppPersistedState
            .appSettings.displaySettings.dashboardTheme
}

fun AppState.getMinFuelThresholdFromSettings(): Float {
    return (this.getBaseAppState().baseAppPersistedState
            .appSettings.notificationSettings.fuelNotificationSettings as FuelNotificationSettings.On).minFuelPercentageThreshold
}

fun AppState.getActiveSession(): ActiveSession {
    return (this.getBaseAppState().activeSession as UnAvailableAvailableData.Available<ActiveSession>).data

}

fun AppState.copyAndReplaceAppSettings(appSettings: AppSettings): AppState {
    return this.copyAndReplaceBaseAppState(LoadableState.Loaded(this.getBaseAppState().copy(baseAppPersistedState =
    this.getBaseAppState().baseAppPersistedState.copy(appSettings = appSettings))))
}

fun AppState.copyAndReplaceRecentlyUsedDongle(dongle: Dongle): AppState {
    val originalPersistedAppState = this.getBaseAppState().baseAppPersistedState
    val updatedPersistedState = originalPersistedAppState.copy(lastConnectedDongle = dongle, knownDongles = originalPersistedAppState.knownDongles.plus(dongle))
    return this.copyAndReplaceBaseAppState(LoadableState.Loaded(this.getBaseAppState().copy(baseAppPersistedState = updatedPersistedState)))
}

fun AppState.copyAndReplaceRecentlyUsedDVehicle(vehicle: Vehicle): AppState {
    val originalPersistedAppState = this.getBaseAppState().baseAppPersistedState
    val updatedPersistedState = originalPersistedAppState.copy(lastConnectedVehicle = vehicle, knownVehicles = originalPersistedAppState.knownVehicles.plus(vehicle))
    return this.copyAndReplaceBaseAppState(LoadableState.Loaded(this.getBaseAppState().copy(baseAppPersistedState = updatedPersistedState)))
}

data class BaseAppState(val mode: AppMode = AppMode.FullApp,
                        val appRunningMode: AppRunningMode = AppRunningMode.Foreground,
                        val baseAppPersistedState: BaseAppPersistedState = BaseAppPersistedState(),
                        val activeSession: UnAvailableAvailableData<ActiveSession> = UnAvailableAvailableData.UnAvailable) : ModuleState {
    companion object {
        const val STATE_KEY = "BaseAppState"
    }

}

data class BaseAppPersistedState(val knownDongles: Set<Dongle> = hashSetOf(),
                                 val knownVehicles: Set<Vehicle> = hashSetOf(),
                                 val lastConnectedDongle: Dongle? = null,
                                 val lastConnectedVehicle: Vehicle? = null,
                                 val appSettings: AppSettings = AppSettings()) {
    constructor(persistedAppState: PersistedAppState) : this(persistedAppState.knownDongles,
            persistedAppState.knownVehicles, persistedAppState.lastConnectedDongle,
            persistedAppState.lastConnectedVehicle,
            persistedAppState.appSettings)
}


sealed class AppMode {
    data class InstantApp(val feature: Feature) : AppMode()
    object FullApp : AppMode()
}


enum class Feature {
    Dashboard,
    SimpleView
}

//todo add more parameters for fixed parameters
//atleast vin is required to create a vehicle instance
data class Vehicle(val vin: String,
                   val supportedPIDs: UnAvailableAvailableData<Set<String>>
                   = UnAvailableAvailableData.UnAvailable,
                   val fuelType: UnAvailableAvailableData<FuelType>
                   = UnAvailableAvailableData.UnAvailable,
                   val obdStandard: UnAvailableAvailableData<OBDStandard>
                   = UnAvailableAvailableData.UnAvailable,
                   val attributes: UnAvailableAvailableData<VehicleAttributes>
                   = UnAvailableAvailableData.UnAvailable)

data class VehicleAttributes(val make: String,
                             val model: String,
                             val manufacturer: String,
                             val modelYear: String)

data class Dongle(val address: String, val name: String)

enum class AppRunningMode {
    Background,
    Foreground
}

enum class UnitSystem {
    Matrix,
    Imperial
}

enum class DashboardTheme {
    Dark,
    Light
}

sealed class FuelNotificationSettings {
    companion object {
        const val DEFAULT_FUEL_PERCENTAGE_LEVEL = 30 // in percent
    }

    data class On(val minFuelPercentageThreshold: Float = (DEFAULT_FUEL_PERCENTAGE_LEVEL.toFloat() / 100)) : FuelNotificationSettings()
    object Off : FuelNotificationSettings()
}

sealed class SpeedNotificationSettings {
    companion object {
        const val DEFAULT_MAX_SPEED_THRESHOLD = 70
    }

    data class On(val maxSpeedThreshold: Int = DEFAULT_MAX_SPEED_THRESHOLD) : SpeedNotificationSettings()
    object Off : SpeedNotificationSettings()
}

data class NotificationSettings(val fuelNotificationSettings: FuelNotificationSettings = FuelNotificationSettings.On(),
                                val speedNotificationSettings: SpeedNotificationSettings = SpeedNotificationSettings.On())

data class DisplaySettings(val dashboardTheme: DashboardTheme = DEFAULT_THEME) {
    companion object {
        val DEFAULT_THEME = DashboardTheme.Dark
    }
}

data class DataSettings(val unitSystem: UnitSystem = DEFAULT_MATRIX_SYSTEM,
        //rpm, speed, throttle, ignition, pendingtroublecodes, dtc(mil)
                        val fastChangingDataRefreshFrequency: Frequency = Frequency(DEFAULT_FAST_CHANGING_DATA_REFRESH_FREQUENCY, TimeUnit.MILLISECONDS),
                        val temperatureRefreshFrequency: Frequency = Frequency(DEFAULT_TEMPERATURE_REFRESH_FREQUENCY, TimeUnit.MILLISECONDS),
                        val fuelLevelRefreshFrequency: Frequency = Frequency(DEFAULT_FUEL_LEVEL_REFRESH_FREQUENCY, TimeUnit.MINUTES),
                        val pressureRefreshFrequency: Frequency = Frequency(DEFAULT_PRESSURE_REFRESH_FREQUENCY, TimeUnit.MINUTES)) {
    companion object {
        val DEFAULT_MATRIX_SYSTEM = UnitSystem.Matrix
        const val DEFAULT_FAST_CHANGING_DATA_REFRESH_FREQUENCY: Long = 50
        const val DEFAULT_TEMPERATURE_REFRESH_FREQUENCY: Long = 500
        const val DEFAULT_FUEL_LEVEL_REFRESH_FREQUENCY: Long = 1
        const val DEFAULT_PRESSURE_REFRESH_FREQUENCY: Long = 1
    }
}

data class AppSettings(val dataSettings: DataSettings = DataSettings(),
                       val notificationSettings: NotificationSettings = NotificationSettings(),
                       val displaySettings: DisplaySettings = DisplaySettings(),
                       val backgroundConnectionEnabled: Boolean = DEFAULT_BACKGROUND_OPERATION_ENABLED,
                       val autoConnectToLastConnectedDongleOnLaunch: Boolean = DEFAULT_AUTO_CONNECTED_ENABLED,
                       val usageReportingEnabled: Boolean = DEFAULT_USAGE_REPORTING_ENABLED,
                       val privacyPolicyAccepted: Boolean = DEFAULT_PRIVACY_POLICY_FLAG_VALUE) {
    companion object {
        const val DEFAULT_BACKGROUND_OPERATION_ENABLED = false
        const val DEFAULT_AUTO_CONNECTED_ENABLED = true
        const val DEFAULT_USAGE_REPORTING_ENABLED = true
        const val DEFAULT_PRIVACY_POLICY_FLAG_VALUE = false
    }
}


data class ActiveSession(val dongle: Dongle,
                         val socket: BluetoothSocket,
                         val engine: OBDEngine,
                         val vehicle: LoadableState<Vehicle, Throwable> = LoadableState.NotLoaded,
                         val liveVehicleData: LoadableState<LiveVehicleData, Throwable>
                         = LoadableState.NotLoaded,
                         val clearDTCsOperationState: ClearDTCOperationState = ClearDTCOperationState.None,
        //todo think of moving report to its own module state
                         val report: LoadableState<Report, Throwable> = LoadableState.NotLoaded,
                         val captureReportOperationState: CaptureReportOperationState = CaptureReportOperationState.None)

sealed class ClearDTCOperationState {

    object None : ClearDTCOperationState()

    object Clearing : ClearDTCOperationState()

    object Successful : ClearDTCOperationState()

    data class Error(val error: ClearDTCError) : ClearDTCOperationState()
}

//todo lets see if this state can be in its own module state instead of baseappstate
sealed class CaptureReportOperationState {

    object None : CaptureReportOperationState()

    object Capturing : CaptureReportOperationState()

    data class Successful(val fileUrl: String) : CaptureReportOperationState()

    data class Error(val error: Throwable) : CaptureReportOperationState()
}

data class LiveVehicleData(val rpm: UnAvailableAvailableData<Float>
                           = UnAvailableAvailableData.UnAvailable,
                           val speed: UnAvailableAvailableData<Float>
                           = UnAvailableAvailableData.UnAvailable,
                           val throttlePosition: UnAvailableAvailableData<Float>
                           = UnAvailableAvailableData.UnAvailable,
                           val fuel: UnAvailableAvailableData<Float>
                           = UnAvailableAvailableData.UnAvailable,
                           val ignition: UnAvailableAvailableData<Boolean>
                           = UnAvailableAvailableData.UnAvailable,
                           val milStatus: UnAvailableAvailableData<MILStatus>
                           = UnAvailableAvailableData.UnAvailable,
                           val monitorStatus: UnAvailableAvailableData<Array<MonitorTest>>
                           = UnAvailableAvailableData.UnAvailable,
                           val currentAirIntakeTemp: UnAvailableAvailableData<Float>
                           = UnAvailableAvailableData.UnAvailable,
                           val currentAmbientTemp: UnAvailableAvailableData<Float>
                           = UnAvailableAvailableData.UnAvailable,
                           val fuelConsumptionRate: UnAvailableAvailableData<Float>
                           = UnAvailableAvailableData.UnAvailable)

data class Report(val engineLoad: UnAvailableAvailableData<Float>
                  = UnAvailableAvailableData.UnAvailable,
                  val fuelPressure: UnAvailableAvailableData<Int>
                  = UnAvailableAvailableData.UnAvailable,
                  val intakeManifoldPressure: UnAvailableAvailableData<Int>
                  = UnAvailableAvailableData.UnAvailable,
                  val timingAdvance: UnAvailableAvailableData<Float>
                  = UnAvailableAvailableData.UnAvailable,
                  val massAirFlow: UnAvailableAvailableData<Float>
                  = UnAvailableAvailableData.UnAvailable,
                  val runTimeSinceEngineStart: UnAvailableAvailableData<Int>
                  = UnAvailableAvailableData.UnAvailable,
                  val runTimeWithMilOn: UnAvailableAvailableData<Int>
                  = UnAvailableAvailableData.UnAvailable,
                  val runTimeSinceDTCCleared: UnAvailableAvailableData<Int>
                  = UnAvailableAvailableData.UnAvailable,
                  val distanceTravelledSinceMILOn: UnAvailableAvailableData<Int>
                  = UnAvailableAvailableData.UnAvailable,
                  val fuelRailPressure: UnAvailableAvailableData<Int>
                  = UnAvailableAvailableData.UnAvailable,
                  val relativeFuelRailPressure: UnAvailableAvailableData<Float>
                  = UnAvailableAvailableData.UnAvailable,
                  val absoluteFuelRailPressure: UnAvailableAvailableData<Float>
                  = UnAvailableAvailableData.UnAvailable,
                  val distanceSinceDTCCleared: UnAvailableAvailableData<Int>
                  = UnAvailableAvailableData.UnAvailable,
                  val barometricPressure: UnAvailableAvailableData<Int>
                  = UnAvailableAvailableData.UnAvailable,
                  val wideBandAirFuelRatio: UnAvailableAvailableData<Float>
                  = UnAvailableAvailableData.UnAvailable,
                  val moduleVoltage: UnAvailableAvailableData<Float>
                  = UnAvailableAvailableData.UnAvailable,
                  val absoluteLoad: UnAvailableAvailableData<Float>
                  = UnAvailableAvailableData.UnAvailable,
                  val fuelAirCommandedEquivalenceRatio: UnAvailableAvailableData<Float>
                  = UnAvailableAvailableData.UnAvailable,
                  val oilTemperature: UnAvailableAvailableData<Int>
                  = UnAvailableAvailableData.UnAvailable,
                  val engineCoolantTemperature: UnAvailableAvailableData<Int>
                  = UnAvailableAvailableData.UnAvailable,
                  val relativeThrottlePosition: UnAvailableAvailableData<Float>
                  = UnAvailableAvailableData.UnAvailable,
                  val absoluteThrottlePositionB: UnAvailableAvailableData<Float>
                  = UnAvailableAvailableData.UnAvailable,
                  val absoluteThrottlePositionC: UnAvailableAvailableData<Float>
                  = UnAvailableAvailableData.UnAvailable,
                  val accelPedalPositionD: UnAvailableAvailableData<Float>
                  = UnAvailableAvailableData.UnAvailable,
                  val accelPedalPositionE: UnAvailableAvailableData<Float>
                  = UnAvailableAvailableData.UnAvailable,
                  val accelPedalPositionF: UnAvailableAvailableData<Float>
                  = UnAvailableAvailableData.UnAvailable,
                  val relativeAccelPedalPosition: UnAvailableAvailableData<Float>
                  = UnAvailableAvailableData.UnAvailable,
                  val commandedThrottleActuator: UnAvailableAvailableData<Float>
                  = UnAvailableAvailableData.UnAvailable,
                  val commandedEGR: UnAvailableAvailableData<Float>
                  = UnAvailableAvailableData.UnAvailable,
                  val egrError: UnAvailableAvailableData<Float>
                  = UnAvailableAvailableData.UnAvailable,
                  val commandedEvaporativePurge: UnAvailableAvailableData<Float>
                  = UnAvailableAvailableData.UnAvailable,
                  val fuelTrimShortTermBank1: UnAvailableAvailableData<Float>
                  = UnAvailableAvailableData.UnAvailable,
                  val fuelTrimShortTermBank2: UnAvailableAvailableData<Float>
                  = UnAvailableAvailableData.UnAvailable,
                  val fuelTrimLongTermBank1: UnAvailableAvailableData<Float>
                  = UnAvailableAvailableData.UnAvailable,
                  val fuelTrimLongTermBank2: UnAvailableAvailableData<Float>
                  = UnAvailableAvailableData.UnAvailable,
                  val ethanolFuelPercent: UnAvailableAvailableData<Float>
                  = UnAvailableAvailableData.UnAvailable,
                  val fuelInjectionTiming: UnAvailableAvailableData<Float>
                  = UnAvailableAvailableData.UnAvailable,
                  val absoluteEvapSystempVaporPressure: UnAvailableAvailableData<Float>
                  = UnAvailableAvailableData.UnAvailable,
                  val evapSystemVaporPressure: UnAvailableAvailableData<Float>
                  = UnAvailableAvailableData.UnAvailable,
                  val warmupsSinceCodeCleared: UnAvailableAvailableData<Int>
                  = UnAvailableAvailableData.UnAvailable)

sealed class MILStatus {
    object Off : MILStatus()
    data class On(val dtcs: List<String>,
                  val frame: UnAvailableAvailableData<FreezeFrame>
                  = UnAvailableAvailableData.UnAvailable) : MILStatus()
}

//todo add all required parameters for freeze frame
data class FreezeFrame(val rpm: Int, val speed: Int)

//------------------------------------------------------------View State--------------------------------------------------------------------------------------


data class SplashScreen(override val screenState: SplashScreenState) : CarConnectView

data class DeviceManagementScreen(override val screenState: DeviceManagementScreenState) : CarConnectView

data class ConnectionScreen(override val screenState: ConnectionScreenState) : CarConnectView

data class SettingsScreen(override val screenState: SettingsScreenState) : CarConnectView


sealed class SplashScreenState : CarConnectIndividualViewState {
    // load successful can lead to either ConnectingToLastConnectedDevice or ShowingDevices
    object ShowPrivacyPolicy: SplashScreenState()
    object LoadingAppState : SplashScreenState()
    object ShowLoadingError : SplashScreenState()
    data class AppStateLoaded(val appState: BaseAppState): SplashScreenState()
}

sealed class ConnectionScreenState : CarConnectIndividualViewState {
    data class ShowStatus(val status: String) : ConnectionScreenState()
    data class ShowSetupError(val error: String) : ConnectionScreenState()

}


sealed class DeviceManagementScreenState : CarConnectIndividualViewState {
    //connect action will move state to connecting
    object LoadingDevices : DeviceManagementScreenState()

    data class ShowingDevices(val devices: Set<BluetoothDevice>, val showUsageReportBanner: Boolean = false) : DeviceManagementScreenState()

    object ShowingBluetoothUnAvailableError : DeviceManagementScreenState()
}


sealed class SettingsScreenState : CarConnectIndividualViewState {
    object ShowingSettings : SettingsScreenState()
    object UpdatingSettigns : SettingsScreenState()
    object HidingClearDTCButton : SettingsScreenState()
    object ShowingClearDTCButton : SettingsScreenState()
    object ShowingClearingDTCsProgress : SettingsScreenState()
    object ClearingDTCsSuccessful : SettingsScreenState()
    object ClearingDtcsFailed : SettingsScreenState()
    data class ShowingUpdateSettingsError(val error: UpdateSettingsError) : SettingsScreenState()

}


// ------------------------------------- Error Categories ----------------------------------------------------------------------------------------------------

sealed class BaseAppStateLoadingError : CarConnectError("BaseAppStateLoadingError") {
    data class UnkownError(override val error: Throwable) : BaseAppStateLoadingError()
    data class IOError(override val error: Throwable) : BaseAppStateLoadingError()
}

sealed class UpdateSettingsError : CarConnectError("UpdateSettingsError") {
    data class UnkownError(override val error: Throwable) : UpdateSettingsError()
}


sealed class OBDDataLoadError : CarConnectError("OBDDataLoadError") {
    data class UnkownError(override val error: Throwable) : OBDDataLoadError()
}

sealed class ClearDTCError : CarConnectError("ClearDTCError") {
    data class UnkownError(override val error: Throwable) : ClearDTCError()
}



