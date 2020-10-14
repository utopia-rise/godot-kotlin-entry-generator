package godot.entrygenerator.generator.clazz

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FunSpec
import godot.entrygenerator.EntryGenerationType
import godot.entrygenerator.generator.function.FunctionRegistrationGeneratorProvider
import godot.entrygenerator.generator.property.PropertyRegistrationGeneratorProvider
import godot.entrygenerator.model.ClassWithMembers
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.js.resolve.diagnostics.findPsi
import org.jetbrains.kotlin.resolve.BindingContext

class JvmClassRegistrationGenerator : ClassRegistrationGenerator() {

    override fun provideRegisterClassControlFlow(classWithMembers: ClassWithMembers, classRegistryControlFlow: FunSpec.Builder, className: ClassName, superClass: String, isTool: Boolean): FunSpec.Builder {
        classRegistryControlFlow.beginControlFlow(
            "registerClass<%T>(%T::class.qualifiedName!!,·%S,·$isTool)·{",
            className,
            className,
            superClass
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

    override fun registerFunctions(functions: List<FunctionDescriptor>, registerClassControlFlow: FunSpec.Builder, className: ClassName) {
        FunctionRegistrationGeneratorProvider
            .provide(EntryGenerationType.JVM)
            .registerFunctions(functions, registerClassControlFlow, className)
    }

    override fun registerSignals(signals: List<PropertyDescriptor>, registerClassControlFlow: FunSpec.Builder) {
        //TODO("Not yet implemented")
    }

    override fun registerProperties(properties: List<PropertyDescriptor>, registerClassControlFlow: FunSpec.Builder, className: ClassName, bindingContext: BindingContext) {
        PropertyRegistrationGeneratorProvider
            .provide(EntryGenerationType.JVM)
            .registerProperties(properties, registerClassControlFlow, className, bindingContext)
    }
}
