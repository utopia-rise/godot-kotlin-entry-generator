# Godot Kotlin EntryGenerator

Entry generator for [Kotlin Native](https://github.com/utopia-rise/godot-kotlin) and [Kotlin/Jvm](https://github.com/utopia-rise/godot-jvm/) binding for the Godot Game Engine

## Overview

This is the entry generator which is called by the annotation processor within the corresponding project. It takes the
annotated classes, properties, functions and generates the entry file from them. This step is crucial, so the end user
does not have to write all the registration glue code by himself. 

[![GitHub](https://img.shields.io/github/license/utopia-rise/godot-jvm?style=flat-square)](LICENSE)

## Important notes

This project cannot work on it's own. It is implemented as a submodule in 
[Kotlin Native](https://github.com/utopia-rise/godot-kotlin) and [Kotlin/Jvm](https://github.com/utopia-rise/godot-jvm/)
and will be called through the corresponding annotation processor inside those projects.


## Developer discussion

Ask questions and collaborate on Discord:
https://discord.gg/qSU2EQs

## Contribution Guidelines:

- **CodeStyle:**  
We enforce the code style to match the official kotlin [coding conventions](https://kotlinlang.org/docs/reference/coding-conventions.html). Read there on how to set those up for your IDE.  
We will enforce this later on through CI and linting.  
- **Branching:**  
We do branching like described in `git-flow`.

We use the `CodeOwner` feature of github. Discuss implementation details with the corresponding maintainer.

## Setting up the project for local development
1. Follow the setup guides for either [Kotlin Native](https://github.com/utopia-rise/godot-kotlin) or [Kotlin/Jvm](https://github.com/utopia-rise/godot-jvm/).
2. Make your changes directly in those projects (as this build is included there with gradle's `includeBuild` functionality)
and commit and push from there.

## Debug entry generation (KotlinCompilerPlugin in general)
- Build a sample project with `./gradlew build --no-daemon -Dorg.gradle.debug=true -Dkotlin.compiler.execution.strategy="in-process" -Dkotlin.daemon.jvm.options="-Xdebug,-Xrunjdwp:transport=dt_socket,address=5005,server=y,suspend=n"`
- Attach remote debugger to process (a preconfigured run configuration for it is present in the sample project `tests` called `DebugEntryGenerator` in the project [Kotlin/Jvm](https://github.com/utopia-rise/godot-jvm/. Copy it from there if you don't want to configure your own))

Note: Compilation with attached debugger will be way slower. Especially for the initial build. So be patient. It takes some time until it hits your breakpoints.

## Example
```kotlin
@RegisterClass
class Invocation : Spatial() {
    @RegisterSignal
    val signalOneParam by signal<Boolean>("refresh")

    @RegisterProperty
    var x = 0

    @RegisterFunction
    fun _ready() {
    }
}
```

will produce:  

JVM:
```kotlin
// THIS FILE IS GENERATED! DO NOT EDIT IT MANUALLY! ALL CHANGES TO IT WILL BE OVERWRITTEN ON EACH BUILD
package godot

/*
imports omitted for simplicity of this example
*/

class Entry : Entry() {
  override fun Context.init() {
    with(registry) {
      registerClass<Invocation>(Invocation::class.qualifiedName!!, "Spatial", false) {
        constructor(KtConstructor0(::Invocation))
        function(Invocation::_ready, ::KtVariant, KtFunctionArgument(NIL, "Unit"))
        signal(Invocation::signalOneParam, KtFunctionArgument(BOOL, "Boolean"))
        property(Invocation::x, ::KtVariant, KtVariant::asInt, LONG, "Int", NONE, "")
      }
      registerEngineTypes()
    }
  }
}
```  

Kotlin/Native:  
```kotlin
// THIS FILE IS GENERATED! DO NOT EDIT IT MANUALLY! ALL CHANGES TO IT WILL BE OVERWRITTEN ON EACH BUILD
@file:Suppress("EXPERIMENTAL_API_USAGE")

package godot

/*
imports omitted for simplicity of this example
*/

@CName("godot_gdnative_init")
fun GDNativeInit(options: godot_gdnative_init_options) {
  Godot.init(options)
}

@CName("godot_gdnative_terminate")
fun GDNativeTerminate(options: godot_gdnative_terminate_options) {
  Godot.terminate(options)
}

@CName("godot_nativescript_init")
fun NativeScriptInit(handle: COpaquePointer) {
  Godot.nativescriptInit(handle)
  with(ClassRegistry(handle)) {
    registerClass("example.Invocation", "Spatial", ::Invocation, false) {
      function("_ready", DISABLED, Invocation::_ready, { Variant() })
      signal("signalOneParam", mapOf("refresh" to BOOL))
      property("x", Invocation::x, getTypeToVariantConversionFunction<Int>(), getVariantToTypeConversionFunction<Int>(), INT, Variant(0), true, DISABLED, GODOT_PROPERTY_HINT_NONE, "")
    }
    registerClass("example.TextureSample", "Spatial", ::TextureSample, false) {
      function("_ready", DISABLED, TextureSample::_ready, { Variant() })
      property("nodePath", TextureSample::nodePath, getTypeToVariantConversionFunction<CoreType>(), getVariantToTypeConversionFunction<NodePath>(), NODE_PATH, null, true, DISABLED, GODOT_PROPERTY_HINT_NONE, "")
      property("texture", TextureSample::texture, getTypeToVariantConversionFunction<Object>(), getVariantToTypeConversionFunction<godot.Object>(), OBJECT, null, true, DISABLED, GODOT_PROPERTY_HINT_RESOURCE_TYPE, "Texture")
    }
  }
}

@CName("godot_nativescript_terminate")
fun NativeScriptTerminate(handle: COpaquePointer) {
  Godot.nativescriptTerminate(handle)
}
```
