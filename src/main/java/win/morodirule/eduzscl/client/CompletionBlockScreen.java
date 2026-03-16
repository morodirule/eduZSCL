package win.morodirule.eduzscl.client;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.client.Minecraft;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import win.morodirule.eduzscl.menu.CompletionBlockMenu;
import win.morodirule.eduzscl.blockentity.CompletionBlockEntity;

import java.util.UUID;

public class CompletionBlockScreen extends Screen implements MenuAccess<CompletionBlockMenu> {
    private static final Logger LOGGER = LoggerFactory.getLogger("CompletionBlockScreen");
    private static final int WINDOW_WIDTH = 400;
    private static final int WINDOW_HEIGHT = 180;
    private static final int BUTTON_WIDTH = 80;
    private static final int BUTTON_HEIGHT = 20;

    private final BlockPos blockPos;
    private final Inventory inventory;
    private CompletionBlockMenu menu;
    private EditBox successCommandField;
    private EditBox failureCommandField;
    private Button saveButton;
    private Button closeButton;

    public CompletionBlockScreen(CompletionBlockMenu menu) {
        super(Component.translatable("gui.eduzscl.completion_editor.title"));
        this.menu = menu;
        this.inventory = null;
        this.blockPos = menu.getBlockPos();
    }

    public CompletionBlockScreen(CompletionBlockMenu menu, Inventory inventory, Component component) {
        super(Component.translatable("gui.eduzscl.completion_editor.title"));
        this.menu = menu;
        this.inventory = inventory;
        this.blockPos = menu.getBlockPos();
    }

    @Override
    public CompletionBlockMenu getMenu() {
        return this.menu;
    }

    @Override
    protected void init() {
        super.init();

        int windowX = (this.width - WINDOW_WIDTH) / 2;
        int windowY = (this.height - WINDOW_HEIGHT) / 2;

        this.successCommandField = new EditBox(
                this.font,
                windowX + 20,
                windowY + 40,
                WINDOW_WIDTH - 40,
                20,
                Component.translatable("gui.eduzscl.completion_editor.success_command")
        );
        this.successCommandField.setMaxLength(256);
        String cachedSuccess = "";
        if (menu != null && menu.getCachedSuccessCommand() != null) {
            cachedSuccess = menu.getCachedSuccessCommand();
        } else if (menu != null && menu.getBlockEntity() != null) {
            cachedSuccess = menu.getBlockEntity().getSuccessCommand();
        }
        this.successCommandField.setValue(cachedSuccess);
        this.addRenderableWidget(successCommandField);

        this.failureCommandField = new EditBox(
                this.font,
                windowX + 20,
                windowY + 90,
                WINDOW_WIDTH - 40,
                20,
                Component.translatable("gui.eduzscl.completion_editor.failure_command")
        );
        this.failureCommandField.setMaxLength(256);
        String cachedFailure = "";
        if (menu != null && menu.getCachedFailureCommand() != null) {
            cachedFailure = menu.getCachedFailureCommand();
        } else if (menu != null && menu.getBlockEntity() != null) {
            cachedFailure = menu.getBlockEntity().getFailureCommand();
        }
        this.failureCommandField.setValue(cachedFailure);
        this.addRenderableWidget(failureCommandField);

        int buttonY = windowY + WINDOW_HEIGHT - 30;
        int buttonCenterX = windowX + WINDOW_WIDTH / 2;

        this.saveButton = Button.builder(
                Component.translatable("gui.eduzscl.completion_editor.save"),
                button -> this.saveAndClose()
        ).bounds(buttonCenterX - BUTTON_WIDTH - 5, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT).build();
        this.addRenderableWidget(saveButton);

        this.closeButton = Button.builder(
                Component.translatable("gui.eduzscl.completion_editor.close"),
                button -> this.onClose()
        ).bounds(buttonCenterX + 5, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT).build();
        this.addRenderableWidget(closeButton);
    }

    private void saveAndClose() {
        saveStateToServer();
        if (this.minecraft != null && this.minecraft.player != null) {
            this.minecraft.player.closeContainer();
        }
        if (this.minecraft != null) {
            this.minecraft.setScreen(null);
        }
    }

    private void saveStateToServer() {
        Minecraft mc = Minecraft.getInstance();
        MinecraftServer server = mc.getSingleplayerServer();
        if (server == null) {
            LOGGER.warn("Singleplayer server is null");
            return;
        }

        if (mc.player == null) {
            LOGGER.warn("Local client player is null");
            return;
        }

        final UUID playerUuid = mc.player.getUUID();
        final String playerName = mc.player.getGameProfile().getName();
        final String successCmd = successCommandField.getValue();
        final String failureCmd = failureCommandField.getValue();
        final BlockPos fallbackPos = this.blockPos;

        server.execute(() -> {
            PlayerList playerList = server.getPlayerList();
            if (playerList == null) return;

            ServerPlayer serverPlayer = playerList.getPlayer(playerUuid);
            if (serverPlayer == null) serverPlayer = playerList.getPlayerByName(playerName);
            if (serverPlayer == null && !playerList.getPlayers().isEmpty()) {
                serverPlayer = playerList.getPlayers().get(0);
            }
            if (serverPlayer == null) return;

            CompletionBlockEntity completionBlock = null;
            if (serverPlayer.containerMenu instanceof CompletionBlockMenu completionMenu) {
                BlockPos menuPos = completionMenu.getBlockPos();
                var menuBe = serverPlayer.serverLevel().getBlockEntity(menuPos);
                if (menuBe instanceof CompletionBlockEntity completion) {
                    completionBlock = completion;
                }
            }

            if (completionBlock == null && fallbackPos != null && !fallbackPos.equals(BlockPos.ZERO)) {
                var be = serverPlayer.serverLevel().getBlockEntity(fallbackPos);
                if (be instanceof CompletionBlockEntity completion) {
                    completionBlock = completion;
                }
            }

            if (completionBlock != null) {
                completionBlock.setSuccessCommand(successCmd);
                completionBlock.setFailureCommand(failureCmd);
                // Store the commands so we can look them up later even if block entity isn't loaded
                win.morodirule.eduzscl.blockentity.AgentBlockEntity.storeCompletionCommands(fallbackPos, successCmd, failureCmd);
            }
        });
    }

    @Override
    public void onClose() {
        saveStateToServer();
        super.onClose();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);

        int windowX = (this.width - WINDOW_WIDTH) / 2;
        int windowY = (this.height - WINDOW_HEIGHT) / 2;

        graphics.fill(windowX, windowY, windowX + WINDOW_WIDTH, windowY + WINDOW_HEIGHT, 0xFF2d2d2d);

        graphics.drawString(this.font, Component.translatable("gui.eduzscl.completion_editor.title").getString(),
                windowX + 20, windowY + 10, 0xFFFFFF);

        graphics.drawString(this.font, Component.translatable("gui.eduzscl.completion_editor.success_label").getString(),
                windowX + 20, windowY + 30, 0xFFAA00);

        graphics.drawString(this.font, Component.translatable("gui.eduzscl.completion_editor.failure_label").getString(),
                windowX + 20, windowY + 80, 0xFFAA00);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
