import org.gradle.api.tasks.testing.logging.TestLogEvent.*

group = "top.sspirits.blog"
version = "1.0.0-SNAPSHOT"

val jvmTargetVersion = "11"
val kotlinVersion by extra("1.3.72")
val log4j2Version by extra("2.13.1")
val koinVersion by extra("2.1.5")

plugins {
    kotlin("jvm") version "1.3.72"
    id("io.vertx.vertx-plugin") version "1.1.1"
}

repositories {
    mavenCentral()
    jcenter()
}

application {
    mainClassName = "io.vertx.core.Launcher"
}

vertx {
    mainVerticle = "top.sspirits.blog.MainVerticle"
    vertxVersion = "4.0.0-milestone5"
    debugSuspend = true
}

dependencies {
    implementation(kotlin("stdlib-jdk8", kotlinVersion))
    implementation(kotlin("reflect", kotlinVersion))

    implementation("io.vertx:vertx-web")
    implementation("io.vertx:vertx-lang-kotlin-coroutines")
    implementation("io.vertx:vertx-lang-kotlin")

    implementation("org.apache.logging.log4j:log4j-slf4j18-impl:$log4j2Version")
    implementation("org.apache.commons:commons-lang3:3.11")
    implementation("org.koin:koin-core:$koinVersion")
    implementation("org.koin:koin-logger-slf4j:$koinVersion")

    testImplementation("io.vertx:vertx-junit5")
    testImplementation("io.vertx:vertx-unit")
}

tasks {
    compileJava {
        sourceCompatibility = jvmTargetVersion
        targetCompatibility = jvmTargetVersion
    }

    compileKotlin {
        kotlinOptions {
            freeCompilerArgs = listOf("-Xjsr305=strict")
            jvmTarget = jvmTargetVersion
        }
    }

    test {
        useJUnitPlatform()
        testLogging {
            events = setOf(PASSED, FAILED, SKIPPED)
        }
    }

    jar {
        manifest.attributes["Multi-Release"] = "true"
    }

    clean {
        delete.add("${rootDir}/logs")
    }
}


