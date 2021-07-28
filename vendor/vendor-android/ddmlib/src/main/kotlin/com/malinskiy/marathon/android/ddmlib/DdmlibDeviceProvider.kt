package com.malinskiy.marathon.android.ddmlib

import com.android.ddmlib.AdbInitOptions
import com.android.ddmlib.AndroidDebugBridge
import com.android.ddmlib.DdmPreferences
import com.android.ddmlib.IDevice
import com.android.ddmlib.TimeoutException
import com.malinskiy.marathon.actor.unboundedChannel
import com.malinskiy.marathon.analytics.internal.pub.Track
import com.malinskiy.marathon.android.AndroidAppInstaller
import com.malinskiy.marathon.android.AndroidConfiguration
import com.malinskiy.marathon.android.executor.logcat.LogcatListener
import com.malinskiy.marathon.device.DeviceProvider
import com.malinskiy.marathon.device.DeviceProvider.DeviceEvent.DeviceConnected
import com.malinskiy.marathon.device.DeviceProvider.DeviceEvent.DeviceDisconnected
import com.malinskiy.marathon.exceptions.NoDevicesException
import com.malinskiy.marathon.execution.Configuration
import com.malinskiy.marathon.execution.StrictRunChecker
import com.malinskiy.marathon.io.AttachmentManager
import com.malinskiy.marathon.io.FileManager
import com.malinskiy.marathon.log.MarathonLogging
import com.malinskiy.marathon.time.Timer
import com.malinskiy.marathon.vendor.VendorConfiguration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.newFixedThreadPoolContext
import java.nio.file.Paths
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.TimeUnit
import kotlin.coroutines.CoroutineContext
import java.util.concurrent.atomic.AtomicBoolean

private const val DEFAULT_DDM_LIB_TIMEOUT = 30000
private const val DEFAULT_DDM_LIB_SLEEP_TIME = 500
private const val PRINT_LOG_TIMEOUT = 20000L
private const val DEFAULT_DDM_LIB_CREATE_BRIDGE_TIMEOUT = Long.MAX_VALUE

