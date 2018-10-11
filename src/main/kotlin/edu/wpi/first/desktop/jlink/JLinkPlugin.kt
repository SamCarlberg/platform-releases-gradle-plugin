package edu.wpi.first.desktop.jlink

import org.gradle.api.Plugin
import org.gradle.api.Project

class JLinkPlugin : Plugin<Project> {

    companion object {
        const val EXTENSION_NAME = "jlink"
        const val JLINK_TASK_GROUP = "JLink"
        const val JLINK_TASK_NAME = "jlink"
        const val JLINK_ZIP_TASK_NAME = "jlinkZip"
    }

    override fun apply(project: Project) {
        println("Applying jlink plugin to $project")
        val container = project.container(JLinkOptions::class.java) { name ->
            require(name.isNotBlank()) {
                "Name of ${JLinkOptions::class.simpleName} must be specified"
            }
            JLinkOptions(name = name)
        }
        project.extensions.add(EXTENSION_NAME, container)

        val jlinkTask = project.tasks.register(JLINK_TASK_NAME) {
            group = JLINK_TASK_GROUP
            description = "Generates a native Java runtime image"
        }

        val jlinkZipTask = project.tasks.register(JLINK_ZIP_TASK_NAME) {
            group = JLINK_ZIP_TASK_NAME
            description = "Generates a ZIP file of a native Java runtime image"
        }

    }
}
