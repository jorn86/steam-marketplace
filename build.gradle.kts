plugins {
    kotlin("jvm")
}

repositories {
    mavenCentral()
}

subprojects {
    apply(plugin = "java-library")
    apply(plugin = "org.jetbrains.kotlin.jvm")

    repositories {
        mavenCentral()
    }

    dependencies {
        implementation("org.slf4j:slf4j-api:1.7.32")
        implementation("com.google.guava:guava:31.0.1-jre")
        implementation(platform("org.jetbrains.kotlin:kotlin-bom"))
        implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
        testImplementation("org.junit.jupiter:junit-jupiter:5.7.2")
    }

    tasks.named<Test>("test") {
        useJUnitPlatform()
    }
}
