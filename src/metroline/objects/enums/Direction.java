package metroline.objects.enums;

/**
 * Enum for station directions
 */
public enum Direction {
    NORTH(0, -1),
    NORTHEAST(1, -1),
    EAST(1, 0),
    SOUTHEAST(1, 1),
    SOUTH(0, 1),
    SOUTHWEST(-1, 1),
    WEST(-1, 0),
    NORTHWEST(-1, -1);

    private final int dx;
    private final int dy;

    Direction(int dx, int dy) {
        this.dx = dx;
        this.dy = dy;
    }

    public int getDx() { return dx; }
    public int getDy() { return dy; }
    public static Direction[] getOrthogonalDirections() {
        return new Direction[]{NORTH, EAST, SOUTH, WEST};
    }
    public Direction getOpposite() {
        switch (this) {
            case NORTH: return SOUTH;
            case NORTHEAST: return SOUTHWEST;
            case EAST: return WEST;
            case SOUTHEAST: return NORTHWEST;
            case SOUTH: return NORTH;
            case SOUTHWEST: return NORTHEAST;
            case WEST: return EAST;
            case NORTHWEST: return SOUTHEAST;
            default: return NORTH;
        }
    }
}
