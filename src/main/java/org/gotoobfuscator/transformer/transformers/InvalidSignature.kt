package org.gotoobfuscator.transformer.transformers

import org.gotoobfuscator.transformer.Transformer
import org.objectweb.asm.tree.ClassNode
import java.util.concurrent.ThreadLocalRandom

class InvalidSignature : Transformer("InvalidSignature") {
    override fun transform(node : ClassNode) {
        if (node.signature == null) node.signature = randomSignature()
        node.methods.forEach { methodNode -> if (methodNode.signature == null) methodNode.signature = randomSignature() }
        node.fields.forEach { fieldNode -> if (fieldNode.signature == null) fieldNode.signature = randomSignature() }
    }

    private fun randomSignature() : String {
        return when (ThreadLocalRandom.current().nextInt(0,4)) {
            0 -> {
                "[B"
            }
            1 -> {
                "[I"
            }
            2 -> {
                "[Z"
            }
            3 -> {
                "[J"
            }
            else -> {
                "[B"
            }
        }
    }
}