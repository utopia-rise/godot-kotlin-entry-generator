package godot.entrygenerator.filebuilder

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.TypeSpec
import godot.entrygenerator.extension.EntryGeneratorExtension
import godot.entrygenerator.model.ClassWithMembers
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.resolve.BindingContext
import java.io.File

abstract class EntryFileBuilder(val bindingContext: BindingContext) {
    protected val entryFileSpec = FileSpec
        .builder("godot", "Entry")
        .addComment("THIS FILE IS GENERATED! DO NOT EDIT IT MANUALLY! ALL CHANGES TO IT WILL BE OVERWRITTEN ON EACH BUILD")

    abstract fun registerClassesWithMembers(classesWithMembers: Set<ClassWithMembers>, extensionToDescriptors: Map<EntryGeneratorExtension, Set<ClassWithMembers>>, messageCollector: MessageCollector): EntryFileBuilder

    open fun build(outputPath: String) {
        entryFileSpec.build().writeTo(File(outputPath))
    }

    fun getExtensionHelperObjectSpec(extensionToDescriptors: Map<EntryGeneratorExtension, Set<ClassWithMembers>>, registeredClassesWithMembers: Set<ClassWithMembers>, messageCollector: MessageCollector): TypeSpec.Builder? {
        val aExtensionOverridesMethod = extensionToDescriptors
            .any { (extension, classesWithMembers) ->
                classesWithMembers.any { classWithMembers -> //regular registration annotated functions
                    classWithMembers.functions.any { functionDescriptor ->
                        extension.overridesFunction(functionDescriptor, messageCollector)
                    }
                }
                    || registeredClassesWithMembers.any { classWithMembers -> //extension specific annotated functions
                    classWithMembers.functions.any { functionDescriptor ->
                        extension.overridesFunction(functionDescriptor, messageCollector)
                    }
                }
            }

        return if (aExtensionOverridesMethod) {
            TypeSpec
                .objectBuilder(ClassName("godot", "ExtensionHelper"))
                .addModifiers(KModifier.PRIVATE)
        } else null
    }
}
