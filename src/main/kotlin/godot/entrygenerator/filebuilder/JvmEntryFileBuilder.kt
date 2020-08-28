package godot.entrygenerator.filebuilder

import godot.entrygenerator.model.ClassWithMembers
import org.jetbrains.kotlin.resolve.BindingContext

class JvmEntryFileBuilder(bindingContext: BindingContext): EntryFileBuilder(bindingContext) {
    override fun registerClassesWithMembers(classesWithMembers: Set<ClassWithMembers>): EntryFileBuilder {
        TODO("Not yet implemented")
    }
}