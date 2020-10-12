package godot.entrygenerator

import godot.entrygenerator.filebuilder.EntryFileBuilderProvider
import godot.entrygenerator.generator.GdnsGenerator
import godot.entrygenerator.generator.ServiceGenerator
import godot.entrygenerator.transformer.transformTypeDeclarationsToClassWithMembers
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.resolve.BindingContext

object EntryGenerator {

    fun generateEntryFile(
        generationType: EntryGenerationType,
        bindingContext: BindingContext,
        outputPath: String,
        classes: Set<ClassDescriptor>,
        properties: Set<PropertyDescriptor>,
        functions: Set<FunctionDescriptor>,
        signals: Set<PropertyDescriptor>
    ) {
        EntryFileBuilderProvider.provideEntryFileBuilder(generationType, bindingContext)
            .registerClassesWithMembers(
                transformTypeDeclarationsToClassWithMembers(
                    classes,
                    properties,
                    functions,
                    signals
                )
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
}
