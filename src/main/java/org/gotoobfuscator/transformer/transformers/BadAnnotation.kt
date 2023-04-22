package org.gotoobfuscator.transformer.transformers

import org.gotoobfuscator.transformer.Transformer
import org.objectweb.asm.tree.AnnotationNode
import org.objectweb.asm.tree.ClassNode

class BadAnnotation : Transformer("BadAnnotation") {
    @Suppress("DuplicatedCode")
    override fun transform(node: ClassNode) {
        node.visibleAnnotations ?: run {
            node.visibleAnnotations = ArrayList<AnnotationNode>()
        }

        node.visibleAnnotations.add(AnnotationNode(""))

        for (method in node.methods) {
            method.visibleAnnotations ?: run {
                method.visibleAnnotations = ArrayList<AnnotationNode>()
            }

            method.visibleAnnotations.add(AnnotationNode(""))
        }

        for (field in node.fields) {
            field.visibleAnnotations ?: run {
                field.visibleAnnotations = ArrayList<AnnotationNode>()
            }

            field.visibleAnnotations.add(AnnotationNode(""))
        }
    }
}