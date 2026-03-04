package win.morodirule.eduzscl.teaching.client;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import win.morodirule.eduzscl.Eduzscl;
import win.morodirule.eduzscl.teaching.AgentBlockMenu;

public class AgentBlockScreen extends Screen implements MenuAccess<AgentBlockMenu> {
    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger("AgentBlockScreen");
    private static final int WINDOW_WIDTH = 400;
    private static final int WINDOW_HEIGHT = 300;
    private static final int TEXT_FIELD_MARGIN = 20;
    private static final int BUTTON_WIDTH = 80;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_MARGIN = 10;

    private final BlockPos blockPos;
    private final Inventory inventory;
    private EditBox codeField;
    private Button runButton;
    private Button resetButton;
    private Button closeButton;
    private AgentBlockMenu menu;

    public AgentBlockScreen(BlockPos blockPos) {
        super(Component.literal("Agent Code Editor"));
        this.blockPos = blockPos;
        this.inventory = null;
    }

    public AgentBlockScreen(AgentBlockMenu menu) {
        super(Component.literal("Agent Code Editor"));
        this.menu = menu;
        this.inventory = null;
        LOGGER.info("AgentBlockScreen created - menu: {}, blockPos: {}, blockEntity: {}", 
            menu, menu.getBlockPos(), menu.getBlockEntity());
        this.blockPos = menu.getBlockPos();
    }

    public AgentBlockScreen(AgentBlockMenu menu, Inventory inventory, Component component) {
        super(Component.literal("Agent Code Editor"));
        this.menu = menu;
        this.inventory = inventory;
        LOGGER.info("AgentBlockScreen created - menu: {}, inventory.player: {}", 
            menu, inventory.player);
        
        // Get blockPos from the menu
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

        int windowX = (this.width - WINDOW_WIDTH) / 2;
        int windowY = (this.height - WINDOW_HEIGHT) / 2;

        int textFieldWidth = WINDOW_WIDTH - (TEXT_FIELD_MARGIN * 2);
        int textFieldHeight = WINDOW_HEIGHT - 80;

        this.codeField = new EditBox(
                this.font,
                windowX + TEXT_FIELD_MARGIN,
                windowY + 30,
                textFieldWidth,
                textFieldHeight,
                Component.literal("Code")
        );
        this.codeField.setMaxLength(10000);
        
        if (menu != null && menu.getBlockEntity() != null) {
            this.codeField.setValue(menu.getBlockEntity().getCode());
        } else {
            this.codeField.setValue("// Write your code here\nconsole.log(\"Hello, Agent!\");\nagent.move(1);\n");
        }
        
        this.addRenderableWidget(this.codeField);

        int buttonY = windowY + WINDOW_HEIGHT - 40;
        int centerX = windowX + WINDOW_WIDTH / 2;

        this.runButton = Button.builder(
                Component.literal("Run"),
                button -> this.runCode()
        ).bounds(centerX - BUTTON_WIDTH - BUTTON_MARGIN, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT).build();
        this.addRenderableWidget(runButton);

        this.resetButton = Button.builder(
                Component.literal("Reset"),
                button -> {
                    codeField.setValue("// Write your code here\nconsole.log(\"Hello, Agent!\");\nagent.move(1);\n");
                }
        ).bounds(centerX + BUTTON_MARGIN, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT).build();
        this.addRenderableWidget(resetButton);

        this.closeButton = Button.builder(
                Component.literal("Close"),
                button -> this.onClose()
        ).bounds(centerX - BUTTON_WIDTH / 2, buttonY + BUTTON_HEIGHT + 5, BUTTON_WIDTH, BUTTON_HEIGHT).build();
        this.addRenderableWidget(closeButton);
    }

    private void runCode() {
        String code = codeField.getValue();
        
        if (menu != null && menu.getBlockEntity() != null) {
            menu.getBlockEntity().setCode(code);
            
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            net.minecraft.world.entity.player.Player player = mc.player;
            
            net.minecraft.server.level.ServerPlayer serverPlayer = null;
            if (mc.getSingleplayerServer() != null) {
                serverPlayer = mc.getSingleplayerServer().getPlayerList().getPlayer(player.getUUID());
            }
            
            if (serverPlayer == null) {
                LOGGER.warn("Could not find server player in single player");
                return;
            }
            
            menu.getBlockEntity().executeCodeFromGui(code, serverPlayer);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int windowX = (this.width - WINDOW_WIDTH) / 2;
        int windowY = (this.height - WINDOW_HEIGHT) / 2;

        graphics.fill(windowX, windowY, windowX + WINDOW_WIDTH, windowY + WINDOW_HEIGHT, 0xFF2d2d2d);
        graphics.fill(windowX + 2, windowY + 2, windowX + WINDOW_WIDTH - 2, windowY + WINDOW_HEIGHT - 2, 0xFF1a1a1a);

        graphics.drawString(this.font, "Agent Code Editor", windowX + TEXT_FIELD_MARGIN, windowY + 10, 0xFFFFFF);

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.codeField.isFocused()) {
            return this.codeField.keyPressed(keyCode, scanCode, modifiers) || super.keyPressed(keyCode, scanCode, modifiers);
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (this.codeField.isFocused()) {
            return this.codeField.charTyped(codePoint, modifiers) || super.charTyped(codePoint, modifiers);
        }
        return super.charTyped(codePoint, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
