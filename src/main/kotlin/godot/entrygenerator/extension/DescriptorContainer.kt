package godot.entrygenerator.extension

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor

data class DescriptorContainer(
    val classes: MutableSet<ClassDescriptor> = mutableSetOf(),
    val properties: MutableSet<PropertyDescriptor> = mutableSetOf(),
    val functions: MutableSet<FunctionDescriptor> = mutableSetOf(),
    val signals: MutableSet<PropertyDescriptor> = mutableSetOf()
)
