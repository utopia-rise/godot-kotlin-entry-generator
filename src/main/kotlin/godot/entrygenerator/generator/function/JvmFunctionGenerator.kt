package godot.entrygenerator.generator.function

import com.squareup.kotlinpoet.ClassName
import org.jetbrains.kotlin.descriptors.FunctionDescriptor

class JvmFunctionGenerator : FunctionRegistrationGenerator() {

    override fun getStringTemplate(functionDescriptor: FunctionDescriptor): String {
        TODO("Not yet implemented")
    }

    override fun getTemplateArgs(functionDescriptor: FunctionDescriptor, className: ClassName): List<Any> {
        TODO("Not yet implemented")
    }
}