package com.malinskiy.marathon.report.junit

import com.malinskiy.marathon.device.DeviceInfo
import com.malinskiy.marathon.device.DevicePoolId
import com.malinskiy.marathon.execution.TestResult
import com.malinskiy.marathon.execution.TestStatus
import com.malinskiy.marathon.io.FileManager
import com.malinskiy.marathon.io.FileType
import com.malinskiy.marathon.report.summary.TestSummary
import com.malinskiy.marathon.report.summary.TestSummaryFormatter
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import javax.xml.stream.XMLOutputFactory
import javax.xml.stream.XMLStreamWriter

class JUnitWriter(
    private val fileManager: FileManager,
    private val fileType: FileType,
    private val testSummaryFormatter: TestSummaryFormatter
) {

    fun testFinished(devicePoolId: DevicePoolId, device: DeviceInfo, testResult: TestResult, testSummary: TestSummary?) {
        val file = fileManager.createFile(fileType, devicePoolId, device, testResult.test)

        try {
            file.createNewFile()
        } catch (error: Throwable) {
            error.printStackTrace()
            System.err.println("Error while creating new file ${file.absolutePath}")
            val newTestMethodName = testResult.test.method.dropLast(TEST_METHOD_DROP_CHAR_COUNT)
            testFinished(
                devicePoolId = devicePoolId,
                device = device,
                testResult = testResult.copy(
                    status = TestStatus.FAILURE,
                    test = testResult.test.copy(method = newTestMethodName),
                ),
                testSummary = testSummary?.copy(
                    test = testSummary.test.copy(method = newTestMethodName)
                )
            )
            return
        }

        val writer = XMLOutputFactory.newFactory().createXMLStreamWriter(FileOutputStream(file), "UTF-8")

        generateXml(writer, testResult, testSummary)
        writer.flush()
        writer.close()
    }

    @Suppress("ComplexMethod")
    private fun generateXml(writer: XMLStreamWriter, testResult: TestResult, testSummary: TestSummary?) {
        @Suppress("MagicNumber")
        fun Long.toJUnitSeconds(): String = (this / 1000.0).toString()

        val test = testResult.test

        val failures = if (testResult.status == TestStatus.FAILURE) 1 else 0
        val ignored = if (testResult.status == TestStatus.IGNORED || testResult.status == TestStatus.ASSUMPTION_FAILURE) 1 else 0

        val formattedTimestamp = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }.format(Date(testResult.endTime))

        writer.document {
            element("testsuite") {
                attribute("name", "common")
                attribute("tests", "1")
                attribute("failures", "$failures")
                attribute("errors", "0")
                attribute("skipped", "$ignored")
                attribute("time", testResult.durationMillis().toJUnitSeconds())
                attribute("timestamp", formattedTimestamp)
                element("properties") {}
                element("testcase") {
                    attribute("classname", "${test.pkg}.${test.clazz}")
                    attribute("name", test.method)
                    attribute("time", testResult.durationMillis().toJUnitSeconds())
                    element("system-out") {
                        writeCData(testSummaryFormatter.formatTestResultSummary(testResult, testSummary))
                    }
                    when (testResult.status) {
                        TestStatus.IGNORED, TestStatus.ASSUMPTION_FAILURE -> {
                            element("skipped") {
                                testResult.stacktrace?.let {
                                    writeCData(it)
                                }
                            }
                        }
                        TestStatus.INCOMPLETE, TestStatus.FAILURE -> {
                            element("failure") {
                                writeCData(testResult.stacktrace ?: "")
                            }
                        }
                        else -> {
                        }
                    }
                }
            }
        }
    }

    private companion object {
        private const val TEST_METHOD_DROP_CHAR_COUNT = 20
    }
}
