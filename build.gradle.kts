import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    `maven-publish`
    id("org.ajoberstar.grgit") version "4.0.2"
}

group = "com.utopia-rise"
version = "0.1.0-${DependenciesVersions.godotVersion}"

repositories {
    mavenCentral()
    jcenter()
}

dependencies {
    implementation(kotlin("stdlib"))
    compileOnly(kotlin("compiler-embeddable"))
    implementation("com.squareup:kotlinpoet:${DependenciesVersions.kotlinPoetVersion}")
}

tasks {
    val sourceJar by creating(Jar::class) {
        archiveBaseName.set(project.name)
        archiveVersion.set(project.version.toString())
        archiveClassifier.set("sources")
        from(sourceSets["main"].allSource)
    }

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
                groupId = "${project.group}"
                artifactId = project.name
                version = "${project.version}"
            }
            from(components.getByName("java"))
            artifact(tasks.getByName("sourceJar"))
        }
    }
}

val currentCommit = grgit.head()
// check if the current commit is tagged
var releaseMode = grgit.tag.list().firstOrNull { tag -> tag.commit.id == currentCommit.id } != null

project.extra["artifacts"] = arrayOf("godotEntryGenerator")
extra["releaseMode"] = releaseMode


apply {
    plugin(BintrayPublish::class.java)
}
