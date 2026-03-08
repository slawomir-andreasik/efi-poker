plugins {
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.hibernate.orm)
    id("jacoco")
}

description = "Spring Boot REST API with PostgreSQL and Spring Modulith"

hibernate {
    enhancement {
        enableLazyInitialization = true
        enableDirtyTracking = true
        enableAssociationManagement = false
        enableExtendedEnhancement = false
    }
}

dependencies {
    implementation(platform(org.springframework.boot.gradle.plugin.SpringBootPlugin.BOM_COORDINATES))
    developmentOnly(platform(org.springframework.boot.gradle.plugin.SpringBootPlugin.BOM_COORDINATES))
    implementation(platform(libs.spring.modulith.bom))
    implementation(project(":api"))
    compileOnly(libs.swagger.annotations)
    testCompileOnly(libs.swagger.annotations)

    // Spring Boot + Modulith
    implementation("org.springframework.modulith:spring-modulith-starter-core")
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-security-oauth2-client")
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    // Jackson 2 bridge (OpenAPI Generator, Logstash Logback require Jackson 2)
    implementation("org.springframework.boot:spring-boot-jackson2")

    // Observability - Metrics + Tracing
    implementation("io.micrometer:micrometer-registry-prometheus")
    implementation("org.springframework.boot:spring-boot-starter-opentelemetry")

    // Structured Logging
    implementation(libs.logstash.logback)

    developmentOnly("org.springframework.boot:spring-boot-docker-compose")

    // Database
    runtimeOnly("org.postgresql:postgresql")
    implementation("org.springframework.boot:spring-boot-starter-liquibase")

    // MapStruct
    implementation(libs.mapstruct)
    annotationProcessor(libs.mapstruct.processor)
    annotationProcessor(libs.swagger.annotations)

    // JWT (Spring Security OAuth2 Resource Server + Nimbus JOSE)
    implementation("org.springframework.boot:spring-boot-starter-security-oauth2-resource-server")

    // Lombok
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)
    annotationProcessor(libs.lombok.mapstruct.binding)
    testCompileOnly(libs.lombok)
    testAnnotationProcessor(libs.lombok)

    // Test
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation(libs.testcontainers)
    testImplementation(libs.testcontainers.postgresql)
    testImplementation(libs.archunit)
    testImplementation("org.springframework.modulith:spring-modulith-starter-test")
    testRuntimeOnly("org.postgresql:postgresql")
}

jacoco {
    toolVersion = libs.versions.jacoco.get()
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required.set(true)
        html.required.set(!providers.environmentVariable("CI").isPresent)
    }
}

tasks.jacocoTestCoverageVerification {
    violationRules {
        rule {
            limit {
                minimum = "0.50".toBigDecimal()
            }
        }
    }
}

tasks.named("check") {
    dependsOn("jacocoTestCoverageVerification")
}

springBoot {
    buildInfo()
}

tasks.bootRun {
    workingDir = rootDir
}

tasks.named<org.springframework.boot.gradle.tasks.bundling.BootBuildImage>("bootBuildImage") {
    imageName.set("ghcr.io/slawomir-andreasik/efi-poker/backend:${project.version}")
    builder.set("paketobuildpacks/ubuntu-noble-builder:latest")
    buildpacks.set(listOf(
        "urn:cnb:builder:paketo-buildpacks/java",
        "docker://docker.io/paketobuildpacks/health-checker"
    ))
    environment.set(mapOf(
        "BP_JVM_CDS_ENABLED" to "true",
        "BP_JVM_VERSION" to "25",
        "BPL_JVM_THREAD_COUNT" to "50", // CDS training run only; runtime set in docker-compose.prod.yml
        "BP_HEALTH_CHECKER_ENABLED" to "true",
        "THC_PORT" to "9091",
        "THC_PATH" to "/actuator/health",
        "TRAINING_RUN_JAVA_TOOL_OPTIONS" to listOf(
            "-Dspring.liquibase.enabled=false",
            "-Dspring.jpa.hibernate.ddl-auto=none",
            "-Dspring.sql.init.mode=never",
            "-Dspring.jpa.properties.hibernate.boot.allow_jdbc_metadata_access=false",
            "-Dspring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect",
            "-Dspring.profiles.active=prod",
        ).joinToString(" "),
    ))
    docker {
        publishRegistry {
            url.set("ghcr.io")
            username.set(providers.environmentVariable("GHCR_USERNAME").getOrElse("slawomir-andreasik"))
            password.set(providers.environmentVariable("GHCR_TOKEN").getOrElse(""))
        }
    }
}
