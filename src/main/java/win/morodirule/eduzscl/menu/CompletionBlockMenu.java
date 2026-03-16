package win.morodirule.eduzscl.menu;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import win.morodirule.eduzscl.registry.ModMenus;
import win.morodirule.eduzscl.blockentity.CompletionBlockEntity;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CompletionBlockMenu extends AbstractContainerMenu {
    public static final MenuType<CompletionBlockMenu> TYPE = ModMenus.COMPLETION_BLOCK_MENU.get();

    private static final Map<Integer, BlockPos> MENU_ID_TO_POS = new ConcurrentHashMap<>();
    private static final Map<Integer, String> MENU_ID_TO_SUCCESS = new ConcurrentHashMap<>();
    private static final Map<Integer, String> MENU_ID_TO_FAILURE = new ConcurrentHashMap<>();

    public static CompletionBlockMenu create(int id, Inventory playerInventory) {
        return new CompletionBlockMenu(id, playerInventory);
    }

    public CompletionBlockMenu(int id, Inventory playerInventory) {
        super(ModMenus.COMPLETION_BLOCK_MENU.get(), id);
        BlockPos pos = MENU_ID_TO_POS.remove(id);
        String cachedSuccess = MENU_ID_TO_SUCCESS.remove(id);
        String cachedFailure = MENU_ID_TO_FAILURE.remove(id);
        this.blockPos = pos != null ? pos : BlockPos.ZERO;
        this.cachedSuccessCommand = cachedSuccess;
        this.cachedFailureCommand = cachedFailure;
    }

    public CompletionBlockMenu(int id, Inventory playerInventory, BlockPos blockPos) {
        super(ModMenus.COMPLETION_BLOCK_MENU.get(), id);
        this.blockPos = blockPos;
        MENU_ID_TO_POS.put(id, blockPos);

        Player player = playerInventory.player;
        BlockEntity be = player.level().getBlockEntity(blockPos);
        this.blockEntity = be instanceof CompletionBlockEntity completion ? completion : null;

        if (this.blockEntity != null) {
            MENU_ID_TO_SUCCESS.put(id, this.blockEntity.getSuccessCommand());
            MENU_ID_TO_FAILURE.put(id, this.blockEntity.getFailureCommand());
        }
    }

    private BlockPos blockPos;
    private CompletionBlockEntity blockEntity;
    private String cachedSuccessCommand = "";
    private String cachedFailureCommand = "";

    public BlockPos getBlockPos() {
        return blockPos;
    }

    public CompletionBlockEntity getBlockEntity() {
        return blockEntity;
    }

    public String getCachedSuccessCommand() {
        return cachedSuccessCommand;
    }

    public String getCachedFailureCommand() {
        return cachedFailureCommand;
    }

    @Override
    public boolean stillValid(Player player) {
        if (blockEntity == null) return false;
        if (player.level().getBlockEntity(blockPos) != blockEntity) {
            return false;
        }
        return player.distanceToSqr(
                blockPos.getX() + 0.5,
                blockPos.getY() + 0.5,
                blockPos.getZ() + 0.5
        ) < 64.0;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slot) {
        return ItemStack.EMPTY;
    }
}
