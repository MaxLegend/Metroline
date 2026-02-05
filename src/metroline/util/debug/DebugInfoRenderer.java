package metroline.util.debug;

import metroline.core.world.World;
import metroline.objects.enums.Direction;
import metroline.objects.gameobjects.*;
import metroline.screens.render.StationRender;
import metroline.util.MetroLogger;

import java.awt.*;
import java.util.Map;


public class DebugInfoRenderer {
    public static void logMemoryUsage(String context) {
        Runtime runtime = Runtime.getRuntime();
        long usedMB = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024;
        long maxMB = runtime.maxMemory() / 1024 / 1024;
        MetroLogger.logInfo(context + " - Memory: " + usedMB + "MB / " + maxMB + "MB");
    }
    public static void renderDebugInfo(Graphics2D g, Object obj, World world, int startY) {
        int yPos = startY;

        if (obj instanceof Station) {
            renderStationDebugInfo(g, (Station) obj, world, yPos);
        } else if (obj instanceof StationLabel) {
            renderLabelDebugInfo(g, (StationLabel) obj, yPos);
        } else if (obj instanceof Tunnel) {
            renderTunnelDebugInfo(g, (Tunnel) obj, yPos);
        }
        else if (obj instanceof River) {
            renderRiverDebugInfo(g, (River) obj, yPos);
        } else if (obj instanceof RiverPoint) {
            renderRiverPointDebugInfo(g, (RiverPoint) obj, yPos);
        }
    }
    private static void renderRiverDebugInfo(Graphics2D g, River river, int yPos) {
        yPos += 15;
        g.drawString("=== SELECTED RIVER ===", 10, yPos);
        yPos += 15;
        g.drawString("Hash: " + river.hashCode(), 10, yPos);
        yPos += 15;
        g.drawString("Control Points: " + river.getPoints().size(), 10, yPos);
        yPos += 15;
        g.drawString("Path Length: " + river.getCalculatedPath().size(), 10, yPos);
        yPos += 15;
        g.drawString("Width: " + river.getWidth(), 10, yPos);
        yPos += 15;
        g.drawString("Color: " + String.format("#%06X", (0xFFFFFF & river.getRiverColor().getRGB())), 10, yPos);
        yPos += 15;

        if (!river.getPoints().isEmpty()) {
            g.drawString("Points:", 10, yPos);
            yPos += 15;
            for (int i = 0; i < river.getPoints().size(); i++) {
                RiverPoint p = river.getPoints().get(i);
                g.drawString("- Point " + (i+1) + ": (" + p.getX() + "," + p.getY() + ")", 20, yPos);
                yPos += 15;
            }
        }
    }

    private static void renderRiverPointDebugInfo(Graphics2D g, RiverPoint point, int yPos) {
        yPos += 15;
        g.drawString("=== SELECTED RIVER POINT ===", 10, yPos);
        yPos += 15;
        g.drawString("Hash: " + point.hashCode(), 10, yPos);
        yPos += 15;
        g.drawString("Position: (" + point.getX() + "," + point.getY() + ")", 10, yPos);
        yPos += 15;
        g.drawString("Order Index: " + point.getOrderIndex(), 10, yPos);
        yPos += 15;

        if (point.getParentRiver() != null) {
            River parent = point.getParentRiver();
            g.drawString("Parent River: " + parent.hashCode(), 10, yPos);
            yPos += 15;
            g.drawString("Points in River: " + parent.getPoints().size(), 10, yPos);
        } else {
            g.drawString("Parent River: NONE", 10, yPos);
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

            for (StationLabel stationLabel : world.getLabelsForStation(station)) {
                g.drawString("- '" + stationLabel.getText() + "' at (" +
                        stationLabel.getX() + "," + stationLabel.getY() + ")", 20, yPos);
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

    private static void renderLabelDebugInfo(Graphics2D g, StationLabel stationLabel, int yPos) {
        yPos += 15;
        g.drawString("=== SELECTED LABEL ===", 10, yPos);
        yPos += 15;
        g.drawString("Hash: " + stationLabel.hashCode(), 10, yPos);
        yPos += 15;
        g.drawString("Text: '" + stationLabel.getText() + "'", 10, yPos);
        yPos += 15;
        g.drawString("Position: (" + stationLabel.getX() + "," + stationLabel.getY() + ")", 10, yPos);
        yPos += 15;
        g.drawString("Parent Station: " + stationLabel.getParentGameObject().getName() +
                " (" + stationLabel.getParentGameObject().getX() + "," +
                stationLabel.getParentGameObject().getY() + ")", 10, yPos);
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
