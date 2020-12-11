package godot.entrygenerator.filebuilder

import com.squareup.kotlinpoet.*
import godot.entrygenerator.EntryGenerationType
import godot.entrygenerator.extension.EntryGeneratorExtension
import godot.entrygenerator.generator.clazz.ClassRegistrationGeneratorProvider
import godot.entrygenerator.model.ClassWithMembers
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.resolve.BindingContext

class KotlinNativeEntryFileBuilder(bindingContext: BindingContext): EntryFileBuilder(bindingContext) {

    init {
        entryFileSpec
            .addAnnotation(
                AnnotationSpec
                    .builder(ClassName("kotlin", "Suppress"))
                    .addMember("%S", "EXPERIMENTAL_API_USAGE")
                    .build()
            )
            .addFunction(generateGDNativeInitFunction())
            .addFunction(generateGDNativeTerminateFunction())
    }

    private val nativeScriptInitFunctionSpec = FunSpec
        .builder("NativeScriptInit")
        .addAnnotation(
            AnnotationSpec
                .builder(ClassName("kotlin.native", "CName"))
                .addMember("%S", "godot_nativescript_init")
                .build()
        )
        .addParameter("handle", ClassName("kotlinx.cinterop", "COpaquePointer"))
        .addStatement("%T.nativescriptInit(handle)", ClassName("godot.core", "Godot"))

    override fun registerClassesWithMembers(classesWithMembers: Set<ClassWithMembers>, extensionToDescriptors: Map<EntryGeneratorExtension, Set<ClassWithMembers>>, messageCollector: MessageCollector): EntryFileBuilder {
        val extensionHelperObjectSpec = getExtensionHelperObjectSpec(extensionToDescriptors, classesWithMembers, messageCollector)

        val classRegistryControlFlow = nativeScriptInitFunctionSpec
            .beginControlFlow(
                "with(%T(handle))·{",
                ClassName("godot.core", "ClassRegistry")
            ) //START: with ClassRegistry

        ClassRegistrationGeneratorProvider
            .provideClassRegistrationProvider(EntryGenerationType.KOTLIN_NATIVE)
            .registerClasses(classesWithMembers, classRegistryControlFlow, bindingContext, messageCollector, extensionToDescriptors, extensionHelperObjectSpec)

        classRegistryControlFlow.endControlFlow() //END: with ClassRegistry

        extensionHelperObjectSpec?.let { entryFileSpec.addType(it.build()) }
        return this
    }

    override fun build(outputPath: String) {
        entryFileSpec.addFunction(nativeScriptInitFunctionSpec.build())
        entryFileSpec.addFunction(generateGDNativeScriptTerminateFunction())
        super.build(outputPath)
    }

    private fun generateGDNativeInitFunction(): FunSpec {
        return FunSpec
            .builder("GDNativeInit")
            .addAnnotation(
                AnnotationSpec
                    .builder(ClassName("kotlin.native", "CName"))
                    .addMember("%S", "godot_gdnative_init")
                    .build()
            )
            .addParameter("options", ClassName("godot.gdnative", "godot_gdnative_init_options"))
            .addStatement("%T.init(options)", ClassName("godot.core", "Godot"))
            .build()
    }

    private fun generateGDNativeTerminateFunction(): FunSpec {
        return FunSpec
            .builder("GDNativeTerminate")
            .addAnnotation(
                AnnotationSpec
                    .builder(ClassName("kotlin.native", "CName"))
                    .addMember("%S", "godot_gdnative_terminate")
                    .build()
            )
            .addParameter("options", ClassName("godot.gdnative", "godot_gdnative_terminate_options"))
            .addStatement("%T.terminate(options)", ClassName("godot.core", "Godot"))
            .build()
    }

    private fun generateGDNativeScriptTerminateFunction(): FunSpec {
        return FunSpec
            .builder("NativeScriptTerminate")
            .addAnnotation(
                AnnotationSpec
                    .builder(ClassName("kotlin.native", "CName"))
                    .addMember("%S", "godot_nativescript_terminate")
                    .build()
            )
            .addParameter("handle", ClassName("kotlinx.cinterop", "COpaquePointer"))
            .addStatement("%T.nativescriptTerminate(handle)", ClassName("godot.core", "Godot"))
            .build()
    }
}
