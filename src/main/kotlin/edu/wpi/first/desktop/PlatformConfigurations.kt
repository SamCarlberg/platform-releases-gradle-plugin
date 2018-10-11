import edu.wpi.first.desktop.Platform
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.kotlin.dsl.project

/**
 * Adds a dependency to the configuration for the given platform.
 */
fun DependencyHandler.add(platform: Platform, dependencyNotation: Any) = add(platform.platformName, dependencyNotation)

/**
 * Creates a [Configuration] for the given platform.  If the given platform is also the current platform (i.e. the
 * operating system running the Gradle build), then the `compileOnly`, `runtimeOnly`, and `testCompile` configurations
 * will extend from the current platform configuration for purposes of being able to run the application and tests.
 */
internal fun Project.platformConfig(platform: Platform): Configuration {
    val configuration = configurations.create(platform.platformName)
    if (platform == buildPlatform) {
        configurations.getByName("compileOnly").extendsFrom(configuration)
        configurations.getByName("runtimeOnly").extendsFrom(configuration)
        configurations.getByName("testCompile").extendsFrom(configuration)
    }
    return configuration
}

/**
 * Creates all the platform-specific configurations for the project.
 */
fun Project.createPlatformConfigurations() = forEachPlatform { platform -> platformConfig(platform) }

/**
 * Adds a dependency on a platform-specific artifact.
 *
 * @param group              the group ID of the artifact
 * @param name               the name of the artifact
 * @param version            the version of the artifact (wildcards are supported)
 * @param classifierFunction a function that takes a native platform and returns the classifier
 *                           for the platform-specific artifact to resolve.
 */
fun DependencyHandler.platform(group: String, name: String, version: String, classifierFunction: (Platform) -> String) {
    forEachPlatform {
        add(it, "$group:$name:$version:${classifierFunction(it)}")
    }
}

/**
 * Adds a dependency on a project that has dependencies on platform-specific libraries. The platform dependencies for
 * the project will be added to the same platform dependency configuration for this project.  The project's `compile`
 * configuration will also be copied to this project's.  Additionally, the project's platform-specific dependencies
 * corresponding to the build platform project will be added to this project's `compileOnly`, `runtime`, and
 * `testCompile` configurations to allow the app's `run` task and all projects' test suites to be able to compile and
 * run.
 *
 * @param path the path to the project
 */
fun DependencyHandler.platformProject(path: String) {
    forEachPlatform {
        platformProject(path, it)
    }
}

internal fun DependencyHandler.platformProject(path: String, platform: Platform) {
    add(platform, project(path, platform.platformName))
    add("compile", project(path, "compile"))
    if (platform == buildPlatform) {
        add("compileOnly", project(path))
        add("runtime", project(path))
        add("testCompile", project(path))
    }
}

/**
 * Performs some action for all supported platforms.
 */
fun forEachPlatform(action: (Platform) -> Unit) {
    Platform.platforms.forEach(action)
}
