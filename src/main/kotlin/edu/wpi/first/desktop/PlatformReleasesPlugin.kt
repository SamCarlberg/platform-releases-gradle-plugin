package edu.wpi.first.desktop

import com.github.jengelman.gradle.plugins.shadow.ShadowPlugin
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import createPlatformConfigurations
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.ApplicationPlugin
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.bundling.Zip
import org.gradle.kotlin.dsl.getByName
import org.gradle.kotlin.dsl.withType

class PlatformReleasesPlugin : Plugin<Project> {

    companion object {
        private val CONFIG_NAMES = listOf("api", "compile", "implementation", "runtime", "runtimeOnly")

        /**
         * The name of the default jlink task.
         */
        const val JLINK_TASK_NAME = "jlink"

        /**
         * The name of the default jlink ZIP task.
         */
        const val JLINK_ZIP_TASK_NAME = "jlinkZip"
    }

    override fun apply(project: Project) {
        with(project) {
            plugins.apply(JavaPlugin::class.java)
            plugins.apply(ShadowPlugin::class.java)

            createPlatformConfigurations()

            val nativeShadowJars = Platform.platforms.map { platform ->
                tasks.register("shadowJar-${platform.platformName}", ShadowJar::class.java) {
                    group = "Shadow"
                    description = "Generates a platform-specific shadow jar for ${platform.platformName}"
                    classifier = platform.platformName
                    val configs = CONFIG_NAMES
                            .filter(project.configurations.names::contains)
                            .map(project.configurations::getByName)
                            .toMutableList()
                    configs.add(project.configurations.getByName(platform.platformName))
                    configurations = configs
                }
            }

            tasks.register("shadowJarAllPlatforms") {
                group = "Shadow"
                description = "Generates all platform-specific shadow jars at once."
                nativeShadowJars.forEach {
                    dependsOn(it)
                }
            }

            // Add jlink tasks to application projects only
            plugins.withType(ApplicationPlugin::class) {
                tasks.register(JLINK_TASK_NAME, JLinkTask::class.java) {
                    group = "Distribution"
                    description = "Generates a native Java runtime image"
                    options {
                        shadowTask = tasks.getByName("shadowJar", ShadowJar::class)
                    }
                }
                tasks.register(JLINK_ZIP_TASK_NAME, Zip::class.java) {
                    group = "Distribution"
                    description = "Generates a ZIP file containing a native Java runtime image and application shadow jar"
                    dependsOn(JLINK_TASK_NAME)
                    doLast {
                        val jlinkTask = tasks.getByName(JLINK_TASK_NAME, JLinkTask::class)
                        from(jlinkTask.options.output)
                        into(destinationDir)
                    }
                }
            }
        }
    }
}
