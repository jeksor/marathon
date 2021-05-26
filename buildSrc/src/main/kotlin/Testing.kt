import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.withType

object Testing {
    fun configure(project: Project) {
        project.dependencies {
            add("testImplementation", TestLibraries.kluent)
            add("testImplementation", TestLibraries.mockitoKotlin)
            add("testImplementation", TestLibraries.spekAPI)
            add("testImplementation", TestLibraries.junit5)
            add("testRuntimeOnly", TestLibraries.spekJUnitPlatformEngine)
            add("testRuntimeOnly", TestLibraries.jupiterEngine)
        }

        project.tasks.withType<Test>().configureEach {
            useJUnitPlatform {
                includeEngines("junit-jupiter", "spek")
            }
        }
    }
}
