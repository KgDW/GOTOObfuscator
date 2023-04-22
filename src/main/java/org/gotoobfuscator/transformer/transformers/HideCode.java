package org.gotoobfuscator.transformer.transformers;

import org.gotoobfuscator.transformer.Transformer;
import org.objectweb.asm.tree.ClassNode;

public final class HideCode extends Transformer {
    public HideCode() {
        super("HideCode");
    }

    @Override
    public void transform(ClassNode node) {
        if (((node.visibleAnnotations == null || node.visibleAnnotations.isEmpty()) && (node.invisibleAnnotations == null || node.invisibleAnnotations.isEmpty())) && notSynthetic(node.access)) node.access |= ACC_SYNTHETIC;

        node.fields.forEach(fieldNode -> {
            if ((fieldNode.visibleAnnotations != null && fieldNode.visibleAnnotations.isEmpty()) || (fieldNode.invisibleAnnotations != null && fieldNode.invisibleAnnotations.isEmpty())) return;

            if (notSynthetic(fieldNode.access)) {
                fieldNode.access |= ACC_SYNTHETIC;
            }
        });

        node.methods.forEach(methodNode -> {
            if ((methodNode.visibleAnnotations != null && methodNode.visibleAnnotations.isEmpty()) || (methodNode.invisibleAnnotations != null && methodNode.invisibleAnnotations.isEmpty())) return;

            if (notSynthetic(methodNode.access)) {
                methodNode.access |= ACC_SYNTHETIC;
            }

            if ((methodNode.access & ACC_BRIDGE) == 0 && !methodNode.name.startsWith("<")) {
                methodNode.access |= ACC_BRIDGE;
            }
        });
    }

    private boolean notSynthetic(int access) {
        return (access & ACC_SYNTHETIC) == 0;
    }
}
