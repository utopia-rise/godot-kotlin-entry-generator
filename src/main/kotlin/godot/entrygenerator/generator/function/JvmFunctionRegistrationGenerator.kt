package godot.entrygenerator.generator.function

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.MemberName.Companion.member
import godot.entrygenerator.extension.EntryGeneratorExtension
import godot.entrygenerator.extension.toParameterKtVariantType
import godot.entrygenerator.extension.toReturnKtVariantType
import godot.entrygenerator.model.ClassWithMembers
import org.jetbrains.kotlin.backend.common.serialization.findPackage
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.js.descriptorUtils.getJetTypeFqName
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

class JvmFunctionRegistrationGenerator : FunctionRegistrationGenerator() {

    override fun getStringTemplate(functionDescriptor: FunctionDescriptor): String {
        return getFunctionTemplateString(functionDescriptor)
    }

    override fun getTemplateArgs(functionDescriptor: FunctionDescriptor, className: ClassName): List<Any> {
        val ktFunctionArgumentClassName = ClassName("godot.runtime", "KtFunctionArgument")
        val returnType = functionDescriptor.returnType

        requireNotNull(returnType) {
            "ReturnType cannot be null. Usually this means there was an error in the kotlin compilation. Try a clean build and submit a bug if this does not help"
        }
        return buildList {
            add(getFunctionReference(functionDescriptor))
            add(functionDescriptor.returnType?.toParameterKtVariantType() ?: ClassName("godot.core.VariantType", "NIL"))

            if (functionDescriptor.valueParameters.isNotEmpty()) {
                functionDescriptor.valueParameters.forEach { valueParameter ->
                    add(valueParameter.type.toParameterKtVariantType())
                }
                functionDescriptor.valueParameters.forEach { valueParameter ->
                    add(ktFunctionArgumentClassName)
                    add(valueParameter.type.toReturnKtVariantType())
                    add(valueParameter.type.getJetTypeFqName(false))
                    add(valueParameter.name.asString())
                }
            }

            add(ktFunctionArgumentClassName)
            add(returnType.toReturnKtVariantType())
            add(returnType.getJetTypeFqName(false))
        }
    }

    override fun registerThroughExtensionHelperAbstraction(
        functionDescriptor: FunctionDescriptor,
        className: ClassName,
        extensionToDescriptors: Map<EntryGeneratorExtension, Set<ClassWithMembers>>,
        extensionHelperObjectSpec: TypeSpec.Builder?,
        messageCollector: MessageCollector
    ): Pair<String, Array<Any>> {
        val containingClassPackagePath = functionDescriptor.containingDeclaration.fqNameSafe.parent().asString()
        val containingClassName = functionDescriptor.containingDeclaration.name.asString()

        val returnTypeFqName = functionDescriptor.returnType?.getJetTypeFqName(false) ?: "kotlin.Unit"

        val funSpecBuilder = FunSpec
            .builder(functionDescriptor.name.asString())
            .addParameter("instance", ClassName(containingClassPackagePath, containingClassName))
            .returns(ClassName(returnTypeFqName.substringBeforeLast("."), returnTypeFqName.substringAfterLast(".")))

        functionDescriptor.valueParameters.forEach {
            val fqName = it.type.getJetTypeFqName(false)
            funSpecBuilder.addParameter(it.name.asString(), ClassName(fqName.substringBeforeLast("."), fqName.substringAfterLast(".")))
        }

        extensionToDescriptors.forEach { (extension, _) ->
            if (extension.overridesFunction(functionDescriptor, messageCollector)) {
                funSpecBuilder.addComment("==== START EXTENSION BLOCK generated by ${extension.provideExtensionName()} with class ${extension::class.qualifiedName} ====")
                extension.beforeFunctionBody(funSpecBuilder, functionDescriptor, messageCollector)
                funSpecBuilder.addComment("==== END EXTENSION BLOCK generated by ${extension.provideExtensionName()} with class ${extension::class.qualifiedName} ====")
            }
        }

        val params = buildString {
            functionDescriptor.valueParameters.forEachIndexed { index, value ->
                if (index > 0) {
                    append(",·")
                }
                append(value.name.asString())
            }
        }
        funSpecBuilder.addStatement("val result = instance.${functionDescriptor.name.asString()}($params)")

        extensionToDescriptors.forEach { (extension, _) ->
            if (extension.overridesFunction(functionDescriptor, messageCollector)) {
                funSpecBuilder.addComment("==== START EXTENSION BLOCK generated by ${extension.provideExtensionName()} with class ${extension::class.qualifiedName} ====")
                extension.afterFunctionBody(funSpecBuilder, functionDescriptor, messageCollector)
                funSpecBuilder.addComment("==== END EXTENSION BLOCK generated by ${extension.provideExtensionName()} with class ${extension::class.qualifiedName} ====")
            }
        }

        funSpecBuilder.addStatement("return result")

        extensionHelperObjectSpec!!.addFunction(funSpecBuilder.build())

        return getStringTemplate(functionDescriptor) to getTemplateArgs(functionDescriptor, className).toTypedArray().apply {
            this[0] = getExtensionHelperFunRef(functionDescriptor)
        }
    }

    private fun getExtensionHelperFunRef(functionDescriptor: FunctionDescriptor): CodeBlock {
        return ClassName("godot", "ExtensionHelper")
            .member(functionDescriptor.name.asString())
            .reference()
    }

    private fun getFunctionTemplateString(
        functionDescriptor: FunctionDescriptor
    ): String {
        return buildString {
            append("function(%L,·%T") //functionReference, returnTypeConverterReference

            if (functionDescriptor.valueParameters.isNotEmpty()) {
                functionDescriptor.valueParameters.forEach { param ->
                    append(",·%T") //Variant type
                }
                functionDescriptor.valueParameters.forEach { _ ->
                    append(",·%T(%T,·%S,·%S)") //argument KtFunctionArgument
                }
            }

            append(",·%T(%T,·%S))") //return KtFunctionArgument
        }
    }

    private fun getFunctionReference(functionDescriptor: FunctionDescriptor): CodeBlock {
        return getContainingClassName(functionDescriptor)
            .member(functionDescriptor.name.asString())
            .reference()
    }

    private fun getContainingClassName(functionDescriptor: FunctionDescriptor): ClassName {
        val classPackage = functionDescriptor.containingDeclaration.findPackage().fqName.asString()
        val className = functionDescriptor.containingDeclaration.name.asString()
        return ClassName(classPackage, className)
    }
}
