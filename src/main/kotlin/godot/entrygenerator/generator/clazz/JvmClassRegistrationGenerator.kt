package godot.entrygenerator.generator.clazz

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.TypeSpec
import godot.entrygenerator.EntryGenerationType
import godot.entrygenerator.EntryGenerator
import godot.entrygenerator.extension.getAnnotationValue
import godot.entrygenerator.generator.function.FunctionRegistrationGeneratorProvider
import godot.entrygenerator.generator.property.PropertyRegistrationGeneratorProvider
import godot.entrygenerator.generator.signal.SignalRegistrationGeneratorProvider
import godot.entrygenerator.model.ClassWithMembers
import godot.entrygenerator.model.REGISTER_CLASS_ANNOTATION
import godot.entrygenerator.model.REGISTER_CLASS_ANNOTATION_NAME_ARGUMENT
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import godot.entrygenerator.model.RegisteredProperty
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.js.resolve.diagnostics.findPsi
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

class JvmClassRegistrationGenerator : ClassRegistrationGenerator() {

    override fun provideRegisterClassControlFlow(classWithMembers: ClassWithMembers, classRegistryControlFlow: FunSpec.Builder, className: ClassName, superClass: String, godotBaseClass: String, isTool: Boolean): FunSpec.Builder {
        val classNameAsString = getClassNameAsString(classWithMembers.classDescriptor)
        EntryGenerator.registeredClassNames.add(classWithMembers.classDescriptor.fqNameSafe.asString() to classNameAsString)
        classRegistryControlFlow.beginControlFlow(
            "registerClass<%T>(%T::class.qualifiedName!!,·%S,·$isTool,·%S,·%S)·{",
            className,
            className,
            superClass,
            godotBaseClass,
            classNameAsString
        )

        classWithMembers.classDescriptor.constructors.forEach { classConstructorDescriptor ->
            val ctorParamsCount = classConstructorDescriptor.valueParameters.size

            require(ctorParamsCount <= 5) { "A constructor cannot have more than 5 params in Godot! Reduce the param count for constructor:\n${classConstructorDescriptor.findPsi()?.text}" }

            if (ctorParamsCount == 0) {
                classRegistryControlFlow.addStatement("constructor(%T(::%T))", ClassName("godot.core", "KtConstructor$ctorParamsCount"), className)
            } else {
                val templateArgs = mutableListOf<ClassName>()
                val templateString = buildString {
                    classConstructorDescriptor.valueParameters.forEach { valueParameter ->
                        append(",·%T::as${valueParameter.type}")
                        templateArgs.add(ClassName("godot.core", "KtVariant"))
                    }
                }

                classRegistryControlFlow.addStatement("constructor(%T(::%T$templateString))", ClassName("godot.core", "KtConstructor$ctorParamsCount"), className, *templateArgs.toTypedArray())
            }
        }

        return classRegistryControlFlow
    }

    private fun getClassNameAsString(classDescriptor: ClassDescriptor): String {
        val customClassName = classDescriptor.annotations.getAnnotationValue(REGISTER_CLASS_ANNOTATION, REGISTER_CLASS_ANNOTATION_NAME_ARGUMENT, "")
        return if (customClassName.isNotEmpty()) {
            customClassName
        } else {
            val packagePath = classDescriptor.fqNameSafe.parent().asString()
            val simpleName = classDescriptor.fqNameSafe.shortName().asString()
            "${packagePath.replace(".", "_")}_$simpleName".removePrefix("_")
        }
    }

    override fun registerFunctions(functions: List<FunctionDescriptor>, registerClassControlFlow: FunSpec.Builder, className: ClassName) {
        FunctionRegistrationGeneratorProvider
            .provide(EntryGenerationType.JVM)
            .registerFunctions(functions, registerClassControlFlow, className)
    }

    override fun registerSignals(signals: List<PropertyDescriptor>, className: ClassName, registerClassControlFlow: FunSpec.Builder) {
        SignalRegistrationGeneratorProvider
            .provide(EntryGenerationType.JVM)
            .registerSignals(signals, className, registerClassControlFlow)
    }

    override fun registerProperties(
        registeredProperties: MutableList<RegisteredProperty>,
        classSpecificRegistryBuilder: TypeSpec.Builder,
        registerClassControlFlow: FunSpec.Builder,
        className: ClassName,
        bindingContext: BindingContext
    ) {
        PropertyRegistrationGeneratorProvider
            .provide(EntryGenerationType.JVM)
            .registerProperties(registeredProperties, classSpecificRegistryBuilder, registerClassControlFlow, className, bindingContext)
    }
}
