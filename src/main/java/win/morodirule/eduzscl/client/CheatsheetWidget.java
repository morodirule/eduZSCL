package win.morodirule.eduzscl.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class CheatsheetWidget implements Renderable {
    private static final int LINE_HEIGHT = 12;
    private static final int PADDING = 4;
    private static final int SEARCH_BOX_HEIGHT = 20;
    private static final int BACKGROUND_COLOR = 0xFF1C1C1C;
    private static final int TEXT_COLOR = 0xFFFFFFFF;
    private static final int HOVER_COLOR = 0xFF3C3C3C;
    
    private final Font font;
    private int x, y, width, height;
    private EditBox searchBox;
    private List<CheatsheetEntry> allEntries;
    private List<CheatsheetEntry> filteredEntries;
    private int scrollOffset = 0;
    private int hoveredIndex = -1;
    private int lastHoveredIndex = -1;
    private long hoverStartTime = 0;
    private static final long HOVER_DELAY = 500; // milliseconds before showing tooltip
    
    public static class CheatsheetEntry {
        public final String name;
        public final String syntax;
        public final String description;
        
        public CheatsheetEntry(String name, String syntax, String description) {
            this.name = name;
            this.syntax = syntax;
            this.description = description;
        }
    }
    
    public CheatsheetWidget(Font font, int x, int y, int width, int height) {
        this.font = font;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        
        this.searchBox = new EditBox(font, x + PADDING, y + PADDING, width - PADDING * 2, SEARCH_BOX_HEIGHT, Component.literal("Search..."));
        this.searchBox.setMaxLength(50);
        
        this.allEntries = initializeEntries();
        this.filteredEntries = new ArrayList<>(allEntries);
    }
    
    private List<CheatsheetEntry> initializeEntries() {
        List<CheatsheetEntry> entries = new ArrayList<>();
        
        // Agent API
        entries.add(new CheatsheetEntry("agent.move(steps)", "agent.move(steps)", "Move the agent forward by the specified number of steps."));
        entries.add(new CheatsheetEntry("agent.turnLeft()", "agent.turnLeft()", "Rotate the agent 90 degrees counterclockwise."));
        entries.add(new CheatsheetEntry("agent.turnRight()", "agent.turnRight()", "Rotate the agent 90 degrees clockwise."));
        entries.add(new CheatsheetEntry("agent.place(blockId)", "agent.place(blockId)", "Place a block in front of the agent. Block ID can be 'stone', 'dirt', etc."));
        entries.add(new CheatsheetEntry("agent.placeUp(blockId)", "agent.placeUp(blockId)", "Place a block above the agent's current position."));
        entries.add(new CheatsheetEntry("agent.placeDown(blockId)", "agent.placeDown(blockId)", "Place a block below the agent's current position."));
        entries.add(new CheatsheetEntry("agent.remove()", "agent.remove()", "Remove the block directly in front of the agent."));
        entries.add(new CheatsheetEntry("agent.getPosition()", "agent.getPosition()", "Return the agent's current position as {x, y, z} object."));
        entries.add(new CheatsheetEntry("agent.getDirection()", "agent.getDirection()", "Return the agent's facing direction (0=South, 1=West, 2=North, 3=East)."));
        entries.add(new CheatsheetEntry("agent.gotoPos(x, y, z)", "agent.gotoPos(x, y, z)", "Teleport the agent to the specified coordinates instantly."));
        entries.add(new CheatsheetEntry("agent.gotoX(x)", "agent.gotoX(x)", "Teleport the agent to the specified X coordinate."));
        entries.add(new CheatsheetEntry("agent.gotoY(y)", "agent.gotoY(y)", "Teleport the agent to the specified Y coordinate."));
        entries.add(new CheatsheetEntry("agent.gotoZ(z)", "agent.gotoZ(z)", "Teleport the agent to the specified Z coordinate."));
        entries.add(new CheatsheetEntry("agent.attack()", "agent.attack()", "Attack and instantly kill any entity in front of the agent."));
        entries.add(new CheatsheetEntry("agent.log(msg)", "agent.log(msg)", "Display a message in the chat for debugging purposes."));
        
        // Player API
        entries.add(new CheatsheetEntry("player.getPosition()", "player.getPosition()", "Return the player's current position as {x, y, z} object."));
        entries.add(new CheatsheetEntry("player.getBlock()", "player.getBlock()", "Return the block the player is looking at."));
        
        // Mod API
        entries.add(new CheatsheetEntry("mod.placeBlock(x, y, z, blockId)", "mod.placeBlock(x, y, z, blockId)", "Place a block at any world position without using the agent."));
        entries.add(new CheatsheetEntry("mod.getBlock(x, y, z)", "mod.getBlock(x, y, z)", "Get the block type at the specified world coordinates."));
        
        // Math API
        entries.add(new CheatsheetEntry("Math.floor(num)", "Math.floor(num)", "Round a number down to the nearest integer."));
        entries.add(new CheatsheetEntry("Math.ceil(num)", "Math.ceil(num)", "Round a number up to the nearest integer."));
        entries.add(new CheatsheetEntry("Math.round(num)", "Math.round(num)", "Round a number to the nearest integer."));
        entries.add(new CheatsheetEntry("Math.random()", "Math.random()", "Generate a random decimal number between 0 and 1."));
        entries.add(new CheatsheetEntry("Math.abs(num)", "Math.abs(num)", "Return the absolute value (remove negative sign)."));
        entries.add(new CheatsheetEntry("Math.max(a, b)", "Math.max(a, b)", "Return the larger of two numbers."));
        entries.add(new CheatsheetEntry("Math.min(a, b)", "Math.min(a, b)", "Return the smaller of two numbers."));
        entries.add(new CheatsheetEntry("Math.pow(base, exp)", "Math.pow(base, exp)", "Raise a number to a power."));
        
        // Console API
        entries.add(new CheatsheetEntry("console.log(...)", "console.log(...)", "Print values to the chat for debugging."));
        
        return entries;
    }
    
    public void updateSearch(String query) {
        final String searchQuery = query.toLowerCase().trim();
        if (searchQuery.isEmpty()) {
            filteredEntries = new ArrayList<>(allEntries);
        } else {
            filteredEntries = allEntries.stream()
                .filter(e -> e.name.toLowerCase().contains(searchQuery) || 
                           e.description.toLowerCase().contains(searchQuery))
                .collect(Collectors.toList());
        }
        scrollOffset = 0;
        hoveredIndex = -1;
    }
    
    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Draw background
        guiGraphics.fill(x, y, x + width, y + height, BACKGROUND_COLOR);
        guiGraphics.drawString(font, "CHEATSHEET", x + PADDING, y + PADDING + 2, TEXT_COLOR, false);
        
        // Draw search box
        searchBox.render(guiGraphics, mouseX, mouseY, partialTick);
        
        // Calculate available space for list
        int listStartY = y + PADDING + SEARCH_BOX_HEIGHT + PADDING + 10;
        int listEndY = y + height - PADDING;
        int listHeight = listEndY - listStartY;
        int visibleLines = listHeight / (LINE_HEIGHT + PADDING);
        
        // Draw scroll area background
        guiGraphics.fill(x + PADDING, listStartY, x + width - PADDING, listEndY, 0xFF0A0A0A);
        
        // Update hover
        hoveredIndex = -1;
        if (mouseY >= listStartY && mouseY < listEndY && mouseX >= x && mouseX < x + width) {
            int relativeY = mouseY - listStartY;
            int index = (relativeY / (LINE_HEIGHT + PADDING)) + scrollOffset;
            if (index >= 0 && index < filteredEntries.size()) {
                hoveredIndex = index;
                if (lastHoveredIndex != hoveredIndex) {
                    hoverStartTime = System.currentTimeMillis();
                    lastHoveredIndex = hoveredIndex;
                }
            }
        } else {
            hoverStartTime = 0;
            lastHoveredIndex = -1;
        }
        
        // Draw entries
        int drawY = listStartY;
        for (int i = scrollOffset; i < filteredEntries.size() && drawY < listEndY; i++) {
            CheatsheetEntry entry = filteredEntries.get(i);
            
            // Draw hover background
            if (hoveredIndex == i) {
                guiGraphics.fill(x + PADDING, drawY, x + width - PADDING, drawY + LINE_HEIGHT + PADDING, HOVER_COLOR);
            }
            
            // Draw entry name
            guiGraphics.drawString(font, entry.name, x + PADDING * 2, drawY + 2, TEXT_COLOR, false);
            
            drawY += LINE_HEIGHT + PADDING;
        }
        
        // Draw tooltip if hovering long enough
        if (hoveredIndex >= 0 && hoveredIndex < filteredEntries.size()) {
            long hoverTime = System.currentTimeMillis() - hoverStartTime;
            if (hoverTime > HOVER_DELAY) {
                CheatsheetEntry entry = filteredEntries.get(hoveredIndex);
                drawTooltip(guiGraphics, entry, mouseX, mouseY);
            }
        }
    }
    
    private void drawTooltip(GuiGraphics guiGraphics, CheatsheetEntry entry, int mouseX, int mouseY) {
        // Wrap the description text
        int maxTooltipWidth = 220;
        List<String> lines = wrapText(entry.description, font, maxTooltipWidth - 8);
        
        // Calculate tooltip dimensions
        int tooltipWidth = Math.min(maxTooltipWidth, 
            Math.max(font.width(entry.syntax) + 8, 
                lines.stream().mapToInt(line -> font.width(line)).max().orElse(0) + 8));
        int tooltipHeight = 4 + LINE_HEIGHT + 4 + (lines.size() * LINE_HEIGHT) + 4;
        
        int tooltipX = mouseX + 10;
        int tooltipY = mouseY + 10;
        
        // Adjust if tooltip goes off screen to the right
        if (tooltipX + tooltipWidth > 256) {
            tooltipX = mouseX - tooltipWidth - 10;
        }
        
        // Adjust if tooltip goes off screen at the bottom
        if (tooltipY + tooltipHeight > 256) {
            tooltipY = mouseY - tooltipHeight - 10;
        }
        
        // Draw tooltip background
        guiGraphics.fill(tooltipX, tooltipY, tooltipX + tooltipWidth, tooltipY + tooltipHeight, 0xFF1a1a1a);
        guiGraphics.drawString(font, entry.syntax, tooltipX + 4, tooltipY + 4, 0xFFFFFF, false);
        
        // Draw description text
        int lineY = tooltipY + LINE_HEIGHT + 8;
        for (String line : lines) {
            guiGraphics.drawString(font, line, tooltipX + 4, lineY, 0xFFAAAA, false);
            lineY += LINE_HEIGHT;
        }
    }
    
    private List<String> wrapText(String text, Font font, int maxWidth) {
        List<String> lines = new ArrayList<>();
        String[] words = text.split(" ");
        StringBuilder currentLine = new StringBuilder();
        
        for (String word : words) {
            String testLine = currentLine.length() == 0 ? word : currentLine + " " + word;
            if (font.width(testLine) <= maxWidth) {
                currentLine.append(currentLine.length() == 0 ? "" : " ").append(word);
            } else {
                if (currentLine.length() > 0) {
                    lines.add(currentLine.toString());
                }
                currentLine = new StringBuilder(word);
            }
        }
        if (currentLine.length() > 0) {
            lines.add(currentLine.toString());
        }
        
        return lines;
    }
    
    public void mouseScroll(double amount) {
        int visibleLines = (height - 60) / (LINE_HEIGHT + PADDING);
        int maxScroll = Math.max(0, filteredEntries.size() - visibleLines);
        scrollOffset = Math.max(0, Math.min((int) (scrollOffset - amount), maxScroll));
    }
    
    public void setFocused(boolean focused) {
        this.searchBox.setFocused(focused);
    }
    
    public boolean keyPressed(int key, int scanCode, int modifiers) {
        if (searchBox.isFocused()) {
            if (key == GLFW.GLFW_KEY_ESCAPE) {
                searchBox.setFocused(false);
                return true;
            }
            searchBox.keyPressed(key, scanCode, modifiers);
            updateSearch(searchBox.getValue());
            return true;
        }
        return false;
    }
    
    public void charTyped(char chr, int modifiers) {
        if (searchBox.isFocused()) {
            searchBox.charTyped(chr, modifiers);
            updateSearch(searchBox.getValue());
        }
    }
}
