import org.openapitools.generator.gradle.plugin.tasks.GenerateTask

plugins {
    id("java-library")
    alias(libs.plugins.openapi.generator)
    alias(libs.plugins.spring.boot) apply false
}

description = "OpenAPI specification, code generation pipeline, and shared API types"

dependencies {
    implementation(platform(org.springframework.boot.gradle.plugin.SpringBootPlugin.BOM_COORDINATES))
    compileOnly("org.springframework:spring-context")
    compileOnly("org.springframework:spring-web")
    compileOnly("jakarta.annotation:jakarta.annotation-api")
    compileOnly("jakarta.validation:jakarta.validation-api")
    compileOnly("jakarta.servlet:jakarta.servlet-api")
    compileOnly("com.fasterxml.jackson.core:jackson-annotations")
    compileOnly("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
    compileOnly(libs.swagger.annotations)
}

val openApiSourceDir = file("src/main/resources/openapi")
val tempSourcesDir = layout.buildDirectory.dir("temp-openapi-sources")
val flattenedDir = layout.buildDirectory.dir("openapi-flattened")
val generatedDir = layout.buildDirectory.dir("generated")

sourceSets {
    main {
        java {
            srcDir(generatedDir.map { it.dir("src/main/java") })
        }
    }
}

openApiGenerate {
    generatorName.set("spring")
    inputSpec.set(flattenedDir.map { "${it.asFile.path}/api-definition.yaml" })
    outputDir.set(generatedDir.map { it.asFile.path })
    apiPackage.set("com.andreasik.efipoker.api")
    modelPackage.set("com.andreasik.efipoker.api.model")

    globalProperties.set(mapOf(
        "apis" to "",
        "models" to "",
        "supportingFiles" to "false"
    ))

    configOptions.set(mapOf(
        "interfaceOnly" to "true",
        "skipDefaultInterface" to "true",
        "useSpringBoot3" to "true",
        "useTags" to "true",
        "openApiNullable" to "false",
        "requestMappingMode" to "api_interface",
        "dateLibrary" to "custom",
        "useBeanValidation" to "true"
    ))

    typeMappings.set(mapOf(
        "DateTime" to "Instant",
        "date-time" to "Instant"
    ))

    importMappings.set(mapOf(
        "Instant" to "java.time.Instant"
    ))
}

tasks.register<Sync>("prepareOpenApiSources") {
    mustRunAfter("cleanGeneratedApi")
    from(openApiSourceDir) {
        include("**/*.yaml", "**/*.yml")
    }
    into(tempSourcesDir)
    rename("api-definition-template.yaml", "api-definition.yaml")
}

tasks.register<GenerateTask>("generateFlattenedSpec") {
    dependsOn("prepareOpenApiSources")

    generatorName.set("openapi-yaml")
    inputSpec.set(tempSourcesDir.map { "${it.asFile.path}/api-definition.yaml" })
    outputDir.set(flattenedDir.map { it.asFile.path })

    configOptions.set(mapOf("outputFile" to "api-definition.yaml"))
    additionalProperties.set(mapOf("flatten" to "true"))

    // Disable build cache - OpenAPI generator reads from tempSourcesDir which
    // is an output of prepareOpenApiSources, so cache key doesn't track actual
    // YAML content changes reliably (see lessons-learned #24)
    outputs.cacheIf { false }
}

tasks.register<Copy>("copyFlattenedSpec") {
    dependsOn("generateFlattenedSpec")
    from(flattenedDir) { include("api-definition.yaml") }
    into("$projectDir/src/main/resources")
}

tasks.named("openApiGenerate") {
    outputs.cacheIf { false }
    dependsOn("cleanGeneratedApi", "generateFlattenedSpec")
    mustRunAfter("cleanGeneratedApi", "generateFlattenedSpec")
}

tasks.named("compileJava") {
    dependsOn("openApiGenerate")
}

tasks.named("processResources") {
    dependsOn("copyFlattenedSpec")
}

tasks.register<GenerateTask>("generateTypescriptClient") {
    dependsOn("generateFlattenedSpec")
    mustRunAfter("generateFlattenedSpec")

    generatorName.set("typescript-fetch")
    inputSpec.set(flattenedDir.map { "${it.asFile.path}/api-definition.yaml" })
    outputDir.set("${project.rootDir}/frontend/src/api/generated")

    configOptions.set(mapOf(
        "typescriptThreePlus" to "true"
    ))

    outputs.cacheIf { false }
}

tasks.register<Delete>("cleanGeneratedApi") {
    delete(tempSourcesDir, flattenedDir, generatedDir)
    // api-definition.yaml is committed - don't delete it (copyFlattenedSpec overwrites it on build)
    delete("${project.rootDir}/frontend/src/api/generated")
}

tasks.named("clean") {
    dependsOn("cleanGeneratedApi")
}

// All Java in this module is generated from OpenAPI - skip Spotless entirely
spotless {
    isEnforceCheck = false
    java {
        targetExclude("build/**")
    }
}
