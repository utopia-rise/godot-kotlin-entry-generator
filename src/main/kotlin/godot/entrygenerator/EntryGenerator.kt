package godot.entrygenerator

import com.squareup.kotlinpoet.*
import godot.entrygenerator.filebuilder.EntryFileBuilderProvider
import godot.entrygenerator.generator.GdnsGenerator
import godot.entrygenerator.generator.ServiceGenerator
import godot.entrygenerator.model.ClassWithMembers
import godot.entrygenerator.transformer.transformTypeDeclarationsToClassWithMembers
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.config.addKotlinSourceRoots
import org.jetbrains.kotlin.cli.common.messages.*
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.config.addJvmClasspathRoots
import org.jetbrains.kotlin.com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.utils.PathUtil
import java.io.File
import java.util.logging.Logger

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

    fun jvmDeleteOldEntryFilesAndReGenerateMainEntryFile(sourceDirs: List<String>, outputPath: String) {
        val LOG = Logger.getLogger(EntryGenerator::class.java.name)
        val messageCollector = object : MessageCollector {
            private var hasErrors = false
            override fun clear() {
                hasErrors = false
            }

            override fun hasErrors(): Boolean {
                return hasErrors
            }

            override fun report(severity: CompilerMessageSeverity, message: String, location: CompilerMessageSourceLocation?) {
                val text = if (location != null) {
                    val path = location.path
                    val position = "$path: (${location.line}, ${location.column}) "
                    position + message
                } else {
                    message
                }

                when {
                    CompilerMessageSeverity.VERBOSE.contains(severity) -> {
                        LOG.finest(text)
                    }
                    severity == CompilerMessageSeverity.ERROR -> {
                        LOG.severe(text)
                        hasErrors = true
                    }
                    severity == CompilerMessageSeverity.INFO -> {
                        LOG.info(text)
                    }
                    else -> {
                        LOG.warning(text)
                    }
                }
            }
        }
        val configuration = CompilerConfiguration().apply {
            val groupingCollector = GroupingMessageCollector(messageCollector, false)
            val severityCollector = GroupingMessageCollector(groupingCollector, false)
            put(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY, severityCollector)
            addJvmClasspathRoots(PathUtil.getJdkClassesRootsFromCurrentJre())
            addKotlinSourceRoots(sourceDirs)
        }
        val environment = KotlinCoreEnvironment.createForProduction(Disposer.newDisposable(), configuration, EnvironmentConfigFiles.JVM_CONFIG_FILES)
        val userClassesFqNames = environment
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
                if (!userClassesFqNames.contains(fqName) && it.name != "MainEntry") {
                    it.delete()
                }
            }

        val entryFileSpec = FileSpec
            .builder("godot", "MainEntry")
            .addComment("THIS FILE IS GENERATED! DO NOT EDIT IT MANUALLY! ALL CHANGES TO IT WILL BE OVERWRITTEN ON EACH BUILD")

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

        initEngineTypesFunSpec.addStatement("%M()", MemberName("godot", "registerVariantMapping"))
        initEngineTypesFunSpec.addStatement("%M()", MemberName("godot", "registerEngineTypes"))
        initEngineTypesFunSpec.addStatement("%M()", MemberName("godot", "registerEngineTypeMethods"))

        classRegistryControlFlow.endControlFlow() //END: with registry

        entryClassSpec.addFunction(initFunctionSpec.build())
        entryClassSpec.addFunction(initEngineTypesFunSpec.build())
        entryFileSpec.addType(entryClassSpec.build())
        entryFileSpec.build().writeTo(File(outputPath))
    }
}
