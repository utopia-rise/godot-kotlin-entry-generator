import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.ajoberstar.grgit.Commit

plugins {
    kotlin("jvm")
    `maven-publish`
    id("org.ajoberstar.grgit") version "4.0.2"
    id("com.utopia-rise.godot-publish")
}

group = "com.utopia-rise"

val baseVersion = "0.1.2"

val currentCommit: Commit = grgit.head()
// check if the current commit is tagged
var tagOnCurrentCommit = grgit.tag.list().firstOrNull { tag -> tag.commit.id == currentCommit.id }
var releaseMode = tagOnCurrentCommit != null

version = if (!releaseMode) {
    "$baseVersion-${DependenciesVersions.godotVersion}-${currentCommit.abbreviatedId}-SNAPSHOT"
} else {
    requireNotNull(tagOnCurrentCommit).name
}

repositories {
    mavenCentral()
    jcenter()
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("org.jetbrains.kotlin:kotlin-compiler-embeddable:1.4.10")
    implementation("com.squareup:kotlinpoet:${DependenciesVersions.kotlinPoetVersion}")
}

tasks {
    build {
        finalizedBy(publishToMavenLocal)
    }

    withType<KotlinCompile>().all {
        kotlinOptions.freeCompilerArgs += "-Xopt-in=kotlin.ExperimentalStdlibApi"
    }
}

publishing {
    publications {
        val godotEntryGenerator by creating(MavenPublication::class) {
            pom {
                name.set(project.name)
                description.set("Godot Kotlin entry code generator.")
            }
            artifactId = project.name
            description = "Godot Kotlin entry code generator."
            from(components.getByName("java"))
        }
    }
}
