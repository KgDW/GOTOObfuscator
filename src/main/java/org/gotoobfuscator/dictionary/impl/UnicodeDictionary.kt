package org.gotoobfuscator.dictionary.impl

import org.gotoobfuscator.dictionary.IDictionary

class UnicodeDictionary(val repeatTime : Int) : IDictionary {
    companion object {
        //private val chinese = ArrayList<Char>()
        val arabic = ArrayList<Char>()

        init {
            //for (i in 13312..40956) { //chinese
            //    chinese.add(i.toChar())
            //}

            for (i in 0x060C..0x06FE) { //Arabic
                arabic.add(i.toChar())
            }
        }
    }

    private val list = ArrayList<String>()

    @Suppress("DuplicatedCode")
    override fun get() : String {
        var s : String
        var ticks = 0
        var repeatTime = repeatTime

        do {
            s = run {
                val b = StringBuilder()

                repeat(repeatTime) {
                    b.append(arabic.random())
                }

                return@run b.toString()
            }

            if (ticks == arabic.size) {
                repeatTime++
                ticks = 0
            }

            ticks++
        } while (list.contains(s))

        list.add(s)

        return s
    }
}