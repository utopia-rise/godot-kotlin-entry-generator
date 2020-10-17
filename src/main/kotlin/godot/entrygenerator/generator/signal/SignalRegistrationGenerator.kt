package godot.entrygenerator.generator.signal

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FunSpec
import godot.entrygenerator.mapper.TypeToVariantAsClassNameMapper
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.psi.KtTypeArgumentList
import org.jetbrains.kotlin.psi.KtValueArgumentList
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.source.KotlinSourceElement

abstract class SignalRegistrationGenerator {

    fun registerSignals(
        signals: List<PropertyDescriptor>,
        registerClassControlFlow: FunSpec.Builder
    ) {
        signals.forEach { propertyDescriptor ->
            signalSanityCheck(propertyDescriptor)
            registerSignal(propertyDescriptor, registerClassControlFlow)
        }
    }

    abstract fun registerSignal(propertyDescriptor: PropertyDescriptor, registerClassControlFlow: FunSpec.Builder)

    private fun signalSanityCheck(propertyDescriptor: PropertyDescriptor) {
        val propertyTypeAsString = propertyDescriptor.type.toString()

        if (!propertyDescriptor.name.asString().startsWith("signal")) {
            throw IllegalStateException("All signals must be prefixed with \"signal\"! Ex: signalButtonPressed. The signal ${propertyDescriptor.fqNameSafe} does not fulfill this criteria.")
        }

        if (propertyTypeAsString.startsWith("Signal")) {
            try {
                propertyTypeAsString.replace("Signal", "").split("<")[0].toInt()
            } catch (e: NumberFormatException) {
                throw IllegalStateException("You annotated ${propertyDescriptor.fqNameSafe} with @RegisterSignal but it's type is no signal! Use \"by signal\" to define signals.")
            }
        } else {
            throw IllegalStateException("You annotated ${propertyDescriptor.fqNameSafe} with @RegisterSignal but it's type is no signal! Use \"by signal\" to define signals.")
        }
    }
}
