package godot.entrygenerator.filebuilder

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.TypeSpec
import godot.entrygenerator.EntryGenerationType
import godot.entrygenerator.generator.clazz.ClassRegistrationGeneratorProvider
import godot.entrygenerator.model.ClassWithMembers
import org.jetbrains.kotlin.resolve.BindingContext

class JvmEntryFileBuilder(bindingContext: BindingContext): EntryFileBuilder(bindingContext) {

    override fun registerClassesWithMembers(classesWithMembers: Set<ClassWithMembers>): EntryFileBuilder {
        val entryClassSpec = TypeSpec
            .classBuilder(ClassName("godot", "Entry"))
            .superclass(ClassName("godot.runtime", "Entry"))

        val initFunctionSpec = FunSpec
            .builder("init")
            .receiver(ClassName("godot.runtime.Entry", "Context"))
            .addModifiers(KModifier.OVERRIDE)

        val classRegistryControlFlow = initFunctionSpec
            .beginControlFlow("with(registry)·{") //START: with registry

        ClassRegistrationGeneratorProvider
            .provideClassRegistrationProvider(EntryGenerationType.JVM)
            .registerClasses(classesWithMembers, classRegistryControlFlow, bindingContext)

        classRegistryControlFlow.endControlFlow() //END: with registry

        entryClassSpec.addFunction(initFunctionSpec.build())
        entryFileSpec.addType(entryClassSpec.build())
        return this
    }
}
