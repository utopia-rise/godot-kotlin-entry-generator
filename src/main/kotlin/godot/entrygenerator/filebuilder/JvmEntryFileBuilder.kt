package godot.entrygenerator.filebuilder

import com.squareup.kotlinpoet.*
import godot.entrygenerator.EntryGenerationType
import godot.entrygenerator.EntryGenerator
import godot.entrygenerator.generator.clazz.ClassRegistrationGeneratorProvider
import godot.entrygenerator.model.ClassWithMembers
import org.jetbrains.kotlin.resolve.BindingContext

class JvmEntryFileBuilder(bindingContext: BindingContext): EntryFileBuilder(bindingContext) {

    override fun registerClassesWithMembers(classesWithMembers: Set<ClassWithMembers>, outputPath: String): EntryFileBuilder {
        val entryClassSpec = TypeSpec
            .classBuilder(ClassName("godot", "Entry"))
            .superclass(ClassName("godot.runtime", "Entry"))

        val initFunctionSpec = FunSpec
            .builder("init")
            .receiver(ClassName("godot.runtime.Entry", "Context"))
            .addModifiers(KModifier.OVERRIDE)

        val initEngineTypesFunSpec = FunSpec
                .builder("initEngineTypes")
                .receiver(ClassName("godot.runtime.Entry", "Context"))
                .addModifiers(KModifier.OVERRIDE)

        val mainEntryRegistryControlFlow = initFunctionSpec
            .beginControlFlow("with(registry)·{") //START: with registry

        EntryGenerator.addCallsToExistingEntryFilesToMainEntryRegistry(outputPath, mainEntryRegistryControlFlow)

        ClassRegistrationGeneratorProvider
            .provideClassRegistrationProvider(EntryGenerationType.JVM)
            .registerClasses(classesWithMembers, mainEntryRegistryControlFlow, bindingContext, outputPath)

        initEngineTypesFunSpec.addStatement("%M()", MemberName("godot", "registerVariantMapping"))
        initEngineTypesFunSpec.addStatement("%M()", MemberName("godot", "registerEngineTypes"))
        initEngineTypesFunSpec.addStatement("%M()", MemberName("godot", "registerEngineTypeMethods"))

        mainEntryRegistryControlFlow.endControlFlow() //END: with registry

        entryClassSpec.addFunction(initFunctionSpec.build())
        entryClassSpec.addFunction(initEngineTypesFunSpec.build())
        entryFileSpec.addType(entryClassSpec.build())
        return this
    }
}
