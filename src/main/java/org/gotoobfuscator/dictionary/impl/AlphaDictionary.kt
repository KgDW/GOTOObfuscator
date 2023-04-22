package org.gotoobfuscator.dictionary.impl

import org.gotoobfuscator.dictionary.IDictionary

// By proguard
class AlphaDictionary : IDictionary {
    var index = 0

    override fun get() : String {
        return get(index++)
    }

    fun get(index: Int): String {
        val baseIndex = index / 26
        val offset = index % 26

        val newChar = ((if (offset < 26) 'a' else 'A' - 26) + offset)

        return if (baseIndex == 0) String(charArrayOf(newChar)) else get(baseIndex - 1) + newChar
    }
}