package com.pos.posApps.Service;

public class StaffRuleException extends RuntimeException {
    private final boolean notFound;

    public StaffRuleException(String message) {
        this(message, false);
    }

    public StaffRuleException(String message, boolean notFound) {
        super(message);
        this.notFound = notFound;
    }

    public boolean isNotFound() {
        return notFound;
    }
}
