package godot.entrygenerator.generator.property

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FunSpec
import godot.entrygenerator.EntryGenerationType
import godot.entrygenerator.extension.getAnnotationValue
import godot.entrygenerator.extension.isCompatibleList
import godot.entrygenerator.extension.isReference
import godot.entrygenerator.model.REGISTER_PROPERTY_ANNOTATION
import godot.entrygenerator.model.REGISTER_PROPERTY_ANNOTATION_RPC_MODE_ARGUMENT
import godot.entrygenerator.model.REGISTER_PROPERTY_ANNOTATION_VISIBLE_IN_EDITOR_ARGUMENT
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.js.descriptorUtils.getJetTypeFqName
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.typeUtil.isEnum
import org.jetbrains.kotlin.types.typeUtil.supertypes

abstract class PropertyRegistrationGenerator {

    fun registerProperties(
        properties: List<PropertyDescriptor>,
        registerClassControlFlow: FunSpec.Builder,
        className: ClassName,
        bindingContext: BindingContext,
        entryGenerationType: EntryGenerationType
    ) {
        properties.forEach { propertyDescriptor ->
            when {
                propertyDescriptor.type.isReference(entryGenerationType) -> registerReference(className, propertyDescriptor, bindingContext, registerClassControlFlow)
                propertyDescriptor.type.isEnum() -> registerEnum(className, propertyDescriptor, bindingContext, registerClassControlFlow)
                propertyDescriptor.type.isCompatibleList() && propertyDescriptor.type.arguments.firstOrNull()?.type?.isEnum() == true -> registerEnumList(className, propertyDescriptor, bindingContext, registerClassControlFlow)
                KotlinBuiltIns.isSetOrNullableSet(propertyDescriptor.type)
                && propertyDescriptor.type.arguments.firstOrNull()?.type?.isEnum() == true -> registerEnumFlag(className, propertyDescriptor, bindingContext, registerClassControlFlow)
                else -> registerProperty(className, propertyDescriptor, bindingContext, registerClassControlFlow)
            }
        }
    }

    protected abstract fun registerEnumFlag(
        className: ClassName,
        propertyDescriptor: PropertyDescriptor,
        bindingContext: BindingContext,
        registerClassControlFlow: FunSpec.Builder
    )

    protected abstract fun registerEnumList(
        className: ClassName,
        propertyDescriptor: PropertyDescriptor,
        bindingContext: BindingContext,
        registerClassControlFlow: FunSpec.Builder
    )

    protected abstract fun registerEnum(
        className: ClassName,
        propertyDescriptor: PropertyDescriptor,
        bindingContext: BindingContext,
        registerClassControlFlow: FunSpec.Builder
    )

    protected abstract fun registerReference(
        className: ClassName,
        propertyDescriptor: PropertyDescriptor,
        bindingContext: BindingContext,
        registerClassControlFlow: FunSpec.Builder
    )

    protected abstract fun registerProperty(
        className: ClassName,
        propertyDescriptor: PropertyDescriptor,
        bindingContext: BindingContext,
        registerClassControlFlow: FunSpec.Builder
    )

    protected fun shouldBeVisibleInEditor(propertyDescriptor: PropertyDescriptor): Boolean {
        return propertyDescriptor
            .annotations
            .getAnnotationValue(
                REGISTER_PROPERTY_ANNOTATION,
                REGISTER_PROPERTY_ANNOTATION_VISIBLE_IN_EDITOR_ARGUMENT,
                true
            )
    }

    protected fun getRpcModeEnum(propertyDescriptor: PropertyDescriptor): String {
        val compilerRpcModeEnumRepresentation = getCompilerRpcModeEnumRepresentation(propertyDescriptor)
        val packagePath = compilerRpcModeEnumRepresentation.first.asString().replace("/", ".")
        val name = compilerRpcModeEnumRepresentation.second
        return "$packagePath.$name"
    }

    private fun getCompilerRpcModeEnumRepresentation(propertyDescriptor: PropertyDescriptor): Pair<ClassId, Name> {
        return propertyDescriptor
            .annotations
            .getAnnotationValue(
                REGISTER_PROPERTY_ANNOTATION,
                REGISTER_PROPERTY_ANNOTATION_RPC_MODE_ARGUMENT,
                Pair(ClassId(FqName("godot.MultiplayerAPI"), Name.identifier("RPCMode")), Name.identifier("DISABLED"))
            )
    }

    protected fun isOfType(type: KotlinType, typeFqName: String): Boolean {
        return if (type.getJetTypeFqName(false) == typeFqName) {
            true
        } else {
            type
                .supertypes()
                .any { it.getJetTypeFqName(false) == typeFqName }
        }
    }
}
