package com.malinskiy.marathon.scenario

import com.malinskiy.marathon.device.DeviceProvider
import com.malinskiy.marathon.execution.TestStatus
import com.malinskiy.marathon.test.StubDevice
import com.malinskiy.marathon.test.Test
import com.malinskiy.marathon.test.TestComponentInfo
import com.malinskiy.marathon.test.assert.shouldBeEqualToAsJson
import com.malinskiy.marathon.test.setupMarathon
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestCoroutineScope
import org.amshove.kluent.shouldBe
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import org.koin.core.context.stopKoin
import java.io.File
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalCoroutinesApi::class)
class DeviceFilteringScenario : Spek(
    {
        afterEachTest {
            stopKoin()
        }

        given("one blacklisted device and empty whitelist") {
            on("execution of two tests") {
                it("should pass on one device") {
                    var output: File? = null
                    val coroutineScope = TestCoroutineScope()

                    val marathon = setupMarathon {
                        val test1 = Test("test", "SimpleTest", "test1", emptySet(), TestComponentInfo())
                        val test2 = Test("test", "SimpleTest", "test2", emptySet(), TestComponentInfo())
                        val device1 = StubDevice(serialNumber = "emulator-5000")
                        val device2 = StubDevice(serialNumber = "emulator-5002")

                        configuration {
                            output = outputDir

                            tests {
                                listOf(test1, test2)
                            }

                            excludeSerialRegexes = listOf("""emulator-5002""".toRegex())
                            includeSerialRegexes = emptyList()

                            vendorConfiguration.deviceProvider.coroutineScope = coroutineScope

                            devices {
                                delay(1000)
                                it.send(DeviceProvider.DeviceEvent.DeviceConnected(device1))
                                it.send(DeviceProvider.DeviceEvent.DeviceConnected(device2))
                            }
                        }

                        device1.executionResults = mapOf(
                            test1 to arrayOf(TestStatus.PASSED),
                            test2 to arrayOf(TestStatus.PASSED)
                        )
                    }

                    val job = coroutineScope.launch {
                        marathon.runAsync()
                    }

                    coroutineScope.advanceTimeBy(TimeUnit.SECONDS.toMillis(20))

                    job.isCompleted shouldBe true
                    File(output!!.absolutePath + "/test_result", "raw.json")
                        .shouldBeEqualToAsJson(File(javaClass.getResource("/output/raw/device_filtering_1.json").file))
                }
            }
        }

        given("one whitelisted device and empty blacklist") {
            on("execution of two tests") {
                it("should pass on one device") {
                    var output: File? = null
                    val coroutineScope = TestCoroutineScope()

                    val marathon = setupMarathon {
                        val test1 = Test("test", "SimpleTest", "test1", emptySet(), TestComponentInfo())
                        val test2 = Test("test", "SimpleTest", "test2", emptySet(), TestComponentInfo())
                        val device1 = StubDevice(serialNumber = "emulator-5000")
                        val device2 = StubDevice(serialNumber = "emulator-5002")

                        configuration {
                            output = outputDir

                            tests {
                                listOf(test1, test2)
                            }

                            excludeSerialRegexes = emptyList()
                            includeSerialRegexes = listOf("""emulator-5002""".toRegex())

                            vendorConfiguration.deviceProvider.coroutineScope = coroutineScope

                            devices {
                                delay(1000)
                                it.send(DeviceProvider.DeviceEvent.DeviceConnected(device1))
                                it.send(DeviceProvider.DeviceEvent.DeviceConnected(device2))
                            }
                        }

                        device2.executionResults = mapOf(
                            test1 to arrayOf(TestStatus.PASSED),
                            test2 to arrayOf(TestStatus.PASSED)
                        )
                    }

                    val job = coroutineScope.launch {
                        marathon.runAsync()
                    }

                    coroutineScope.advanceTimeBy(TimeUnit.SECONDS.toMillis(20))

                    job.isCompleted shouldBe true
                    File(output!!.absolutePath + "/test_result", "raw.json")
                        .shouldBeEqualToAsJson(File(javaClass.getResource("/output/raw/device_filtering_2.json").file))
                }
            }
        }

        given("one blacklisted device and one whitelisted") {
            on("execution of two tests") {
                it("should pass on one device") {
                    var output: File? = null
                    val coroutineScope = TestCoroutineScope()

                    val marathon = setupMarathon {
                        val test1 = Test("test", "SimpleTest", "test1", emptySet(), TestComponentInfo())
                        val test2 = Test("test", "SimpleTest", "test2", emptySet(), TestComponentInfo())
                        val device1 = StubDevice(serialNumber = "emulator-5000")
                        val device2 = StubDevice(serialNumber = "emulator-5002")
                        val device3 = StubDevice(serialNumber = "emulator-5004")

                        configuration {
                            output = outputDir

                            tests {
                                listOf(test1, test2)
                            }

                            excludeSerialRegexes = listOf("""emulator-5002""".toRegex())
                            includeSerialRegexes = listOf("""emulator-500[2,4]""".toRegex())

                            vendorConfiguration.deviceProvider.coroutineScope = coroutineScope

                            devices {
                                delay(1000)
                                it.send(DeviceProvider.DeviceEvent.DeviceConnected(device1))
                                it.send(DeviceProvider.DeviceEvent.DeviceConnected(device2))
                                it.send(DeviceProvider.DeviceEvent.DeviceConnected(device3))
                            }
                        }

                        device3.executionResults = mapOf(
                            test1 to arrayOf(TestStatus.PASSED),
                            test2 to arrayOf(TestStatus.PASSED)
                        )
                    }

                    val job = coroutineScope.launch {
                        marathon.runAsync()
                    }

                    coroutineScope.advanceTimeBy(TimeUnit.SECONDS.toMillis(20))

                    job.isCompleted shouldBe true
                    File(output!!.absolutePath + "/test_result", "raw.json")
                        .shouldBeEqualToAsJson(File(javaClass.getResource("/output/raw/device_filtering_3.json").file))
                }
            }
        }
    })
