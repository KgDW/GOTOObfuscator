package org.gotoobfuscator.plugin

import org.gotoobfuscator.Obfuscator
import org.gotoobfuscator.plugin.annotation.Transformers
import org.gotoobfuscator.transformer.Transformer
import org.objectweb.asm.Type
import java.io.File
import java.net.URL
import java.net.URLClassLoader
import java.util.jar.JarFile

class PluginManager(private val instance : Obfuscator) {
    private val pluginPath = File("GotoPlugins/")

    fun searchPlugins() {
        if (!pluginPath.exists()) {
            pluginPath.mkdir()
        }

        println("搜索插件...")

        pluginPath.listFiles()?.forEach {
            if (it.name.endsWith(".jar")) {
                loadPlugin(it)
            }
        }
    }

    private fun loadPlugin(input : File) {
        println("Loading plugin ${input.absolutePath}")

        try {
            val classList = ArrayList<Class<*>>()
            val loader = URLClassLoader(arrayOf(URL("file:${input.absolutePath}")),PluginManager::class.java.classLoader)

            JarFile(input).use { file ->
                for (entry in file.entries()) {
                    if (entry.name.endsWith(".class")) {
                        try {
                            val name = entry.name.split(".class")[0].replace("/", ".")
                            val clazz = loader.loadClass(name)

                            classList.add(clazz)
                        } catch (e: ClassNotFoundException) {
                            e.printStackTrace()
                        }
                    }
                }
            }

            for (clazz in classList) {
                for (method in clazz.declaredMethods) {
                    method.isAccessible = true

                    val annotation = method.getDeclaredAnnotation(Transformers::class.java)

                    if (annotation != null) {
                        try {
                            @Suppress("UNCHECKED_CAST")
                            instance.addTransformers(method.invoke(null) as List<Transformer>)
                        } catch (e : Throwable) {
                            e.addSuppressed(Throwable("Error calling method: ${clazz.name}.${method.name}${Type.getMethodDescriptor(method)}"))

                            e.printStackTrace()
                        }
                    }
                }
            }
        } catch (e : Throwable) {
            e.addSuppressed(Throwable("An error was encountered loading the plugin: ${input.absolutePath}"))

            e.printStackTrace()
        }
    }
}