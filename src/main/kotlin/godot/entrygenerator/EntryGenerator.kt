package godot.entrygenerator

import godot.entrygenerator.extension.DescriptorContainer
import godot.entrygenerator.extension.EntryGeneratorExtension
import godot.entrygenerator.filebuilder.EntryFileBuilderProvider
import godot.entrygenerator.generator.GdnsGenerator
import godot.entrygenerator.generator.ServiceGenerator
import godot.entrygenerator.transformer.transformTypeDeclarationsToClassWithMembers
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.resolve.BindingContext

object EntryGenerator {

    fun generateEntryFile(
        generationType: EntryGenerationType,
        bindingContext: BindingContext,
        messageCollector: MessageCollector,
        outputPath: String,
        registrationContainer: DescriptorContainer,
        extensionToDescriptors: Map<EntryGeneratorExtension, DescriptorContainer>
    ) {
        EntryFileBuilderProvider.provideEntryFileBuilder(generationType, bindingContext)
            .registerClassesWithMembers(
                transformTypeDeclarationsToClassWithMembers(
                    registrationContainer.classes,
                    registrationContainer.properties,
                    registrationContainer.functions,
                    registrationContainer.signals
                ),
                extensionToDescriptors
                    .map {
                        it.key to transformTypeDeclarationsToClassWithMembers(
                            it.value.classes,
                            it.value.properties,
                            it.value.functions,
                            it.value.properties
                        )
                    }
                    .toMap(),
                messageCollector
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
