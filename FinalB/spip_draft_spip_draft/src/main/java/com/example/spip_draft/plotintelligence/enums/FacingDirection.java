package com.example.spip_draft.plotintelligence.enums;

/**
 * Canonical facing directions used by the plot intelligence engine.
 */
public enum FacingDirection {
    NORTH,
    SOUTH,
    EAST,
    WEST,
    NORTH_EAST,
    NORTH_WEST,
    SOUTH_EAST,
    SOUTH_WEST,
    UNKNOWN;

    public boolean isCornerDirection() {
        return this == NORTH_EAST
                || this == NORTH_WEST
                || this == SOUTH_EAST
                || this == SOUTH_WEST;
    }
}