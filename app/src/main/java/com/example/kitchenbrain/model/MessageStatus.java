package com.example.kitchenbrain.model;

public enum MessageStatus {
    SENDING("sending"),
    SENT("sent"),
    DELIVERED("delivered"),
    READ("read"),
    FAILED("failed");

    private final String value;

    MessageStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static MessageStatus fromString(String value) {
        if (value == null) return SENDING;
        for (MessageStatus status : values()) {
            if (status.value.equalsIgnoreCase(value)) {
                return status;
            }
        }
        return SENDING;
    }

    public boolean isTerminal() {
        return this == READ || this == FAILED;
    }

    public boolean canRetry() {
        return this == FAILED || this == SENDING;
    }
}
