package win.morodirule.eduzscl.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CodeEditorWidget implements Renderable {
    private static final int CURSOR_BLINK_INTERVAL = 500;
    private static final int LINE_HEIGHT = 12;
    private static final int PADDING = 4;
    private static final int AUTOCOMPLETE_HEIGHT = 120;
    
    private final Font font;
    private int x, y, width, height;
    private StringBuilder text = new StringBuilder();
    private int cursorLine = 0;
    private int cursorColumn = 0;
    private int scrollOffset = 0;
    private long lastCursorBlink = 0;
    private boolean cursorVisible = true;
    private boolean focused = false;
    
    private List<AutocompleteEntry> autocompleteEntries = new ArrayList<>();
    private boolean showAutocomplete = false;
    private int selectedAutocomplete = 0;
    private int autocompleteScroll = 0;
    
    private static final Pattern WORD_PATTERN = Pattern.compile("[a-zA-Z_][a-zA-Z0-9_]*$");
    
    private static final List<String> KEYWORDS = Arrays.asList(
        "var", "let", "const", "function", "return", "if", "else", "for", "while", "do",
        "switch", "case", "break", "continue", "true", "false", "null", "undefined",
        "this", "new", "typeof", "instanceof", "try", "catch", "finally", "throw"
    );
    
    private static final List<APIEntry> API_ENTRIES = Arrays.asList(
        new APIEntry("console.log", "console.log(a, b, ...)", "Prints values to chat"),
        new APIEntry("player.getPosition", "player.getPosition()", "Returns player position {x, y, z}"),
        new APIEntry("player.getBlock", "player.getBlock()", "Returns block player is looking at"),
        new APIEntry("agent.move", "agent.move(steps)", "Move agent forward"),
        new APIEntry("agent.turnLeft", "agent.turnLeft()", "Turn agent left"),
        new APIEntry("agent.turnRight", "agent.turnRight()", "Turn agent right"),
        new APIEntry("agent.place", "agent.place(blockId)", "Place block in front"),
        new APIEntry("agent.placeUp", "agent.placeUp(blockId)", "Place block above"),
        new APIEntry("agent.placeDown", "agent.placeDown(blockId)", "Place block below"),
        new APIEntry("agent.remove", "agent.remove()", "Remove block in front"),
        new APIEntry("agent.getPosition", "agent.getPosition()", "Get agent position"),
        new APIEntry("agent.getDirection", "agent.getDirection()", "Get agent direction"),
        new APIEntry("agent.gotoX", "agent.gotoX(x)", "Teleport to x"),
        new APIEntry("agent.gotoY", "agent.gotoY(y)", "Teleport to y"),
        new APIEntry("agent.gotoZ", "agent.gotoZ(z)", "Teleport to z"),
        new APIEntry("agent.gotoPos", "agent.gotoPos(x, y, z)", "Teleport to position"),
        new APIEntry("mod.placeBlock", "mod.placeBlock(x, y, z, blockId)", "Place a block"),
        new APIEntry("mod.getBlock", "mod.getBlock(x, y, z)", "Get block at position"),
        new APIEntry("Math.floor", "Math.floor(num)", "Round down"),
        new APIEntry("Math.round", "Math.round(num)", "Round to nearest"),
        new APIEntry("Math.ceil", "Math.ceil(num)", "Round up"),
        new APIEntry("Math.random", "Math.random()", "Random number 0-1"),
        new APIEntry("agent.log", "agent.log(msg)", "Log message")
    );
    
    public CodeEditorWidget(Font font, int x, int y, int width, int height) {
        this.font = font;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }
    
    public String getText() {
        return text.toString();
    }
    
    public void setText(String text) {
        this.text = new StringBuilder(text);
        this.cursorLine = 0;
        this.cursorColumn = 0;
        this.scrollOffset = 0;
    }
    
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        String[] lines = getLines();

        int visibleLines = (height - LINE_HEIGHT * 2) / LINE_HEIGHT;
        int maxScroll = Math.max(0, lines.length - visibleLines);
        scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));

        for (int i = scrollOffset; i < lines.length && i < scrollOffset + visibleLines; i++) {
            int lineY = y + PADDING + (i - scrollOffset) * LINE_HEIGHT + LINE_HEIGHT;

            String lineNum = String.valueOf(i + 1);
            graphics.drawString(font, lineNum, x + PADDING, lineY - font.lineHeight + 2, 0xFF666666);

            if (i < lines.length) {
                graphics.drawString(font, lines[i], x + PADDING + 30, lineY - font.lineHeight + 2, 0xFFCCCCCC);
            }
        }

        if (focused && cursorVisible) {
            String[] currentLines = getLines();
            if (cursorLine < currentLines.length) {
                String beforeCursor = cursorLine < currentLines.length ?
                    currentLines[cursorLine].substring(0, Math.min(cursorColumn, currentLines[cursorLine].length())) : "";
                int cursorX = x + PADDING + 30 + font.width(beforeCursor);
                int cursorY = y + PADDING + (cursorLine - scrollOffset) * LINE_HEIGHT + LINE_HEIGHT;
                graphics.fill(cursorX, cursorY - font.lineHeight + 2, cursorX + 1, cursorY + 2, 0xFFFFFFFF);
            }
        }

        if (showAutocomplete && !autocompleteEntries.isEmpty()) {
            renderAutocomplete(graphics, mouseX, mouseY);
        }

        if (System.currentTimeMillis() - lastCursorBlink > CURSOR_BLINK_INTERVAL) {
            cursorVisible = !cursorVisible;
            lastCursorBlink = System.currentTimeMillis();
        }
    }
    
    private void renderAutocomplete(GuiGraphics graphics, int mouseX, int mouseY) {
        String[] currentLines = getLines();
        String currentLine = cursorLine < currentLines.length ? currentLines[cursorLine] : "";
        String beforeCursor = currentLine.substring(0, Math.min(cursorColumn, currentLine.length()));
        
        int cursorXPos = x + PADDING + 30 + font.width(beforeCursor);
        int autocompleteX = cursorXPos;
        
        if (autocompleteX + 250 > x + width) {
            autocompleteX = x + width - 260;
        }
        if (autocompleteX < x + PADDING) {
            autocompleteX = x + PADDING;
        }
        
        int autocompleteY = y + PADDING + (cursorLine - scrollOffset) * LINE_HEIGHT + LINE_HEIGHT + 2;
        
        if (autocompleteY + AUTOCOMPLETE_HEIGHT > y + height) {
            autocompleteY = y + PADDING + (cursorLine - scrollOffset) * LINE_HEIGHT - AUTOCOMPLETE_HEIGHT - 2;
        }
        if (autocompleteY < y + PADDING) {
            autocompleteY = y + PADDING;
        }

        graphics.fill(autocompleteX - 2, autocompleteY - 2, autocompleteX + 252, autocompleteY + AUTOCOMPLETE_HEIGHT + 2, 0xFF000000);
        graphics.fill(autocompleteX, autocompleteY, autocompleteX + 250, autocompleteY + AUTOCOMPLETE_HEIGHT, 0xEE2D2D2D);

        int outlineColor = 0xFF888888;
        graphics.fill(autocompleteX, autocompleteY, autocompleteX + 1, autocompleteY + AUTOCOMPLETE_HEIGHT, outlineColor);
        graphics.fill(autocompleteX + 249, autocompleteY, autocompleteX + 250, autocompleteY + AUTOCOMPLETE_HEIGHT, outlineColor);
        graphics.fill(autocompleteX, autocompleteY, autocompleteX + 250, autocompleteY + 1, outlineColor);
        graphics.fill(autocompleteX, autocompleteY + AUTOCOMPLETE_HEIGHT - 1, autocompleteX + 250, autocompleteY + AUTOCOMPLETE_HEIGHT, outlineColor);
        
        graphics.fill(autocompleteX + 1, autocompleteY + 1, autocompleteX + 2, autocompleteY + AUTOCOMPLETE_HEIGHT - 1, 0xFF444444);
        graphics.fill(autocompleteX + 248, autocompleteY + 1, autocompleteX + 249, autocompleteY + AUTOCOMPLETE_HEIGHT - 1, 0xFF444444);
        graphics.fill(autocompleteX + 1, autocompleteY + 1, autocompleteX + 249, autocompleteY + 2, 0xFF444444);
        graphics.fill(autocompleteX + 1, autocompleteY + AUTOCOMPLETE_HEIGHT - 2, autocompleteX + 249, autocompleteY + AUTOCOMPLETE_HEIGHT - 1, 0xFF444444);
        
        int visibleEntries = (AUTOCOMPLETE_HEIGHT - 20) / (LINE_HEIGHT + 2);
        int maxScroll = Math.max(0, autocompleteEntries.size() - visibleEntries);
        autocompleteScroll = Math.max(0, Math.min(autocompleteScroll, maxScroll));
        
        for (int i = autocompleteScroll; i < autocompleteEntries.size() && i < autocompleteScroll + visibleEntries; i++) {
            AutocompleteEntry entry = autocompleteEntries.get(i);
            int entryY = autocompleteY + 5 + (i - autocompleteScroll) * (LINE_HEIGHT + 2);
            
            if (i == selectedAutocomplete) {
                graphics.fill(autocompleteX + 2, entryY - 1, autocompleteX + 248, entryY + LINE_HEIGHT, 0xFF3D5A80);
            }
            
            int color = i == selectedAutocomplete ? 0xFFFFFFFF : 0xFFB0B0B0;
            graphics.drawString(font, entry.displayText, autocompleteX + 5, entryY, color);
        }
        
        if (!autocompleteEntries.isEmpty() && selectedAutocomplete < autocompleteEntries.size()) {
            AutocompleteEntry entry = autocompleteEntries.get(selectedAutocomplete);
            graphics.fill(autocompleteX, autocompleteY + AUTOCOMPLETE_HEIGHT - 18, autocompleteX + 250, autocompleteY + AUTOCOMPLETE_HEIGHT - 17, 0xFF555555);
            graphics.drawString(font, entry.description, autocompleteX + 5, autocompleteY + AUTOCOMPLETE_HEIGHT - 14, 0xFFAAAAAA);
        }
    }
    
    private String[] getLines() {
        String content = text.toString();
        if (content.isEmpty()) {
            return new String[]{""};
        }
        return content.split("\n", -1);
    }
    
    private void updateCursorPosition() {
        String[] lines = getLines();
        cursorLine = Math.max(0, Math.min(cursorLine, lines.length - 1));
        cursorColumn = Math.max(0, Math.min(cursorColumn, lines[cursorLine].length()));
    }
    
    private void insertText(String insert) {
        String[] lines = getLines();
        StringBuilder sb = new StringBuilder();
        
        for (int i = 0; i < lines.length; i++) {
            if (i == cursorLine) {
                String line = lines[i];
                sb.append(line.substring(0, cursorColumn));
                sb.append(insert);
                sb.append(line.substring(cursorColumn));
            } else {
                sb.append(lines[i]);
            }
            if (i < lines.length - 1) {
                sb.append("\n");
            }
        }
        
        text = sb;
        
        String[] newLines = insert.split("\n", -1);
        if (newLines.length > 1) {
            cursorLine += newLines.length - 1;
            cursorColumn = newLines[newLines.length - 1].length();
        } else {
            cursorColumn += insert.length();
        }
        
        updateCursorPosition();
    }
    
    public void handleEnter() {
        insertText("\n");
    }
    
    public void handleBackspace() {
        if (cursorColumn > 0) {
            String[] lines = getLines();
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < lines.length; i++) {
                if (i == cursorLine) {
                    String line = lines[i];
                    sb.append(line.substring(0, cursorColumn - 1));
                    sb.append(line.substring(cursorColumn));
                } else {
                    sb.append(lines[i]);
                }
                if (i < lines.length - 1) sb.append("\n");
            }
            text = sb;
            cursorColumn--;
        } else if (cursorLine > 0) {
            String[] lines = getLines();
            int prevLineLength = lines[cursorLine - 1].length();
            
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < lines.length; i++) {
                if (i == cursorLine - 1) {
                    sb.append(lines[i]);
                } else if (i == cursorLine) {
                    if (i < lines.length) {
                        if (i > 0) sb.append("\n");
                        sb.append(lines[i]);
                    }
                } else {
                    if (i > 0) sb.append("\n");
                    sb.append(lines[i]);
                }
            }
            text = sb;
            cursorLine--;
            cursorColumn = prevLineLength;
        }
    }
    
    public void handleDelete() {
        String[] lines = getLines();
        if (cursorColumn < lines[cursorLine].length()) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < lines.length; i++) {
                if (i == cursorLine) {
                    String line = lines[i];
                    sb.append(line.substring(0, cursorColumn));
                    sb.append(line.substring(cursorColumn + 1));
                } else {
                    sb.append(lines[i]);
                }
                if (i < lines.length - 1) sb.append("\n");
            }
            text = sb;
        } else if (cursorLine < lines.length - 1) {
            String[] newLines = new String[lines.length - 1];
            int newIdx = 0;
            for (int i = 0; i < lines.length; i++) {
                if (i == cursorLine) {
                    newLines[newIdx++] = lines[i] + lines[i + 1];
                } else if (i != cursorLine + 1) {
                    newLines[newIdx++] = lines[i];
                }
            }
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < newLines.length; i++) {
                sb.append(newLines[i]);
                if (i < newLines.length - 1) sb.append("\n");
            }
            text = sb;
        }
    }
    
    public void handleArrowUp() {
        if (showAutocomplete && !autocompleteEntries.isEmpty()) {
            selectedAutocomplete = (selectedAutocomplete - 1 + autocompleteEntries.size()) % autocompleteEntries.size();
            updateAutocompleteScroll();
            return;
        }
        if (cursorLine > 0) {
            cursorLine--;
            updateCursorPosition();
        }
    }
    
    public void handleArrowDown() {
        if (showAutocomplete && !autocompleteEntries.isEmpty()) {
            selectedAutocomplete = (selectedAutocomplete + 1) % autocompleteEntries.size();
            updateAutocompleteScroll();
            return;
        }
        String[] lines = getLines();
        if (lines != null && cursorLine < lines.length - 1) {
            cursorLine++;
            updateCursorPosition();
        }
    }
    
    public void handleArrowLeft() {
        if (cursorColumn > 0) {
            cursorColumn--;
        } else if (cursorLine > 0) {
            cursorLine--;
            String[] lines = getLines();
            if (lines != null && cursorLine >= 0 && cursorLine < lines.length) {
                cursorColumn = lines[cursorLine].length();
            }
        }
    }
    
    public void handleArrowRight() {
        String[] lines = getLines();
        if (lines != null && cursorLine < lines.length) {
            if (cursorColumn < lines[cursorLine].length()) {
                cursorColumn++;
            } else if (cursorLine < lines.length - 1) {
                cursorLine++;
                cursorColumn = 0;
            }
        }
    }
    
    public void handleTab() {
        if (showAutocomplete && !autocompleteEntries.isEmpty()) {
            AutocompleteEntry entry = autocompleteEntries.get(selectedAutocomplete);
            String prefix = getCurrentWord();
            String completion = entry.insertText;
            if (completion.startsWith(prefix)) {
                completion = completion.substring(prefix.length());
            }
            completeAutocomplete(completion);
        } else {
            insertText("    ");
        }
    }
    
    private void updateAutocompleteScroll() {
        int visibleEntries = (AUTOCOMPLETE_HEIGHT - 30) / LINE_HEIGHT;
        if (selectedAutocomplete < autocompleteScroll) {
            autocompleteScroll = selectedAutocomplete;
        } else if (selectedAutocomplete >= autocompleteScroll + visibleEntries) {
            autocompleteScroll = selectedAutocomplete - visibleEntries + 1;
        }
    }
    
    public void triggerAutocomplete() {
        String currentWord = getCurrentWord();
        if (currentWord.length() < 1) {
            showAutocomplete = false;
            return;
        }
        
        autocompleteEntries.clear();
        String lowerWord = currentWord.toLowerCase();
        
        for (APIEntry api : API_ENTRIES) {
            if (api.name.toLowerCase().startsWith(lowerWord)) {
                autocompleteEntries.add(new AutocompleteEntry(api.name, api.insertText, api.description));
            }
        }
        
        for (String keyword : KEYWORDS) {
            if (keyword.toLowerCase().startsWith(lowerWord)) {
                autocompleteEntries.add(new AutocompleteEntry(keyword, keyword, "keyword"));
            }
        }
        
        if (autocompleteEntries.isEmpty()) {
            showAutocomplete = false;
        } else {
            showAutocomplete = true;
            selectedAutocomplete = 0;
            autocompleteScroll = 0;
        }
    }
    
    private void completeAutocomplete(String completion) {
        String currentWord = getCurrentWord();
        String[] lines = getLines();
        
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lines.length; i++) {
            if (i == cursorLine) {
                String line = lines[i];
                sb.append(line.substring(0, cursorColumn - currentWord.length()));
                sb.append(currentWord);
                sb.append(completion);
                sb.append(line.substring(cursorColumn));
            } else {
                sb.append(lines[i]);
            }
            if (i < lines.length - 1) sb.append("\n");
        }
        
        text = sb;
        cursorColumn = cursorColumn + completion.length();
        showAutocomplete = false;
        updateCursorPosition();
    }
    
    private String getCurrentWord() {
        String[] lines = getLines();
        if (cursorLine >= lines.length) return "";
        
        String line = lines[cursorLine];
        int pos = Math.min(cursorColumn, line.length());
        
        Matcher matcher = WORD_PATTERN.matcher(line.substring(0, pos));
        String word = "";
        while (matcher.find()) {
            word = matcher.group();
        }
        return word;
    }
    
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        try {
            if (mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height) {
                focused = true;
                
                if (showAutocomplete && mouseX >= x + PADDING + 30 && mouseX < x + PADDING + 280) {
                    int autocompleteX = x + PADDING + 30;
                    int autocompleteY = y + PADDING + (cursorLine - scrollOffset) * LINE_HEIGHT + LINE_HEIGHT + 2;
                    if (autocompleteY + AUTOCOMPLETE_HEIGHT > y + height) {
                        autocompleteY = y + PADDING + (cursorLine - scrollOffset) * LINE_HEIGHT - AUTOCOMPLETE_HEIGHT - 2;
                    }
                    
                    if (mouseX >= autocompleteX && mouseX < autocompleteX + 250 &&
                        mouseY >= autocompleteY && mouseY < autocompleteY + AUTOCOMPLETE_HEIGHT) {
                        int clickedEntry = (int) ((mouseY - autocompleteY - 5) / LINE_HEIGHT) + autocompleteScroll;
                        if (clickedEntry >= 0 && clickedEntry < autocompleteEntries.size()) {
                            selectedAutocomplete = clickedEntry;
                            AutocompleteEntry entry = autocompleteEntries.get(selectedAutocomplete);
                            String prefix = getCurrentWord();
                            String completion = entry.insertText;
                            if (completion.startsWith(prefix)) {
                                completion = completion.substring(prefix.length());
                            }
                            completeAutocomplete(completion);
                            return true;
                        }
                    }
                }
                
                int clickLine = (int) ((mouseY - y - PADDING) / LINE_HEIGHT) + scrollOffset;
                String[] lines = getLines();
                if (lines == null || lines.length == 0) {
                    return true;
                }
                clickLine = Math.max(0, Math.min(clickLine, lines.length - 1));
                
                int relX = (int) (mouseX - x - PADDING - 30);
                cursorLine = clickLine;
                cursorColumn = 0;
                String line = lines[clickLine];
                if (line != null) {
                    for (int i = 0; i < line.length(); i++) {
                        if (font.width(line.substring(0, i + 1)) > relX) {
                            cursorColumn = i;
                            break;
                        }
                        cursorColumn = i + 1;
                    }
                }
                
                return true;
            }
            focused = false;
            showAutocomplete = false;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
    
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!focused) return false;
        
        try {
            if (showAutocomplete) {
                if (keyCode == GLFW.GLFW_KEY_UP) {
                    handleArrowUp();
                    return true;
                }
                if (keyCode == GLFW.GLFW_KEY_DOWN) {
                    handleArrowDown();
                    return true;
                }
                if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_TAB) {
                    handleTab();
                    return true;
                }
                if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                    showAutocomplete = false;
                    return true;
                }
            }
            
            if (keyCode == GLFW.GLFW_KEY_ENTER) {
                handleEnter();
                triggerAutocomplete();
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
                handleBackspace();
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_DELETE) {
                handleDelete();
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_UP) {
                handleArrowUp();
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_DOWN) {
                handleArrowDown();
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_LEFT) {
                handleArrowLeft();
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_RIGHT) {
                handleArrowRight();
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_HOME) {
                cursorColumn = 0;
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_END) {
                String[] lines = getLines();
                if (lines != null && cursorLine >= 0 && cursorLine < lines.length) {
                    cursorColumn = lines[cursorLine].length();
                }
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return false;
    }
    
    public boolean charTyped(char codePoint, int modifiers) {
        if (!focused) return false;
        
        try {
            if (codePoint >= 32) {
                insertText(String.valueOf(codePoint));
                cursorVisible = true;
                lastCursorBlink = System.currentTimeMillis();
                
                if (Character.isLetterOrDigit(codePoint) || codePoint == '_') {
                    triggerAutocomplete();
                } else {
                    showAutocomplete = false;
                }
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
    
    public boolean isFocused() {
        return focused;
    }
    
    public void setFocused(boolean focused) {
        this.focused = focused;
        if (!focused) {
            showAutocomplete = false;
        }
    }
    
    private static class AutocompleteEntry {
        String displayText;
        String insertText;
        String description;
        
        AutocompleteEntry(String displayText, String insertText, String description) {
            this.displayText = displayText;
            this.insertText = insertText;
            this.description = description;
        }
    }
    
    private static class APIEntry {
        String name;
        String insertText;
        String description;
        
        APIEntry(String name, String insertText, String description) {
            this.name = name;
            this.insertText = insertText;
            this.description = description;
        }
    }
}
