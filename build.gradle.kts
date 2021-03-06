import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `java`
    `java-gradle-plugin`
    `kotlin-dsl`
    // Makes sure to update the kotlin version below in the test resources as well.
    kotlin("jvm") version "1.2.51"
    `maven-publish`
    id ("com.gradle.plugin-publish") version "0.9.10"
}

group = "edu.wpi.first.desktop"
version = "0.1.0"

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
    testCompile(group = "org.junit-pioneer", name = "junit-pioneer", version = "0.2.2")
    testRuntime(create(group = "org.junit.platform", name = "junit-platform-launcher", version = "1.0.0"))
}

publishing {
    repositories {
        // Work around Gradle TestKit limitations in order to allow for compileOnly dependencies
        maven {
            name = "test"
            url = uri("$buildDir/plugin-test-repository")
        }
    }

    publications {
        create<MavenPublication>("mavenJar") {
            from(components.getByName("java"))
        }
    }
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
        register("PlatformReleases") {
            id = "edu.wpi.first.desktop.platform-releases"
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
        }
    }
}

tasks {
    val publishPluginsToTestRepository by creating {
        dependsOn("publishPluginMavenPublicationToTestRepository")
    }
    val processTestResources: ProcessResources by getting
    val writeTestProperties by creating(WriteProperties::class) {
        outputFile = processTestResources.destinationDir.resolve("test.properties")
        property("version", version)
        property("kotlinVersion", "1.2.51")
    }
    processTestResources.dependsOn(writeTestProperties)
    "test" {
        dependsOn(publishPluginsToTestRepository)
    }
}
