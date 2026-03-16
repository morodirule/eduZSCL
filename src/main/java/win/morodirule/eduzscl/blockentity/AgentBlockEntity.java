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
    
    private static final String DEFAULT_CODE = "// Write your code here\nconsole.log(\"Hello, Agent!\");\nagent.move(1);\n";
    private String code = DEFAULT_CODE;
    private Direction direction = Direction.NORTH;
    private BlockPos agentPosition = BlockPos.ZERO;
    private BlockPos linkedCompletionBlockPos = BlockPos.ZERO;
    private int executionCount = 0;
    private static final int MAX_OPERATIONS = 100;
    private ServerPlayer executingPlayer;
    private String currentLessonId = null;
    private volatile boolean executionInProgress = false;

    // Original position and direction to restore after execution
    private BlockPos originalPosition = BlockPos.ZERO;
    private Direction originalDirection = Direction.NORTH;

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
        
        // Store original position and direction for restoration after execution
        originalPosition = this.worldPosition;
        originalDirection = this.direction;
        
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
        
        // Store original position and direction for restoration after execution
        originalPosition = this.worldPosition;
        originalDirection = this.direction;
        
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
        // Capture the starting state before any movement occurs
        this.originalPosition = this.worldPosition;
        this.originalDirection = this.direction;

        CompletableFuture.runAsync(() -> {
            TeachingAgent.setCurrentPlayer(player);
            Lesson lesson = getCurrentLesson();
            AgentAPI agentApi = null;
            try {
                RhinoContext rhino = new RhinoContext(lesson);
                Scriptable scope = rhino.getRuntime().initStandardObjects();
                rhino.setupGlobals(scope);

                agentApi = new AgentAPI(this, player, lesson);
                ScriptableObject.putProperty(scope, "agent", agentApi, rhino.getRuntime());

                rhino.execute(codeToRun, scope);
            } catch (Exception e) {
                if (executingPlayer != null) {
                    TeachingAgent.sendMessageToPlayer(executingPlayer, "§cError: " + e.getMessage());
                }
                LOGGER.error("Agent code execution error: {}", e.getMessage());
            } finally {
                executionInProgress = false;
                final AgentAPI agentApiFinal = agentApi;

            if (level instanceof ServerLevel serverLevel) {
                serverLevel.getServer().execute(() -> {
                    AgentBlockEntity activeAgent = agentApiFinal != null ? agentApiFinal.getAgentBlock() : this;

                    boolean hasLink = hasLinkedCompletionBlock();
                    BlockPos linkedPos = getLinkedCompletionBlockPos();
                    CompletionBlockEntity completionBlock = findCompletionBlock(serverLevel);

                    if (completionBlock != null) {
                        completionBlock.onAgentExecutionComplete(serverLevel, player);
                        boolean success = serverLevel.hasNeighborSignal(completionBlock.getBlockPos());
                        if (success) {
                            resetUserCodeToDefault();
                        }
                    } else if (!hasLink) {
                        LOGGER.info("No completion block linked in agent NBT at {}", this.worldPosition);
                        TeachingAgent.sendMessageToPlayer(player, "§cLesson failed! No completion block link is set in the agent.");
                    } else {
                        String rootCause;
                        if (!serverLevel.isLoaded(linkedPos)) {
                            rootCause = "linked completion position is not loaded";
                        } else if (!(serverLevel.getBlockState(linkedPos).getBlock() instanceof CompletionBlock)) {
                            rootCause = "linked position does not contain a completion block";
                        } else if (!(serverLevel.getBlockEntity(linkedPos) instanceof CompletionBlockEntity)) {
                            rootCause = "linked position does not contain a completion block entity";
                        } else {
                            rootCause = "unknown (invalid link state)";
                        }

                        LOGGER.info("Linked completion block reference invalid for agent at {}: {}", this.worldPosition, rootCause);
                        TeachingAgent.sendMessageToPlayer(player, "§cLesson failed! Completion block linked in agent is invalid: " + rootCause + ". Please reconnect with connector and try again.");

                        // Reset invalid link to force reconnect next run
                        setLinkedCompletionBlockPos(BlockPos.ZERO);
                        LOGGER.info("Agent linked completion block was reset for agent at {}", this.worldPosition);
                    }

                    // Always restore agent to original position and direction using the active instance
                    activeAgent.restoreAgentToOriginalState();

                    executingPlayer = null;
                    TeachingAgent.setCurrentPlayer(null);
                });
            } else {
                // Fallback (should not happen), do restoration on current thread.
                AgentBlockEntity activeAgent = this;
                if (agentApiFinal != null) {
                    activeAgent = agentApiFinal.getAgentBlock();
                }
                activeAgent.restoreAgentToOriginalState();
                executingPlayer = null;
                TeachingAgent.setCurrentPlayer(null);
            }
        }
        });
    }
    
    private void executeCommand(ServerLevel level, ServerPlayer player, String command) {
        if (command == null || command.isEmpty()) return;
        
        MinecraftServer server = level.getServer();
        if (server == null) return;
        
        LOGGER.info("Executing completion command: {} for player {}", command, player.getName().getString());
        try {
            server.getCommands().performPrefixedCommand(
                player.createCommandSourceStack().withSuppressedOutput(),
                command
            );
            LOGGER.info("Command executed successfully");
        } catch (Exception e) {
            LOGGER.error("Failed to execute completion command: {}", e.getMessage());
        }
    }

    private void restoreAgentToOriginalState() {
        Level level = this.getLevel();
        if (level == null || level.isClientSide) {
            return;
        }

        BlockPos targetPos = originalPosition == null ? this.worldPosition : this.originalPosition;
        Direction targetDir = originalDirection == null ? this.direction : this.originalDirection;

        // Physically move back if the block entity was relocated during execution
        AgentBlockEntity current = this;
        if (!targetPos.equals(this.worldPosition)) {
            AgentBlockEntity moved = this.setAgentPosition(targetPos);
            if (moved != null) {
                current = moved;
            }
        }

        current.agentPosition = targetPos;
        current.direction = targetDir;

        BlockState state = level.getBlockState(targetPos);
        if (state.hasProperty(AgentBlock.FACING)) {
            level.setBlock(targetPos, state.setValue(AgentBlock.FACING, targetDir), 3);
            if (level instanceof ServerLevel serverLevel) {
                serverLevel.getChunkSource().blockChanged(targetPos);
            }
        }

        current.setChanged();
        LOGGER.info("Agent restored to original position {} and direction {}", targetPos, targetDir);
    }
    
    private CompletionBlockEntity findCompletionBlock(ServerLevel level) {
        if (hasLinkedCompletionBlock()) {
            BlockPos checkPos = getLinkedCompletionBlockPos();
            if (level.isLoaded(checkPos) && level.getBlockState(checkPos).getBlock() instanceof CompletionBlock) {
                BlockEntity be = level.getBlockEntity(checkPos);
                if (be instanceof CompletionBlockEntity completion) {
                    LOGGER.info("Linked completion block found at {}", checkPos);
                    return completion;
                }

                // Recovery: block is correct type but missing block entity. Try to create and reattach.
                LOGGER.warn("Linked completion block at {} is missing entity; attempting recovery.", checkPos);
                if (level instanceof ServerLevel serverLevel) {
                    BlockState state = level.getBlockState(checkPos);
                    CompletionBlockEntity created = new CompletionBlockEntity(checkPos, state);
                    level.setBlockEntity(created);
                    serverLevel.sendBlockUpdated(checkPos, state, state, 3);

                    BlockEntity recoveredBe = level.getBlockEntity(checkPos);
                    if (recoveredBe instanceof CompletionBlockEntity recoveredCompletion) {
                        LOGGER.info("Recovered and attached completion block entity at {}", checkPos);
                        return recoveredCompletion;
                    }
                }

                LOGGER.warn("Recovery failed for linked completion block at {}", checkPos);
            } else {
                LOGGER.info("Linked completion block at {} is missing or wrong type", checkPos);
            }
        }

        return null; // Only linked completion blocks are valid now
    }
    
    public BlockPos getAgentPosition() {
        if (agentPosition.equals(BlockPos.ZERO)) {
            agentPosition = this.worldPosition;
        }
        return agentPosition;
    }

    public boolean hasLinkedCompletionBlock() {
        return linkedCompletionBlockPos != null && !linkedCompletionBlockPos.equals(BlockPos.ZERO);
    }

    public BlockPos getLinkedCompletionBlockPos() {
        return linkedCompletionBlockPos;
    }

    public void setLinkedCompletionBlockPos(BlockPos linkedCompletionBlockPos) {
        this.linkedCompletionBlockPos = linkedCompletionBlockPos == null ? BlockPos.ZERO : linkedCompletionBlockPos;
        setChanged();
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

    private void resetUserCodeToDefault() {
        this.code = DEFAULT_CODE;
        setChanged();
        LOGGER.info("Agent code reset to default after successful lesson at {}", this.worldPosition);
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
        if (tag.contains("LinkedCompletionX") && tag.contains("LinkedCompletionY") && tag.contains("LinkedCompletionZ")) {
            this.linkedCompletionBlockPos = new BlockPos(tag.getInt("LinkedCompletionX"), tag.getInt("LinkedCompletionY"), tag.getInt("LinkedCompletionZ"));
            LOGGER.info("Loaded linked completion block from NBT: {}", this.linkedCompletionBlockPos);
        }
        if (tag.contains("OriginalX") && tag.contains("OriginalY") && tag.contains("OriginalZ")) {
            this.originalPosition = new BlockPos(tag.getInt("OriginalX"), tag.getInt("OriginalY"), tag.getInt("OriginalZ"));
        }
        if (tag.contains("OriginalDirection")) {
            this.originalDirection = Direction.from3DDataValue(tag.getInt("OriginalDirection"));
        }
    }
    
    private CompoundTag saveToTag(CompoundTag tag) {
        tag.putString("Code", code);
        tag.putInt("Direction", direction.get3DDataValue());
        if (currentLessonId != null) {
            tag.putString(CURRENT_LESSON_KEY, currentLessonId);
        }
        if (hasLinkedCompletionBlock()) {
            tag.putInt("LinkedCompletionX", linkedCompletionBlockPos.getX());
            tag.putInt("LinkedCompletionY", linkedCompletionBlockPos.getY());
            tag.putInt("LinkedCompletionZ", linkedCompletionBlockPos.getZ());
        }
        if (originalPosition != null && !originalPosition.equals(BlockPos.ZERO)) {
            tag.putInt("OriginalX", originalPosition.getX());
            tag.putInt("OriginalY", originalPosition.getY());
            tag.putInt("OriginalZ", originalPosition.getZ());
        }
        if (originalDirection != null) {
            tag.putInt("OriginalDirection", originalDirection.get3DDataValue());
        }
        LOGGER.info("Saved code to NBT: {}", code.substring(0, Math.min(50, code.length())));
        return tag;
    }
}
