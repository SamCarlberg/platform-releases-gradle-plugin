package edu.wpi.first.desktop

import org.gradle.api.Project
import org.gradle.api.internal.AbstractTask
import org.gradle.internal.jvm.Jvm
import org.gradle.jvm.tasks.Jar
import shadow.org.apache.commons.io.output.ByteArrayOutputStream
import java.nio.charset.Charset

// Note: must be open so Gradle can create a proxy subclass
open class JLinkTask : AbstractTask() {

    /**
     * The jlink options.
     */
    internal val options: JLinkOptions = JLinkOptions()

    init {
        doLast("jlink execution") {
            outputs.dir(options.output)
            if (options.shadowTask == null) {
                throw IllegalStateException("No JAR task specified")
            }
            dependsOn(options.shadowTask)
            val shadowJarLocation = options.shadowTask!!.outputs.files.singleFile.absolutePath
            options.execJLink(project, shadowJarLocation)

            // Copy the application JAR into the jlink bin directory
            project.copy {
                from(shadowJarLocation)
                rename {
                    "${project.name.toLowerCase()}.jar"
                }
                into("${options.output}/bin")
            }
        }
    }

    /**
     * Configures the jlink options.
     */
    fun options(options: JLinkOptions.() -> Unit) {
        this.options.options()
    }
}

class JLinkOptions {

    var shadowTask: Jar? = null

    /**
     * The modules to link. These MUST be on the module path or included in the JDK. If not set, `jdeps` will be run
     * on the output JAR file from [shadowTask] to automatically determine the modules used.
     */
    var modules: List<String> = listOf()

    /**
     * Link service provider modules and their dependencies.
     */
    var bindServices = false

    /**
     * Enable compression of resources.
     */
    var compressionLevel: CompressionLevel = CompressionLevel.NONE

    /**
     * Specifies the byte order of the generated image. The default value is the format of your system's architecture.
     */
    var endianness: Endianness = Endianness.SYSTEM_DEFAULT

    /**
     * Suppresses a fatal error when signed modular JARs are linked in the runtime image.
     * The signature-related files of the signed modular JARs are not copied to the runtime image.
     */
    var ignoreSigningInformation = false

    /**
     * Specifies the module path.
     */
    var modulePath = ""

    /**
     * Excludes header files from the generated image.
     */
    var excludeHeaderFiles = false

    /**
     * Excludes man pages from the generated image.
     */
    var excludeManPages = false

    /**
     * Strips debug symbols from the generated image.
     */
    var stripDebug = false

    /**
     * Optimize `Class.forName` calls to constant class loads.
     */
    var optimizeClassForName = false

    /**
     * Specifies the location of the generated runtime image. By default, this is `${project.dir}/build/jlink`.
     */
    var output: Any = "build/jlink/"

    enum class CompressionLevel {
        /**
         * Do no compression on the generated image.
         */
        NONE,

        /**
         * Share constant string objects.
         */
        CONSTANT_STRING_SHARING,

        /**
         * ZIP compression on the generated image.
         */
        ZIP
    }

    enum class Endianness {
        /**
         * Use the endianness of the build system.
         */
        SYSTEM_DEFAULT,

        /**
         * Force little-endian byte order in the generated image.
         */
        LITTLE,

        /**
         * Force big-endian byte order in the generated image.
         */
        BIG
    }
}

private val javaBin = Jvm.current().javaHome.resolve("bin")

/**
 * Runs `jdeps` on the specified JAR file to determine its module dependencies.
 *
 * @param jar the path to the JAR file on which to run `jdeps`
 */
fun jdeps(project: Project, jar: String): List<String> {
    return ByteArrayOutputStream().use { os ->
        // Get the standard library modules used by Shuffleboard and its dependencies
        project.exec {
            commandLine = listOf(javaBin.resolve("jdeps").toString(), "--list-deps", jar)
            standardOutput = os
        }
        val out = os.toString(Charset.defaultCharset())
        out.split("\n")
                .filter { it.startsWith("   ") }
                .filter { !it.contains('/') }
                .filter { it == it.toLowerCase() }
                .map { it.substring(3) }
    }
}

private fun JLinkOptions.buildCommandLine(project: Project, jar: String): List<String> {
    val commandBuilder = mutableListOf<String>()
    commandBuilder.add(javaBin.resolve("jlink").toString())

    commandBuilder.add("--add-modules")
    if (modules.isEmpty()) {
        // No user-defined modules, run jdeps and use the modules it finds
        commandBuilder.add(jdeps(project, jar).joinToString(separator = ","))
    } else {
        // Only use the user-specified modules
        commandBuilder.add(modules.joinToString(separator = ","))
    }

    if (modulePath.isNotEmpty()) {
        commandBuilder.add("--module-path")
        commandBuilder.add(modulePath)
    }

    if (bindServices) {
        commandBuilder.add("--bind-services")
    }

    commandBuilder.add("--compress=${compressionLevel.ordinal}")

    if (endianness != JLinkOptions.Endianness.SYSTEM_DEFAULT) {
        commandBuilder.add("--endian")
        commandBuilder.add(endianness.name.toLowerCase())
    }

    if (ignoreSigningInformation) {
        commandBuilder.add("--ignore-signing-information")
    }

    if (excludeHeaderFiles) {
        commandBuilder.add("--no-header-files")
    }

    if (excludeManPages) {
        commandBuilder.add("--no-man-pages")
    }

    if (stripDebug) {
        commandBuilder.add("--strip-debug")
    }

    if (optimizeClassForName) {
        commandBuilder.add("--class-for-name")
    }

    commandBuilder.add("--output")
    commandBuilder.add(output.toString())

    return commandBuilder
}

private fun JLinkOptions.execJLink(project: Project, jar: String) {
    project.exec {
        commandLine = buildCommandLine(project, jar)
    }
}
