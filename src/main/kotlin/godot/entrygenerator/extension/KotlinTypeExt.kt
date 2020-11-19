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
        this.isUnit() -> ClassName("godot.core.VariantType", "NIL")
        this.isInt() || this.isLong() -> ClassName("godot.core.VariantType", "LONG")
        this.isFloat() || this.isDouble() -> ClassName("godot.core.VariantType", "DOUBLE")
        this.getJetTypeFqName(false) == "kotlin.String" -> ClassName("godot.core.VariantType", "STRING")
        this.isBooleanOrNullableBoolean() -> ClassName("godot.core.VariantType", "BOOL")
        this.isCoreType() -> ClassName("godot.core.VariantType", this.getJetTypeFqName(false).substringAfterLast(".").toUpperCase())
        this.isAnyOrNullableAny() || this.supertypes().any { it.isAnyOrNullableAny() } -> ClassName("godot.core.VariantType", "OBJECT")
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
        this.isAnyOrNullableAny() || this.supertypes().any { it.isAnyOrNullableAny() } -> "asObject"
        else -> throw IllegalStateException("ReturnType $this cannot be handled by godot")
    }
}

fun KotlinType.getCastingStringTemplate() = when {
    isInt() -> "(any·as·%T).toInt()"
    isFloat() -> "(any·as·%T).toFloat()"
    else -> "any·as·%T"
}

fun KotlinType.getCastingStringTemplateTypeArg() = when {
    isInt() -> Long::class.asClassName()
    isFloat() -> Double::class.asClassName()
    else -> {
        val fqName = getJetTypeFqName(false)
        val packagePath = fqName.substringBeforeLast(".")
        val className = fqName.substringAfterLast(".")
        ClassName(packagePath, className)
    }
}
