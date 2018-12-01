version = "1.0-SNAPSHOT"

plugins {
    `java-library`
    `java-gradle-plugin`
    `kotlin-dsl`
    `maven-publish`
}

group = "com.github.fraenkelc"
repositories {
    jcenter()
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