plugins {
    id("base")
}

description = "React frontend application"

val bunExecutable = file("${System.getenv("HOME")}/.bun/bin/bun")
    .takeIf { it.exists() }?.absolutePath ?: "bun"

val bunInstall = tasks.register<Exec>("bunInstall") {
    inputs.file("package.json")
    inputs.file("bun.lock")
    outputs.dir("node_modules")
    commandLine(bunExecutable, "install")
}

val buildFrontend = tasks.register<Exec>("buildFrontend") {
    dependsOn(bunInstall)
    inputs.dir("src")
    inputs.file("package.json")
    inputs.file("vite.config.ts")
    inputs.file("tsconfig.json")
    inputs.file("index.html")
    inputs.dir("public")
    outputs.dir("dist")
    commandLine(bunExecutable, "run", "build")
}

val testFrontend = tasks.register<Exec>("testFrontend") {
    dependsOn(bunInstall)
    inputs.dir("src")
    inputs.file("package.json")
    inputs.file("vitest.config.ts")
    val markerFile = layout.buildDirectory.file("test-results/.frontend-tests-passed").get().asFile
    outputs.file(markerFile)
    commandLine(bunExecutable, "run", "test", "--run")
    doLast {
        markerFile.apply {
            parentFile.mkdirs()
            writeText("ok")
        }
    }
}

tasks.named("build") {
    dependsOn(buildFrontend)
}

tasks.named("check") {
    dependsOn(testFrontend)
}

tasks.named<Delete>("clean") {
    delete("dist")
}
