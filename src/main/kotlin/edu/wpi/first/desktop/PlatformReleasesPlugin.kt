package edu.wpi.first.desktop

import com.github.jengelman.gradle.plugins.shadow.ShadowPlugin
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import createPlatformConfigurations
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin

class PlatformReleasesPlugin : Plugin<Project> {

    companion object {
        private val CONFIG_NAMES = listOf("api", "compile", "implementation", "runtime", "runtimeOnly")
    }

    override fun apply(project: Project) {
        with(project) {
            plugins.apply(JavaPlugin::class.java)
            plugins.apply(ShadowPlugin::class.java)

            createPlatformConfigurations()

            val platformShadowJars = Platform.platforms.map { platform ->
                tasks.register("shadowJar-${platform.platformName}", ShadowJar::class.java) {
                    group = "Shadow"
                    description = "Generates a platform-specific shadow jar for ${platform.platformName}."
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
                platformShadowJars.forEach {
                    dependsOn(it)
                }
            }
        }
    }
}
