import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "1.9.24"
    id("org.jetbrains.intellij") version "1.17.4"
}

group = providers.gradleProperty("pluginGroup").get()
version = providers.gradleProperty("pluginVersion").get()

repositories {
    mavenCentral()
}

// See https://github.com/JetBrains/gradle-intellij-plugin
intellij {
    // platformType = RD -> builds/tests against Rider itself instead of IntelliJ IDEA.
    // Requires Rider to be installed locally, or it will be downloaded automatically.
    type.set(providers.gradleProperty("platformType"))
    version.set(providers.gradleProperty("platformVersion"))

    plugins.set(listOf())
}

tasks {
    withType<KotlinCompile> {
        kotlinOptions {
            jvmTarget = "17"
        }
    }

    patchPluginXml {
        sinceBuild.set(providers.gradleProperty("pluginSinceBuild"))
        // Left unset intentionally: keeps the plugin compatible with future
        // Rider versions since no internal/unstable APIs are used.
        untilBuild.set(provider { null })
    }

    signPlugin {
        certificateChain.set(System.getenv("CERTIFICATE_CHAIN"))
        privateKey.set(System.getenv("PRIVATE_KEY"))
        password.set(System.getenv("PRIVATE_KEY_PASSWORD"))
    }

    publishPlugin {
        token.set(System.getenv("PUBLISH_TOKEN"))
    }
}
