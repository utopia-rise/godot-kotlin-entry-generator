package godot.entrygenerator.extension

import com.squareup.kotlinpoet.ClassName
import org.jetbrains.kotlin.js.descriptorUtils.getJetTypeFqName
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.typeUtil.*


fun KotlinType.isCoreType(): Boolean {
    return coreTypes.contains(getJetTypeFqName(false))
}

fun KotlinType.isResource(): Boolean {
    return this.getJetTypeFqName(false) == "godot.Resource"
        || this
        .supertypes()
        .map { it.getJetTypeFqName(false) }
        .any { it == "godot.Resource" }
}

fun KotlinType.isCompatibleList(): Boolean {
    return when {
        getJetTypeFqName(false) == "godot.core.GodotArray" -> true
        else -> supertypes().any { it.getJetTypeFqName(false) == "godot.core.GodotArray" }
    }
}

fun KotlinType.getCompatibleListType(): String {
    return getJetTypeFqName(false).getCompatibleListType()
}

private fun KotlinType.getRegistrableTypeAsFqNameString(): String? {
    return when {
        getJetTypeFqName(false).isGodotPrimitive()
            || isCoreType() -> getJetTypeFqName(false)
        else -> null
    }
}

fun KotlinType.getFirstRegistrableTypeAsFqNameStringOrNull(): String? {
    return getRegistrableTypeAsFqNameString()
        ?: supertypes()
            .firstOrNull { it.getRegistrableTypeAsFqNameString() != null }
            ?.getRegistrableTypeAsFqNameString()
}

private val coreTypes = listOf(
    "godot.core.Vector2",
    "godot.core.Rect2",
    "godot.core.Vector3",
    "godot.core.Transform2D",
    "godot.core.Plane",
    "godot.core.Quat",
    "godot.core.AABB",
    "godot.core.Basis",
    "godot.core.Transform",
    "godot.core.Color",
    "godot.core.NodePath",
    "godot.core.RID",
    "godot.Object",
    "godot.core.Dictionary",
    "godot.core.PoolByteArray",
    "godot.core.PoolIntArray",
    "godot.core.PoolRealArray",
    "godot.core.PoolStringArray",
    "godot.core.PoolColorArray",
    "godot.core.PoolVector2Array",
    "godot.core.PoolVector3Array",

    "godot.core.VariantArray",
    "godot.core.ObjectArray",
    "godot.core.EnumArray",
    "godot.core.BoolVariantArray",
    "godot.core.IntVariantArray",
    "godot.core.RealVariantArray",
    "godot.core.StringVariantArray",
    "godot.core.AABBArray",
    "godot.core.BasisArray",
    "godot.core.ColorArray",
    "godot.core.NodePathArray",
    "godot.core.PlaneArray",
    "godot.core.QuatArray",
    "godot.core.Rect2Array",
    "godot.core.RIDArray",
    "godot.core.Transform2DArray",
    "godot.core.TransformArray",
    "godot.core.Vector2Array",
    "godot.core.Vector3Array"
)



fun KotlinType?.toKtVariantType(): ClassName {
    return when {
        this == null -> throw IllegalStateException("Type is null")
        this.isUnit() -> ClassName("godot.core.KtVariant.Type", "NIL")
        this.isInt() || this.isLong() -> ClassName("godot.core.KtVariant.Type", "LONG")
        this.isFloat() || this.isDouble() -> ClassName("godot.core.KtVariant.Type", "DOUBLE")
        this.getJetTypeFqName(false) == "kotlin.String" -> ClassName("godot.core.KtVariant.Type", "STRING")
        this.isBooleanOrNullableBoolean() -> ClassName("godot.core.KtVariant.Type", "BOOL")
        this.isCoreType() -> ClassName("godot.core.KtVariant.Type", this.getJetTypeFqName(false).substringAfterLast(".").toUpperCase())
        this.isAnyOrNullableAny() -> ClassName("godot.core.KtVariant.Type", "OBJECT")
        else -> throw IllegalStateException("ReturnType $this cannot be handled by godot")
    }
}

fun KotlinType.toKtVariantConversionFunctionName(): String {
    return when {
        this.isInt() -> "asInt"
        this.isLong() -> "asLong"
        this.isFloat() -> "asFloat"
        this.isDouble() -> "asDouble"
        this.getJetTypeFqName(false) == "kotlin.String" -> "asString"
        this.isBooleanOrNullableBoolean() -> "asBoolean"
        this.isCoreType() -> "as${this.getJetTypeFqName(false).substringAfterLast(".")}"
        this.isAnyOrNullableAny() -> "asObject"
        else -> throw IllegalStateException("ReturnType $this cannot be handled by godot")
    }
}
