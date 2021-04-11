plugins {
    `kotlin-dsl`
}

repositories {
    jcenter()
    mavenCentral()
}

gradlePlugin {
    plugins {
        create("godotPublishPlugin") {
            id = "com.utopia-rise.godot-publish"
            displayName = "Gradle plugin for publishing godot kotlin jvm to maven central"
            implementationClass = "publish.mavencentral.PublishToMavenCentralPlugin"
        }
    }
    isAutomatedPublishing = false
}

dependencies {
    implementation(kotlin("gradle-plugin", version = "1.3.72"))
    implementation("com.jfrog.bintray.gradle:gradle-bintray-plugin:1.8.5")
}
