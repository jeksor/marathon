package com.malinskiy.marathon.scenario

import com.malinskiy.marathon.device.DeviceProvider
import com.malinskiy.marathon.execution.TestStatus
import com.malinskiy.marathon.execution.strategy.impl.retry.fixedquota.FixedQuotaRetryStrategy
import com.malinskiy.marathon.test.StubDevice
import com.malinskiy.marathon.test.Test
import com.malinskiy.marathon.test.TestComponentInfo
import com.malinskiy.marathon.test.assert.shouldBeEqualToAsJson
import com.malinskiy.marathon.test.setupMarathon
import com.malinskiy.marathon.time.Timer
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestCoroutineScope
import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldBeEqualTo
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import org.koin.core.context.stopKoin
import java.io.File
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalCoroutinesApi::class)
class UncompletedScenarios : Spek(
    {
        afterEachTest {
            stopKoin()
        }

        given("one device that never completes tests") {
            on("100 uncompleted tests executed") {
                it("should return") {
                    var output: File? = null
                    val coroutineScope = TestCoroutineScope()

                    val marathon = setupMarathon {
                        val test1 = Test("test", "SimpleTest", "test1", emptySet(), TestComponentInfo())
                        val device1 = StubDevice(serialNumber = "serial-1")

                        configuration {
                            output = outputDir

                            tests {
                                listOf(test1)
                            }

                            uncompletedTestRetryQuota = 100

                            vendorConfiguration.deviceProvider.coroutineScope = coroutineScope

                            devices {
                                delay(1000)
                                it.send(DeviceProvider.DeviceEvent.DeviceConnected(device1))
                            }
                        }

                        device1.executionResults = mapOf(test1 to Array(101) { TestStatus.INCOMPLETE })
                    }

                    val job = coroutineScope.launch {
                        marathon.runAsync()
                    }

                    coroutineScope.advanceTimeBy(TimeUnit.SECONDS.toMillis(600))

                    job.isCompleted shouldBe true

                    File(output!!.absolutePath + "/test_result", "raw.json")
                        .shouldBeEqualToAsJson(File(javaClass.getResource("/output/raw/uncompleted_scenario_1.json").file))
                }
            }

            on("100 uncompleted tests while throwing exception") {
                it("should return") {
                    var output: File? = null
                    val coroutineScope = TestCoroutineScope()
                    val timerMock: Timer = mock()

                    val marathon = setupMarathon {
                        val test1 = Test("test", "SimpleTest", "test1", emptySet(), TestComponentInfo())
                        val device1 =
                            StubDevice(serialNumber = "serial-1", crashWithTestBatchException = true)

                        configuration {
                            output = outputDir
                            timer = timerMock

                            tests {
                                listOf(test1)
                            }

                            uncompletedTestRetryQuota = 100

                            vendorConfiguration.deviceProvider.coroutineScope = coroutineScope

                            devices {
                                delay(1000)
                                it.send(DeviceProvider.DeviceEvent.DeviceConnected(device1))
                            }
                        }

                        device1.executionResults = mapOf(test1 to Array(101) { TestStatus.INCOMPLETE })
                    }

                    var i = 0L
                    whenever(timerMock.currentTimeMillis()).then { i++ }

                    val job = coroutineScope.launch {
                        marathon.runAsync()
                    }

                    coroutineScope.advanceTimeBy(TimeUnit.SECONDS.toMillis(600))

                    job.isCompleted shouldBe true

                    File(output!!.absolutePath + "/test_result", "raw.json")
                        .shouldBeEqualToAsJson(File(javaClass.getResource("/output/raw/uncompleted_scenario_1.json").file))
                }
            }

            on("after all retries") {
                it("should report test as failed") {
                    var output: File? = null
                    val coroutineScope = TestCoroutineScope()

                    val marathon = setupMarathon {
                        val test1 = Test("test", "SimpleTest", "test1", emptySet(), TestComponentInfo())
                        val device1 = StubDevice(serialNumber = "serial-1")

                        configuration {
                            output = outputDir

                            tests {
                                listOf(test1)
                            }

                            uncompletedTestRetryQuota = 3

                            vendorConfiguration.deviceProvider.coroutineScope = coroutineScope

                            devices {
                                delay(1000)
                                it.send(DeviceProvider.DeviceEvent.DeviceConnected(device1))
                            }
                        }

                        device1.executionResults = mapOf(test1 to Array(4) { TestStatus.INCOMPLETE })
                    }

                    val job = coroutineScope.launch {
                        marathon.runAsync()
                    }

                    coroutineScope.advanceTimeBy(TimeUnit.SECONDS.toMillis(600))

                    job.isCompleted shouldBe true

                    File(output!!.absolutePath + "/test_result", "raw.json")
                        .shouldBeEqualToAsJson(File(javaClass.getResource("/output/raw/uncompleted_scenario_2.json").file))
                }
            }

            on("with retry strategy") {
                it("should report test as failed") {
                    var output: File? = null
                    val coroutineScope = TestCoroutineScope()

                    val marathon = setupMarathon() {
                        val test1 = Test("test", "SimpleTest", "test1", emptySet(), TestComponentInfo())
                        val device1 = StubDevice(serialNumber = "serial-1")

                        configuration {
                            output = outputDir

                            tests {
                                listOf(test1)
                            }

                            uncompletedTestRetryQuota = 3
                            retryStrategy = FixedQuotaRetryStrategy(10, 3)

                            vendorConfiguration.deviceProvider.coroutineScope = coroutineScope

                            devices {
                                delay(1000)
                                it.send(DeviceProvider.DeviceEvent.DeviceConnected(device1))
                            }
                        }

                        device1.executionResults = mapOf(test1 to Array(100) { TestStatus.INCOMPLETE })
                    }

                    val job = coroutineScope.launch {
                        marathon.runAsync()
                    }

                    coroutineScope.advanceTimeBy(TimeUnit.SECONDS.toMillis(600))

                    job.isCompleted shouldBe true

                    File(output!!.absolutePath + "/test_result", "raw.json")
                }
            }
        }
    })
