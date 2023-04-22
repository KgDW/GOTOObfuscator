package org.gotoobfuscator.utils

import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.tree.*
import java.lang.NullPointerException

object ASMUtils {
    fun isString(node : AbstractInsnNode) : Boolean {
        return node is LdcInsnNode && node.cst is String
    }

    fun getString(node : AbstractInsnNode) : String {
        return (node as LdcInsnNode).cst.toString()
    }

    fun getNumber(node : AbstractInsnNode) : Number {
        when (node) {
            is LdcInsnNode -> {
                return node.cst as Number
            }
            is IntInsnNode -> {
                when (node.opcode) {
                    SIPUSH -> {
                        return node.operand.toShort()
                    }
                    BIPUSH -> {
                        return node.operand.toByte()
                    }
                }
            }
            else -> {
                when (node.opcode) {
                    ICONST_M1 -> return -1
                    ICONST_0 -> return 0
                    ICONST_1 -> return 1
                    ICONST_2 -> return 2
                    ICONST_3 -> return 3
                    ICONST_4 -> return 4
                    ICONST_5 -> return 5
                    LCONST_0 -> return 0L
                    LCONST_1 -> return 1L
                    FCONST_0 -> return 0.0F
                    FCONST_1 -> return 1.0F
                    FCONST_2 -> return 2.0F
                    DCONST_0 -> return 0.0
                    DCONST_1 -> return 1.0
                }
            }
        }

        throw IllegalArgumentException()
    }

    fun isNumber(node : AbstractInsnNode) : Boolean {
        when (node) {
            is LdcInsnNode -> {
                val cst = node.cst
                return cst is Double || cst is Float || cst is Long || cst is Int
            }
            is IntInsnNode -> {
                if (node.opcode == SIPUSH || node.opcode == BIPUSH) {
                    return true
                }
            }
            else -> {
                when (node.opcode) {
                    ICONST_M1 -> return true
                    ICONST_0 -> return true
                    ICONST_1 -> return true
                    ICONST_2 -> return true
                    ICONST_3 -> return true
                    ICONST_4 -> return true
                    ICONST_5 -> return true
                    LCONST_0 -> return true
                    LCONST_1 -> return true
                    FCONST_0 -> return true
                    FCONST_1 -> return true
                    FCONST_2 -> return true
                    DCONST_0 -> return true
                    DCONST_1 -> return true
                }
            }
        }

        return false
    }

    fun createNumberNode(value : Int) : AbstractInsnNode {
        when (val opcode = getNumberOpcode(value)) {
            ICONST_M1, ICONST_0, ICONST_1, ICONST_2, ICONST_3, ICONST_4, ICONST_5 -> return InsnNode(opcode)

            else -> {
                if (value >= -128 && value <= 127) return IntInsnNode(BIPUSH, value)

                return if (value >= -32768 && value <= 32767) IntInsnNode(SIPUSH, value) else LdcInsnNode(value)
            }
        }
    }

    fun createNumberNode(value : Short) : AbstractInsnNode {
        when(value) {
            (-1).toShort() -> return InsnNode(ICONST_M1)
            0.toShort() -> return InsnNode(ICONST_0)
            1.toShort() -> return InsnNode(ICONST_1)
            2.toShort() -> return InsnNode(ICONST_2)
            3.toShort() -> return InsnNode(ICONST_3)
            4.toShort() -> return InsnNode(ICONST_4)
            5.toShort() -> return InsnNode(ICONST_5)

            else -> {
                if (value >= -128 && value <= 127) return IntInsnNode(BIPUSH,value.toInt())
            }
        }

        return IntInsnNode(SIPUSH,value.toInt())
    }

    fun createNumberNode(value : Byte) : AbstractInsnNode {
        when(value) {
            (-1).toByte() -> return InsnNode(ICONST_M1)
            0.toByte() -> return InsnNode(ICONST_0)
            1.toByte() -> return InsnNode(ICONST_1)
            2.toByte() -> return InsnNode(ICONST_2)
            3.toByte() -> return InsnNode(ICONST_3)
            4.toByte() -> return InsnNode(ICONST_4)
            5.toByte() -> return InsnNode(ICONST_5)
        }

        return IntInsnNode(BIPUSH,value.toInt())
    }

    fun getNumberOpcode(value : Int) : Int {
        when(value) {
            -1 -> return ICONST_M1
            0 -> return ICONST_0
            1 -> return ICONST_1
            2 -> return ICONST_2
            3 -> return ICONST_3
            4 -> return ICONST_4
            5 -> return ICONST_5

            else -> {
                if (value >= -128 && value <= 127) return BIPUSH
                return if (value >= -32768 && value <= 32767) SIPUSH else LDC
            }
        }
    }

    @Suppress("SpellCheckingInspection")
    fun getClinitMethodNode(node: ClassNode) : MethodNode? {
        return getMethodNode(node, "<clinit>")
    }

    fun getInitMethodNode(node : ClassNode) : MethodNode {
        var methodNode : MethodNode? = getMethodNode(node, "<init>")

        if (methodNode == null) {
            println("WTF?! ${node.name} doest have init method???!")

            methodNode = MethodNode(ACC_PUBLIC, "<init>", "()V", null, null).apply {
                visitCode()
                visitVarInsn(ALOAD,0)
                visitMethodInsn(INVOKESPECIAL,"java/lang/Object","<init>","()V",false)
                visitInsn(RETURN)
                visitEnd()
            }
        }

        return methodNode
    }

    @Suppress("SpellCheckingInspection")
    fun getClinitMethodNodeOrCreateNew(node: ClassNode) : MethodNode {
        var method = getMethodNode(node, "<clinit>")

        if (method == null) {
            method = MethodNode(ACC_STATIC,"<clinit>","()V",null,null)

            method.instructions.add(InsnNode(RETURN))

            node.methods.add(method)
        }

        return method
    }

    fun getMethodNode(node: ClassNode, methodName: String): MethodNode? {
        return node.methods.find {
            it.name == methodName
        }
    }

    fun isInterfaceClass(node : ClassNode) : Boolean {
        return node.access.and(ACC_INTERFACE) != 0
    }

    fun isSpecialMethod(node : MethodNode) : Boolean {
        return node.access.and(ACC_NATIVE) != 0 || node.access.and(ACC_ABSTRACT) != 0
    }
}