class DdmlibDeviceProvider(
    private val track: Track,
    private val timer: Timer,
    private val config: Configuration,
    private val androidAppInstaller: AndroidAppInstaller,
    private val fileManager: FileManager,
    private val strictRunChecker: StrictRunChecker,
    private val logcatListener: LogcatListener,
    private val attachmentManager: AttachmentManager
) : DeviceProvider, CoroutineScope {
    private val logger = MarathonLogging.logger("AndroidDeviceProvider")

    private lateinit var adb: AndroidDebugBridge

    private val channel: Channel<DeviceProvider.DeviceEvent> = unboundedChannel()
    private val devices: ConcurrentMap<String, DdmlibAndroidDevice> = ConcurrentHashMap()

    private lateinit var listener: AndroidDebugBridge.IDeviceChangeListener

    private val bootWaitContext = newFixedThreadPoolContext(4, "AndroidDeviceProvider-BootWait")
    override val coroutineContext: CoroutineContext
        get() = bootWaitContext

    override val deviceInitializationTimeoutMillis: Long = 180_000

    override suspend fun initialize(vendorConfiguration: VendorConfiguration) {
        check(vendorConfiguration is AndroidConfiguration) { "Invalid configuration $vendorConfiguration passed" }
        DdmPreferences.setTimeOut(DEFAULT_DDM_LIB_TIMEOUT)
        val adbInitOptions = AdbInitOptions.Builder()
            .enableUserManagedAdbMode(5037)
            .setClientSupportEnabled(false)
            .build()
        AndroidDebugBridge.init(adbInitOptions)

        val absolutePath = Paths.get(vendorConfiguration.androidSdk.absolutePath, "platform-tools", "adb").toFile().absolutePath

        listener = object : AndroidDebugBridge.IDeviceChangeListener {
            override fun deviceChanged(device: IDevice?, changeMask: Int) {
                logger.debug { "Device changed: $device" }

                device?.let {
                    launch(context = bootWaitContext) {
                        val maybeNewAndroidDevice =
                            DdmlibAndroidDevice(
                                it,
                                absolutePath,
                                track,
                                timer,
                                androidAppInstaller,
                                attachmentManager,
                                fileManager,
                                vendorConfiguration.serialStrategy,
                                logcatListener,
                                strictRunChecker
                            )
                        val healthy = maybeNewAndroidDevice.healthy

                        logger.debug { "Device ${device.serialNumber} changed state. Healthy = $healthy" }
                        if (healthy) {
                            verifyBooted(maybeNewAndroidDevice)
                            val androidDevice = getDeviceOrPut(maybeNewAndroidDevice)
                            notifyConnected(androidDevice)
                        } else {
                            //This shouldn't have any side effects even if device was previously removed
                            logger.debug { "Device is not healthy, notifying disconnected $device" }
                            notifyDisconnected(maybeNewAndroidDevice)
                        }
                    }
                }
            }

            override fun deviceConnected(device: IDevice?) {
                logger.debug { "Device connected: $device" }

                device?.let {
                    launch {
                        val maybeNewAndroidDevice = DdmlibAndroidDevice(
                            ddmsDevice = it,
                            track = track,
                            timer = timer,
                            serialStrategy = vendorConfiguration.serialStrategy,
                            androidAppInstaller = androidAppInstaller,
                            attachmentManager = attachmentManager,
                            reportsFileManager = fileManager,
                            adbPath = absolutePath,
                            logcatListener = logcatListener,
                            strictRunChecker = strictRunChecker
                        )

                        val healthy = maybeNewAndroidDevice.healthy
                        logger.debug("Device ${maybeNewAndroidDevice.serialNumber} connected. Healthy = $healthy")

                        if (healthy) {
                            verifyBooted(maybeNewAndroidDevice)
                            val androidDevice = getDeviceOrPut(maybeNewAndroidDevice)
                            notifyConnected(androidDevice)
                        }
                    }
                }
            }

            override fun deviceDisconnected(device: IDevice?) {
                device?.let {
                    launch {
                        logger.debug { "Device ${device.serialNumber} disconnected" }
                        matchDdmsToDevice(it)?.let {
                            notifyDisconnected(it)
                            it.dispose()
                        }
                    }
                }
            }

            private suspend fun verifyBooted(device: DdmlibAndroidDevice) {
                if (!waitForBoot(device)) throw TimeoutException("Timeout waiting for device ${device.serialNumber} to boot")
            }

            private suspend fun waitForBoot(device: DdmlibAndroidDevice): Boolean {
                var booted = false

                track.trackProviderDevicePreparing(device) {
                    for (i in 1..30) {
                        if (device.booted) {
                            logger.debug { "Device ${device.serialNumber} booted!" }
                            booted = true
                            break
                        } else {
                            delay(1000)
                            logger.debug { "Device ${device.serialNumber} is still booting..." }
                        }

                        if (Thread.interrupted() || !isActive) {
                            booted = true
                            break
                        }
                    }
                }

                return booted
            }

            private fun notifyConnected(device: DdmlibAndroidDevice) {
                logger.debug { "Notify device connected $device" }

                launch {
                    logger.debug { "Send DeviceConnected message for $device" }
                    channel.send(DeviceConnected(device))
                }
            }

            private fun notifyDisconnected(device: DdmlibAndroidDevice) {
                launch {
                    androidAppInstaller.onDisconnected(device)
                    channel.send(DeviceDisconnected(device))
                    logcatListener.onDeviceDisconnected(device)
                }
            }
        }
        AndroidDebugBridge.addDeviceChangeListener(listener)
        adb = AndroidDebugBridge.createBridge(absolutePath, false, DEFAULT_DDM_LIB_CREATE_BRIDGE_TIMEOUT, TimeUnit.MILLISECONDS)
        logger.debug { "Created ADB bridge" }

        var getDevicesCountdown = config.noDevicesTimeoutMillis
        val sleepTime = DEFAULT_DDM_LIB_SLEEP_TIME
        while (!adb.hasInitialDeviceList() || !adb.hasDevices() && getDevicesCountdown >= 0) {
            logger.debug { "No devices, waiting..." }

            try {
                Thread.sleep(sleepTime.toLong())
            } catch (e: InterruptedException) {
                throw TimeoutException("Timeout getting device list", e)
            }
            getDevicesCountdown -= sleepTime
        }

        logger.debug { "Finished waiting for device list" }

        adb.devices.forEach {
            logger.debug { "Notifying inital connected list: $it" }
            listener.deviceConnected(it)
        }

        logger.debug { "Finished notifying" }

        if (!adb.hasInitialDeviceList() || printStackTraceAfterTimeout(PRINT_LOG_TIMEOUT) { !adb.hasDevices() }) {
            logger.debug { "Throwing no devices exception in DdmlibDeviceProvider" }
            throw NoDevicesException("No devices found.")
        }

        logger.debug { "Finished DdmlibDeviceProvider initialization" }
    }

    private fun <T> printStackTraceAfterTimeout(timeoutMillis: Long, block: () -> T): T {
        val currentThread = Thread.currentThread()
        val isBlockFinished = AtomicBoolean(false)

        Thread {
            Thread.sleep(timeoutMillis)
            if (!isBlockFinished.get() && currentThread.isAlive) {
                logger.debug { "Task is not finished within timeout. Printing thread stacktrace:" }
                currentThread
                    .stackTrace
                    .forEach { logger.debug { it } }
            }
        }.start()

        val result = block()

        isBlockFinished.set(true)

        return result
    }

    private fun getDeviceOrPut(androidDevice: DdmlibAndroidDevice): DdmlibAndroidDevice {
        val newAndroidDevice = devices.getOrPut(androidDevice.serialNumber) {
            androidDevice
        }

        if (newAndroidDevice != androidDevice) {
            logger.debug { "There was a device with the same serial number as the new device ($newAndroidDevice), disposing old device" }
            androidDevice.dispose()
            logger.debug { "Old device disposed ($androidDevice)" }
        }

        return newAndroidDevice
    }

    private fun matchDdmsToDevice(device: IDevice): DdmlibAndroidDevice? {
        val observedDevices = devices.values
        return observedDevices.findLast {
            device == it.ddmsDevice ||
                device.serialNumber == it.ddmsDevice.serialNumber
        }
    }

    private fun AndroidDebugBridge.hasDevices(): Boolean = devices.isNotEmpty()

    override suspend fun terminate() {
        devices.values.forEach {
            it.waitForAsyncWork()
        }
        if (::listener.isInitialized) {
            AndroidDebugBridge.removeDeviceChangeListener(listener)
        }
        bootWaitContext.close()
        channel.close()
    }

    override fun subscribe() = channel

}
