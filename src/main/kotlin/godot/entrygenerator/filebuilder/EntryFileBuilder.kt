package godot.entrygenerator.filebuilder

import com.squareup.kotlinpoet.FileSpec
import godot.entrygenerator.model.ClassWithMembers
import org.jetbrains.kotlin.resolve.BindingContext
import java.io.File

abstract class EntryFileBuilder(val bindingContext: BindingContext) {
    protected val entryFileSpec = FileSpec
        .builder("godot", "MainEntry")
        .addComment("THIS FILE IS GENERATED! DO NOT EDIT IT MANUALLY! ALL CHANGES TO IT WILL BE OVERWRITTEN ON EACH BUILD")

    abstract fun registerClassesWithMembers(classesWithMembers: Set<ClassWithMembers>, outputPath: String): EntryFileBuilder

    open fun build(outputPath: String) {
        entryFileSpec.build().writeTo(File(outputPath))
    }
}
