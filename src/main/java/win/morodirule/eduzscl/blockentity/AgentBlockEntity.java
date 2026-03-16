package win.morodirule.eduzscl.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
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
import win.morodirule.eduzscl.api.AgentAPI;
import win.morodirule.eduzscl.block.AgentBlock;
import win.morodirule.eduzscl.block.CompletionBlock;
import win.morodirule.eduzscl.registry.ModBlockEntities;
import win.morodirule.eduzscl.registry.ModBlocks;
import win.morodirule.eduzscl.teaching.RhinoContext;
import win.morodirule.eduzscl.teaching.TeachingAgent;
import win.morodirule.eduzscl.teaching.TeachingAgent.Lesson;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class AgentBlockEntity extends BlockEntity implements MenuProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger("AgentBlockEntity");
    
    private static final String AGENT_X_KEY = "agentX";
    private static final String AGENT_Y_KEY = "agentY";
    private static final String AGENT_Z_KEY = "agentZ";
    private static final String CURRENT_LESSON_KEY = "currentLesson";
    public static final long STEP_DELAY_MS = 500L;
    
    private String code = "// Write your code here\nconsole.log(\"Hello, Agent!\");\nagent.move(1);\n";
    private Direction direction = Direction.NORTH;
    private BlockPos agentPosition = BlockPos.ZERO;
    private int executionCount = 0;
    private static final int MAX_OPERATIONS = 100;
    private ServerPlayer executingPlayer;
    private String currentLessonId = null;
    private volatile boolean executionInProgress = false;

    public AgentBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.AGENT_BLOCK_ENTITY.get(), pos, state);
        if (state.hasProperty(AgentBlock.FACING)) {
            this.direction = state.getValue(AgentBlock.FACING);
        }
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
        setChanged();
    }
    
    public void receiveSyncData(String code, int direction, String lessonId) {
        this.code = code;
        this.direction = Direction.from3DDataValue(direction);
        this.currentLessonId = lessonId;
        if (level != null && worldPosition != null) {
            BlockState state = level.getBlockState(worldPosition);
            if (state.hasProperty(AgentBlock.FACING)) {
                level.setBlock(worldPosition, state.setValue(AgentBlock.FACING, this.direction), 3);
            }
        }
        LOGGER.info("Synced code from server: {}", code.substring(0, Math.min(50, code.length())));
    }
    
    public String getCurrentLessonId() {
        return currentLessonId;
    }
    
    public void setCurrentLessonId(String lessonId) {
        this.currentLessonId = lessonId;
        setChanged();
    }
    
    public Lesson getCurrentLesson() {
        if (currentLessonId == null) {
            return TeachingAgent.LessonManager.getDefaultLesson();
        }
        return TeachingAgent.LessonManager.getLesson(currentLessonId);
    }
    
    public int getMaxOperations() {
        Lesson lesson = getCurrentLesson();
        return lesson != null ? lesson.getMaxOperations() : MAX_OPERATIONS;
    }

    @Override
    public Component getDisplayName() {
        return Component.literal("Agent Code Editor");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player p) {
        return new win.morodirule.eduzscl.menu.AgentBlockMenu(id, inv, this.worldPosition);
    }

    public void executeCode(ServerPlayer player) {
        if (executionInProgress) {
            TeachingAgent.sendMessageToPlayer(player, "§eAgent is already executing code.");
            return;
        }

        executingPlayer = player;
        executionCount = 0;
        agentPosition = this.worldPosition;
        startExecutionAsync(player, this.code);
    }

    public String executeCodeFromGui(String code, Player player) {
        if (player == null) {
            return "No player provided";
        }
        
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return "Must be on server to execute code";
        }
        
        this.code = code;
        setChanged();
        if (executionInProgress) {
            return "Agent is already executing code";
        }

        executingPlayer = serverPlayer;
        executionCount = 0;
        agentPosition = this.worldPosition;
        startExecutionAsync(serverPlayer, code);
        return "Code execution started";
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

    public void setExecutingPlayer(ServerPlayer player) {
        this.executingPlayer = player;
    }

    public boolean waitForNextStep() {
        try {
            Thread.sleep(STEP_DELAY_MS);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            sendMessage("§cExecution interrupted.");
            return false;
        }
    }

    private void startExecutionAsync(ServerPlayer player, String codeToRun) {
        executionInProgress = true;
        CompletableFuture.runAsync(() -> {
            TeachingAgent.setCurrentPlayer(player);
            Lesson lesson = getCurrentLesson();

            try {
                RhinoContext rhino = new RhinoContext(lesson);
                Scriptable scope = rhino.getRuntime().initStandardObjects();
                rhino.setupGlobals(scope);

                AgentAPI agentApi = new AgentAPI(this, player, lesson);
                ScriptableObject.putProperty(scope, "agent", agentApi, rhino.getRuntime());

                rhino.execute(codeToRun, scope);
            } catch (Exception e) {
                if (executingPlayer != null) {
                    TeachingAgent.sendMessageToPlayer(executingPlayer, "§cError: " + e.getMessage());
                }
                LOGGER.error("Agent code execution error: {}", e.getMessage());
            } finally {
                executionInProgress = false;
                
                if (level instanceof ServerLevel serverLevel) {
                    CompletionBlockEntity completionBlock = findCompletionBlock(serverLevel);
                    if (completionBlock != null) {
                        String successCmd = completionBlock.getSuccessCommand();
                        if (successCmd != null && !successCmd.isEmpty()) {
                            executeCommand(serverLevel, successCmd);
                        }
                        TeachingAgent.sendMessageToPlayer(player, "§aLesson completed!");
                    } else {
                        String failureCmd = "";
                        CompletionBlockEntity nearestBlock = findNearestCompletionBlock(serverLevel);
                        if (nearestBlock != null) {
                            failureCmd = nearestBlock.getFailureCommand();
                        }
                        if (failureCmd != null && !failureCmd.isEmpty()) {
                            executeCommand(serverLevel, failureCmd);
                        }
                        TeachingAgent.sendMessageToPlayer(player, "§cLesson failed! Completion block not powered.");
                    }
                }
                
                executingPlayer = null;
                TeachingAgent.setCurrentPlayer(null);
            }
        });
    }
    
    private void executeCommand(ServerLevel level, String command) {
        if (command == null || command.isEmpty()) return;
        
        MinecraftServer server = level.getServer();
        if (server == null) return;
        
        LOGGER.info("Executing completion command: {}", command);
        server.getCommands().performPrefixedCommand(
            server.createCommandSourceStack().withSuppressedOutput(),
            command
        );
    }
    
    private CompletionBlockEntity findCompletionBlock(ServerLevel level) {
        int scanRadius = 16;
        BlockPos agentPos = getAgentPosition();
        
        for (int x = -scanRadius; x <= scanRadius; x++) {
            for (int y = -scanRadius; y <= scanRadius; y++) {
                for (int z = -scanRadius; z <= scanRadius; z++) {
                    BlockPos checkPos = agentPos.offset(x, y, z);
                    BlockState state = level.getBlockState(checkPos);
                    
                    if (state.getBlock() instanceof CompletionBlock) {
                        int power = level.getBestNeighborSignal(checkPos);
                        if (power > 0) {
                            LOGGER.info("Completion block found and powered at {}", checkPos);
                            BlockEntity be = level.getBlockEntity(checkPos);
                            if (be instanceof CompletionBlockEntity completion) {
                                return completion;
                            }
                        }
                    }
                }
            }
        }
        
        return null;
    }
    
    private CompletionBlockEntity findNearestCompletionBlock(ServerLevel level) {
        int scanRadius = 16;
        BlockPos agentPos = getAgentPosition();
        
        for (int x = -scanRadius; x <= scanRadius; x++) {
            for (int y = -scanRadius; y <= scanRadius; y++) {
                for (int z = -scanRadius; z <= scanRadius; z++) {
                    BlockPos checkPos = agentPos.offset(x, y, z);
                    BlockState state = level.getBlockState(checkPos);
                    
                    if (state.getBlock() instanceof CompletionBlock) {
                        BlockEntity be = level.getBlockEntity(checkPos);
                        if (be instanceof CompletionBlockEntity completion) {
                            return completion;
                        }
                    }
                }
            }
        }
        
        return null;
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

        if (level.isClientSide) {
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
        
        // Save the current state to NBT before moving
        CompoundTag stateTag = new CompoundTag();
        saveToTag(stateTag);
        
        level.removeBlockEntity(this.worldPosition);
        level.setBlock(this.worldPosition, Blocks.AIR.defaultBlockState(), 3);
        
        level.setBlock(pos, currentState, 3);
        
        BlockEntity newBlockEntity = level.getBlockEntity(pos);
        if (newBlockEntity == null) {
            newBlockEntity = ModBlockEntities.AGENT_BLOCK_ENTITY.get().create(pos, currentState);
            if (newBlockEntity != null) {
                level.setBlockEntity(newBlockEntity);
            }
        }
        
        if (newBlockEntity instanceof AgentBlockEntity newAgentBlock) {
            // Load all state from NBT (code, direction, lesson, etc)
            newAgentBlock.loadFromTag(stateTag);
            newAgentBlock.agentPosition = pos;
            // Ensure the loaded state is marked for persistence
            newAgentBlock.setChanged();
            
            LOGGER.info("Loaded state into new agent block: code length = {}", newAgentBlock.code.length());

            if (level instanceof ServerLevel serverLevel) {
                // Set the block entity in the level BEFORE sending updates
                level.setBlockEntity(newAgentBlock);
                
                // Force flush the update tag so clients receive the complete NBT
                CompoundTag updateTag = newAgentBlock.getUpdateTag(serverLevel.registryAccess());
                LOGGER.info("Update tag contains code of length: {}", 
                    updateTag.contains("Code") ? updateTag.getString("Code").length() : 0);
                
                // Update the old position with air block
                serverLevel.sendBlockUpdated(this.worldPosition, Blocks.AIR.defaultBlockState(), Blocks.AIR.defaultBlockState(), 3);
                // Send full block entity data to all clients at the new position
                serverLevel.sendBlockUpdated(pos, currentState, currentState, 3);
                
                // If a player executed this code, reopen their menu at the new location
                if (executingPlayer != null && executingPlayer.containerMenu instanceof win.morodirule.eduzscl.menu.AgentBlockMenu) {
                    LOGGER.info("Reopening AgentBlockMenu for player at new position: {}, code: {}", pos, newAgentBlock.code.substring(0, Math.min(50, newAgentBlock.code.length())));
                    newAgentBlock.setExecutingPlayer(executingPlayer);
                    executingPlayer.openMenu(newAgentBlock);
                }
            }
            LOGGER.info("Moved agent from {} to {}, code: {}", this.worldPosition, pos, newAgentBlock.code.substring(0, Math.min(50, newAgentBlock.code.length())));
            return newAgentBlock;
        }
        
        LOGGER.error("New block entity at {} is not an AgentBlockEntity. Reverting move.", pos);
        level.setBlock(this.worldPosition, currentState, 3);
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        return this;
    }

    public Direction getAgentDirection() {
        return direction;
    }

    public void setAgentDirection(Direction direction) {
        this.direction = direction;
        if (level != null && worldPosition != null) {
            BlockState state = level.getBlockState(worldPosition);
            if (state.hasProperty(AgentBlock.FACING)) {
                if (level.isClientSide) {
                    setChanged();
                    return;
                }
                level.setBlock(worldPosition, state.setValue(AgentBlock.FACING, direction), 3);
            }
        }
        setChanged();
    }

    public void turnLeft() {
        this.direction = this.direction.getCounterClockWise();
        if (level != null && worldPosition != null) {
            BlockState state = level.getBlockState(worldPosition);
            if (state.hasProperty(AgentBlock.FACING)) {
                if (level.isClientSide) {
                    setChanged();
                    return;
                }
                level.setBlock(worldPosition, state.setValue(AgentBlock.FACING, this.direction), 3);
            }
        }
        setChanged();
    }

    public void turnRight() {
        this.direction = this.direction.getClockWise();
        if (level != null && worldPosition != null) {
            BlockState state = level.getBlockState(worldPosition);
            if (state.hasProperty(AgentBlock.FACING)) {
                if (level.isClientSide) {
                    setChanged();
                    return;
                }
                level.setBlock(worldPosition, state.setValue(AgentBlock.FACING, this.direction), 3);
            }
        }
        setChanged();
    }
    
    public void syncCustomData() {
        this.setChanged();
    }
    
    @Override
    public CompoundTag getPersistentData() {
        CompoundTag tag = new CompoundTag();
        saveToTag(tag);
        return tag;
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
    }
    
    @Override
    public void saveAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        saveToTag(tag);
    }
    
    @Override
    public void loadAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        loadFromTag(tag);
    }
    
    @Override
    public CompoundTag getUpdateTag(net.minecraft.core.HolderLookup.Provider provider) {
        CompoundTag tag = super.getUpdateTag(provider);
        saveToTag(tag);
        LOGGER.info("getUpdateTag called at position {}, code: {}", this.worldPosition, code.substring(0, Math.min(30, code.length())));
        return tag;
    }
    
    @Override
    public void handleUpdateTag(CompoundTag tag, net.minecraft.core.HolderLookup.Provider provider) {
        super.handleUpdateTag(tag, provider);
        LOGGER.info("handleUpdateTag called at position {}", this.worldPosition);
        loadFromTag(tag);
    }
    
    private void loadFromTag(CompoundTag tag) {
        if (tag.contains("Code")) {
            this.code = tag.getString("Code");
            LOGGER.info("Loaded code from NBT: {}", this.code.substring(0, Math.min(50, this.code.length())));
        }
        if (tag.contains("Direction")) {
            this.direction = Direction.from3DDataValue(tag.getInt("Direction"));
        }
        if (tag.contains(CURRENT_LESSON_KEY)) {
            this.currentLessonId = tag.getString(CURRENT_LESSON_KEY);
            LOGGER.info("Loaded lesson from NBT: {}", this.currentLessonId);
        }
    }
    
    private CompoundTag saveToTag(CompoundTag tag) {
        tag.putString("Code", code);
        tag.putInt("Direction", direction.get3DDataValue());
        if (currentLessonId != null) {
            tag.putString(CURRENT_LESSON_KEY, currentLessonId);
        }
        LOGGER.info("Saved code to NBT: {}", code.substring(0, Math.min(50, code.length())));
        return tag;
    }
}
