package game.objects.enums;

/**
 * Enum for station types
 */
public enum StationType {
    PLANNED,
    BUILDING,
    REGULAR,    // Square
    TRANSFER,   // Circle (when near different color)
    TERMINAL,   // Diamond
    TRANSIT     // Cross
}
