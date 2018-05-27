package com.exp.carconnect.base.state

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import com.exp.carconnect.base.*
import com.exp.carconnect.base.store.PersistedAppState
import com.exp.carconnect.obdlib.OBDEngine
import com.exp.carconnect.obdlib.obdmessage.FuelType
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


fun AppState.addActiveSession(activeSession: ActiveSession): AppState {
    return this.copyAndReplaceBaseAppState(LoadableState.Loaded(this.getBaseAppState()
            .copy(activeSession = UnAvailableAvailableData.Available(activeSession))))
}

fun AppState.killActiveSession(): AppState {
    return this.copyAndReplaceBaseAppState(LoadableState.Loaded(this.getBaseAppState()
            .copy(activeSession = UnAvailableAvailableData.UnAvailable)))
}

fun AppState.getActiveSession(): ActiveSession {
    return (this.getBaseAppState().activeSession as UnAvailableAvailableData.Available<ActiveSession>).data

}

fun AppState.copyAndReplaceAppSettings(appSettings: AppSettings): AppState {
    return this.copyAndReplaceBaseAppState(LoadableState.Loaded(this.getBaseAppState().copy(baseAppPersistedState =
    this.getBaseAppState().baseAppPersistedState.copy(appSettings = appSettings))))
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
                   = UnAvailableAvailableData.UnAvailable)

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
    data class On(val minFuelPercentageThreshold: Float = .3f) : FuelNotificationSettings()
    object Off : FuelNotificationSettings()
}

sealed class SpeedNotificationSettings {
    data class On(val maxSpeedThreshold: Int = 70) : SpeedNotificationSettings()
    object Off : SpeedNotificationSettings()
}

data class NotificationSettings(val fuelNotificationSettings: FuelNotificationSettings = FuelNotificationSettings.On(),
                                val speedNotificationSettings: SpeedNotificationSettings = SpeedNotificationSettings.On())

data class DisplaySettings(val dashboardTheme: DashboardTheme = DashboardTheme.Dark)

data class DataSettings(val unitSystem: UnitSystem = UnitSystem.Matrix,
        //rpm, speed, throttle, ignition, pendingtroublecodes, dtc(mil)
                        val fastChangingDataRefreshFrequency: Frequency = Frequency(50, TimeUnit.MILLISECONDS),
                        val temperatureRefreshFrequency: Frequency = Frequency(500, TimeUnit.MILLISECONDS),
                        val fuelLevelRefreshFrequency: Frequency = Frequency(1, TimeUnit.MINUTES),
                        val pressureRefreshFrequency: Frequency = Frequency(1, TimeUnit.MINUTES))

data class AppSettings(val dataSettings: DataSettings = DataSettings(),
                       val notificationSettings: NotificationSettings = NotificationSettings(),
                       val displaySettings: DisplaySettings = DisplaySettings(),
                       val backgroundConnectionEnabled: Boolean = true,
                       val autoConnectToLastConnectedDongleOnLaunch: Boolean = true)


data class ActiveSession(val dongle: Dongle,
                         val socket: BluetoothSocket,
                         val engine: OBDEngine,
                         val vehicle: LoadableState<Vehicle, Throwable> = LoadableState.NotLoaded,
                         val currentVehicleData: LoadableState<VehicleData, Throwable>
                         = LoadableState.NotLoaded)

data class VehicleData(val rpm: UnAvailableAvailableData<Float>
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
                       val currentAirIntakeTemp: UnAvailableAvailableData<Float>
                       = UnAvailableAvailableData.UnAvailable,
                       val currentAmbientTemp: UnAvailableAvailableData<Float>
                       = UnAvailableAvailableData.UnAvailable,
                       val fuelConsumptionRate: UnAvailableAvailableData<Float>
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
data class SettingsScreen(override val screenState: SettignsScreenState) : CarConnectView


sealed class SplashScreenState : CarConnectIndividualViewState {
    // load successful can lead to either ConnectingToLastConnectedDevice or ShowingDevices
    object LoadingAppState : SplashScreenState()

    object ShowLoadingError : SplashScreenState()
}

sealed class ConnectionScreenState : CarConnectIndividualViewState {
    data class ShowStatus(val status: String) : ConnectionScreenState()
    data class ShowSetupError(val error: String) : ConnectionScreenState()

}


sealed class DeviceManagementScreenState : CarConnectIndividualViewState {
    //connect action will move state to connecting
    object LoadingDevices : DeviceManagementScreenState()

    data class ShowingDevices(val devices: Set<BluetoothDevice>) : DeviceManagementScreenState()

    object ShowingBluetoothUnAvailableError : DeviceManagementScreenState()
}


sealed class SettignsScreenState : CarConnectIndividualViewState {
    object ShowingSettings : SettignsScreenState()
    object UpdatingSettigns : SettignsScreenState()
    data class ShowingUpdateSettingsError(val error: UpdateSettingsError) : SettignsScreenState()

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



