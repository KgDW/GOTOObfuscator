package org.gotoobfuscator.utils

import java.util.concurrent.ThreadLocalRandom

object RandomUtils {
    private val ILLEGAL_JAVA_NAMES = arrayOf(
        "abstract", "assert", "boolean", "break",
        "byte", "case", "catch", "char", "class",
        "const", "continue", "default", "do",
        "double", "else", "enum", "extends",
        "false", "final", "finally", "float",
        "for", "goto", "if", "implements",
        "import", "instanceof", "int", "interface",
        "long", "native", "new", "null",
        "package", "private", "protected", "public",
        "return", "short", "static", "strictfp",
        "super", "switch", "synchronized", "this",
        "throw", "throws", "transient", "true",
        "try", "void", "volatile", "while"
    )

    val UNICODE = CharArray(Char.MAX_VALUE.code) {
        it.toChar()
    }

    const val ALPHAS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"

    fun randomString(length : Int,pool : String) : String {
        val stringBuilder = StringBuilder()

        for (i in 0..length) {
            stringBuilder.append(pool[ThreadLocalRandom.current().nextInt(0,pool.length)])
        }

        return stringBuilder.toString()
    }

    fun randomString(length : Int,charPool : CharArray) : String {
        val stringBuilder = StringBuilder()

        for (i in 0..length) {
            stringBuilder.append(charPool[ThreadLocalRandom.current().nextInt(0,charPool.size)])
        }

        return stringBuilder.toString()
    }

    fun randomString(length : Int,list : List<Char>) : String {
        val stringBuilder = StringBuilder()

        for (i in 0..length) {
            stringBuilder.append(list[ThreadLocalRandom.current().nextInt(0,list.size)])
        }

        return stringBuilder.toString()
    }

    fun randomIllegalJavaName(): String {
        return ILLEGAL_JAVA_NAMES[ThreadLocalRandom.current().nextInt(0, ILLEGAL_JAVA_NAMES.size)]
    }
}