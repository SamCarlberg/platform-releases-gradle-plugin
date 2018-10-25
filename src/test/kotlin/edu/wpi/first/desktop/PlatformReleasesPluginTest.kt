package edu.wpi.first.desktop

import platformProject
import org.gradle.api.Project
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.plugins.ApplicationPlugin
import org.gradle.kotlin.dsl.dependencies
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll

class PlatformReleasesPluginTest {

    @Test
    fun `Platform-specific shadow tasks should always exist`() {
        val project = makeProject()

        assertAll("Platform builds", Platform.platforms.stream()
                .map { it.platformName }
                .map { { project.tasks.getByName("shadowJar-$it") } })

        project.tasks.getByName("shadowJarAllPlatforms") // throws an exception if the task does not exist
    }

    @Test
    fun `Platform-specific project dependencies`() {
        val rootProject = makeProject("root")
        val subProject = makeProject("sub", rootProject)

        rootProject.dependencies {
            platformProject(":sub")
        }

        assertAll(Platform.platforms.stream()
                .map { platform ->
                    val platformName = platform.platformName
                    val platformConfig = rootProject.configurations.getByName(platformName)
                    val dependencies = platformConfig.dependencies
                    {
                        assertEquals(1, dependencies.size, "There should only be one dependency for $platformName")

                        val projectDependency = dependencies.first() as ProjectDependency
                        assertEquals(subProject, projectDependency.dependencyProject)
                        assertEquals(platformName, projectDependency.targetConfiguration)
                    }
                }
        )
    }

    private fun Project.applyApplicationPlugin() {
        pluginManager.apply(ApplicationPlugin::class.java)
    }

    private fun makeProject(projectName: String = "test-project", parentProject: Project? = null): Project {
        val project = ProjectBuilder.builder()
                .withParent(parentProject)
                .withName(projectName)
                .build()
        project.pluginManager.apply(PlatformReleasesPlugin::class.java)
        return project
    }
}
