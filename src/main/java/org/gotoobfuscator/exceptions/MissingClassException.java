package org.gotoobfuscator.exceptions;

public final class MissingClassException extends RuntimeException {
    private final String missingClassName;

    public MissingClassException(String missingClassName) {
        this.missingClassName = missingClassName;

        addSuppressed(new Throwable("Missing class " + missingClassName));
    }

    public String getMissingClassName() {
        return missingClassName;
    }
}
