package org.gotoobfuscator

import com.google.gson.GsonBuilder
import org.gotoobfuscator.plugin.PluginManager
import org.gotoobfuscator.transformer.transformers.*
import org.gotoobfuscator.transformer.transformers.rename.ClassRename
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.PrintStream
import java.nio.charset.StandardCharsets
import kotlin.collections.ArrayList

object Main {
    private const val version = "5.2"

    private val gson = GsonBuilder().setPrettyPrinting().create()

    @JvmStatic
    fun main(args : Array<String>) {
        println("--- Goto obfuscator $version ---")
        println("By Yaran")

        if (args.size < 2) {
            printHelp()
            return
        }

        val file = File(args[1])

        when (args[0].lowercase()) {
            "handle" -> {
                handle(file)
            }
            "build" -> {
                build(file)
            }
            else -> {
                printHelp()
            }
        }
    }

    private fun printHelp() {
        println("handle <Config file name> -> Use the existing configuration for")
        println("build <Config file name> -> Build a config")
    }

    private fun handle(configFile : File) {
        if (!configFile.exists()) {
            println("${configFile.absolutePath} Does not exist!")
            return
        }

        FileInputStream(configFile).reader().use {
            val config = gson.fromJson(it.readText(), Config::class.java)

            val obfuscator = Obfuscator(File(config.input), File(config.output))

            if (config.mainClass.isNotEmpty()) {
                obfuscator.mainClass = config.mainClass
            }

            for (path in config.libraries) {
                val file = File(path)

                if (!file.exists()) {
                    System.err.println("WARNING: Library file: ${file.absolutePath} not exists!")
                    continue
                }

                println("Found library ${if (file.isDirectory) "directory" else "file"}: ${file.absolutePath}")
                obfuscator.addLibraries(file)
            }

            obfuscator.addExtractZips(config.extractZips)

            obfuscator.addSkipClasses(config.skipClasses)

            obfuscator.dictionaryFile = config.classRenameDictionaryFile

            ClassRename.exclude.addAll(config.classRenameExclude)

            obfuscator.corruptCRC = config.corruptCRC
            obfuscator.corruptDate = config.corruptDate
            obfuscator.classFolder = config.classFolder
            obfuscator.duplicateResource = config.duplicateResource
            obfuscator.extractorMode = config.extractorMode
            obfuscator.dictionaryMode = config.classRenameDictionaryMode
            obfuscator.useComputeMaxs = config.useComputeMaxs
            obfuscator.multiThreadLoadLibraries = config.multiThreadLoadLibraries
            obfuscator.preVerify = config.preVerify

            obfuscator.setPackerEnable(config.packerEnable)
            obfuscator.setConstantPackerEnable(config.constantPackerEnable)

            obfuscator.threadPoolSize = config.threadPoolSize

            if (config.classRename) obfuscator.addTransformers(ClassRename())
            if (config.stringEncryptorEnable) obfuscator.addTransformers(StringEncryptor())
            if (config.hideCodeEnable) obfuscator.addTransformers(HideCode())
            if (config.numberEncryptorEnable) obfuscator.addTransformers(NumberEncryptor())
            if (config.junkCodeEnable) obfuscator.addTransformers(JunkCode())
            if (config.sourceRename) obfuscator.addTransformers(SourceRename())
            if (config.badAnnotationEnable) obfuscator.addTransformers(BadAnnotation())
            if (config.crasherEnable) obfuscator.addTransformers(Crasher())
            if (config.invalidSignatureEnable) obfuscator.addTransformers(InvalidSignature())
            if (config.variableRenameEnable) obfuscator.addTransformers(VariableRename())

            PluginManager(obfuscator).searchPlugins()

            obfuscator.start()
        }
    }

    private fun build(configFile : File) {
        println("Try to write the config...")

        val config = Config()

        FileOutputStream(configFile).use {
            it.write(gson.toJson(config).toByteArray(StandardCharsets.UTF_8))
        }

        println("The configuration is saved in: ${configFile.absolutePath}")
    }
}