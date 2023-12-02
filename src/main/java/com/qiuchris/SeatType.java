package com.qiuchris;

public enum SeatType {
    GENERAL("General"),
    RESTRICTED("Restricted"),
    ANY("Any");

    private final String name;

    SeatType(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }
}
