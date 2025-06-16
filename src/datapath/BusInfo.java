package datapath;

import java.awt.Point;
import java.util.List;

public class BusInfo {
    public final String id; // Bus ID (matches BusID enum)
    public final List<Point> path; // List of points defining the bus path
    public final String color; // Optional color (hex, e.g., "#000000")
    public final float thickness; // Line thickness

    public BusInfo(String id, List<Point> path, String color, float thickness) {
        this.id = id;
        this.path = path;
        this.color = color;
        this.thickness = thickness;
    }
}