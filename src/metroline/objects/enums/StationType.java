package metroline.objects.enums;

/**
 * Enum for station types
 */
public enum StationType {
    DESTROYED,
    CLOSED,
    PLANNED,
    BUILDING,
    REGULAR,    // Square
    TRANSFER,   // Circle (when near different color)
    TERMINAL,   // Diamond
    TRANSIT     // Cross
}
