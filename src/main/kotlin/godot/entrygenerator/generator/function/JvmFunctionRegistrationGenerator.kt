package godot.entrygenerator.generator.function

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.MemberName.Companion.member
import godot.entrygenerator.extension.toKtVariantConversionFunctionName
import godot.entrygenerator.extension.toKtVariantType
import org.jetbrains.kotlin.backend.common.serialization.findPackage
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
            add(getReturnValueConverterReference(functionDescriptor))

            if (functionDescriptor.valueParameters.isNotEmpty()) {
                functionDescriptor.valueParameters.forEach { valueParameter ->
                    val ktVariantClassName = ClassName("godot.core", "KtVariant")
                    val conversionFunction = ktVariantClassName.member(valueParameter.type.toKtVariantConversionFunctionName()).reference()
                    add(conversionFunction)
                }
                functionDescriptor.valueParameters.forEach { valueParameter ->
                    add(ktFunctionArgumentClassName)
                    add(valueParameter.type.toKtVariantType())
                    add(valueParameter.type.getJetTypeFqName(false).substringAfterLast("."))
                    add(valueParameter.name.asString())
                }
            }

            add(ktFunctionArgumentClassName)
            add(returnType.toKtVariantType())
            add(returnType.getJetTypeFqName(false).substringAfterLast("."))
        }
    }

    private fun getFunctionTemplateString(
        functionDescriptor: FunctionDescriptor
    ): String {
        return buildString {
            append("function(%L,·%L") //functionReference, returnTypeConverterReference

            if (functionDescriptor.valueParameters.isNotEmpty()) {
                functionDescriptor.valueParameters.forEach { _ ->
                    append(",·%L") //type mapping function
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
        return ClassName("godot.core", "KtVariant")
            .constructorReference()
    }

    private fun getContainingClassName(functionDescriptor: FunctionDescriptor): ClassName {
        val classPackage = functionDescriptor.containingDeclaration.findPackage().fqName.asString()
        val className = functionDescriptor.containingDeclaration.name.asString()
        return ClassName(classPackage, className)
    }

//    private fun getReturnsDsl(functionDescriptor: FunctionDescriptor): CodeBlock {
//        val returnsType = getReturnType(functionDescriptor)
//        return CodeBlock
//            .builder()
//            .beginControlFlow("returns =")
//            .addStatement("type = %T", returnsType)
//            .addStatement("className = %S", requireNotNull(functionDescriptor.returnType?.getJetTypeFqName(false)?.substringAfterLast(".")) { "ReturnType cannot be null. Usually this means there was an error in the kotlin compilation. Try a clean build and submit a bug if this does not help" })
//            .endControlFlow()
//            .build()
//    }

//    private fun getReturnType(functionDescriptor: FunctionDescriptor): ClassName {
//        return functionDescriptor
//            .returnType
//            .toKtVariantType()
//    }

//    private fun getArgsDsl(functionDescriptor: FunctionDescriptor): CodeBlock {
//        val args = functionDescriptor.valueParameters
//
//        val argsCodeBlock = CodeBlock
//            .builder()
//
//        if (args.size != 1) {
//            argsCodeBlock.addStatement("args = arrayOf(")
//        }
//
//        args.forEachIndexed { index, argument ->
//            requireNotNull(argument) { "An argument type of function ${functionDescriptor.fqNameSafe} is null. This means there was an error in the type resolving in the compilation process. Try a clean build and submit a bug if this does not help" }
//            if (index != 0) {
//                argsCodeBlock.add(",")
//            }
//            val controlFlowString = if (args.size == 1) {
//                "arg ="
//            } else {
//                ""
//            }
//            argsCodeBlock
//                .beginControlFlow(controlFlowString)
//                .addStatement("name = %S", argument.name)
//                .addStatement("type = %T", argument.type.toKtVariantType())
//                .addStatement("className = %S", argument.type.getJetTypeFqName(false).substringAfterLast("."))
//                .endControlFlow()
//        }
//
//        if (args.size != 1) {
//            argsCodeBlock.addStatement(")")
//        }
//
//        return argsCodeBlock.build()
//    }
}
