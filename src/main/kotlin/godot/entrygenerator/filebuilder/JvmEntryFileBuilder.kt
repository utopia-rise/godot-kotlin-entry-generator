package godot.entrygenerator.filebuilder

import com.squareup.kotlinpoet.*
import godot.entrygenerator.EntryGenerationType
import godot.entrygenerator.generator.clazz.ClassRegistrationGeneratorProvider
import godot.entrygenerator.model.ClassWithMembers
import org.jetbrains.kotlin.resolve.BindingContext
import java.io.File

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

        val classRegistryControlFlow = initFunctionSpec
            .beginControlFlow("with(registry)·{") //START: with registry

        File(outputPath)
            .walkTopDown()
            .filter { it.isFile && it.exists() && it.extension == "kt" }
            .map { entryFile ->
                if (entryFile.name != "MainEntry") {
                    entryFile.absolutePath.removePrefix(outputPath).removePrefix("/godot/").replace("/", ".").removeSuffix("Entry.kt")
                } else null
            }
            .filterNotNull()
            .forEach { classFqName ->
                val packagePath = classFqName.substringBeforeLast(".")
                val classNameAsString = classFqName.substringAfterLast(".")
                classRegistryControlFlow.addStatement("%T.register(this)", ClassName("godot.$packagePath", "${classNameAsString}Registrar"))
            }

        ClassRegistrationGeneratorProvider
            .provideClassRegistrationProvider(EntryGenerationType.JVM)
            .registerClasses(classesWithMembers, classRegistryControlFlow, bindingContext, outputPath)

        initEngineTypesFunSpec.addStatement("%M()", MemberName("godot", "registerVariantMapping"))
        initEngineTypesFunSpec.addStatement("%M()", MemberName("godot", "registerEngineTypes"))
        initEngineTypesFunSpec.addStatement("%M()", MemberName("godot", "registerEngineTypeMethods"))

        classRegistryControlFlow.endControlFlow() //END: with registry

        entryClassSpec.addFunction(initFunctionSpec.build())
        entryClassSpec.addFunction(initEngineTypesFunSpec.build())
        entryFileSpec.addType(entryClassSpec.build())
        return this
    }
}
