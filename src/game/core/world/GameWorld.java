package game.core.world;

import game.core.GameObject;
import game.core.GameTime;
import game.core.world.tiles.GameTileBig;
import game.objects.Label;
import game.objects.PathPoint;
import game.objects.Station;
import game.objects.Tunnel;
import game.objects.enums.Direction;
import game.objects.enums.StationType;
import game.objects.enums.TunnelType;
import screens.MainFrame;
import screens.WorldGameScreen;
import screens.WorldSandboxScreen;
import util.MessageUtil;
import util.MetroLogger;

import java.awt.*;
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GameWorld extends World {
    private transient MainFrame mainFrame;
    private static final String SAVE_FILE = "game_save.metro";
    public int money;
    public GameWorld() {
        super();
    }

    public GameWorld(int width, int height, boolean hasOrganicPatches, boolean hasRivers, Color worldColor, int money) {
        super(null, width, height, hasOrganicPatches,hasRivers,worldColor, SAVE_FILE);
        this.mainFrame = MainFrame.getInstance();
        this.money = money;
    }

    @Override
    public World getWorld() {
        return this;
    }
    public int getMoney() {
        return money;
    }

    public boolean canAfford(int amount) {
        return money >= amount;
    }

    public boolean addMoney(int amount) {
        if (amount < 0 && !canAfford(-amount)) {
            return false;
        }
        money += amount;
        return true;
    }

    public void setMoney(int amount) {
        this.money = amount;
    }
    public void updateConnectedTunnels(Station station) {
        for (Tunnel tunnel : getTunnels()) {
            if (tunnel.getStart() == station || tunnel.getEnd() == station) {
                Station otherEnd = (tunnel.getStart() == station) ? tunnel.getEnd() : tunnel.getStart();

                if (station.getType() == StationType.BUILDING &&
                        otherEnd.getType() == StationType.BUILDING) {
                    tunnel.setType(TunnelType.BUILDING, getGameTime());
                }
                else if (station.getType() != StationType.PLANNED &&
                        station.getType() != StationType.BUILDING &&
                        otherEnd.getType() != StationType.PLANNED &&
                        otherEnd.getType() != StationType.BUILDING) {
                    tunnel.setType(TunnelType.ACTIVE,getGameTime());
                }
            }
        }
    }
}
