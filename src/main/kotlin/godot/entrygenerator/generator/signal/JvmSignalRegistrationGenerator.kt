package godot.entrygenerator.generator.signal

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.MemberName.Companion.member
import godot.entrygenerator.extension.toKtVariantType
import org.jetbrains.kotlin.backend.common.serialization.findPackage
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.js.descriptorUtils.getJetTypeFqName
import org.jetbrains.kotlin.types.KotlinType

class JvmSignalRegistrationGenerator : SignalRegistrationGenerator() {

    override fun registerSignal(propertyDescriptor: PropertyDescriptor, registerClassControlFlow: FunSpec.Builder) {
        val signalArguments = propertyDescriptor
            .type
            .arguments
            .map { it.type }

        registerClassControlFlow
            .addStatement(
                getStringTemplate(signalArguments),
                *getSignalArguments(propertyDescriptor, signalArguments)
            )
    }

    private fun getStringTemplate(signalArguments: List<KotlinType>): String {
        return buildString {
            append("signal(%L") //signalPropertyReference

            //a KtFunctionArgument per signal argument
            signalArguments.forEach { _ ->
                append(",·%T(%T,·%S)")
            }

            append(")") //signal closing
        }
    }

    private fun getSignalArguments(propertyDescriptor: PropertyDescriptor, signalArguments: List<KotlinType>): Array<Any> {
        return buildList {
            add(getPropertyReference(propertyDescriptor)) //signalPropertyReference

            //a KtFunctionArgument per signal argument
            signalArguments.forEach { type ->
                add(ClassName("godot.runtime", "KtFunctionArgument"))
                add(type.toKtVariantType())
                add(type.getJetTypeFqName(false).substringAfterLast("."))
            }
        }.toTypedArray()
    }

    private fun getPropertyReference(propertyDescriptor: PropertyDescriptor): CodeBlock {
        val classPackage = propertyDescriptor.containingDeclaration.findPackage().fqName.asString()
        val className = propertyDescriptor.containingDeclaration.name.asString()
        return ClassName(classPackage, className)
            .member(propertyDescriptor.name.asString())
            .reference()
    }
}
