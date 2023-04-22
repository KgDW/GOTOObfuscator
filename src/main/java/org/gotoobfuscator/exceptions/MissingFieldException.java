package org.gotoobfuscator.exceptions;

public final class MissingFieldException  extends RuntimeException {
    private final String missingFieldName;
    private final String missingFieldDesc;

    public MissingFieldException(String missingFieldName, String missingFieldDesc) {
        this.missingFieldName = missingFieldName;
        this.missingFieldDesc = missingFieldDesc;

        addSuppressed(new Throwable("Missing field " + missingFieldName + missingFieldDesc));
    }

    public String getMissingFieldName() {
        return missingFieldName;
    }

    public String getMissingFieldDesc() {
        return missingFieldDesc;
    }
}
