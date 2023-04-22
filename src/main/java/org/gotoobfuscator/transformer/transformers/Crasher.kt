package org.gotoobfuscator.transformer.transformers

import org.gotoobfuscator.Obfuscator
import org.gotoobfuscator.obj.Resource
import org.gotoobfuscator.transformer.Transformer
import org.gotoobfuscator.utils.RandomUtils
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.tree.ClassNode
import java.io.File
import java.util.concurrent.ThreadLocalRandom

class Crasher : Transformer("Crasher") {
    override fun onStart(obfuscator: Obfuscator) {
        for (i in 0..100) {
            setup(null,obfuscator)
            setup("META-INF/",obfuscator);
        }
    }

    private fun setup(basePath : String?,obfuscator : Obfuscator) {
        val node = ClassNode()

        node.name = "<html><img src=\"https:" + RandomUtils.randomString(10, "0123456789")
        node.access = ACC_PUBLIC
        node.version = V1_8

        val writer = ClassWriter(0)
        node.accept(writer)

        val builder = StringBuilder()

        if (basePath != null) {
            builder.append(basePath)
        }

        for (i in 0..250) {
            builder.append(
                when (ThreadLocalRandom.current().nextInt(0,11)) {
                    0 -> {
                        "&"
                    }
                    1 -> {
                        "_"
                    }
                    2 -> {
                        "\n"
                    }
                    3 -> {
                        "`"
                    }
                    4 -> {
                        "goto"
                    }
                    5 -> {
                        ";"
                    }
                    6 -> {
                        "'"
                    }
                    7 -> {
                        "%"
                    }
                    8 -> {
                        "$"
                    }
                    9 -> {
                        "@"
                    }
                    10 -> {
                        "#"
                    }
                    else -> {
                        "+"
                    }
                }
            ).append(File.separator)
        }

        builder.append(RandomUtils.randomIllegalJavaName()).append(".class")

        val s = builder.toString()

        obfuscator.resources[s] = Resource(s,writer.toByteArray())
    }
}