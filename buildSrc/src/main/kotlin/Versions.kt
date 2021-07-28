object Versions {
    val marathon = System.getenv("DEPLOY_VERSION_OVERRIDE") ?: "0.5.4"

    val coroutines = "1.4.3"
    val ktor = "1.5.4"

    val ddmlib = "27.1.3"
    val dexTestParser = "830520963019a6cefada34fc5eb396003c1468d5" // contains patch https://github.com/linkedin/dex-test-parser/pull/46
    val kotlinLogging = "1.4.9"
    val slf4jAPI = "1.0.0"
    val logbackClassic = "1.2.3"
    val axmlParser = "1.0"
    val bugsnag = "3.6.1"

    val androidGradleVersion = "4.2.2"

    val spek = "1.1.5"
    val junit5 = "5.6.0"
    val kluent = "1.64"

    val espressoCore = "3.3.0"
    val androidxTest = "1.3.0"
    val junit = "4.12"
    val gson = "2.8.5"
    val apacheCommonsText = "1.3"
    val apacheCommonsIO = "2.6"
    val apacheCommonsCollections = "4.4"
    val influxDbClient = "2.13"
    val argParser = "2.0.7"
    val jacksonDatabind = "2.9.5"
    val jacksonKotlin = "2.9.4.1"
    val jacksonYaml = "2.9.6"
    val jacksonJSR310 = "2.9.6"
    val ddPlist = "1.21"
    val guava = "26.0-jre"
    val rsync4j = "3.1.2-12"
    val sshj = "0.26.0"
    val testContainers = "1.15.3"
    val jupiterEngine = "5.1.0"
    val jansi = "1.17.1"
    val scalr = "4.2"
    val allure = "2.13.5"
    val allureEnvironment = "1.0.0"
    val mockitoKotlin = "2.0.0"
    val googleAnalitycsWrapper = "2.0.0"
    val koin = "2.0.1"
    val jsonAssert = "1.5.0"
}

object BuildPlugins {
    val androidGradle = "com.android.tools.build:gradle:${Versions.androidGradleVersion}"
}

object Libraries {
    val ddmlib = "com.android.tools.ddms:ddmlib:${Versions.ddmlib}"
    val androidCommon = "com.android.tools:common:${Versions.ddmlib}"
    val dexTestParser = "com.github.lukaville:dex-test-parser:${Versions.dexTestParser}"
    val kotlinBom = "org.jetbrains.kotlin:kotlin-bom"
    val kotlinReflect = "org.jetbrains.kotlin:kotlin-reflect"
    val kotlinLogging = "io.github.microutils:kotlin-logging:${Versions.kotlinLogging}"
    val slf4jAPI = "com.github.nfrankel:slf4k:${Versions.slf4jAPI}"
    val logbackClassic = "ch.qos.logback:logback-classic:${Versions.logbackClassic}"
    val kotlinCoroutines = "org.jetbrains.kotlinx:kotlinx-coroutines-core:${Versions.coroutines}"
    val ktorClient = "io.ktor:ktor-client-core:${Versions.ktor}"
    val ktorAuth = "io.ktor:ktor-client-auth-jvm:${Versions.ktor}"
    val ktorApacheClient = "io.ktor:ktor-client-apache:${Versions.ktor}"
    val axmlParser = "com.shazam:axmlparser:${Versions.axmlParser}"
    val gson = "com.google.code.gson:gson:${Versions.gson}"
    val apacheCommonsText = "org.apache.commons:commons-text:${Versions.apacheCommonsText}"
    val apacheCommonsIO = "commons-io:commons-io:${Versions.apacheCommonsIO}"
    val apacheCommonsCollections = "org.apache.commons:commons-collections4:${Versions.apacheCommonsCollections}"
    val influxDbClient = "org.influxdb:influxdb-java:${Versions.influxDbClient}"
    val argParser = "com.xenomachina:kotlin-argparser:${Versions.argParser}"
    val jacksonDatabind = "com.fasterxml.jackson.core:jackson-databind:${Versions.jacksonDatabind}"
    val jacksonAnnotations = "com.fasterxml.jackson.core:jackson-annotations:${Versions.jacksonDatabind}"
    val jacksonKotlin = "com.fasterxml.jackson.module:jackson-module-kotlin:${Versions.jacksonKotlin}"
    val jacksonYaml = "com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:${Versions.jacksonYaml}"
    val jacksonJSR310 = "com.fasterxml.jackson.datatype:jackson-datatype-jsr310:${Versions.jacksonJSR310}"
    val ddPlist = "com.googlecode.plist:dd-plist:${Versions.ddPlist}"
    val guava = "com.google.guava:guava:${Versions.guava}"
    val rsync4j = "com.github.fracpete:rsync4j-all:${Versions.rsync4j}"
    val sshj = "com.hierynomus:sshj:${Versions.sshj}"
    val jansi = "org.fusesource.jansi:jansi:${Versions.jansi}"
    val scalr = "org.imgscalr:imgscalr-lib:${Versions.scalr}"
    val allure = "io.qameta.allure:allure-java-commons:${Versions.allure}"
    val allureEnvironment = "com.github.automatedowl:allure-environment-writer:${Versions.allureEnvironment}"
    val koin = "io.insert-koin:koin-core:${Versions.koin}"
    val bugsnag = "com.bugsnag:bugsnag:${Versions.bugsnag}"
}

object TestLibraries {
    val spekAPI = "org.jetbrains.spek:spek-api:${Versions.spek}"
    val spekJUnitPlatformEngine = "org.jetbrains.spek:spek-junit-platform-engine:${Versions.spek}"
    val junit5 = "org.junit.jupiter:junit-jupiter:${Versions.junit5}"
    val kluent = "org.amshove.kluent:kluent:${Versions.kluent}"
    val ktorClientMock = "io.ktor:ktor-client-mock-jvm:${Versions.ktor}"
    val kotlinCoroutinesTest = "org.jetbrains.kotlinx:kotlinx-coroutines-test:${Versions.coroutines}"

    val androidxTestRunner = "androidx.test:runner:${Versions.androidxTest}"
    val espressoCore = "androidx.test.espresso:espresso-core:${Versions.espressoCore}"
    val junit = "junit:junit:${Versions.junit}"
    val mockitoKotlin = "com.nhaarman.mockitokotlin2:mockito-kotlin:${Versions.mockitoKotlin}"
    val jupiterEngine = "org.junit.jupiter:junit-jupiter-engine:${Versions.jupiterEngine}"
    val koin = "io.insert-koin:koin-test:${Versions.koin}"
    val jsonAssert = "org.skyscreamer:jsonassert:${Versions.jsonAssert}"

    val testContainers = "org.testcontainers:testcontainers:${Versions.testContainers}"
    val testContainersInflux = "org.testcontainers:influxdb:${Versions.testContainers}"
}

object Analytics {
    val googleAnalyticsWrapper = "com.brsanthu:google-analytics-java:${Versions.googleAnalitycsWrapper}"
}
