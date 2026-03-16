package win.morodirule.eduzscl.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import win.morodirule.eduzscl.registry.ModBlockEntities;

public class CompletionBlockEntity extends BlockEntity implements MenuProvider {
    private String successCommand = "";
    private String failureCommand = "";

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
