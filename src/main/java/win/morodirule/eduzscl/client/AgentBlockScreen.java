package win.morodirule.eduzscl.client;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import win.morodirule.eduzscl.menu.AgentBlockMenu;
import win.morodirule.eduzscl.blockentity.AgentBlockEntity;
import win.morodirule.eduzscl.teaching.TeachingAgent;
import win.morodirule.eduzscl.teaching.TeachingAgent.Lesson;
import win.morodirule.eduzscl.teaching.TeachingAgent.Tip;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AgentBlockScreen extends Screen implements MenuAccess<AgentBlockMenu> {
    private static final Logger LOGGER = LoggerFactory.getLogger("AgentBlockScreen");
    private static final int WINDOW_WIDTH = 500;
    private static final int WINDOW_HEIGHT = 400;
    private static final int TEXT_FIELD_MARGIN = 20;
    private static final int BUTTON_WIDTH = 80;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_MARGIN = 10;
    private static final int SIDEBAR_WIDTH = 160;

    private final BlockPos blockPos;
    private final Inventory inventory;
    private CodeEditorWidget codeEditor;
    private Button runButton;
    private Button resetButton;
    private Button closeButton;
    private Button lessonPrevButton;
    private Button lessonNextButton;
    private AgentBlockMenu menu;
    private int currentLessonIndex = 0;
    private List<Lesson> availableLessons;
    private String currentGoal = "";
    private String currentTips = "";
    private int codeAreaX;

    public AgentBlockScreen(BlockPos blockPos) {
        super(Component.translatable("gui.eduzscl.agent_editor.title"));
        this.blockPos = blockPos;
        this.inventory = null;
    }

    public AgentBlockScreen(AgentBlockMenu menu) {
        super(Component.translatable("gui.eduzscl.agent_editor.title"));
        this.menu = menu;
        this.inventory = null;
        LOGGER.info("AgentBlockScreen created - menu: {}, blockPos: {}, blockEntity: {}", 
            menu, menu.getBlockPos(), menu.getBlockEntity());
        this.blockPos = menu.getBlockPos();
    }

    public AgentBlockScreen(AgentBlockMenu menu, Inventory inventory, Component component) {
        super(Component.translatable("gui.eduzscl.agent_editor.title"));
        this.menu = menu;
        this.inventory = inventory;
        LOGGER.info("AgentBlockScreen created - menu: {}, inventory.player: {}", 
            menu, inventory.player);
        
        if (menu != null) {
            BlockPos menuPos = menu.getBlockPos();
            LOGGER.info("Menu blockPos from getBlockPos(): {}", menuPos);
            this.blockPos = menuPos;
        } else {
            this.blockPos = BlockPos.ZERO;
        }
        
        LOGGER.info("AgentBlockScreen final blockPos: {}", this.blockPos);
    }

    @Override
    public AgentBlockMenu getMenu() {
        return this.menu;
    }

    @Override
    protected void init() {
        super.init();

        this.availableLessons = TeachingAgent.LessonManager.getAllLessons();
        if (availableLessons.isEmpty()) {
            availableLessons = TeachingAgent.getLessons();
        }
        
        if (menu != null && menu.getBlockEntity() != null && menu.getBlockEntity().getCurrentLessonId() != null) {
            for (int i = 0; i < availableLessons.size(); i++) {
                if (availableLessons.get(i).getId().equals(menu.getBlockEntity().getCurrentLessonId())) {
                    currentLessonIndex = i;
                    break;
                }
            }
        }
        
        updateLessonInfo();

        int windowX = (this.width - WINDOW_WIDTH) / 2;
        int windowY = (this.height - WINDOW_HEIGHT) / 2;

        int textFieldWidth = WINDOW_WIDTH - SIDEBAR_WIDTH - (TEXT_FIELD_MARGIN * 3);
        int textFieldHeight = WINDOW_HEIGHT - 80;
        
        codeAreaX = windowX + TEXT_FIELD_MARGIN + SIDEBAR_WIDTH;

        this.codeEditor = new CodeEditorWidget(
                this.font,
                codeAreaX,
                windowY + 30,
                textFieldWidth,
                textFieldHeight
        );
        
        // Try to get the code from cached menu data first (for fresh block entities after moves)
        // Fall back to block entity code if cache is empty
        String codeToDisplay = null;
        if (menu != null && menu.getCachedCode() != null) {
            codeToDisplay = menu.getCachedCode();
            LOGGER.info("Using cached code from menu, length: {}", codeToDisplay.length());
        } else if (menu != null && menu.getBlockEntity() != null) {
            codeToDisplay = menu.getBlockEntity().getCode();
            LOGGER.info("Using code from block entity, length: {}", codeToDisplay.length());
        } else {
            codeToDisplay = Component.translatable("gui.eduzscl.agent_editor.default_code").getString();
            LOGGER.info("Using default code");
        }
        
        this.codeEditor.setText(codeToDisplay);

        int buttonY = windowY + WINDOW_HEIGHT - 40;
        int codeAreaCenterX = windowX + TEXT_FIELD_MARGIN + SIDEBAR_WIDTH + textFieldWidth / 2;

        this.runButton = Button.builder(
                Component.translatable("gui.eduzscl.agent_editor.run"),
                button -> this.runCode()
        ).bounds(codeAreaCenterX - BUTTON_WIDTH - BUTTON_MARGIN, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT).build();
        this.addRenderableWidget(runButton);

        this.resetButton = Button.builder(
                Component.translatable("gui.eduzscl.agent_editor.reset"),
                button -> {
                    codeEditor.setText(Component.translatable("gui.eduzscl.agent_editor.default_code").getString());
                }
        ).bounds(codeAreaCenterX + BUTTON_MARGIN, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT).build();
        this.addRenderableWidget(resetButton);

        this.closeButton = Button.builder(
                Component.translatable("gui.eduzscl.agent_editor.close"),
                button -> this.onClose()
        ).bounds(codeAreaCenterX - BUTTON_WIDTH / 2, buttonY + BUTTON_HEIGHT + 5, BUTTON_WIDTH, BUTTON_HEIGHT).build();
        this.addRenderableWidget(closeButton);

        
        int sidebarX = windowX + TEXT_FIELD_MARGIN;
        
        this.lessonPrevButton = Button.builder(
                Component.literal("<"),
                button -> {
                    if (currentLessonIndex > 0) {
                        currentLessonIndex--;
                        updateLessonInfo();
                        applyCurrentLesson();
                    }
                }
        ).bounds(sidebarX, windowY + 30, 25, BUTTON_HEIGHT).build();
        this.addRenderableWidget(lessonPrevButton);
        
        this.lessonNextButton = Button.builder(
                Component.literal(">"),
                button -> {
                    if (currentLessonIndex < availableLessons.size() - 1) {
                        currentLessonIndex++;
                        updateLessonInfo();
                        applyCurrentLesson();
                    }
                }
        ).bounds(sidebarX + SIDEBAR_WIDTH - 25, windowY + 30, 25, BUTTON_HEIGHT).build();
        this.addRenderableWidget(lessonNextButton);
    }
    
    private void updateLessonInfo() {
        if (currentLessonIndex >= 0 && currentLessonIndex < availableLessons.size()) {
            Lesson lesson = availableLessons.get(currentLessonIndex);
            currentGoal = lesson.getGoal();
            
            Tip[] tips = lesson.getTips();
            StringBuilder sb = new StringBuilder();
            for (Tip tip : tips) {
                sb.append("- ").append(tip.text()).append("\n");
            }
            currentTips = sb.toString();
        }
    }
    
    private void applyCurrentLesson() {
        saveEditorStateToServer(false);
    }

    private void runCode() {
        saveEditorStateToServer(true, true);
    }

    private void saveEditorStateToServer(boolean includeCode) {
        saveEditorStateToServer(includeCode, false);
    }

    private void saveEditorStateToServer(boolean includeCode, boolean runAfterSave) {
        Minecraft mc = Minecraft.getInstance();
        MinecraftServer server = mc.getSingleplayerServer();
        if (server == null) {
            LOGGER.warn("Singleplayer server is null while Agent screen is open");
            return;
        }

        if (mc.player == null) {
            LOGGER.warn("Local client player is null while Agent screen is open");
            return;
        }

        final UUID playerUuid = mc.player.getUUID();
        final String playerName = mc.player.getGameProfile().getName();
        final String codeToSave = includeCode && codeEditor != null ? codeEditor.getText() : null;
        final String lessonIdToSave = currentLessonIndex >= 0 && currentLessonIndex < availableLessons.size()
            ? availableLessons.get(currentLessonIndex).getId()
            : null;
        final BlockPos fallbackPos = this.blockPos;

        server.execute(() -> {
            PlayerList playerList = server.getPlayerList();
            if (playerList == null) {
                LOGGER.warn("PlayerList is null on integrated server thread");
                return;
            }

            ServerPlayer serverPlayer = playerList.getPlayer(playerUuid);
            if (serverPlayer == null) {
                serverPlayer = playerList.getPlayerByName(playerName);
            }
            if (serverPlayer == null && !playerList.getPlayers().isEmpty()) {
                serverPlayer = playerList.getPlayers().get(0);
            }
            if (serverPlayer == null) {
                LOGGER.warn("Could not resolve server player on integrated server thread");
                return;
            }

            AgentBlockEntity serverAgent = null;
            if (serverPlayer.containerMenu instanceof AgentBlockMenu serverMenu) {
                BlockPos menuPos = serverMenu.getBlockPos();
                BlockEntity menuBe = serverPlayer.serverLevel().getBlockEntity(menuPos);
                if (menuBe instanceof AgentBlockEntity agent) {
                    serverAgent = agent;
                }
            }

            if (serverAgent == null && fallbackPos != null && !fallbackPos.equals(BlockPos.ZERO)) {
                BlockEntity be = serverPlayer.serverLevel().getBlockEntity(fallbackPos);
                if (be instanceof AgentBlockEntity agent) {
                    serverAgent = agent;
                }
            }

            if (serverAgent == null) {
                LOGGER.warn("Could not resolve server agent block entity on server thread");
                return;
            }

            if (codeToSave != null) {
                serverAgent.setCode(codeToSave);
            }
            if (lessonIdToSave != null) {
                serverAgent.setCurrentLessonId(lessonIdToSave);
            }

            if (runAfterSave) {
                serverAgent.executeCode(serverPlayer);
            }
        });

        if (menu != null && menu.getBlockEntity() != null) {
            if (codeToSave != null) {
                menu.getBlockEntity().setCode(codeToSave);
            }
            if (lessonIdToSave != null) {
                menu.getBlockEntity().setCurrentLessonId(lessonIdToSave);
            }
        }
    }

    @Override
    public void onClose() {
        saveEditorStateToServer(true);
        super.onClose();
    }

    private void renderSidebar(GuiGraphics graphics, int windowX, int windowY) {
        int sidebarX = windowX + TEXT_FIELD_MARGIN;
        
        String lessonTitle = "Lesson";
        if (currentLessonIndex >= 0 && currentLessonIndex < availableLessons.size()) {
            lessonTitle = availableLessons.get(currentLessonIndex).getTitle();
        }
        graphics.drawString(this.font, lessonTitle, sidebarX + 30, windowY + 35, 0xFFAA00);
        
        int maxOps = 100;
        if (menu != null && menu.getBlockEntity() != null) {
            maxOps = menu.getBlockEntity().getMaxOperations();
        }
        graphics.drawString(this.font, Component.translatable("gui.eduzscl.agent_editor.max_ops", maxOps).getString(), sidebarX + 5, windowY + 55, 0xAAAAAA);
        
        graphics.drawString(this.font, Component.translatable("gui.eduzscl.agent_editor.goal").getString(), sidebarX + 5, windowY + 75, 0x00FF00);
        
        String[] goalLines = splitString(currentGoal, SIDEBAR_WIDTH - 10);
        int yOffset = 90;
        for (String line : goalLines) {
            graphics.drawString(this.font, line, sidebarX + 5, windowY + yOffset, 0xFFFFFF);
            yOffset += 12;
        }
        
        if (!currentTips.isEmpty()) {
            yOffset += 10;
            graphics.drawString(this.font, Component.translatable("gui.eduzscl.agent_editor.tips").getString(), sidebarX + 5, windowY + yOffset, 0x00FFFF);
            yOffset += 15;
            
            String[] tipLines = splitString(currentTips, SIDEBAR_WIDTH - 10);
            for (String line : tipLines) {
                if (yOffset < windowY + WINDOW_HEIGHT - 20) {
                    graphics.drawString(this.font, line, sidebarX + 5, windowY + yOffset, 0xFFCCCCCC);
                    yOffset += 12;
                }
            }
        }
    }
    
    private String[] splitString(String text, int maxWidth) {
        if (text == null || text.isEmpty()) {
            return new String[]{""};
        }
        java.util.List<String> lines = new java.util.ArrayList<>();
        String[] words = text.split(" ");
        StringBuilder currentLine = new StringBuilder();
        
        for (String word : words) {
            String testLine = currentLine.length() == 0 ? word : currentLine + " " + word;
            if (font.width(testLine) <= maxWidth) {
                currentLine = new StringBuilder(testLine);
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
        
        return lines.toArray(new String[0]);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.codeEditor.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (this.codeEditor.charTyped(codePoint, modifiers)) {
            return true;
        }
        return super.charTyped(codePoint, modifiers);
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.codeEditor.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        
        int windowX = (this.width - WINDOW_WIDTH) / 2;
        int windowY = (this.height - WINDOW_HEIGHT) / 2;

        graphics.fill(windowX, windowY, windowX + WINDOW_WIDTH, windowY + WINDOW_HEIGHT, 0xFF2d2d2d);
        
        int sidebarX = windowX + TEXT_FIELD_MARGIN;
        graphics.fill(sidebarX, windowY + 2, sidebarX + SIDEBAR_WIDTH, windowY + WINDOW_HEIGHT - 2, 0xFF252525);
        
        graphics.fill(codeAreaX, windowY + 2, windowX + WINDOW_WIDTH - 2, windowY + WINDOW_HEIGHT - 2, 0xFF1a1a1a);
        
        graphics.drawString(this.font, "Code Editor", codeAreaX, windowY + 10, 0xFFFFFF);
        
        renderSidebar(graphics, windowX, windowY);
        
        this.codeEditor.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
