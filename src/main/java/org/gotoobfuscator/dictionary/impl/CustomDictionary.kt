package org.gotoobfuscator.dictionary.impl

import org.gotoobfuscator.Obfuscator
import org.gotoobfuscator.dictionary.IDictionary
import java.io.FileInputStream
import java.nio.charset.StandardCharsets

class CustomDictionary : IDictionary {
    companion object {
        @JvmStatic
        val list = ArrayList<String>()

        init {
            FileInputStream(Obfuscator.Instance.dictionaryFile).bufferedReader(StandardCharsets.UTF_8).use { list.addAll(it.readLines()) }
        }
    }

    private val used = ArrayList<String>()

    @Suppress("DuplicatedCode")
    override fun get(): String {
        var s : String
        var ticks = 0
        var repeatTime = 1

        do {
            s = run {
                val b = StringBuilder()

                repeat(repeatTime) {
                    b.append(list.random())
                }

                return@run b.toString()
            }

            if (ticks == list.size) {
                repeatTime++
                ticks = 0
            }

            ticks++
        } while (used.contains(s))

        used.add(s)

        return s
    }
}