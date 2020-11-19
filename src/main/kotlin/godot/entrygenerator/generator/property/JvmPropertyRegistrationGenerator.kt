package godot.entrygenerator.generator.property

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.MemberName.Companion.member
import godot.entrygenerator.EntryGenerationType
import godot.entrygenerator.extension.*
import godot.entrygenerator.generator.property.defaultvalue.DefaultValueExtractorProvider
import godot.entrygenerator.generator.property.hintstring.PropertyHintStringGeneratorProvider
import godot.entrygenerator.generator.property.typehint.PropertyTypeHintProvider
import org.jetbrains.kotlin.backend.common.serialization.findPackage
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.js.descriptorUtils.getJetTypeFqName
import org.jetbrains.kotlin.psi.KtCallableReferenceExpression
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

class JvmPropertyRegistrationGenerator : PropertyRegistrationGenerator() {
    override fun registerEnumFlag(className: ClassName, propertyDescriptor: PropertyDescriptor, bindingContext: BindingContext, registerClassControlFlow: FunSpec.Builder) {
        registerClassControlFlow
            .addStatement(
                "enumFlagProperty(%L)",
                getPropertyReference(propertyDescriptor)
            )
    }

    override fun registerEnumList(className: ClassName, propertyDescriptor: PropertyDescriptor, bindingContext: BindingContext, registerClassControlFlow: FunSpec.Builder) {
        TODO("Not yet implemented")
    }

    override fun registerEnum(className: ClassName, propertyDescriptor: PropertyDescriptor, bindingContext: BindingContext, registerClassControlFlow: FunSpec.Builder) {
        registerClassControlFlow
            .addStatement(
                "enumProperty(%L)",
                getPropertyReference(propertyDescriptor)
            )
    }

    override fun registerProperty(className: ClassName, propertyDescriptor: PropertyDescriptor, bindingContext: BindingContext, registerClassControlFlow: FunSpec.Builder) {
        val (defaultValueStringTemplate, defaultValueStringTemplateValues) = DefaultValueExtractorProvider
            .provide(propertyDescriptor, bindingContext, EntryGenerationType.JVM)
            .getDefaultValue(null)

        registerClassControlFlow
            .addStatement(
                "property(%L,·%L,·{·any:·Any·->·${propertyDescriptor.type.getCastingStringTemplate()}·},·%T,·%S,·%T,·%S,·${defaultValueStringTemplate.replace(" ", "·")})",
                getPropertyReference(propertyDescriptor),
                getGetterValueConverterReference(),
                propertyDescriptor.type.getCastingStringTemplateTypeArg(),
                propertyDescriptor.type.toKtVariantType(),
                propertyDescriptor.type.getJetTypeFqName(false),
                PropertyTypeHintProvider.provide(propertyDescriptor, EntryGenerationType.JVM),
                PropertyHintStringGeneratorProvider.provide(propertyDescriptor, bindingContext, EntryGenerationType.JVM).getHintString(),
                *defaultValueStringTemplateValues
            )
    }

    private fun getPropertyReference(propertyDescriptor: PropertyDescriptor): CodeBlock {
        return getContainingClassName(propertyDescriptor)
            .member(propertyDescriptor.name.asString())
            .reference()
    }

    private fun getGetterValueConverterReference(): CodeBlock {
        return MemberName("godot.core", "getVariantType")
            .reference()
    }

    private fun getContainingClassName(propertyDescriptor: PropertyDescriptor): ClassName {
        val classPackage = propertyDescriptor.containingDeclaration.findPackage().fqName.asString()
        val className = propertyDescriptor.containingDeclaration.name.asString()
        return ClassName(classPackage, className)
    }
}
