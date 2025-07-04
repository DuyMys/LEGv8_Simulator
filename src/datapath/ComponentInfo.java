package datapath;

public class ComponentInfo {
    public final String id; // Component ID (matches ComponentID enum)
    public final int x; // Top-left x-coordinate
    public final int y; // Top-left y-coordinate
    public final int width; // Component width
    public final int height; // Component height
    public final String label; // Component label (can be multi-line)
    public final Integer labelRelativeX; // Optional x-offset for label
    public final Integer labelRelativeY; // Optional y-offset for label

    public ComponentInfo(String id, int x, int y, int width, int height, 
                         String label, Integer labelRelativeX, Integer labelRelativeY) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.label = label;
        this.labelRelativeX = labelRelativeX;
        this.labelRelativeY = labelRelativeY;
    }
}