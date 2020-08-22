plugins {
    `kotlin-dsl`
}

repositories {
    jcenter()
    mavenCentral()
}

dependencies {
    implementation(kotlin("gradle-plugin", version = "1.3.72"))
    implementation("com.jfrog.bintray.gradle:gradle-bintray-plugin:1.8.5")
}