package org.gotoobfuscator.dictionary.impl

import org.gotoobfuscator.dictionary.IDictionary

// By proguard
class NumberDictionary : IDictionary {
    var index = 0

    override fun get() : String {
        return get(index++)
    }

    fun get(index: Int): String {
        val baseIndex = index / 10
        val offset = index % 10

        val newChar = ((if (offset < 10) '0' else '9' - 10) + offset)

        return if (baseIndex == 0) String(charArrayOf(newChar)) else get(baseIndex - 1) + newChar
    }
}