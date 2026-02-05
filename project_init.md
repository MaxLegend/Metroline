# Metro Editor Project - Claude Init Documentation

## Project Overview
Java-based metro map editor application using Swing GUI. Allows creating stations, colored metro lines, tunnels, and managing complete metro systems.

---

## Core Application

### Main.java
Entry point. Handles app startup, exception handling, crash reporting.

### MainFrame.java
Main application window frame. Container for all screens.

### MainFrameUI.java
UI setup and configuration for MainFrame.

---

## Core World & Game Logic

### World.java
Base world container. Manages world grid (WorldTile[]), game objects (stations, tunnels), serialization, save/load.

### GameWorld.java
Extended world with gameplay logic, simulation, world updates.

### WorldEdge.java
Interface/marker for world boundaries.

---

## Tiles System

### Tile.java
Base tile interface.

### WorldTile.java
World grid tile - basic terrain/background tile.

### GameTile.java
Game logic tile - overlay for gameplay mechanics.

---

## Game Objects (Metro Elements)

### GameObject.java
Base class for all game objects (stations, tunnels). Has position, name, unique ID.

### Station.java
Metro station object. Has connections (Direction → Station), color (StationColors), type (StationType), label, custom properties.

### StationLabel.java
Text label for station name display.

### Tunnel.java
Tunnel connecting stations. Has start/end stations, type (TunnelType), path points, color.

### PathPoint.java
Point in tunnel path for curved/bent tunnels.

---

## Enums & Constants

### Direction.java
Enum for connection directions (North, South, East, West, etc.).

### StationColors.java
Enum for metro line colors.

### StationType.java
Enum for station types (normal, interchange, terminal, etc.).

### TunnelType.java
Enum for tunnel types (straight, single bend, multi-segment).

### GameConstants.java
Game-wide constants (grid size, colors, defaults).

---

## Screens (UI Panels)

### GameScreen.java
Base interface for game screens.

### MenuScreen.java
Main menu screen.

### WorldScreen.java
Base class for world display screens.

### CachedWorldScreen.java
World screen with rendering cache optimization.

### GameWorldScreen.java
Main gameplay screen. Renders world, handles game loop, FPS tracking, camera control.

### WorldMenuScreen.java
Menu for world management (new/load/delete worlds).

### GlobalSettingsScreen.java
Global app settings screen.

### GameWorldSettingsScreen.java
Per-world settings screen.

---

## Rendering

### StationRender.java
Station rendering logic (shapes, colors, selection highlights).

### TunnelRender.java
Tunnel rendering (lines, curves, multi-segment paths).

### StationPositionCache.java
Cache for station screen positions (optimization).

### DebugInfoRenderer.java
Debug overlay (FPS, coordinates, object counts).

---

## Input Handling

### KeyboardController.java
Keyboard input processing (camera movement, shortcuts).

### MouseController.java
Mouse input base handling.

### GameClickController.java
Game-specific click handling (station/tunnel creation, selection, editing).

---

## Selection System

### Selectable.java
Interface for selectable objects.

### SelectionManager.java
Manages currently selected objects.

### SelectionListener.java
Listener interface for selection changes.

### GameSelectionListener.java
Game-specific selection event handler.

---

## UI Windows & Panels

### LinesLegendWindow.java
Legend window showing all metro lines and their colors.

### GameInfoWindow.java
Info panel displaying game stats, selected object details.

---

## Utilities

### MetroLogger.java
Application logging system.

### MetroSerializer.java
World save/load serialization.

### ParsingUtils.java
Data parsing utilities.

### LngUtil.java
Localization/translation utilities.

### ITranslatable.java
Interface for translatable objects.

### MathUtil.java
Math helper functions.

### ColorUtil.java
Color manipulation utilities.

### ImageUtil.java
Image loading/processing utilities.

### ImageCacheUtil.java
Image caching system.

### UserInterfaceUtil.java
UI helper methods.

### StyleUtil.java
UI styling utilities.

### IntegerDocumentFilter.java
Text field filter for integer-only input.

---

## Custom UI Components

### MetrolineButton.java
Custom styled button.

### MetrolineCheckbox.java
Custom styled checkbox.

### MetrolineLabel.java
Custom styled label.

### MetrolineTextField.java
Custom styled text field.

### MetrolineToggleButton.java
Custom styled toggle button.

### MetrolineSlider.java
Custom styled slider.

### MetrolineSliderUI.java
Custom slider UI implementation.

### MetrolineMenuItem.java
Custom styled menu item.

### MetrolinePopupMenu.java
Custom styled popup menu.

### MetrolineFont.java
Font configuration.

### PrerenderIcon.java
Pre-rendered icon cache.

### CachedBackgroundScreen.java
Screen with cached background rendering.

---

## Tooltips

### CursorTooltip.java
Tooltip following cursor.

### ComponentTooltip.java
Tooltip attached to UI component.

---

## Additional

### MetrolineAPI.java
API interface/facade.

### GlobalSettings.java
Global application settings storage.

### SoundPreloader.java
Sound assets preloading.

### WorldImageCompressor.java
World image compression utility.

### WorldThreadImageCompressor.java
Threaded world image compression.

---

## Architecture Summary

**Data Layer**: World → GameWorld → Tiles + GameObjects (Station, Tunnel)  
**View Layer**: Screens → Rendering → UI Components  
**Control Layer**: Controllers (Mouse, Keyboard, Click) → SelectionManager  
**Persistence**: MetroSerializer, GlobalSettings  
**Utilities**: Logging, Localization, Image/Color helpers
