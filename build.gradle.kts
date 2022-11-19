import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.springframework.boot") version "2.7.5"
    id("io.spring.dependency-management") version "1.1.0"
    kotlin("jvm") version "1.7.20"
    kotlin("kapt") version "1.7.20"
    kotlin("plugin.jpa") version "1.7.20"
    kotlin("plugin.spring") version "1.7.20"
}

group = "com.github"
version = "0.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_17

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

repositories {
    mavenLocal()
    mavenCentral()
    maven { url = uri("https://repo.spring.io/milestone") }
}

dependencies {
    // spring
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")

    // kotlin
    implementation(kotlin("reflect"))
    implementation(kotlin("stdlib-jdk8"))
    implementation("io.projectreactor.kotlin:reactor-kotlin-extensions")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")
    implementation("net.java.dev.jna:jna:5.12.1")

    // reactor
    implementation("io.projectreactor:reactor-tools")
    implementation("io.projectreactor.addons:reactor-extra")

    // html parser
    implementation("org.jsoup:jsoup:1.15.3")

    // open cv
    //implementation("org.bytedeco:opencv-platform:4.6.0-1.5.8")
    implementation("org.bytedeco:opencv:4.6.0-1.5.8")
    implementation("org.bytedeco:opencv:4.6.0-1.5.8:linux-arm64")
    implementation("org.bytedeco:opencv:4.6.0-1.5.8:linux-x86")
    implementation("org.bytedeco:opencv:4.6.0-1.5.8:linux-x86_64")
    //implementation("org.bytedeco:openblas:0.3.21-1.5.8")
    implementation("org.bytedeco:openblas:0.3.21-1.5.8:linux-arm64")
    implementation("org.bytedeco:openblas:0.3.21-1.5.8:linux-x86")
    implementation("org.bytedeco:openblas:0.3.21-1.5.8:linux-x86_64")
    developmentOnly("org.bytedeco:opencv:4.6.0-1.5.8:windows-x86_64")
    developmentOnly("org.bytedeco:openblas:0.3.21-1.5.8:windows-x86_64")

    // sqlite db driver
    implementation("org.xerial:sqlite-jdbc")
    implementation("com.github.gwenn:sqlite-dialect:0.1.2")

    // h2 db driver
    implementation("com.h2database:h2")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("io.kotest:kotest-runner-junit5-jvm:5.5.4")
    testImplementation(kotlin("test"))
    testImplementation("io.projectreactor:reactor-test")

    compileOnly("org.springframework:spring-context-indexer:6.0.0")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
    kapt("org.springframework.boot:spring-boot-configuration-processor")
    kapt("org.springframework:spring-context-indexer")

    developmentOnly("org.springframework.boot:spring-boot-devtools")
}

tasks.withType<Jar> {
    archiveClassifier.set("")
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = "17"
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}