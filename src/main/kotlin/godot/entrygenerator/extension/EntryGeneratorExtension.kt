package godot.entrygenerator.extension

import com.squareup.kotlinpoet.*
import godot.entrygenerator.model.ClassWithMembers
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.resolve.BindingContext

/**
 * Base class each entry generation extension entry point should inherit from.
 *
 * You have to use a regular class with a default constructor and NO kotlin object
 */
abstract class EntryGeneratorExtension {
    /**
     * Provide a meaningful name for the extension
     *
     * Will be used to generate comment so the user can distinguish which code was generated by which extension
     */
    abstract fun provideExtensionName(): String

    /**
     * If you need to process custom annotations, provide them as fully qualified strings
     *
     * example: com.mycompany.annotations.MyCoolAnnotation
     */
    open fun provideFullyQualifiedAnnotations(): Set<String> = emptySet()

    /**
     * Perform some sanity checks about the detected descriptors for your extension.
     *
     * throw an error here if your annotations are used wrongly
     * @param extensionDescriptorContainer contains descriptors specific to this extension (code annotated with your custom annotations if provided)
     * @param messageCollector use this messageCollector if you want to print logs. Normal print statements won't output anything in the context your extension will run in
     */
    open fun performSanityChecks(extensionDescriptorContainer: DescriptorContainer, messageCollector: MessageCollector) {}

    /**
     * Hook before the entry generation starts
     *
     * @param registrationDescriptorContainer contains descriptors for regular registration (code registered with the regular registration annotations)
     * @param extensionDescriptorContainer contains descriptors specific to this extension (code annotated with your custom annotations if provided)
     * @param bindingContext the binding context from the compiler. Needs to be provided for some advanced functions in the compiler api
     * @param messageCollector use this messageCollector if you want to print logs. Normal print statements won't output anything in the context your extension will run in
     */
    open fun beforeEntryGenerationHook(registrationDescriptorContainer: DescriptorContainer, extensionDescriptorContainer: DescriptorContainer, bindingContext: BindingContext, messageCollector: MessageCollector) {}

    /**
     * Hook after the entry generation has finished
     *
     * @param registrationDescriptorContainer contains descriptors for regular registration (code registered with the regular registration annotations)
     * @param extensionDescriptorContainer contains descriptors specific to this extension (code annotated with your custom annotations if provided)
     * @param bindingContext the binding context from the compiler. Needs to be provided for some advanced functions in the compiler api
     * @param messageCollector use this messageCollector if you want to print logs. Normal print statements won't output anything in the context your extension will run in
     */
    open fun afterEntryGenerationHook(registrationDescriptorContainer: DescriptorContainer, extensionDescriptorContainer: DescriptorContainer, bindingContext: BindingContext, messageCollector: MessageCollector) {}

    /**
     * If your extension needs to generate code to the root of the entry file, you can to this here.
     *
     * **Note:** your code will be placed **AFTER** the main entry generation code.
     *
     * Provide comments to your generated classes, functions and properties so the user can distinguish what was generated by your extension
     * @param entryFileSpec kotlin poet file spec of the entry file. Add your code to this file spec. DO NOT CALL [FileSpec.Builder.build]!
     * @param descriptorContainer contains descriptors specific to this extension (code annotated with your custom annotations if provided)
     * @param messageCollector use this messageCollector if you want to print logs. Normal print statements won't output anything in the context your extension will run in
     */
    open fun writeToEntryFileRoot(entryFileSpec: FileSpec.Builder, descriptorContainer: Set<ClassWithMembers>, messageCollector: MessageCollector) {}

    /**
     * If you need to register classes, functions or properties (or signals which are properties in the end) return true here. Otherwise [registerFunctions], [registerSignals], [registerProperties] are ignored!
     * @param classWithMembers contains descriptors specific to this extension (code annotated with your custom annotations if provided) but enriched with classDescriptors for classes the user wants to register
     * @param messageCollector use this messageCollector if you want to print logs. Normal print statements won't output anything in the context your extension will run in
     */
    open fun registersElementsForClass(classWithMembers: ClassWithMembers, messageCollector: MessageCollector): Boolean = false

    /**
     * If you need to register custom functions to godot you can do this here. [registersElementsForClass] needs to return true in order for this function to be called for that particular class!
     *
     * Your function will be registered AFTER the regular registration code
     *
     * For examples of how your code must look like to be registered successfully, just look at a generated Entry file and the "registerFunction" calls there
     * @param registeredFunctions functions the user annotated with @RegisterFunction
     * @param functionsAnnotatedForThisExtension functions that were annotated with a annotation you provided in [provideFullyQualifiedAnnotations]
     * @param registerClassControlFlow kotlin poet control flow for class and member registration. Add your code generation logic here.
     * @param className kotlin poet [ClassName] for the containing class (convenience -> same as constructing it yourself with [FunctionDescriptor.getContainingDeclaration])
     * @param bindingContext the binding context from the compiler. Needs to be provided for some advanced functions in the compiler api
     * @param messageCollector use this messageCollector if you want to print logs. Normal print statements won't output anything in the context your extension will run in
     */
    open fun registerFunctions(
        registeredFunctions: List<FunctionDescriptor>,
        functionsAnnotatedForThisExtension: List<FunctionDescriptor>,
        registerClassControlFlow: FunSpec.Builder,
        className: ClassName,
        bindingContext: BindingContext,
        messageCollector: MessageCollector
    ) {
    }

