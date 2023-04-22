package org.gotoobfuscator.transformer.transformers

import org.gotoobfuscator.Obfuscator
import org.gotoobfuscator.dictionary.impl.UnicodeDictionary
import org.gotoobfuscator.transformer.Transformer
import org.gotoobfuscator.utils.ASMUtils
import org.gotoobfuscator.utils.InstructionModifier
import org.gotoobfuscator.utils.RandomUtils
import org.objectweb.asm.Handle
import org.objectweb.asm.tree.*
import java.util.concurrent.ThreadLocalRandom

class JunkCode : Transformer("JunkCode") {
    private val repeat = "${RandomUtils.randomString(4,UnicodeDictionary.arabic)}\n".repeat(ThreadLocalRandom.current().nextInt(1000,2000))

    private var handleMethods = 0

    @Suppress("SpellCheckingInspection")
    override fun transform(node: ClassNode) {
        if (ASMUtils.isInterfaceClass(node)) return

        for (method in node.methods) {
            if (ASMUtils.isSpecialMethod(method)) continue

            val modifier = InstructionModifier()

            for (instruction in method.instructions) {
                if (instruction is LabelNode) {
                    if (method.instructions.indexOf(instruction) == method.instructions.size() - 1) continue

                    val label = LabelNode()

                    val list = InsnList().apply {
                        add(label)
                        add(ASMUtils.createNumberNode(ThreadLocalRandom.current().nextInt(0, Int.MAX_VALUE)))
                        add(JumpInsnNode(IFGE, instruction))

                        add(InvokeDynamicInsnNode(" ","()V", Handle(H_INVOKESTATIC," "," ","(IJIJIJIJIJIJIJIJIJIJIJIJIJ)L;",false)))

                        add(InsnNode(ACONST_NULL))
                        add(MethodInsnNode(INVOKESTATIC,repeat,repeat,"([[[[[[[[[[[[[[[[[[[[[L;)V",false))
                    }

                    modifier.prepend(instruction,list)
                }
            }

            modifier.apply(method)

            handleMethods++
        }
    }

    override fun finish(obfuscator : Obfuscator) {
        print("Handled $handleMethods methods")
    }
}