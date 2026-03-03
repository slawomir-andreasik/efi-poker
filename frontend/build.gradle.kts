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
    outputs.dir("dist")
    commandLine("bun", "run", "build")
}

val testFrontend = tasks.register<Exec>("testFrontend") {
    dependsOn(bunInstall)
    inputs.dir("src")
    outputs.upToDateWhen { true }
    commandLine("bun", "run", "test", "--run")
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
