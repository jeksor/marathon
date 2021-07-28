package com.malinskiy.marathon.analytics

import com.google.gson.Gson
import com.malinskiy.marathon.analytics.external.influx.InfluxDbProvider
import com.malinskiy.marathon.analytics.external.influx.InfluxDbTracker
import com.malinskiy.marathon.analytics.internal.pub.Track
import com.malinskiy.marathon.analytics.internal.sub.DelegatingTrackerInternal
import com.malinskiy.marathon.analytics.internal.sub.ExecutionReportGenerator
import com.malinskiy.marathon.analytics.internal.sub.TrackerInternal
import com.malinskiy.marathon.cache.test.CacheTestResultsTracker
import com.malinskiy.marathon.execution.AnalyticsConfiguration.InfluxDbConfiguration
import com.malinskiy.marathon.execution.Configuration
import com.malinskiy.marathon.io.AttachmentManager
import com.malinskiy.marathon.io.FileManager
import com.malinskiy.marathon.io.FileType
import com.malinskiy.marathon.log.MarathonLogging
import com.malinskiy.marathon.report.allure.AllureReporter
import com.malinskiy.marathon.report.attachment.AttachmentTestEventInflator
import com.malinskiy.marathon.report.device.DeviceInfoJsonReporter
import com.malinskiy.marathon.report.html.HtmlSummaryReporter
import com.malinskiy.marathon.report.junit.FinalJUnitReporter
import com.malinskiy.marathon.report.junit.JUnitReporter
import com.malinskiy.marathon.report.junit.JUnitWriter
import com.malinskiy.marathon.report.listener.ListenerReporter
import com.malinskiy.marathon.report.logs.LogsProvider
import com.malinskiy.marathon.report.logs.LogReportTestEventInflator
import com.malinskiy.marathon.report.raw.RawJsonReporter
import com.malinskiy.marathon.report.stdout.StdoutReporter
import com.malinskiy.marathon.report.summary.TestSummaryFormatter
import com.malinskiy.marathon.report.test.TestJsonReporter
import com.malinskiy.marathon.report.timeline.TimelineReporter
import com.malinskiy.marathon.report.timeline.TimelineSummaryProvider
import com.malinskiy.marathon.report.trace.TraceReporter
import com.malinskiy.marathon.time.Timer
import java.io.File

internal class TrackerFactory(
    private val configuration: Configuration,
    private val fileManager: FileManager,
    private val attachmentManager: AttachmentManager,
    private val cacheTestResultsTracker: CacheTestResultsTracker,
    private val logsProvider: LogsProvider,
    private val gson: Gson,
    private val timer: Timer,
    private val track: Track
) {

    val log = MarathonLogging.logger("TrackerFactory")

    fun create(): TrackerInternal {
        val defaultTrackers = mutableListOf<TrackerInternal>(createExecutionReportGenerator())

        if (configuration.analyticsConfiguration is InfluxDbConfiguration) {
            val config = configuration.analyticsConfiguration
            val influxDbTracker = createInfluxDbTracker(config)
            influxDbTracker?.let { defaultTrackers.add(it) }
        }

        val delegatingTrackerInternal = DelegatingTrackerInternal(defaultTrackers)
        val mappingTracker = MappingTracker(delegatingTrackerInternal)

        track.add(mappingTracker)
        track.add(cacheTestResultsTracker)
        configuration.customAnalyticsTracker?.let { track.add(it) }

        return delegatingTrackerInternal
    }

    private fun createInfluxDbTracker(config: InfluxDbConfiguration): InfluxDbTracker? {
        val db = try {
            InfluxDbProvider(config).createDb()
        } catch (e: Exception) {
            log.warn(e) { "Failed to reach InfluxDB at ${config.url}" }
            null
        }
        return db?.let { InfluxDbTracker(it, config.dbName, config.retentionPolicyConfiguration.name) }
    }

    private fun createExecutionReportGenerator(): ExecutionReportGenerator {
        val testResultDescriptionFactory = TestSummaryFormatter()
        val testEventInflators = {
            listOf(
                LogReportTestEventInflator(logsProvider.getFullReport()),
                AttachmentTestEventInflator(attachmentManager)
            )
        }

        return ExecutionReportGenerator(
            reporters = listOfNotNull(
                DeviceInfoJsonReporter(fileManager, gson),
                JUnitReporter(JUnitWriter(fileManager, FileType.TEST, testResultDescriptionFactory)),
                FinalJUnitReporter(JUnitWriter(fileManager, FileType.TEST_FINAL, testResultDescriptionFactory)),
                TimelineReporter(TimelineSummaryProvider(), gson, configuration.outputDir),
                TraceReporter(configuration.outputDir),
                RawJsonReporter(fileManager, gson),
                TestJsonReporter(fileManager, gson),
                AllureReporter(configuration, File(configuration.outputDir, "allure-results"), testResultDescriptionFactory),
                HtmlSummaryReporter(gson, configuration.outputDir, configuration, testResultDescriptionFactory),
                StdoutReporter(timer),
                configuration.listener?.let { ListenerReporter(it) }
            ),
            testEventInflatorsFactory = testEventInflators
        )
    }
}
