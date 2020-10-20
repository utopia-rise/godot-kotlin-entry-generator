package godot.entrygenerator.generator.property

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.MemberName.Companion.member
import godot.entrygenerator.EntryGenerationType
import godot.entrygenerator.extension.assignmentPsi
import godot.entrygenerator.extension.toKtVariantConversionFunctionName
import godot.entrygenerator.extension.toKtVariantType
import godot.entrygenerator.generator.property.defaultvalue.DefaultValueExtractorProvider
import godot.entrygenerator.generator.property.hintstring.PropertyHintStringGeneratorProvider
import godot.entrygenerator.generator.property.typehint.PropertyTypeHintProvider
import org.jetbrains.kotlin.backend.common.serialization.findPackage
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.js.descriptorUtils.getJetTypeFqName
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

    override fun registerReference(className: ClassName, propertyDescriptor: PropertyDescriptor, bindingContext: BindingContext, registerClassControlFlow: FunSpec.Builder) {
        val errorText by lazy {
            "${propertyDescriptor.fqNameSafe} is a KtReference but not registered with the delegate refProperty. KtReferences have to be registered with the delegate refProperty! Example: @RegisterProperty var resourceTest by refProperty(::NavigationMesh)"
        }
        requireNotNull(propertyDescriptor.delegateField) { errorText }
        require(bindingContext.getType(propertyDescriptor.assignmentPsi)?.getJetTypeFqName(false) == "godot.runtime.KtReferenceDelegateProvider") { errorText }

        registerClassControlFlow
            .addStatement(
                "property(%L,·%L,·%L,·%T,·%S,·%T,·%S,·isRef·=·true)",
                getPropertyReference(propertyDescriptor),
                getGetterValueConverterReference(),
                getSetterValueConverterReference(propertyDescriptor),
                propertyDescriptor.type.toKtVariantType(),
                propertyDescriptor.containingDeclaration.fqNameSafe,
                PropertyTypeHintProvider.provide(propertyDescriptor, EntryGenerationType.JVM),
                PropertyHintStringGeneratorProvider.provide(propertyDescriptor, bindingContext, EntryGenerationType.JVM).getHintString()
            )
    }

    override fun registerProperty(className: ClassName, propertyDescriptor: PropertyDescriptor, bindingContext: BindingContext, registerClassControlFlow: FunSpec.Builder) {
        val (defaultValueStringTemplate, defaultValueStringTemplateValues) = DefaultValueExtractorProvider
            .provide(propertyDescriptor, bindingContext, EntryGenerationType.JVM)
            .getDefaultValue(ClassName("godot.core", "KtVariant"))

        registerClassControlFlow
            .addStatement(
                "property(%L,·%L,·%L,·%T,·%S,·%T,·%S,·${defaultValueStringTemplate.replace(" ", "·")})",
                getPropertyReference(propertyDescriptor),
                getGetterValueConverterReference(),
                getSetterValueConverterReference(propertyDescriptor),
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
