package org.gotoobfuscator.transformer.transformers

import org.gotoobfuscator.transformer.Transformer
import org.objectweb.asm.tree.ClassNode

class SourceRename : Transformer("SourceRename") {
    override fun transform(node: ClassNode) {
        node.sourceFile = ""
        node.sourceDebug = ""
    }
}