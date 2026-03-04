package win.morodirule.eduzscl.teaching;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import win.morodirule.eduzscl.Eduzscl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AgentBlockMenu extends AbstractContainerMenu {
    private static final Logger LOGGER = LoggerFactory.getLogger("AgentBlockMenu");
    private static final Map<Integer, BlockPos> MENU_ID_TO_POS = new ConcurrentHashMap<>();
    
    // Supplier for client-side menu creation
    public static AgentBlockMenu create(int id, Inventory playerInventory) {
        return new AgentBlockMenu(id, playerInventory);
    }

    // Client constructor
    public AgentBlockMenu(int id, Inventory playerInventory) {
        super(Eduzscl.AGENT_BLOCK_MENU.get(), id);
        BlockPos pos = MENU_ID_TO_POS.remove(id);
        this.blockPos = pos != null ? pos : BlockPos.ZERO;
        LOGGER.info("AgentBlockMenu client constructor - id: {}, blockPos: {}", id, this.blockPos);
        
        // Try to get blockEntity from level using the BlockPos
        if (this.blockPos != BlockPos.ZERO && playerInventory.player != null && 
            playerInventory.player.level() != null) {
            BlockEntity be = playerInventory.player.level().getBlockEntity(this.blockPos);
            this.blockEntity = be instanceof AgentBlockEntity ? (AgentBlockEntity) be : null;
            LOGGER.info("Client found blockEntity: {}", this.blockEntity);
        } else {
            this.blockEntity = null;
            LOGGER.info("Client could not find blockEntity - blockPos: {}, player: {}, level: {}", 
                this.blockPos, playerInventory.player, 
                playerInventory.player != null ? playerInventory.player.level() : "null");
        }
    }

    // Server constructor
    public AgentBlockMenu(int id, Inventory playerInventory, BlockPos blockPos) {
        super(Eduzscl.AGENT_BLOCK_MENU.get(), id);
        this.blockPos = blockPos;
        MENU_ID_TO_POS.put(id, blockPos);
        LOGGER.info("AgentBlockMenu server constructor - id: {}, blockPos: {}", id, blockPos);
        Player player = playerInventory.player;
        BlockEntity be = player.level().getBlockEntity(blockPos);
        this.blockEntity = be instanceof AgentBlockEntity ? (AgentBlockEntity) be : null;
    }

    private BlockPos blockPos;
    private AgentBlockEntity blockEntity;

    public BlockPos getBlockPos() {
        return blockPos;
    }

    public AgentBlockEntity getBlockEntity() {
        return blockEntity;
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
