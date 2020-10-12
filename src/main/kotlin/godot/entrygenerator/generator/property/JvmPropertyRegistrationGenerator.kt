package godot.entrygenerator.generator.property

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.MemberName.Companion.member
import godot.entrygenerator.EntryGenerationType
import godot.entrygenerator.extension.toKtVariantConversionFunctionName
import godot.entrygenerator.extension.toKtVariantType
import godot.entrygenerator.generator.property.hintstring.PropertyHintStringGeneratorProvider
import godot.entrygenerator.generator.property.typehint.PropertyTypeHintProvider
import org.jetbrains.kotlin.backend.common.serialization.findPackage
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.js.descriptorUtils.getJetTypeFqName
import org.jetbrains.kotlin.resolve.BindingContext

class JvmPropertyRegistrationGenerator : PropertyRegistrationGenerator() {
    override fun registerEnumFlag(className: ClassName, propertyDescriptor: PropertyDescriptor, bindingContext: BindingContext, registerClassControlFlow: FunSpec.Builder) {
        TODO("Not yet implemented")
    }

    override fun registerEnumList(className: ClassName, propertyDescriptor: PropertyDescriptor, bindingContext: BindingContext, registerClassControlFlow: FunSpec.Builder) {
        TODO("Not yet implemented")
    }

    override fun registerEnum(className: ClassName, propertyDescriptor: PropertyDescriptor, bindingContext: BindingContext, registerClassControlFlow: FunSpec.Builder) {
        TODO("Not yet implemented")
    }

    override fun registerProperty(className: ClassName, propertyDescriptor: PropertyDescriptor, bindingContext: BindingContext, registerClassControlFlow: FunSpec.Builder) {
        registerClassControlFlow
            .addStatement(
                "property(%L,·%L,·%L,·%T,·%S,·%T,·%S)",
                getPropertyReference(propertyDescriptor),
                getGetterValueConverterReference(),
                getSetterValueConverterReference(propertyDescriptor),
                propertyDescriptor.type.toKtVariantType(),
                propertyDescriptor.type.getJetTypeFqName(false).substringAfterLast("."),
                PropertyTypeHintProvider.provide(propertyDescriptor, EntryGenerationType.JVM),
                PropertyHintStringGeneratorProvider.provide(propertyDescriptor, bindingContext).getHintString()
            )
    }

    private fun getPropertyReference(propertyDescriptor: PropertyDescriptor): CodeBlock {
        return getContainingClassName(propertyDescriptor)
            .member(propertyDescriptor.name.asString())
            .reference()
    }

    private fun getGetterValueConverterReference(): CodeBlock {
        return ClassName("godot.core", "KtVariant")
            .constructorReference()
    }

    private fun getSetterValueConverterReference(propertyDescriptor: PropertyDescriptor): CodeBlock {
        val ktVariantClassName = ClassName("godot.core", "KtVariant")
        return ktVariantClassName.member(propertyDescriptor.type.toKtVariantConversionFunctionName()).reference()
    }

    private fun getContainingClassName(propertyDescriptor: PropertyDescriptor): ClassName {
        val classPackage = propertyDescriptor.containingDeclaration.findPackage().fqName.asString()
        val className = propertyDescriptor.containingDeclaration.name.asString()
        return ClassName(classPackage, className)
    }
}
