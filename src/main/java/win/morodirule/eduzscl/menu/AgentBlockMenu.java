package win.morodirule.eduzscl.menu;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import win.morodirule.eduzscl.registry.ModMenus;
import win.morodirule.eduzscl.blockentity.AgentBlockEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AgentBlockMenu extends AbstractContainerMenu {
    public static final MenuType<AgentBlockMenu> TYPE = ModMenus.AGENT_BLOCK_MENU.get();
    
    private static final Logger LOGGER = LoggerFactory.getLogger("AgentBlockMenu");
    private static final Map<Integer, BlockPos> MENU_ID_TO_POS = new ConcurrentHashMap<>();
    private static final Map<Integer, String> MENU_ID_TO_CODE = new ConcurrentHashMap<>();
    private static final Map<Integer, String> MENU_ID_TO_LESSON = new ConcurrentHashMap<>();
    
    public static AgentBlockMenu create(int id, Inventory playerInventory) {
        return new AgentBlockMenu(id, playerInventory);
    }

    public AgentBlockMenu(int id, Inventory playerInventory) {
        super(ModMenus.AGENT_BLOCK_MENU.get(), id);
        BlockPos pos = MENU_ID_TO_POS.remove(id);
        String cachedCode = MENU_ID_TO_CODE.remove(id);
        String cachedLessonId = MENU_ID_TO_LESSON.remove(id);
        this.blockPos = pos != null ? pos : BlockPos.ZERO;
        this.cachedCode = cachedCode;
        this.cachedLessonId = cachedLessonId;
        LOGGER.info("AgentBlockMenu client constructor - id: {}, blockPos: {}, cachedCode present: {}", id, this.blockPos, cachedCode != null);
        
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

    public AgentBlockMenu(int id, Inventory playerInventory, BlockPos blockPos) {
        super(ModMenus.AGENT_BLOCK_MENU.get(), id);
        if (blockPos == null) {
            LOGGER.error("AgentBlockMenu server constructor received null blockPos for id {}", id);
            this.blockPos = BlockPos.ZERO;
        } else {
            this.blockPos = blockPos;
            MENU_ID_TO_POS.put(id, blockPos);
        }
        LOGGER.info("AgentBlockMenu server constructor - id: {}, blockPos: {}", id, this.blockPos);
        Player player = playerInventory.player;
        BlockEntity be = player.level().getBlockEntity(this.blockPos);
        this.blockEntity = be instanceof AgentBlockEntity ? (AgentBlockEntity) be : null;
        
        // Cache the code so the client receives it even if the block entity NBT hasn't synced yet
        if (this.blockEntity instanceof AgentBlockEntity agent) {
            MENU_ID_TO_CODE.put(id, agent.getCode());
            MENU_ID_TO_LESSON.put(id, agent.getCurrentLessonId());
            LOGGER.info("Cached code for menu id {}: length = {}", id, agent.getCode().length());
        }
    }

    private BlockPos blockPos;
    private AgentBlockEntity blockEntity;
    private String cachedCode;
    private String cachedLessonId;

    public BlockPos getBlockPos() {
        return blockPos;
    }

    public AgentBlockEntity getBlockEntity() {
        return blockEntity;
    }
    
    public String getCachedCode() {
        return cachedCode;
    }

    public String getCachedLessonId() {
        return cachedLessonId;
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
