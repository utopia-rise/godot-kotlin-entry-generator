package godot.entrygenerator.generator.property.typehint

import com.squareup.kotlinpoet.ClassName
import godot.entrygenerator.extension.isCompatibleList
import godot.entrygenerator.extension.isCoreType
import godot.entrygenerator.extension.isResource
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.types.typeUtil.isEnum

object PropertyTypeHintProvider {

    fun provide(
        propertyDescriptor: PropertyDescriptor
    ): ClassName {
        return when {
            KotlinBuiltIns.isInt(propertyDescriptor.type) ->
                if (propertyDescriptor.annotations.hasAnnotation(FqName("godot.annotation.IntFlag"))) {
                    ClassName("godot.gdnative.godot_property_hint", "GODOT_PROPERTY_HINT_FLAGS")
                } else {
                    PrimitivesTypeHintGenerator(propertyDescriptor).getPropertyTypeHint()
                }

            KotlinBuiltIns.isString(propertyDescriptor.type) ->
                when {
                    propertyDescriptor.annotations.hasAnnotation(FqName("godot.annotation.MultilineText")) -> {
                        ClassName(
                            "godot.gdnative.godot_property_hint",
                            "GODOT_PROPERTY_HINT_MULTILINE_TEXT"
                        )
                    }
                    propertyDescriptor.annotations.hasAnnotation(FqName("godot.annotation.PlaceHolderText")) -> {
                        ClassName(
                            "godot.gdnative.godot_property_hint",
                            "GODOT_PROPERTY_HINT_PLACEHOLDER_TEXT"
                        )
                    }
                    else -> {
                        PrimitivesTypeHintGenerator(propertyDescriptor).getPropertyTypeHint()
                    }
                }

            KotlinBuiltIns.isLong(propertyDescriptor.type)
                    || KotlinBuiltIns.isFloat(propertyDescriptor.type)
                    || KotlinBuiltIns.isDouble(propertyDescriptor.type)
                    || KotlinBuiltIns.isBoolean(propertyDescriptor.type) -> PrimitivesTypeHintGenerator(propertyDescriptor).getPropertyTypeHint()
            propertyDescriptor.type.isEnum() -> throw UnsupportedOperationException("Hint type for enum is always the same, so it is handled by binding at runtime")
            propertyDescriptor.type.isCoreType() && !propertyDescriptor.type.isCompatibleList() -> CoreTypeTypeHintGenerator(propertyDescriptor).getPropertyTypeHint()
            propertyDescriptor.type.isResource() -> ClassName(
                "godot.gdnative.godot_property_hint",
                "GODOT_PROPERTY_HINT_RESOURCE_TYPE"
            )
            propertyDescriptor.type.isCompatibleList() -> ArrayTypeHintGenerator(propertyDescriptor).getPropertyTypeHint()
            KotlinBuiltIns.isSetOrNullableSet((propertyDescriptor.type)) -> throw UnsupportedOperationException("Hint type for enum is always the same, so it is handled by binding at runtime")
            else -> throw IllegalStateException("There is no type hint generator for the property descriptor $propertyDescriptor")
        }
    }
}