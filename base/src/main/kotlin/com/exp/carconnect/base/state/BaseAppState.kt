package com.exp.carconnect.base.state

import android.bluetooth.BluetoothDevice
import com.exp.carconnect.base.Frequency
import com.exp.carconnect.base.LoadableState
import com.exp.carconnect.base.UnAvailableAvailableData
import com.exp.carconnect.obdlib.obdmessage.FuelType
import java.util.*
import java.util.concurrent.TimeUnit


sealed class CarConnectHighLevelActions {
    object InitApp : CarConnectHighLevelActions()
}


data class AppState(val baseAppState: BaseAppState = BaseAppState(),
                    val extendedState: Map<String, ExtendedState> = mapOf(),
                    val backStack: CarConnectViewBackStack = CarConnectViewBackStack())


interface ExtendedState {
    val stateId: String
}


data class BaseAppState(val mode: AppMode = AppMode.FullApp,
                        val appRunningMode: AppRunningMode = AppRunningMode.Foreground,
                        val appStateLoadingState: LoadableState<BaseAppPersistedState, AppStateLoadingError> = LoadableState.NotLoaded(),
                        val activeSession: UnAvailableAvailableData<ActiveSession>
                        = UnAvailableAvailableData.UnAvailable())

data class BaseAppPersistedState(val knownDongles: Set<Dongle> = hashSetOf(),
                                 val knownVehicles: Set<Vehicle> = hashSetOf(),
                                 val lastConnectedDongle: Dongle? = null,
                                 val lastConnectedVehicle: Vehicle? = null,
                                 val appSettings: AppSettings = AppSettings())


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
                   = UnAvailableAvailableData.UnAvailable(),
                   val fuelType: UnAvailableAvailableData<FuelType>
                   = UnAvailableAvailableData.UnAvailable())

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
                         val vehicle: Vehicle,
                         val currentVehicleData: UnAvailableAvailableData<VehicleData>
                         = UnAvailableAvailableData.UnAvailable())

data class VehicleData(val rpm: UnAvailableAvailableData<Float>
                       = UnAvailableAvailableData.UnAvailable(),
                       val speed: UnAvailableAvailableData<Float>
                       = UnAvailableAvailableData.UnAvailable(),
                       val throttlePosition: UnAvailableAvailableData<Float>
                       = UnAvailableAvailableData.UnAvailable(),
                       val fuel: UnAvailableAvailableData<Float>
                       = UnAvailableAvailableData.UnAvailable(),
                       val ignition: UnAvailableAvailableData<Boolean>
                       = UnAvailableAvailableData.UnAvailable(),
                       val milStatus: UnAvailableAvailableData<MILStatus>
                       = UnAvailableAvailableData.UnAvailable(),
                       val currentAirIntakeTemp: UnAvailableAvailableData<Float>
                       = UnAvailableAvailableData.UnAvailable(),
                       val currentAmbientTemp: UnAvailableAvailableData<Float>
                       = UnAvailableAvailableData.UnAvailable(),
                       val fuelConsumptionRate: UnAvailableAvailableData<Float>
                       = UnAvailableAvailableData.UnAvailable())

sealed class MILStatus {
    object Off : MILStatus()
    data class On(val dtcs: List<String>,
                  val frame: UnAvailableAvailableData<FreezeFrame>
                  = UnAvailableAvailableData.UnAvailable()) : MILStatus()
}

//todo add all required parameters for freeze frame
data class FreezeFrame(val rpm: Int, val speed: Int)

//------------------------------------------------------------View State-----------------------------------------------------------------------------------------------------

data class CarConnectViewBackStack(val stack: Stack<CarConnectView> = defaultBackStack())

interface CarConnectIndividualViewState

interface CarConnectView {
    val screenState: CarConnectIndividualViewState
}

data class SplashScreen(override val screenState: SplashScreenState) : CarConnectView
data class DeviceManagementScreen(override val screenState: DeviceManagementScreenState) : CarConnectView
data class ConnectionScreen(override val screenState: ConnectionScreenState) : CarConnectView
data class SettingsScreen(override val screenState: SettignsScreenState) : CarConnectView


sealed class SplashScreenState : CarConnectIndividualViewState {
    // load successful can lead to either ConnectingToLastConnectedDevice or ShowingDevices
    object LoadingAppState : SplashScreenState()

    object ShowingLoadingError : SplashScreenState()
}

sealed class ConnectionScreenState : CarConnectIndividualViewState {
    //connected action will move the state to Running Setup, connection error will move the state to ConnectionError
    data class Connecting(val device: BluetoothDevice) : ConnectionScreenState()

    // successful setup leads to finishing setup
    object RunningSetup : ConnectionScreenState()

    // successful setup leads to finishing setup
    data class ShowingConnectionError(val error: ConnectionError) : ConnectionScreenState()

    data class ShwowingSetupError(val error: SetupError) : ConnectionScreenState()

    object FinishingSetup : ConnectionScreenState()

}


sealed class DeviceManagementScreenState : CarConnectIndividualViewState {
    //connect action will move state to connecting
    object ShowingDevices : DeviceManagementScreenState()
}


sealed class SettignsScreenState : CarConnectIndividualViewState {
    object ShowingSettings : SettignsScreenState()
    object UpdatingSettigns : SettignsScreenState()
    data class ShowingUpdateSettingsError(val error: UpdateSettingsError) : SettignsScreenState()

}


abstract class CarConnectError(val type: String) : Throwable(type) {
    //todo can this throwable be nullable?
    abstract val error: Throwable

    init {
        super.initCause(error)
    }
}

sealed class AppStateLoadingError : CarConnectError("AppStateLoadingError") {
    data class UnkownError(override val error: Throwable) : AppStateLoadingError()
    data class IOError(override val error: Throwable) : AppStateLoadingError()
}

sealed class ConnectionError : CarConnectError("ConnectionError") {
    data class UnkownError(override val error: Throwable) : ConnectionError()
}

sealed class SetupError : CarConnectError("SetupError") {
    data class UnkownError(override val error: Throwable) : SetupError()
}

sealed class UpdateSettingsError : CarConnectError("UpdateSettingsError") {
    data class UnkownError(override val error: Throwable) : UpdateSettingsError()
}


fun defaultBackStack(): Stack<CarConnectView> {
    return Stack<CarConnectView>().apply { push(SplashScreen(SplashScreenState.LoadingAppState)) }
}

fun asa() {
    val aa = SplashScreen(SplashScreenState.LoadingAppState)

}
