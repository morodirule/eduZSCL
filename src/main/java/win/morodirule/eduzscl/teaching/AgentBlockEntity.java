package win.morodirule.eduzscl.teaching;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import dev.latvian.mods.rhino.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import win.morodirule.eduzscl.Eduzscl;

import java.util.HashMap;
import java.util.Map;

public class AgentBlockEntity extends BlockEntity implements net.minecraft.world.MenuProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger("AgentBlockEntity");
    
    private static final String AGENT_X_KEY = "agentX";
    private static final String AGENT_Y_KEY = "agentY";
    private static final String AGENT_Z_KEY = "agentZ";
    
    private String code = "// Write your code here\nconsole.log(\"Hello, Agent!\");\nagent.move(1);\n";
    private Direction direction = Direction.NORTH;
    private BlockPos agentPosition = BlockPos.ZERO;
    private int executionCount = 0;
    private static final int MAX_OPERATIONS = 100;
    private ServerPlayer executingPlayer;

    public AgentBlockEntity(BlockPos pos, BlockState state) {
        super(Eduzscl.AGENT_BLOCK_ENTITY.get(), pos, state);
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
        setChanged();
    }

    @Override
    public net.minecraft.network.chat.Component getDisplayName() {
        return net.minecraft.network.chat.Component.literal("Agent Code Editor");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player p) {
        return new AgentBlockMenu(id, inv, this.worldPosition);
    }

    public void executeCode(ServerPlayer player) {
        executingPlayer = player;
        executionCount = 0;
        
        agentPosition = this.worldPosition;
        
        // Set global player context for APIs
        TeachingAgent.setCurrentPlayer(player);
        
        try {
            RhinoContext rhino = new RhinoContext();
            // Use the same Context for scope creation and execution
            Scriptable scope = rhino.getRuntime().initStandardObjects();
            // Setup standard globals (mod, player, console)
            rhino.setupGlobals(scope);
            
            AgentAPI agentApi = new AgentAPI(this, player);
            ScriptableObject.putProperty(scope, "agent", agentApi, rhino.getRuntime());
            
            rhino.execute(code, scope);
            
        } catch (Exception e) {
            if (executingPlayer != null) {
                TeachingAgent.sendMessageToPlayer(executingPlayer, "§cError: " + e.getMessage());
            }
            LOGGER.error("Agent code execution error: {}", e.getMessage());
        } finally {
            executingPlayer = null;
            TeachingAgent.setCurrentPlayer(null);
        }
    }

    public String executeCodeFromGui(String code, net.minecraft.world.entity.player.Player player) {
        if (player == null) {
            return "No player provided";
        }
        
        if (!(player instanceof net.minecraft.server.level.ServerPlayer serverPlayer)) {
            return "Must be on server to execute code";
        }
        
        executingPlayer = serverPlayer;
        executionCount = 0;
        agentPosition = this.worldPosition;
        
        TeachingAgent.setCurrentPlayer(serverPlayer);
        
        try {
            RhinoContext rhino = new RhinoContext();
            Scriptable scope = rhino.getRuntime().initStandardObjects();
            rhino.setupGlobals(scope);
            
            AgentAPI agentApi = new AgentAPI(this, serverPlayer);
            ScriptableObject.putProperty(scope, "agent", agentApi, rhino.getRuntime());
            
            rhino.execute(code, scope);
            
            return "Code executed successfully";
            
        } catch (Exception e) {
            String error = "Error: " + e.getMessage();
            TeachingAgent.sendMessageToPlayer(serverPlayer, "§c" + error);
            return error;
        } finally {
            executingPlayer = null;
            TeachingAgent.setCurrentPlayer(null);
        }
    }

    public void sendMessage(String message) {
        if (executingPlayer != null) {
            TeachingAgent.sendMessageToPlayer(executingPlayer, message);
        } else {
            LOGGER.info("Agent: {}", message);
        }
    }

    public void log(String message) {
        sendMessage("§b" + message);
    }

    public boolean incrementOperations() {
        return ++executionCount <= MAX_OPERATIONS;
    }

    public BlockPos getAgentPosition() {
        if (agentPosition.equals(BlockPos.ZERO)) {
            agentPosition = this.worldPosition;
        }
        return agentPosition;
    }

    public AgentBlockEntity setAgentPosition(BlockPos pos) {
        Level level = this.getLevel();
        if (level == null) {
            this.agentPosition = pos;
            setChanged();
            return this;
        }
        
        if (pos.equals(this.worldPosition)) {
            this.agentPosition = pos;
            setChanged();
            return this;
        }
        
        BlockState currentState = level.getBlockState(this.worldPosition);
        
        if (!level.isEmptyBlock(pos)) {
            LOGGER.warn("Cannot move block to occupied position: {}", pos);
            return this;
        }
        
        level.setBlock(pos, currentState, 3);
        
        BlockEntity newBlockEntity = level.getBlockEntity(pos);
        if (newBlockEntity instanceof AgentBlockEntity newAgentBlock) {
            newAgentBlock.agentPosition = pos;
            newAgentBlock.direction = this.direction;
            newAgentBlock.code = this.code;
            newAgentBlock.setChanged();

            // Remove the old block entity
            level.removeBlockEntity(this.worldPosition);
            level.setBlock(this.worldPosition, Blocks.AIR.defaultBlockState(), 3);
            
            // Server-side notification - need to sync to clients
            if (level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                serverLevel.sendBlockUpdated(this.worldPosition, Blocks.AIR.defaultBlockState(), Blocks.AIR.defaultBlockState(), 3);
                serverLevel.sendBlockUpdated(pos, currentState, currentState, 3);
            }
            return newAgentBlock;
        }
        
        // If for some reason the new block entity is not an AgentBlockEntity,
        // revert and return current. This should not happen if block states are correct.
        LOGGER.error("New block entity at {} is not an AgentBlockEntity. Reverting move.", pos);
        level.setBlock(this.worldPosition, currentState, 3); // Revert placing the block
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3); // Clear the new position
        return this;
    }

    public Direction getAgentDirection() {
        return direction;
    }

    public void setAgentDirection(Direction direction) {
        this.direction = direction;
        setChanged();
    }

    public void turnLeft() {
        this.direction = this.direction.getCounterClockWise();
        setChanged();
    }

    public void turnRight() {
        this.direction = this.direction.getClockWise();
        setChanged();
    }

    // NBT save/load disabled - needs update for 1.21.4
    /*
    protected void saveAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        
        tag.putString("code", code);
        tag.putInt("direction", direction.get2DDataValue());
        tag.putInt(AGENT_X_KEY, agentPosition.getX());
        tag.putInt(AGENT_Y_KEY, agentPosition.getY());
        tag.putInt(AGENT_Z_KEY, agentPosition.getZ());
    }

    public void load(net.minecraft.core.HolderLookup.Provider provider, CompoundTag tag) {
        super.load(provider, tag);
        
        this.code = tag.getString("code");
        this.direction = Direction.from2DDataValue(tag.getInt("direction"));
        this.agentPosition = new BlockPos(
            tag.getInt(AGENT_X_KEY),
            tag.getInt(AGENT_Y_KEY),
            tag.getInt(AGENT_Z_KEY)
        );
    }
    */

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
    }
}
