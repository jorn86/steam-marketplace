plugins {
    application
}

dependencies {
    implementation(project(":core"))
    runtimeOnly("org.slf4j:slf4j-simple:1.7.32")
}

application {
    mainClass.set("org.hertsig.steam.app.AppKt")
}
