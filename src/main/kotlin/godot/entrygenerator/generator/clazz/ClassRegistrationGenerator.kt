package godot.entrygenerator.generator.clazz

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FunSpec
import godot.entrygenerator.extension.getAnnotationValue
import godot.entrygenerator.extension.getSuperTypeNameAsString
import godot.entrygenerator.model.ClassWithMembers
import godot.entrygenerator.model.REGISTER_CLASS_ANNOTATION
import godot.entrygenerator.model.REGISTER_CLASS_ANNOTATION_TOOL_ARGUMENT
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

abstract class ClassRegistrationGenerator {

    abstract fun provideRegisterClassControlFlow(
        classWithMembers: ClassWithMembers,
        classRegistryControlFlow: FunSpec.Builder,
        className: ClassName,
        superClass: String,
        isTool: Boolean
    ): FunSpec.Builder

    abstract fun registerFunctions(
        functions: List<FunctionDescriptor>,
        registerClassControlFlow: FunSpec.Builder,
        className: ClassName
    )

    abstract fun registerSignals(
        signals: List<PropertyDescriptor>,
        registerClassControlFlow: FunSpec.Builder
    )

    abstract fun registerProperties(
        properties: List<PropertyDescriptor>,
        registerClassControlFlow: FunSpec.Builder,
        className: ClassName,
        bindingContext: BindingContext
    )

    fun registerClasses(
        classesWithMembers: Set<ClassWithMembers>,
        classRegistryControlFlow: FunSpec.Builder,
        bindingContext: BindingContext
    ) {
        classesWithMembers.forEach { classWithMembers ->
            val classNameAsString = classWithMembers.classDescriptor.name.asString()
            val packagePath = classWithMembers.classDescriptor.fqNameSafe.parent().asString()
            val className = ClassName(packagePath, classNameAsString)
            val superClass = classWithMembers.classDescriptor.getSuperTypeNameAsString()

            val registerClassControlFlow = provideRegisterClassControlFlow(
                classWithMembers, classRegistryControlFlow, className, superClass, isTool(classWithMembers.classDescriptor)
            ) //START: registerClass

            registerFunctions(
                classWithMembers.functions,
                registerClassControlFlow,
                className
            )

            registerSignals(
                classWithMembers.signals,
                registerClassControlFlow
            )

            registerProperties(
                classWithMembers.properties,
                registerClassControlFlow,
                className,
                bindingContext
            )

            registerClassControlFlow.endControlFlow() //END: registerClass
        }
    }

    private fun isTool(classDescriptor: ClassDescriptor): Boolean {
        return classDescriptor
            .annotations
            .getAnnotationValue(REGISTER_CLASS_ANNOTATION, REGISTER_CLASS_ANNOTATION_TOOL_ARGUMENT, false)
    }
}