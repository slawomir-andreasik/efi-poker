plugins {
    alias(libs.plugins.spring.boot) apply false
    alias(libs.plugins.spotless) apply false
}

description = "Sprint Planning Poker for Scrum teams"

val appVersion = file("version.properties").readText().trim().substringAfter("=")

allprojects {
    group = "com.andreasik"
    version = appVersion // version.properties is the literal source of truth (SNAPSHOT on dev, release on main)
}

configure(subprojects.filter { it.name != "frontend" }) {
    apply(plugin = "java")
    apply(plugin = "com.diffplug.spotless")

    configure<JavaPluginExtension> {
        toolchain {
            languageVersion = JavaLanguageVersion.of(25)
        }
    }

    configure<com.diffplug.gradle.spotless.SpotlessExtension> {
        java {
            googleJavaFormat()
            removeUnusedImports()
            trimTrailingWhitespace()
            endWithNewline()
        }
        format("misc") {
            target("*.md", ".gitignore")
            trimTrailingWhitespace()
            endWithNewline()
        }
    }

    tasks.withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
        options.compilerArgs.addAll(listOf("-parameters"))
        options.isFork = true
        options.forkOptions.memoryMaximumSize = "512m"
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
        jvmArgs("-Xmx512m", "-XX:+UseG1GC")
        maxParallelForks = (Runtime.getRuntime().availableProcessors() / 2).coerceAtLeast(1)
        testLogging {
            events("passed", "skipped", "failed")
        }
    }

}
