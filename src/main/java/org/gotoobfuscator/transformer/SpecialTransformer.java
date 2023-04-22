package org.gotoobfuscator.transformer;

public abstract class SpecialTransformer {
    private final String name;
    private boolean enable;

    protected SpecialTransformer(String name) {
        this.name = name;
    }

    public final String getName() {
        return name;
    }

    public final boolean isEnable() {
        return enable;
    }

    public final void setEnable(boolean enable) {
        this.enable = enable;
    }
}
