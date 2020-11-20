package godot.entrygenerator.generator.function

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.MemberName.Companion.member
import godot.entrygenerator.extension.getCastingStringTemplate
import godot.entrygenerator.extension.getCastingStringTemplateTypeArg
import godot.entrygenerator.extension.toKtVariantType
import org.jetbrains.kotlin.backend.common.serialization.findPackage
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.js.descriptorUtils.getJetTypeFqName
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.asSimpleType
import org.jetbrains.kotlin.types.typeUtil.isFloat
import org.jetbrains.kotlin.types.typeUtil.isInt
import org.jetbrains.kotlin.types.typeUtil.isLong

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
            add(getReturnValueConverterReference(functionDescriptor))

            if (functionDescriptor.valueParameters.isNotEmpty()) {
                functionDescriptor.valueParameters.forEach { valueParameter ->
                    add(valueParameter.type.getCastingStringTemplateTypeArg())
                }
                functionDescriptor.valueParameters.forEach { valueParameter ->
                    add(ktFunctionArgumentClassName)
                    add(valueParameter.type.toKtVariantType())
                    add(valueParameter.type.getJetTypeFqName(false))
                    add(valueParameter.name.asString())
                }
            }

            add(ktFunctionArgumentClassName)
            add(returnType.toKtVariantType())
            add(returnType.getJetTypeFqName(false))
        }
    }

    private fun getFunctionTemplateString(
        functionDescriptor: FunctionDescriptor
    ): String {
        return buildString {
            append("function(%L,·%L") //functionReference, returnTypeConverterReference

            if (functionDescriptor.valueParameters.isNotEmpty()) {
                functionDescriptor.valueParameters.forEach { param ->
                    append(",·{·any:·Any·->·${param.type.getCastingStringTemplate()}·}") //type mapping function
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

    private fun getReturnValueConverterReference(functionDescriptor: FunctionDescriptor): CodeBlock {
        return MemberName("godot.core", "getVariantType")
            .reference()
    }

    private fun getContainingClassName(functionDescriptor: FunctionDescriptor): ClassName {
        val classPackage = functionDescriptor.containingDeclaration.findPackage().fqName.asString()
        val className = functionDescriptor.containingDeclaration.name.asString()
        return ClassName(classPackage, className)
    }
}
