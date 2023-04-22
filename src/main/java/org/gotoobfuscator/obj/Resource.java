package org.gotoobfuscator.obj;

public final class Resource {
    private final String name;
    private final byte[] data;

    public Resource(String name, byte[] data) {
        this.name = name;
        this.data = data;
    }

    public String getName() {
        return name;
    }

    public byte[] getData() {
        return data;
    }
}
