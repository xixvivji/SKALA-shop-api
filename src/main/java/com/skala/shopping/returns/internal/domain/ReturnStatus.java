package com.skala.shopping.returns.internal.domain;

public enum ReturnStatus {
    REQUESTED, COLLECTING, INSPECTING, APPROVED, REJECTED, REFUNDED;

    public boolean canTransitionTo(ReturnStatus next) {
        return switch (this) {
            case REQUESTED -> next == COLLECTING;
            case COLLECTING -> next == INSPECTING;
            case INSPECTING -> next == APPROVED || next == REJECTED;
            case APPROVED -> next == REFUNDED;
            case REJECTED, REFUNDED -> false;
        };
    }
}
