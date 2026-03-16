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
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
        jvmArgs("-Xmx512m", "-XX:+UseG1GC")
        maxParallelForks = 2
        testLogging {
            events("passed", "skipped", "failed")
        }
    }

    val testTask = tasks.named<Test>("test")

    tasks.register<Test>("unitTest") {
        description = "Run unit tests only (no Spring context, fast feedback)"
        group = "verification"
        testClassesDirs = testTask.get().testClassesDirs
        classpath = testTask.get().classpath
        useJUnitPlatform { includeTags("unit") }
        jvmArgs("-Xmx256m", "-XX:+UseG1GC")
    }

    tasks.register<Test>("integrationTest") {
        description = "Run integration + module tests (Spring context, Testcontainers)"
        group = "verification"
        testClassesDirs = testTask.get().testClassesDirs
        classpath = testTask.get().classpath
        useJUnitPlatform { includeTags("component", "module") }
        jvmArgs("-Xmx512m", "-XX:+UseG1GC")
        maxParallelForks = 1
        shouldRunAfter("unitTest")
    }

}
