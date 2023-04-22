package org.gotoobfuscator.transformer.transformers

import org.gotoobfuscator.transformer.Transformer
import org.gotoobfuscator.utils.RandomUtils
import org.objectweb.asm.tree.ClassNode

class VariableRename : Transformer("VariableRename") {
    override fun transform(node: ClassNode) {
        for (method in node.methods) {
            if (method.localVariables != null) {
                for (localVariable in method.localVariables) {
                    if (localVariable.name != "this")
                        localVariable.name = RandomUtils.randomIllegalJavaName()
                }
            }
        }
    }
}