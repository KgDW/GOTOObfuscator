package org.gotoobfuscator.obj;

import org.gotoobfuscator.exceptions.MissingClassException;
import org.gotoobfuscator.utils.GotoClassWriter;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

public final class ClassWrapper {
    private final String name;
    private final ClassNode classNode;
    private final byte[] originalBytes;

    public ClassWrapper(byte[] data) {
        this.originalBytes = data;
        this.classNode = new ClassNode();

        final ClassReader reader = new ClassReader(data);
        reader.accept(classNode,ClassReader.SKIP_FRAMES);

        this.name = this.classNode.name;
    }

    public ClassWrapper(ClassNode classNode) {
        this.classNode = classNode;
        this.name = this.classNode.name;
        this.originalBytes = new byte[0];
    }

    public ClassWrapper(ClassNode classNode, byte[] originalBytes) {
        this.classNode = classNode;
        this.name = this.classNode.name;
        this.originalBytes = originalBytes;
    }

    public byte[] toByteArray(boolean useComputeMaxs) {
        GotoClassWriter writer = new GotoClassWriter(useComputeMaxs ? ClassWriter.COMPUTE_MAXS : ClassWriter.COMPUTE_FRAMES);

        try {
            classNode.accept(writer);

            return writer.toByteArray();
        } catch (TypeNotPresentException | MissingClassException e) {
            if (e instanceof TypeNotPresentException) {
                System.err.println("Can't find the class: " + ((TypeNotPresentException) e).typeName() + " Try to use COMPUTE_MAXS");
            } else {
                System.err.println("Can't find the class: " + ((MissingClassException) e).getMissingClassName() + " Try to use COMPUTE_MAXS");
            }
        } catch (Throwable e) {
            e.addSuppressed(new Throwable("Write out" + name + "Error Trying to use COMPUTE_MAXS	"));

            e.printStackTrace();
        }

        try {
            writer = new GotoClassWriter(ClassWriter.COMPUTE_MAXS);

            classNode.accept(writer);

            return writer.toByteArray();
        } catch (Throwable e) {
            e.addSuppressed(new Throwable("Unable to write will return null bytes: " + name));
            e.printStackTrace();

            return new byte[0];
        }
    }

    public String getName() {
        return name;
    }

    public ClassNode getClassNode() {
        return classNode;
    }

    public byte[] getOriginalBytes() {
        return originalBytes;
    }
}
