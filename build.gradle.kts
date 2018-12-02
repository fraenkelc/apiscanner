import groovy.lang.Closure

plugins {
    `java-library`
    `java-gradle-plugin`
    `kotlin-dsl`
    `maven-publish`
    id("com.gradle.plugin-publish") version "0.10.0"
    id("com.palantir.git-version") version "0.12.0-rc2"
}

val gitVersion: Closure<*> by extra

group = "com.github.fraenkelc"
version = gitVersion(mapOf("prefix" to "version-"))

repositories {
    jcenter()
}

pluginBundle {
    website = "https://github.com/fraenkelc/apiscanner"
    vcsUrl = "https://github.com/fraenkelc/apiscanner"
    description = "Plugin that makes suggestions for api and implementation dependencies based on bytecode analysis."
    tags = listOf("dependency-management", "implementation", "api")
    (plugins) {
        "com.github.fraenkelc.apiscanner.ApiScannerPlugin" {
            displayName = "Gradle apiscanner plugin"
        }
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

dependencies {
    implementation(group = "org.ow2.asm", name = "asm", version = "7.0")
    implementation(group = "org.ow2.asm", name = "asm-tree", version = "7.0")

    implementation(kotlin("stdlib"))

    testImplementation(kotlin("test-junit5"))
    testImplementation(group = "org.junit.jupiter", name = "junit-jupiter-api", version = "5.3.2")
    testImplementation(group = "org.junit.jupiter", name = "junit-jupiter-params", version = "5.3.2")
    testImplementation(group = "org.junit-pioneer", name = "junit-pioneer", version = "0.3.0")
    testRuntimeOnly(group = "org.junit.jupiter", name = "junit-jupiter-engine", version = "5.3.2")

    testImplementation(gradleTestKit())
}