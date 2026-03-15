plugins {
    id("base")
}

description = "React frontend application"

val bunInstall = tasks.register<Exec>("bunInstall") {
    inputs.file("package.json")
    inputs.file("bun.lock")
    outputs.dir("node_modules")
    commandLine("bun", "install")
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
    commandLine("bun", "run", "build")
}

val testFrontend = tasks.register<Exec>("testFrontend") {
    dependsOn(bunInstall)
    inputs.dir("src")
    inputs.file("package.json")
    inputs.file("vitest.config.ts")
    outputs.file(layout.buildDirectory.file("test-results/.frontend-tests-passed"))
    commandLine("bun", "run", "test", "--run")
    doLast {
        layout.buildDirectory.file("test-results/.frontend-tests-passed").get().asFile.apply {
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
