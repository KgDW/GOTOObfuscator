package org.gotoobfuscator.utils

import org.gotoobfuscator.Obfuscator
import org.objectweb.asm.ClassWriter
import java.util.*

class GotoClassWriter(flags : Int) : ClassWriter(flags) {
    override fun getCommonSuperClass(type1 : String, type2 : String): String {
        val superClasses1 = getSuperClasses(type1)
        val superClasses2 = getSuperClasses(type2)
        val size = superClasses1.size.coerceAtMost(superClasses2.size)
        var i = 0
        while (i < size && superClasses1[i] == superClasses2[i]) {
            i++
        }
        return if (i == 0) {
            "java/lang/Object"
        } else {
            superClasses1[i - 1]
        }
    }

    private fun getSuperClasses(type : String) : ArrayList<String> {
        var superType : String? = type
        val superClass = ArrayList<String>()

        superClass.add(superType!!)

        while (Obfuscator.Instance.getClassNode(superType!!).superName.also { superType = it } != null) {
            superClass.add(superType!!)
        }

        superClass.reverse()

        return superClass
    }
}