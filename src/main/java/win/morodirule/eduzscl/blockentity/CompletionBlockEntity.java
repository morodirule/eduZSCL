package win.morodirule.eduzscl.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import win.morodirule.eduzscl.registry.ModBlockEntities;
import win.morodirule.eduzscl.teaching.TeachingAgent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CompletionBlockEntity extends BlockEntity implements MenuProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger("CompletionBlockEntity");
    private String successCommand = "say Level completed!";
    private String failureCommand = "say Level failed!";

    public CompletionBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.COMPLETION_BLOCK_ENTITY.get(), pos, state);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.eduzscl.completion_block");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new win.morodirule.eduzscl.menu.CompletionBlockMenu(id, inv, this.worldPosition);
    }

    public String getSuccessCommand() {
        return successCommand;
    }

    public void setSuccessCommand(String command) {
        this.successCommand = command;
        setChanged();
    }

    public String getFailureCommand() {
        return failureCommand;
    }

    public void setFailureCommand(String command) {
        this.failureCommand = command;
        setChanged();
    }

    public void onAgentExecutionComplete(ServerLevel level, ServerPlayer player) {
        if (level == null || player == null) {
            return;
        }

        boolean powered = level.hasNeighborSignal(this.worldPosition);
        String command = powered ? this.successCommand : this.failureCommand;

        if (command == null || command.isEmpty()) {
            TeachingAgent.sendMessageToPlayer(player, powered ? "§aLesson completed!" : "§cLesson failed! Completion block not powered.");
            return;
        }

        MinecraftServer server = level.getServer();
        if (server == null) {
            LOGGER.error("Could not execute completion command; server is null");
            return;
        }

        try {
            server.getCommands().performPrefixedCommand(
                player.createCommandSourceStack().withSuppressedOutput(),
                command
            );
            TeachingAgent.sendMessageToPlayer(player, powered ? "§aLesson completed!" : "§cLesson failed! Completion block not powered.");
        } catch (Exception e) {
            LOGGER.error("Failed to execute completion command: {}", e.getMessage());
            TeachingAgent.sendMessageToPlayer(player, "§cError executing completion command: " + e.getMessage());
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        tag.putString("SuccessCommand", successCommand);
        tag.putString("FailureCommand", failureCommand);
    }

    @Override
    public void loadAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        if (tag.contains("SuccessCommand")) {
            successCommand = tag.getString("SuccessCommand");
        }
        if (tag.contains("FailureCommand")) {
            failureCommand = tag.getString("FailureCommand");
        }
    }
}
