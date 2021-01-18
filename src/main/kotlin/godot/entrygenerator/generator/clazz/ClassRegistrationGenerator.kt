package godot.entrygenerator.generator.clazz

import com.squareup.kotlinpoet.*
import godot.entrygenerator.extension.getAnnotationValue
import godot.entrygenerator.extension.getSuperTypeNameAsString
import godot.entrygenerator.model.*
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import java.io.File
import org.jetbrains.kotlin.resolve.descriptorUtil.getAllSuperclassesWithoutAny
import org.jetbrains.kotlin.resolve.descriptorUtil.getSuperClassNotAny

abstract class ClassRegistrationGenerator {

    abstract fun provideRegisterClassControlFlow(
        classWithMembers: ClassWithMembers,
        classRegistryControlFlow: FunSpec.Builder,
        className: ClassName,
        superClass: String,
        godotBaseClass: String,
        isTool: Boolean
    ): FunSpec.Builder

    abstract fun registerFunctions(
        functions: List<FunctionDescriptor>,
        registerClassControlFlow: FunSpec.Builder,
        className: ClassName
    )

    abstract fun registerSignals(
        signals: List<PropertyDescriptor>,
        className: ClassName,
        registerClassControlFlow: FunSpec.Builder
    )

    abstract fun registerProperties(
        properties: MutableList<RegisteredProperty>,
        classSpecificRegistryBuilder: TypeSpec.Builder,
        registerClassControlFlow: FunSpec.Builder,
        className: ClassName,
        bindingContext: BindingContext
    )

    fun registerClasses(
        classesWithMembers: Set<ClassWithMembers>,
        mainEntryRegistryControlFlow: FunSpec.Builder,
        bindingContext: BindingContext,
        outputPath: String
    ) {
        classesWithMembers.forEach { classWithMembers ->
            val classNameAsString = classWithMembers.classDescriptor.name.asString()
            val packagePath = classWithMembers.classDescriptor.fqNameSafe.parent().asString()
            val className = ClassName(packagePath, classNameAsString)
            val superClassFqName = classWithMembers.classDescriptor.getSuperTypeNameAsString()
            val godotBaseClass = classWithMembers
                .classDescriptor
                .getAllSuperclassesWithoutAny()
                .first { superClassDescriptor ->
                    superClassDescriptor
                        .annotations
                        .mapNotNull { it.fqName?.asString() }
                        .contains(GODOT_BASE_TYPE_ANNOTATION)
                }
                .fqNameSafe
                .asString()
                .substringAfterLast(".")

            val inheritedUserClass = classWithMembers
                .classDescriptor
                .getSuperClassNotAny()
                ?.let { superClass ->
                    val isGodotBaseClass = superClass
                        .annotations
                        .mapNotNull { it.fqName?.asString() }
                        .contains(GODOT_BASE_TYPE_ANNOTATION)

                    if (isGodotBaseClass) {
                        null
                    } else {
                        superClass
                    }
                }

            classWithMembers.addRegisteredMembersOfSuperclassesForScriptInheritance(classesWithMembers)

            val registerClassControlFlow = provideRegisterClassControlFlow(
                classWithMembers,
                FunSpec
                    .builder("register")
                    .addModifiers(KModifier.OVERRIDE)
                    .addParameter("registry", ClassName("godot.runtime", "ClassRegistry"))
                    .beginControlFlow("with(registry)"), //START: with registry
                className,
                superClassFqName,
                godotBaseClass,
                isTool(classWithMembers.classDescriptor)
            ) //START: registerClass

            val classSpecificRegistryBuilder = TypeSpec
                .classBuilder("${classNameAsString}Registrar")
                .addModifiers(KModifier.OPEN)

            registerFunctions(
                classWithMembers.functions,
                registerClassControlFlow,
                className
            )

            registerSignals(
                classWithMembers.signals,
                className,
                registerClassControlFlow
            )

            registerProperties(
                classWithMembers.properties,
                classSpecificRegistryBuilder,
                registerClassControlFlow,
                className,
                bindingContext
            )

            registerClassControlFlow.endControlFlow() //END: registerClass
            registerClassControlFlow.endControlFlow() //END: with registry

            classSpecificRegistryBuilder
                .addFunction(registerClassControlFlow.build())

            if (inheritedUserClass != null) {
                val superClassPackagePath = inheritedUserClass.fqNameSafe.asString().substringBeforeLast(".")
                val superClassSimpleName = inheritedUserClass.fqNameSafe.asString().substringAfterLast(".")
                classSpecificRegistryBuilder.superclass(ClassName("godot.$superClassPackagePath", "${superClassSimpleName}Registrar"))
            } else {
                classSpecificRegistryBuilder.addSuperinterface(ClassName("godot.runtime", "ClassRegistrar"))
            }

            FileSpec
                .builder("godot.$packagePath", "${classNameAsString}Entry")
                .addComment("THIS FILE IS GENERATED! DO NOT EDIT IT MANUALLY! ALL CHANGES TO IT WILL BE OVERWRITTEN ON EACH BUILD")
                .addType(classSpecificRegistryBuilder.build())
                .build()
                .writeTo(File(outputPath))

            mainEntryRegistryControlFlow
                .addStatement("%T().register(registry)", ClassName("godot.$packagePath", "${classNameAsString}Registrar"))
        }
    }

    private fun isTool(classDescriptor: ClassDescriptor): Boolean {
        return classDescriptor
            .annotations
            .getAnnotationValue(REGISTER_CLASS_ANNOTATION, REGISTER_CLASS_ANNOTATION_TOOL_ARGUMENT, false)
    }
}
