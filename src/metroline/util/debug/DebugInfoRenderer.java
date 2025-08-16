package metroline.util.debug;

import metroline.core.world.World;
import metroline.objects.enums.Direction;
import metroline.objects.gameobjects.PathPoint;
import metroline.objects.gameobjects.Station;
import metroline.objects.gameobjects.Tunnel;
import metroline.objects.gameobjects.Label;
import java.awt.*;
import java.util.Map;


public class DebugInfoRenderer {
    public static void renderDebugInfo(Graphics2D g, Object obj, World world, int startY) {
        int yPos = startY;

        if (obj instanceof Station) {
            renderStationDebugInfo(g, (Station) obj, world, yPos);
        } else if (obj instanceof Label) {
            renderLabelDebugInfo(g, (Label) obj, yPos);
        } else if (obj instanceof Tunnel) {
            renderTunnelDebugInfo(g, (Tunnel) obj, yPos);
        }
    }

    private static void renderStationDebugInfo(Graphics2D g, Station station, World world, int yPos) {
        yPos += 15;
        g.drawString("=== SELECTED STATION ===", 10, yPos);
        yPos += 15;
        g.drawString("Hash: " + station.hashCode(), 10, yPos);
        yPos += 15;
        g.drawString("Position: (" + station.getX() + "," + station.getY() + ")", 10, yPos);
        yPos += 15;
        g.drawString("Name: " + station.getName(), 10, yPos);
        yPos += 15;
        g.drawString("Type: " + station.getType(), 10, yPos);
        yPos += 15;
        g.drawString("Color: " + String.format("#%06X", (0xFFFFFF & station.getColor().getRGB())), 10, yPos);
        yPos += 15;

        if (!world.getLabelsForStation(station).isEmpty()) {
            g.drawString("Labels (" + world.getLabelsForStation(station).size() + "):", 10, yPos);
            yPos += 15;

            for (Label label : world.getLabelsForStation(station)) {
                g.drawString("- '" + label.getText() + "' at (" +
                        label.getX() + "," + label.getY() + ")", 20, yPos);
                yPos += 15;
            }
        }
        // Информация о соединениях
        if (!station.getConnections().isEmpty()) {
            g.drawString("Connections:", 10, yPos);
            yPos += 15;

            for (Map.Entry<Direction, Station> entry : station.getConnections().entrySet()) {
                g.drawString("- " + entry.getKey() + " -> Station " + entry.getValue().getName(), 20, yPos);
                yPos += 15;
            }
        }
    }

    private static void renderLabelDebugInfo(Graphics2D g, Label label, int yPos) {
        yPos += 15;
        g.drawString("=== SELECTED LABEL ===", 10, yPos);
        yPos += 15;
        g.drawString("Hash: " + label.hashCode(), 10, yPos);
        yPos += 15;
        g.drawString("Text: '" + label.getText() + "'", 10, yPos);
        yPos += 15;
        g.drawString("Position: (" + label.getX() + "," + label.getY() + ")", 10, yPos);
        yPos += 15;
        g.drawString("Parent Station: " + label.getParentGameObject().getName() +
                " (" + label.getParentGameObject().getX() + "," +
                label.getParentGameObject().getY() + ")", 10, yPos);
    }

    private static void renderTunnelDebugInfo(Graphics2D g, Tunnel tunnel, int yPos) {
        yPos += 15;
        g.drawString("=== SELECTED TUNNEL ===", 10, yPos);
        yPos += 15;
        g.drawString("Hash: " + tunnel.hashCode(), 10, yPos);
        yPos += 15;

        // Информация о станциях
        Station start = tunnel.getStart();
        Station end = tunnel.getEnd();
        g.drawString("From: " + start.getName() + " (" + start.getX() + "," + start.getY() + ")", 10, yPos);
        yPos += 15;
        g.drawString("To: " + end.getName() + " (" + end.getX() + "," + end.getY() + ")", 10, yPos);
        yPos += 15;

        // Информация о точках пути
        g.drawString("Path points (" + tunnel.getPath().size() + "):", 10, yPos);
        yPos += 15;

        for (int i = 0; i < tunnel.getPath().size(); i++) {
            PathPoint p = tunnel.getPath().get(i);
            String pointType = (i == 0) ? "START" : (i == tunnel.getPath().size()-1) ? "END" : "CTRL";
            g.drawString(pointType + " " + i + ": (" + p.getX() + "," + p.getY() + ")", 20, yPos);
            yPos += 15;
        }
    }
}
