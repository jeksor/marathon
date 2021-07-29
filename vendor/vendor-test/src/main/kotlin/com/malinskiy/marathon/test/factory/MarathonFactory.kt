package com.malinskiy.marathon.test.factory

import com.google.gson.Gson
import com.malinskiy.marathon.Marathon
import com.malinskiy.marathon.di.analyticsModule
import com.malinskiy.marathon.di.cacheModule
import com.malinskiy.marathon.di.marathonConfiguration
import com.malinskiy.marathon.execution.Configuration
import com.malinskiy.marathon.execution.ConfigurationStrictRunChecker
import com.malinskiy.marathon.execution.StrictRunChecker
import com.malinskiy.marathon.execution.progress.ProgressReporter
import com.malinskiy.marathon.io.AttachmentManager
import com.malinskiy.marathon.io.CachedFileHasher
import com.malinskiy.marathon.io.FileHasher
import com.malinskiy.marathon.io.FileManager
import com.malinskiy.marathon.io.Md5FileHasher
import com.malinskiy.marathon.time.SystemTimer
import com.malinskiy.marathon.time.Timer
import org.koin.core.context.startKoin
import org.koin.dsl.module
import java.time.Clock

class MarathonFactory {
    private val configurationFactory: ConfigurationFactory = ConfigurationFactory()

    var timer: Timer? = null

    fun configuration(block: ConfigurationFactory.() -> Unit) = configurationFactory.apply(block)

    fun build(): Marathon {
        val configuration = configurationFactory.build()

        val coreTestModule = module {
            single { FileManager(get<Configuration>().outputDir) }
            single { AttachmentManager(get<Configuration>().outputDir) }
            single<FileHasher> { CachedFileHasher(Md5FileHasher()) }
            single { Gson() }
            single<Clock> { Clock.systemDefaultZone() }
            single { timer ?: SystemTimer(get()) }
            single { ProgressReporter() }
            single<StrictRunChecker> { ConfigurationStrictRunChecker(get()) }
            single { Marathon(get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get()) }
        }

        val marathonStartKoin = startKoin {
            marathonConfiguration(configuration)
            modules(coreTestModule)
            modules(cacheModule)
            modules(analyticsModule)
            modules(configuration.vendorConfiguration.modules())
        }
        return marathonStartKoin.koin.get()
    }
}
