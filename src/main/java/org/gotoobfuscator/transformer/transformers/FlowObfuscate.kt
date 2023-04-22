package org.gotoobfuscator.transformer.transformers

import org.gotoobfuscator.transformer.Transformer
import org.gotoobfuscator.utils.ASMUtils
import org.gotoobfuscator.utils.InstructionModifier
import org.objectweb.asm.Type
import org.objectweb.asm.tree.*
import java.util.concurrent.ThreadLocalRandom
import kotlin.math.max

// 没完成
class FlowObfuscate : Transformer("FlowObfuscate") {
    override fun transform(node: ClassNode) {
        for (method in node.methods) {
            if (method.instructions.size() == 0) continue

            val modifier = InstructionModifier()
            val objReturn = Type.getReturnType(method.desc).sort == Type.OBJECT
            val labelNode = LabelNode()
            var retLabel = method.instructions.toArray().copyOfRange(0, max(0,method.instructions.size() - 1)).findLast { it is LabelNode }

            if (objReturn) {
                retLabel = run {
                    if (method.instructions.last is LabelNode) {
                        return@run method.instructions.last
                    } else {
                        method.instructions.insert(method.instructions.last,labelNode)

                        return@run labelNode
                    }
                }
            }

            if (retLabel is LabelNode) {
                for (instruction in method.instructions) {
                    if (instruction is LabelNode) {
                        if (instruction == retLabel) {
                            continue
                        }

                        val list = InsnList()

                        when (ThreadLocalRandom.current().nextInt(0, 5)) {
                            0 -> { // if (null == null)
                                list.add(InsnNode(ACONST_NULL))
                                list.add(JumpInsnNode(IFNONNULL, retLabel))
                            }
                            1 -> { // if (任何大于零的数 > 0)
                                list.add(
                                    ASMUtils.createNumberNode(
                                        ThreadLocalRandom.current().nextInt(Int.MIN_VALUE, 0)
                                    )
                                )
                                list.add(InsnNode(ICONST_0))
                                list.add(JumpInsnNode(IF_ICMPGE, retLabel))
                            }
                            2 -> { // if (任何小于零的数 < 0)
                                list.add(
                                    ASMUtils.createNumberNode(
                                        ThreadLocalRandom.current().nextInt(1, Int.MAX_VALUE)
                                    )
                                )
                                list.add(InsnNode(ICONST_0))
                                list.add(JumpInsnNode(IF_ICMPLE, retLabel))
                            }
                            3 -> { // if (true)
                                list.add(InsnNode(ICONST_1))
                                list.add(JumpInsnNode(IFEQ, retLabel))
                            }
                            4 -> { // if (两个等数相等)
                                val i = ThreadLocalRandom.current().nextInt()

                                list.add(ASMUtils.createNumberNode(i))
                                list.add(ASMUtils.createNumberNode(i))
                                list.add(JumpInsnNode(IF_ICMPNE, retLabel))
                            }
                        }

                        modifier.append(instruction, list)
                    }
                }

                modifier.apply(method)

                if (objReturn) {
                    method.instructions.insert(retLabel,InsnNode(ARETURN))
                    method.instructions.insert(retLabel,InsnNode(ACONST_NULL))
                }
            }
        }
    }
}