package godot.entrygenerator.extension

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.asTypeName
import godot.entrygenerator.EntryGenerationType
import org.jetbrains.kotlin.js.descriptorUtils.getJetTypeFqName
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.typeUtil.*


fun KotlinType.isCoreType(): Boolean {
    return coreTypes.contains(getJetTypeFqName(false))
}

private fun getReferenceFqName(entryGenerationType: EntryGenerationType) = when (entryGenerationType) {
    EntryGenerationType.KOTLIN_NATIVE -> "godot.Resource"
    EntryGenerationType.JVM -> "godot.Reference"
}

fun KotlinType.isReference(entryGenerationType: EntryGenerationType): Boolean {
    return this.getJetTypeFqName(false) == getReferenceFqName(entryGenerationType)
        || this
        .supertypes()
        .map { it.getJetTypeFqName(false) }
        .any { it == getReferenceFqName(entryGenerationType) }
}

fun KotlinType.isCompatibleList(): Boolean {
    return when {
        getJetTypeFqName(false) == "godot.core.GodotArray" || getJetTypeFqName(false) == "godot.core.VariantArray" -> true
        else -> supertypes().any { it.getJetTypeFqName(false) == "godot.core.GodotArray" || it.getJetTypeFqName(false) == "godot.core.VariantArray" }
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


fun KotlinType?.toReturnKtVariantType(): ClassName {
    return when {
        this == null -> throw IllegalStateException("Type is null")
        isUnit() -> ClassName("godot.core.VariantType", "NIL")
        isInt() || isLong() -> ClassName("godot.core.VariantType", "LONG")
        isFloat() || isDouble() -> ClassName("godot.core.VariantType", "DOUBLE")
        getJetTypeFqName(false) == "kotlin.String" -> ClassName("godot.core.VariantType", "STRING")
        isBooleanOrNullableBoolean() -> ClassName("godot.core.VariantType", "BOOL")
        getJetTypeFqName(false) == "godot.core.VariantArray" -> ClassName("godot.core.VariantType", "ARRAY")
        getJetTypeFqName(false) == "godot.core.RID" -> ClassName("godot.core.VariantType", "_RID")
        isCoreType() -> ClassName("godot.core.VariantType", getJetTypeFqName(false).substringAfterLast(".").toUpperCase())
        isAnyOrNullableAny() -> ClassName("godot.core.VariantType", "ANY")
        supertypes().any { it.isAnyOrNullableAny() } -> ClassName("godot.core.VariantType", "OBJECT")
        else -> throw IllegalStateException("ReturnType $this cannot be handled by godot")
    }
}

fun KotlinType.toParameterKtVariantType() = when {
    isInt() -> ClassName("godot.core.VariantType", "JVM_INT")
    isLong() -> ClassName("godot.core.VariantType", "LONG")
    isFloat() -> ClassName("godot.core.VariantType", "JVM_FLOAT")
    isDouble() -> ClassName("godot.core.VariantType", "DOUBLE")
    getJetTypeFqName(false) == "kotlin.String" -> ClassName("godot.core.VariantType", "STRING")
    isBooleanOrNullableBoolean() -> ClassName("godot.core.VariantType", "BOOL")
    getJetTypeFqName(false) == "godot.core.VariantArray" -> ClassName("godot.core.VariantType", "ARRAY")
    getJetTypeFqName(false) == "godot.core.RID" -> ClassName("godot.core.VariantType", "_RID")
    isCoreType() -> ClassName("godot.core.VariantType", getJetTypeFqName(false).substringAfterLast(".").toUpperCase())
    isUnit() -> ClassName("godot.core.VariantType", "NIL")
    isAnyOrNullableAny() -> ClassName("godot.core.VariantType", "ANY")
    supertypes().any { it.isAnyOrNullableAny() } -> ClassName("godot.core.VariantType", "OBJECT")
    else -> throw IllegalStateException("ParameterType $this cannot be handled by godot")
}
