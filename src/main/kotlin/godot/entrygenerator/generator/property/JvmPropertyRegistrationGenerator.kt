package godot.entrygenerator.generator.property

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.MemberName.Companion.member
import godot.entrygenerator.EntryGenerationType
import godot.entrygenerator.extension.toParameterKtVariantType
import godot.entrygenerator.extension.toReturnKtVariantType
import godot.entrygenerator.generator.property.defaultvalue.DefaultValueExtractorProvider
import godot.entrygenerator.generator.property.hintstring.PropertyHintStringGeneratorProvider
import godot.entrygenerator.generator.property.typehint.PropertyTypeHintProvider
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.js.descriptorUtils.getJetTypeFqName
import org.jetbrains.kotlin.resolve.BindingContext

class JvmPropertyRegistrationGenerator : PropertyRegistrationGenerator() {
    override fun registerEnumFlag(className: ClassName, propertyDescriptor: PropertyDescriptor, bindingContext: BindingContext, registerClassControlFlow: FunSpec.Builder) {
        val (defaultValueStringTemplate, defaultValueStringTemplateValues) = DefaultValueExtractorProvider
            .provide(propertyDescriptor, bindingContext, EntryGenerationType.JVM)
            .getDefaultValue(null)

        registerClassControlFlow
            .addStatement(
                "enumFlagProperty(%L,·${defaultValueStringTemplate.replace(" ", "·")})",
                getPropertyReference(propertyDescriptor, className),
                *defaultValueStringTemplateValues
            )
    }

    override fun registerEnumList(className: ClassName, propertyDescriptor: PropertyDescriptor, bindingContext: BindingContext, registerClassControlFlow: FunSpec.Builder) {
        TODO("Not yet implemented")
    }

    override fun registerEnum(className: ClassName, propertyDescriptor: PropertyDescriptor, bindingContext: BindingContext, registerClassControlFlow: FunSpec.Builder) {
        val (defaultValueStringTemplate, defaultValueStringTemplateValues) = DefaultValueExtractorProvider
            .provide(propertyDescriptor, bindingContext, EntryGenerationType.JVM)
            .getDefaultValue(null)

        registerClassControlFlow
            .addStatement(
                "enumProperty(%L,·${defaultValueStringTemplate.replace(" ", "·")})",
                getPropertyReference(propertyDescriptor, className),
                *defaultValueStringTemplateValues
            )
    }

    override fun registerProperty(className: ClassName, propertyDescriptor: PropertyDescriptor, bindingContext: BindingContext, registerClassControlFlow: FunSpec.Builder) {
        val (defaultValueStringTemplate, defaultValueStringTemplateValues) = DefaultValueExtractorProvider
            .provide(propertyDescriptor, bindingContext, EntryGenerationType.JVM)
            .getDefaultValue(null)

        registerClassControlFlow
            .addStatement(
                "property(%L,·%T,·%T,·%S,·%T,·%S,·${defaultValueStringTemplate.replace(" ", "·")})",
                getPropertyReference(propertyDescriptor, className),
                propertyDescriptor.type.toParameterKtVariantType(),
                propertyDescriptor.type.toReturnKtVariantType(),
                propertyDescriptor.type.getJetTypeFqName(false),
                PropertyTypeHintProvider.provide(propertyDescriptor, EntryGenerationType.JVM),
                PropertyHintStringGeneratorProvider.provide(propertyDescriptor, bindingContext, EntryGenerationType.JVM).getHintString(),
                *defaultValueStringTemplateValues
            )
    }

    private fun getPropertyReference(propertyDescriptor: PropertyDescriptor, className: ClassName): CodeBlock {
        return className
            .member(propertyDescriptor.name.asString())
            .reference()
    }
}
