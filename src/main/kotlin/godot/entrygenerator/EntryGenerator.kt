package godot.entrygenerator

import com.squareup.kotlinpoet.*
import godot.entrygenerator.compiler.CompilerEnvironmentProvider
import godot.entrygenerator.filebuilder.EntryFileBuilderProvider
import godot.entrygenerator.generator.GdnsGenerator
import godot.entrygenerator.generator.ServiceGenerator
import godot.entrygenerator.model.ClassWithMembers
import godot.entrygenerator.transformer.transformTypeDeclarationsToClassWithMembers
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import java.io.File

object EntryGenerator {

    fun generateEntryFiles(
        generationType: EntryGenerationType,
        bindingContext: BindingContext,
        outputPath: String,
        classes: Set<ClassDescriptor>,
        properties: Set<PropertyDescriptor>,
        functions: Set<FunctionDescriptor>,
        signals: Set<PropertyDescriptor>
    ) {
        EntryFileBuilderProvider
            .provideMainEntryFileBuilder(generationType, bindingContext)
            .registerClassesWithMembers(
                transformTypeDeclarationsToClassWithMembers(
                    classes,
                    properties,
                    functions,
                    signals
                ),
                outputPath
            )
            .build(outputPath)
    }

    fun generateGdnsFiles(
        outputPath: String,
        gdnLibFilePath: String,
        cleanGeneratedGdnsFiles: Boolean,
        classes: Set<ClassDescriptor>
    ) {
        GdnsGenerator.generateGdnsFiles(outputPath, gdnLibFilePath, cleanGeneratedGdnsFiles, classes)
    }

    fun generateServiceFile(serviceFileDir: String) = ServiceGenerator.generateServiceFile(serviceFileDir)

    fun deleteOldEntryFilesAndReGenerateMainEntryFile(sourceDirs: List<String>, outputPath: String) {
        val userClassesFqNames = CompilerEnvironmentProvider
            .provide(sourceDirs)
            .getSourceFiles()
            .flatMap { ktFile ->
                ktFile
                    .children
                    .filterIsInstance<KtClass>()
                    .mapNotNull { ktClass -> ktClass.fqName?.asString() }
            }

        File(outputPath)
            .walkTopDown()
            .filter { it.isFile && it.exists() && it.extension == "kt" }
            .forEach {
                val fqName = it.absolutePath.removePrefix(outputPath).removePrefix("/godot/").replace("/", ".").removeSuffix("Entry.kt")
                if (!userClassesFqNames.contains(fqName) && it.name != "Entry") {
                    it.delete()
                }
            }

        val mainEntryRegistryControlFlow = FunSpec
            .builder("init")
            .receiver(ClassName("godot.runtime.Entry", "Context"))
            .addModifiers(KModifier.OVERRIDE)

        addCallsToExistingEntryFilesToMainEntryRegistry(outputPath, setOf(), mainEntryRegistryControlFlow)

        FileSpec
            .builder("godot", "Entry")
            .addComment("THIS FILE IS GENERATED! DO NOT EDIT IT MANUALLY! ALL CHANGES TO IT WILL BE OVERWRITTEN ON EACH BUILD")
            .addType(
                TypeSpec
                    .classBuilder(ClassName("godot", "Entry"))
                    .superclass(ClassName("godot.runtime", "Entry"))
                    .addFunction(mainEntryRegistryControlFlow.build())
                    .addFunction(
                        FunSpec
                            .builder("initEngineTypes")
                            .receiver(ClassName("godot.runtime.Entry", "Context"))
                            .addModifiers(KModifier.OVERRIDE)
                            .addStatement("%M()", MemberName("godot", "registerVariantMapping"))
                            .addStatement("%M()", MemberName("godot", "registerEngineTypes"))
                            .addStatement("%M()", MemberName("godot", "registerEngineTypeMethods"))
                            .build()
                    )
                    .build()
            )
            .build()
            .writeTo(File(outputPath))
    }

    internal fun addCallsToExistingEntryFilesToMainEntryRegistry(outputPath: String, classesWithMembersInCurrentCompilationRound: Set<ClassWithMembers>, mainEntryRegistryControlFlow: FunSpec.Builder) {
        val classesFqNamesInCurrentCompilationRound = classesWithMembersInCurrentCompilationRound.map { it.classDescriptor.fqNameSafe.asString() }
        File(outputPath)
            .walkTopDown()
            .filter { it.isFile && it.exists() && it.extension == "kt" }
            .map { entryFile ->
                if (entryFile.name != "Entry") {
                    entryFile.absolutePath.removePrefix(outputPath).removePrefix("/godot/").replace("/", ".").removeSuffix("Entry.kt")
                } else null
            }
            .filterNotNull()
            .filter { classFqName -> !classesFqNamesInCurrentCompilationRound.contains(classFqName) }
            .forEach { classFqName ->
                val packagePath = classFqName.substringBeforeLast(".")
                val classNameAsString = classFqName.substringAfterLast(".")
                mainEntryRegistryControlFlow.addStatement("%T.register(registry)", ClassName("godot.$packagePath", "${classNameAsString}Registrar"))
            }
    }
}
