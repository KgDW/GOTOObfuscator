package org.gotoobfuscator.transformer;

import org.gotoobfuscator.Obfuscator;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;

public abstract class Transformer implements Opcodes {
    private final String name;

    protected Transformer(String name) {
        this.name = name;
    }

    public void onStart(Obfuscator obfuscator) { }

    public void transform(ClassNode node) { }

    public void finish(Obfuscator obfuscator) { }

    protected final Label[] newLabels(int size) {
        final Label[] labels = new Label[size];

        for (int i = 0; i < size; i++) {
            labels[i] = new Label();
        }

        return labels;
    }

    protected final void print(Object o) {
        System.out.println("[" + name + "] " + o);
    }

    public String getName() {
        return name;
    }
}
