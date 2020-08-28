package godot.entrygenerator.generator

import java.io.File

object ServiceGenerator {
    fun generateServiceFile(projectRoot: String) {
        val serviceFile = File("$projectRoot/src/main/resources/META-INF/services/godot.runtime.Entry")
        serviceFile.mkdirs()
        serviceFile.writeText("godot.Entry")
    }
}