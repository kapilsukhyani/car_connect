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
                       val backgroundConnectionEnabled: Boolean = DEFAULT_BACKGROND_OPERATION_ENABLED,
                       val autoConnectToLastConnectedDongleOnLaunch: Boolean = DEFAULT_AUTO_CONNECTED_ENABLED) {
    companion object {
        const val DEFAULT_BACKGROND_OPERATION_ENABLED = false
        const val DEFAULT_AUTO_CONNECTED_ENABLED = true
    }
}


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

data class SettingsScreen(override val screenState: SettingsScreenState) : CarConnectView


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


sealed class SettingsScreenState : CarConnectIndividualViewState {
    object ShowingSettings : SettingsScreenState()
    object UpdatingSettigns : SettingsScreenState()
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



