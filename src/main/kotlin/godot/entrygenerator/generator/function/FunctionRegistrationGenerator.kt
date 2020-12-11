package godot.entrygenerator.generator.function

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.TypeSpec
import godot.entrygenerator.extension.EntryGeneratorExtension
import godot.entrygenerator.extension.getAnnotationValue
import godot.entrygenerator.model.ClassWithMembers
import godot.entrygenerator.model.REGISTER_FUNCTION_ANNOTATION
import godot.entrygenerator.model.REGISTER_FUNCTION_ANNOTATION_RPC_MODE_ARGUMENT
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

abstract class FunctionRegistrationGenerator {

    protected abstract fun getStringTemplate(functionDescriptor: FunctionDescriptor): String
    protected abstract fun getTemplateArgs(functionDescriptor: FunctionDescriptor, className: ClassName): List<Any>
    protected abstract fun registerThroughExtensionHelperAbstraction(
        functionDescriptor: FunctionDescriptor,
        className: ClassName,
        extensionToDescriptors: Map<EntryGeneratorExtension, Set<ClassWithMembers>>,
        extensionHelperObjectSpec: TypeSpec.Builder?,
        messageCollector: MessageCollector
    ): Pair<String, Array<Any>>

    fun registerFunctions(
        functions: List<FunctionDescriptor>,
        registerClassControlFlow: FunSpec.Builder,
        className: ClassName,
        extensionToDescriptors: Map<EntryGeneratorExtension, Set<ClassWithMembers>>,
        extensionHelperObjectSpec: TypeSpec.Builder?,
        messageCollector: MessageCollector
    ) {
        functions.forEach { functionDescriptor ->
            if (extensionToDescriptors.any { it.key.overridesFunction(functionDescriptor, messageCollector) }) {
                val (template, args) = registerThroughExtensionHelperAbstraction(functionDescriptor, className, extensionToDescriptors, extensionHelperObjectSpec, messageCollector)
                registerClassControlFlow
                    .addStatement(
                        template,
                        *args
                    )
            } else {
                registerClassControlFlow
                    .addStatement(
                        getStringTemplate(functionDescriptor),
                        *getTemplateArgs(functionDescriptor, className).toTypedArray()
                    )
            }
        }
    }

    protected fun getRpcModeEnum(functionDescriptor: FunctionDescriptor): ClassName {
        val compilerRpcModeEnumRepresentation = getCompilerRpcModeEnumRepresentation(functionDescriptor)
        val packagePath = compilerRpcModeEnumRepresentation.first.asString().replace("/", ".")
        val name = compilerRpcModeEnumRepresentation.second
        return ClassName(packagePath, name.asString())
    }

    private fun getCompilerRpcModeEnumRepresentation(functionDescriptor: FunctionDescriptor): Pair<ClassId, Name> {
        return functionDescriptor
            .annotations
            .getAnnotationValue(
                REGISTER_FUNCTION_ANNOTATION,
                REGISTER_FUNCTION_ANNOTATION_RPC_MODE_ARGUMENT,
                Pair(ClassId(FqName("godot.MultiplayerAPI"), Name.identifier("RPCMode")), Name.identifier("DISABLED"))
            )
    }
}
