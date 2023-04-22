package org.gotoobfuscator.packer

import org.apache.commons.io.IOUtils
import org.gotoobfuscator.transformer.SpecialTransformer
import org.gotoobfuscator.utils.ASMUtils
import org.gotoobfuscator.utils.InstructionModifier
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.tree.*
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.lang.StringBuilder
import java.security.MessageDigest
import java.util.concurrent.ThreadLocalRandom

class ConstantPacker : SpecialTransformer("ConstantPacker") {
    companion object {
        private const val STRING = 0
        private const val INT = 1
        private const val LONG = 2
        private const val FLOAT = 3
        private const val DOUBLE = 4
    }

    private val map = HashMap<String,Any>()

    @Suppress("DuplicatedCode")
    fun accept(node : ClassNode) {
        node.methods.forEach { method ->
            val modifier = InstructionModifier()

            method.instructions.forEach { insn ->
                when {
                    ASMUtils.isString(insn) -> {
                        val string = ASMUtils.getString(insn)

                        push(string)

                        modifier.replace(insn,
                            LdcInsnNode(sha512(string.encodeToByteArray())),
                            MethodInsnNode(INVOKESTATIC, "org/gotoobfuscator/runtime/Const","get","(Ljava/lang/String;)Ljava/lang/Object;"),
                            TypeInsnNode(CHECKCAST,"java/lang/String"))
                    }
                    insn is LdcInsnNode -> {
                        when (insn.cst) {
                            is Int -> {
                                push(insn.cst)

                                modifier.replace(insn,
                                    LdcInsnNode(sha512(insn.cst.toString().encodeToByteArray())),
                                    MethodInsnNode(INVOKESTATIC, "org/gotoobfuscator/runtime/Const","get","(Ljava/lang/String;)Ljava/lang/Object;"),
                                    TypeInsnNode(CHECKCAST,"java/lang/Integer"),
                                    MethodInsnNode(INVOKEVIRTUAL,"java/lang/Integer","intValue","()I")
                                )
                            }
                            is Long -> {
                                push(insn.cst)

                                modifier.replace(insn,
                                    LdcInsnNode(sha512(insn.cst.toString().encodeToByteArray())),
                                    MethodInsnNode(INVOKESTATIC, "org/gotoobfuscator/runtime/Const","get","(Ljava/lang/String;)Ljava/lang/Object;"),
                                    TypeInsnNode(CHECKCAST,"java/lang/Long"),
                                    MethodInsnNode(INVOKEVIRTUAL,"java/lang/Long","longValue","()J")
                                )
                            }
                            is Double -> {
                                push(insn.cst)

                                modifier.replace(insn,
                                    LdcInsnNode(sha512(insn.cst.toString().encodeToByteArray())),
                                    MethodInsnNode(INVOKESTATIC, "org/gotoobfuscator/runtime/Const","get","(Ljava/lang/String;)Ljava/lang/Object;"),
                                    TypeInsnNode(CHECKCAST,"java/lang/Double"),
                                    MethodInsnNode(INVOKEVIRTUAL,"java/lang/Double","doubleValue","()D")
                                )
                            }
                            is Float -> {
                                push(insn.cst)

                                modifier.replace(insn,
                                    LdcInsnNode(sha512(insn.cst.toString().encodeToByteArray())),
                                    MethodInsnNode(INVOKESTATIC, "org/gotoobfuscator/runtime/Const","get","(Ljava/lang/String;)Ljava/lang/Object;"),
                                    TypeInsnNode(CHECKCAST,"java/lang/Float"),
                                    MethodInsnNode(INVOKEVIRTUAL,"java/lang/Float","floatValue","()F")
                                )
                            }
                        }
                    }
                    insn is IntInsnNode -> {
                        push(insn.operand)

                        modifier.replace(insn,
                            LdcInsnNode(sha512(insn.operand.toString().encodeToByteArray())),
                            MethodInsnNode(INVOKESTATIC, "org/gotoobfuscator/runtime/Const","get","(Ljava/lang/String;)Ljava/lang/Object;"),
                            TypeInsnNode(CHECKCAST,"java/lang/Integer"),
                            MethodInsnNode(INVOKEVIRTUAL,"java/lang/Integer","intValue","()I")
                        )
                    }
                }
            }

            modifier.apply(method)
        }
    }

    private fun push(obj : Any) {
        val key = sha512(obj.toString().encodeToByteArray())

        if (!map.containsKey(key)) {
            map[key] = obj
        }
    }

    fun buildClass() : ByteArray {
        return IOUtils.toByteArray(ConstantPacker::class.java.getResourceAsStream("/org/gotoobfuscator/runtime/Const.class"))
    }

    private fun sha512(b : ByteArray) : String {
        val builder = StringBuilder()
        val digest = MessageDigest.getInstance("SHA-512")

        val digested = digest.digest(b)

        for (byte in digested) {
            builder.append(Integer.toHexString(byte.toInt().and(0xFF)))
        }

        return builder.toString()
    }

    fun build() : ByteArray {
        val bos = ByteArrayOutputStream()
        val dos = DataOutputStream(bos)

        dos.writeInt(map.size)

        map.forEach { entry ->
            val key = entry.key
            val obj = entry.value

            dos.writeUTF(key)

            when (obj) {
                is String -> {
                    dos.writeInt(STRING)
                    dos.writeUTF(obj)
                }
                is Int -> {
                    dos.writeInt(INT)
                    dos.writeInt(obj)
                }
                is Long -> {
                    dos.writeInt(LONG)
                    dos.writeLong(obj)
                }
                is Float -> {
                    dos.writeInt(FLOAT)
                    dos.writeFloat(obj)
                }
                is Double -> {
                    dos.writeInt(DOUBLE)
                    dos.writeDouble(obj)
                }
            }
        }

        dos.close()

        return bos.toByteArray()
    }
}