    /**
     * If you need to register custom signals to godot you can do this here. [registersElementsForClass] needs to return true in order for this function to be called for that particular class!
     *
     * Your signal will be registered AFTER the regular registration code
     *
     * For examples of how your code must look like to be registered successfully, just look at a generated Entry file and the "registerSignal" calls there
     * @param registeredSignals signals the user annotated with @RegisterSignal
     * @param signalsAnnotatedForThisExtension signals that were annotated with a annotation you provided in [provideFullyQualifiedAnnotations]
     * @param registerClassControlFlow kotlin poet control flow for class and member registration. Add your code generation logic here.
     * @param className kotlin poet [ClassName] for the containing class (convenience -> same as constructing it yourself with [PropertyDescriptor.getContainingDeclaration])
     * @param bindingContext the binding context from the compiler. Needs to be provided for some advanced functions in the compiler api
     * @param messageCollector use this messageCollector if you want to print logs. Normal print statements won't output anything in the context your extension will run in
     */
    open fun registerSignals(
        registeredSignals: List<PropertyDescriptor>,
        signalsAnnotatedForThisExtension: List<PropertyDescriptor>,
        registerClassControlFlow: FunSpec.Builder,
        className: ClassName,
        bindingContext: BindingContext,
        messageCollector: MessageCollector
    ) {
    }

    /**
     * If you need to register custom properties to godot you can do this here. [registersElementsForClass] needs to return true in order for this function to be called for that particular class!
     *
     * Your property will be registered AFTER the regular registration code
     *
     * For examples of how your code must look like to be registered successfully, just look at a generated Entry file and the "registerProperty" calls there
     * @param registeredProperties properties the user annotated with @RegisterProperty
     * @param propertiesAnnotatedForThisExtension properties that were annotated with a annotation you provided in [provideFullyQualifiedAnnotations]
     * @param registerClassControlFlow kotlin poet control flow for class and member registration. Add your code generation logic here.
     * @param className kotlin poet [ClassName] for the containing class (convenience -> same as constructing it yourself with [PropertyDescriptor.getContainingDeclaration])
     * @param bindingContext the binding context from the compiler. Needs to be provided for some advanced functions in the compiler api
     * @param messageCollector use this messageCollector if you want to print logs. Normal print statements won't output anything in the context your extension will run in
     */
    open fun registerProperties(
        registeredProperties: List<PropertyDescriptor>,
        propertiesAnnotatedForThisExtension: List<PropertyDescriptor>,
        registerClassControlFlow: FunSpec.Builder,
        className: ClassName,
        bindingContext: BindingContext,
        messageCollector: MessageCollector
    ) {
    }

    /**
     * If you need to inject code before or after a particular function in a script gets called, return true here.
     * @param functionDescriptor for a function cou can inject code into. The function only appears here if the user annotated it with @RegisterFunction
     * @param messageCollector use this messageCollector if you want to print logs. Normal print statements won't output anything in the context your extension will run in
     */
    open fun overridesFunction(functionDescriptor: FunctionDescriptor, messageCollector: MessageCollector): Boolean = false

    /**
     * If you need to inject code BEFORE a particular function in a script gets called and you returned *true* for this function in [overridesFunction],
     * you can generate code that gets executed everytime before this function gets called as if the code you generated would have been written at the beginning of the function block
     *
     * **Note:** you can return here and the function of the user never gets called! Be careful what you are doing and clearly document why it returns and the users code does not get called if you do so!
     * @param beforeOnReadyFunSpec kotlin poet funSpec you can generate code into that should be called before the users function gets called
     * @param functionDescriptor functionDescriptor for a function cou can inject code into. The function only appears here if the user annotated it with @RegisterFunction
     * @param messageCollector use this messageCollector if you want to print logs. Normal print statements won't output anything in the context your extension will run in
     */
    open fun beforeFunctionBody(beforeOnReadyFunSpec: FunSpec.Builder, functionDescriptor: FunctionDescriptor, messageCollector: MessageCollector) {}

    /**
     * If you need to inject code AFTER a particular function in a script gets called and you returned *true* for this function in [overridesFunction],
     * you can generate code that gets executed everytime after this function gets called as if the code you generated would have been written at the end of the function block.
     *
     * **Note:** you can manipulate the result returned by the users function here! It is stored in a local variable named "result". Be careful if you do that!
     * Clearly document on why and how you alter the result of the users function as it's really hard to debug for them if you change the returned object! The returning type has to be the same as the one the user defined in the script though!
     * @param afterOnReadyFunSpec kotlin poet funSpec you can generate code into that should be executed after the users function was called
     * @param functionDescriptor functionDescriptor for a function cou can inject code into. The function only appears here if the user annotated it with @RegisterFunction
     */
    open fun afterFunctionBody(afterOnReadyFunSpec: FunSpec.Builder, functionDescriptor: FunctionDescriptor, messageCollector: MessageCollector) {}

}
