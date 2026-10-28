package com.launchcheck.audit.safety;

public class UnsafeTargetException extends RuntimeException {
    private final String code;

    public UnsafeTargetException(String code) {
        super(code);
        this.code = code;
    }

    public String code() { return code; }
}
