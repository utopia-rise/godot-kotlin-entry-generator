package godot.entrygenerator.generator.property.defaultvalue

import godot.entrygenerator.extension.isCompatibleList
import godot.entrygenerator.extension.isResource
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.resolve.BindingContext

object DefaultValueExtractorProvider {

    fun provide(
        propertyDescriptor: PropertyDescriptor,
        bindingContext: BindingContext
    ): DefaultValueExtractor {
        return when {
            propertyDescriptor.type.isResource() -> ResourceDefaultValueExtractor(
                propertyDescriptor,
                bindingContext
            )
            propertyDescriptor.type.isCompatibleList() -> ArrayDefaultValueExtractor(
                propertyDescriptor,
                bindingContext
            )
            KotlinBuiltIns.isSetOrNullableSet((propertyDescriptor.type)) -> EnumFlagDefaultValueExtractor(
                propertyDescriptor,
                bindingContext
            )
            else -> DefaultValueExtractor(
                propertyDescriptor,
                bindingContext
            )
        }
    }
}