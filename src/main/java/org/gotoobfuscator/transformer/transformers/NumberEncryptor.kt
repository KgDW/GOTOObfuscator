package org.gotoobfuscator.transformer.transformers

import org.gotoobfuscator.Obfuscator
import org.gotoobfuscator.transformer.Transformer
import org.gotoobfuscator.utils.ASMUtils
import org.gotoobfuscator.utils.InstructionModifier
import org.objectweb.asm.tree.*
import java.util.concurrent.ThreadLocalRandom

class NumberEncryptor : Transformer("NumberEncryptor") {
    private var encrypted = 0

    override fun transform(node : ClassNode) {
        for (method in node.methods) {
            val modifier = InstructionModifier()

            for (instruction in method.instructions) {
                if (ASMUtils.isNumber(instruction)) {
                    when (val number = ASMUtils.getNumber(instruction)) {
                        is Double -> {
                            val bits = java.lang.Double.doubleToLongBits(number)
                            val list = processLong(bits)

                            list.add(MethodInsnNode(INVOKESTATIC,"java/lang/Double","longBitsToDouble","(J)D",false))

                            modifier.replace(instruction,list)
                        }
                        is Float -> {
                            val bits = java.lang.Float.floatToIntBits(number)
                            val list = processInt(bits)

                            list.add(MethodInsnNode(INVOKESTATIC,"java/lang/Float","intBitsToFloat","(I)F",false))

                            modifier.replace(instruction,list)
                        }
                        is Long -> {
                            modifier.replace(instruction,processLong(number))
                        }
                        is Int,is Short,is Byte -> {
                            modifier.replace(instruction,processInt(number.toInt()))
                        }
                    }

                    encrypted++
                }
            }

            modifier.apply(method)
        }
    }

    private fun processInt(i : Int) : InsnList {
        val random = ThreadLocalRandom.current().nextInt()
        val xor = i.xor(random)
        val list = InsnList()
        val leftToLong = ThreadLocalRandom.current().nextBoolean()

        list.add(ASMUtils.createNumberNode(random))

        if (leftToLong) {
            list.add(InsnNode(I2L))
        }

        if (ThreadLocalRandom.current().nextBoolean()) { // ~
            list.add(ASMUtils.createNumberNode(xor.inv()))
            list.add(InsnNode(ICONST_M1))
            list.add(InsnNode(IXOR))
        } else {
            list.add(ASMUtils.createNumberNode(xor))
        }

        if (leftToLong) {
            list.add(InsnNode(I2L))
            list.add(InsnNode(LXOR))
            list.add(InsnNode(L2I))
        } else {
            list.add(InsnNode(IXOR))
        }

        return list
    }

    private fun processLong(l : Long) : InsnList {
        val list = InsnList()
        val random = ThreadLocalRandom.current().nextInt().toLong()
        val xor = l.xor(random)

        when (ThreadLocalRandom.current().nextInt(0,3)) {
            0 -> {
                list.add(LdcInsnNode(random))
                list.add(InsnNode(L2I))
                list.add(InsnNode(I2L))
            }
            1 -> {
                list.add(LdcInsnNode(random.toInt()))
                list.add(InsnNode(I2L))
            }
            2 -> {
                list.add(LdcInsnNode(random))
            }
        }

        if (ThreadLocalRandom.current().nextBoolean()) { // ~
            list.add(LdcInsnNode(xor.inv()))
            list.add(LdcInsnNode(-1L))
            list.add(InsnNode(LXOR))
        } else {
            list.add(LdcInsnNode(xor))
        }

        list.add(InsnNode(LXOR))

        return list
    }

    override fun finish(obfuscator : Obfuscator) {
        print("Encrypted $encrypted numbers")
    }
}