import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `java`
    `java-gradle-plugin`
    `kotlin-dsl`
    kotlin("jvm") version "1.2.51"
    `maven-publish`
    id ("com.gradle.plugin-publish") version "0.9.10"
}

group = "edu.wpi.first.desktop"
version = "0.1.0"

println(project.properties["pubVersion"])

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    compileOnly(gradleApi())
    compile(kotlin("stdlib-jdk8"))
    compile("com.github.jengelman.gradle.plugins:shadow:2.0.3")
    testCompileOnly(gradleTestKit())

    fun junitJupiter(name: String, version: String = "5.2.0") =
            create(group = "org.junit.jupiter", name = name, version = version)
    compileOnly(create(group = "com.google.code.findbugs", name = "annotations", version = "3.0.1"))
    testCompile(junitJupiter(name = "junit-jupiter-api"))
    testCompile(junitJupiter(name = "junit-jupiter-engine"))
    testCompile(junitJupiter(name = "junit-jupiter-params"))
    testRuntime(create(group = "org.junit.platform", name = "junit-platform-launcher", version = "1.0.0"))
}


configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_1_8
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

tasks.withType<Test> {
    useJUnitPlatform()
}

gradlePlugin {
    plugins {
        create("PlatformReleases") {
            id = "edu.wpi.first.desktop.PlatformReleases"
            displayName = "Platform releases"
            implementationClass = "edu.wpi.first.desktop.PlatformReleasesPlugin"
            description = "A Gradle plugin for handling platform-specific dependencies and releases."
        }
    }
}

pluginBundle {
    val plugin = gradlePlugin.plugins["PlatformReleases"]

    website = "https://github.com/wpilibsuite/platform-releases"
    vcsUrl = "https://github.com/wpilibsuite/platform-releases"
    tags = listOf("javafx")

    plugins {
        create("PlatformReleases") {
            id = plugin.id
            displayName = plugin.displayName
            version = project.version.toString()
            description = plugin.description
        }
    }
}
