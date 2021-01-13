package godot.entrygenerator.compiler

import godot.entrygenerator.EntryGenerator
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.config.addKotlinSourceRoots
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSourceLocation
import org.jetbrains.kotlin.cli.common.messages.GroupingMessageCollector
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.config.addJvmClasspathRoots
import org.jetbrains.kotlin.com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.utils.PathUtil
import java.util.logging.Logger

object CompilerEnvironmentProvider {
    fun provide(sourceDirs: List<String>): KotlinCoreEnvironment {
        val logger = Logger.getLogger(EntryGenerator::class.java.name)
        val messageCollector = object : MessageCollector {
            private var hasErrors = false
            override fun clear() {
                hasErrors = false
            }

            override fun hasErrors(): Boolean {
                return hasErrors
            }

            override fun report(severity: CompilerMessageSeverity, message: String, location: CompilerMessageSourceLocation?) {
                val text = if (location != null) {
                    val path = location.path
                    val position = "$path: (${location.line}, ${location.column}) "
                    position + message
                } else {
                    message
                }

                when {
                    CompilerMessageSeverity.VERBOSE.contains(severity) -> {
                        logger.finest(text)
                    }
                    severity == CompilerMessageSeverity.ERROR -> {
                        logger.severe(text)
                        hasErrors = true
                    }
                    severity == CompilerMessageSeverity.INFO -> {
                        logger.info(text)
                    }
                    else -> {
                        logger.warning(text)
                    }
                }
            }
        }
        val configuration = CompilerConfiguration().apply {
            val groupingCollector = GroupingMessageCollector(messageCollector, false)
            val severityCollector = GroupingMessageCollector(groupingCollector, false)
            put(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY, severityCollector)
            addJvmClasspathRoots(PathUtil.getJdkClassesRootsFromCurrentJre())
            addKotlinSourceRoots(sourceDirs)
        }
        return KotlinCoreEnvironment.createForProduction(Disposer.newDisposable(), configuration, EnvironmentConfigFiles.JVM_CONFIG_FILES)
    }
}
