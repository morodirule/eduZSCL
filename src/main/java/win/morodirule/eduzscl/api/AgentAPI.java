package win.morodirule.eduzscl.api;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import dev.latvian.mods.rhino.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import win.morodirule.eduzscl.blockentity.AgentBlockEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class AgentAPI {
    private static final Logger LOGGER = LoggerFactory.getLogger("AgentAPI");
    
    private AgentBlockEntity agentBlock;
    private final ServerPlayer player;

    public AgentAPI(AgentBlockEntity agentBlock, ServerPlayer player) {
        this.agentBlock = agentBlock;
        this.player = player;
    }

    private boolean checkOperations() {
        return agentBlock.incrementOperations();
    }

    private Level resolveLevel() {
        Level level = agentBlock.getLevel();
        if (level == null) {
            return null;
        }
        if (!level.isClientSide) {
            return level;
        }
        return player != null && player.getServer() != null ? player.serverLevel() : null;
    }

    public void log(Object... args) {
        StringBuilder sb = new StringBuilder();
        for (Object arg : args) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(arg != null ? arg.toString() : "null");
        }
        agentBlock.log(sb.toString());
    }

    private <T> T onServerThread(Callable<T> action, T fallback) {
        if (player == null || player.getServer() == null) {
            return fallback;
        }

        try {
            if (player.getServer().isSameThread()) {
                return action.call();
            }
        } catch (Exception e) {
            LOGGER.error("Agent action failed on server thread: {}", e.getMessage());
            return fallback;
        }

        CompletableFuture<T> future = new CompletableFuture<>();
        player.getServer().execute(() -> {
            try {
                future.complete(action.call());
            } catch (Exception e) {
                LOGGER.error("Agent action failed: {}", e.getMessage());
                future.complete(fallback);
            }
        });

        try {
            return future.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return fallback;
        } catch (ExecutionException e) {
            LOGGER.error("Agent action completion failed: {}", e.getMessage());
            return fallback;
        }
    }

    public String gotoX(int x) {
        if (!checkOperations()) return "Operation limit exceeded";

        String result = onServerThread(() -> {
            BlockPos current = agentBlock.getAgentPosition();
            BlockPos newPos = new BlockPos(x, current.getY(), current.getZ());

            Level level = resolveLevel();
            if (level == null) return "No world";

            BlockState state = level.getBlockState(newPos);
            if (!state.isAir()) {
                return "Cannot teleport to occupied position";
            }

            this.agentBlock = agentBlock.setAgentPosition(newPos);
            return "Teleported to x=" + x;
        }, "No server");

        if (!"No server".equals(result)) {
            if (!agentBlock.waitForNextStep()) return "Execution interrupted";
        }
        return result;
    }

    public String gotoY(int y) {
        if (!checkOperations()) return "Operation limit exceeded";

        String result = onServerThread(() -> {
            BlockPos current = agentBlock.getAgentPosition();
            BlockPos newPos = new BlockPos(current.getX(), y, current.getZ());

            Level level = resolveLevel();
            if (level == null) return "No world";

            BlockState state = level.getBlockState(newPos);
            if (!state.isAir()) {
                return "Cannot teleport to occupied position";
            }

            this.agentBlock = agentBlock.setAgentPosition(newPos);
            return "Teleported to y=" + y;
        }, "No server");

        if (!"No server".equals(result)) {
            if (!agentBlock.waitForNextStep()) return "Execution interrupted";
        }
        return result;
    }

    public String gotoZ(int z) {
        if (!checkOperations()) return "Operation limit exceeded";

        String result = onServerThread(() -> {
            BlockPos current = agentBlock.getAgentPosition();
            BlockPos newPos = new BlockPos(current.getX(), current.getY(), z);

            Level level = resolveLevel();
            if (level == null) return "No world";

            BlockState state = level.getBlockState(newPos);
            if (!state.isAir()) {
                return "Cannot teleport to occupied position";
            }

            this.agentBlock = agentBlock.setAgentPosition(newPos);
            return "Teleported to z=" + z;
        }, "No server");

        if (!"No server".equals(result)) {
            if (!agentBlock.waitForNextStep()) return "Execution interrupted";
        }
        return result;
    }

    public String gotoPos(int x, int y, int z) {
        if (!checkOperations()) return "Operation limit exceeded";

        String result = onServerThread(() -> {
            BlockPos newPos = new BlockPos(x, y, z);

            Level level = resolveLevel();
            if (level == null) return "No world";

            BlockState state = level.getBlockState(newPos);
            if (!state.isAir()) {
                return "Cannot teleport to occupied position";
            }

            this.agentBlock = agentBlock.setAgentPosition(newPos);
            return "Teleported to (" + x + ", " + y + ", " + z + ")";
        }, "No server");

        if (!"No server".equals(result)) {
            if (!agentBlock.waitForNextStep()) return "Execution interrupted";
        }
        return result;
    }

    public String move(int steps) {
        if (!checkOperations()) return "Operation limit exceeded";
        
        if (steps < 1 || steps > 100) return "Steps must be between 1 and 100";

        Direction dir = onServerThread(() -> agentBlock.getAgentDirection(), null);
        if (dir == null) return "No server";

        for (int i = 0; i < steps; i++) {
            final int stepIndex = i + 1;
            String stepResult = onServerThread(() -> {
                Level level = resolveLevel();
                if (level == null) return "No world";

                BlockPos current = agentBlock.getAgentPosition();
                BlockPos next = current.relative(dir);
                BlockState state = level.getBlockState(next);

                if (!state.isAir()) {
                    return "Blocked at step " + stepIndex;
                }

                this.agentBlock = agentBlock.setAgentPosition(next);
                return null;
            }, "No server");

            if (stepResult != null) {
                return stepResult;
            }

            if (!agentBlock.waitForNextStep()) return "Execution interrupted";
        }

        return "Moved " + steps + " blocks " + dir.getName();
    }

    public String turnLeft() {
        if (!checkOperations()) return "Operation limit exceeded";

        String result = onServerThread(() -> {
            agentBlock.turnLeft();
            return "Turned left, now facing " + agentBlock.getAgentDirection().getName();
        }, "No server");

        if (!"No server".equals(result)) {
            if (!agentBlock.waitForNextStep()) return "Execution interrupted";
        }
        return result;
    }

    public String turnRight() {
        if (!checkOperations()) return "Operation limit exceeded";

        String result = onServerThread(() -> {
            agentBlock.turnRight();
            return "Turned right, now facing " + agentBlock.getAgentDirection().getName();
        }, "No server");

        if (!"No server".equals(result)) {
            if (!agentBlock.waitForNextStep()) return "Execution interrupted";
        }
        return result;
    }

    public String turnTo(String direction) {
        if (!checkOperations()) return "Operation limit exceeded";
        
        try {
            Direction dir = Direction.valueOf(direction.toUpperCase());
            String result = onServerThread(() -> {
                agentBlock.setAgentDirection(dir);
                return "Now facing " + dir.getName();
            }, "No server");

            if (!"No server".equals(result)) {
                if (!agentBlock.waitForNextStep()) return "Execution interrupted";
            }
            return result;
        } catch (IllegalArgumentException e) {
            return "Invalid direction: " + direction + ". Use north, south, east, or west";
        }
    }

    public String place(String blockId) {
        if (!checkOperations()) return "Operation limit exceeded";
        String result = onServerThread(() -> {
            Level level = resolveLevel();

            if (level == null) return "No world";

            BlockPos forward = agentBlock.getAgentPosition().relative(agentBlock.getAgentDirection());

            if (!level.isEmptyBlock(forward)) {
                return "Position already occupied";
            }

            Block block = getBlockById(blockId);
            if (block == Blocks.AIR) {
                return "Unknown block: " + blockId;
            }

            level.setBlock(forward, block.defaultBlockState(), 3);
            return "Placed " + blockId + " at (" + forward.getX() + ", " + forward.getY() + ", " + forward.getZ() + ")";
        }, "No server");

        if (!"No server".equals(result)) {
            if (!agentBlock.waitForNextStep()) return "Execution interrupted";
        }
        return result;
    }

    public String placeDown(String blockId) {
        if (!checkOperations()) return "Operation limit exceeded";

        String result = onServerThread(() -> {
            Level level = resolveLevel();
            if (level == null) return "No world";

            BlockPos down = agentBlock.getAgentPosition().below();

            if (!level.isEmptyBlock(down)) {
                return "Position already occupied";
            }

            Block block = getBlockById(blockId);
            if (block == Blocks.AIR) {
                return "Unknown block: " + blockId;
            }

            level.setBlock(down, block.defaultBlockState(), 3);
            return "Placed " + blockId + " below";
        }, "No server");

        if (!"No server".equals(result)) {
            if (!agentBlock.waitForNextStep()) return "Execution interrupted";
        }
        return result;
    }

    public String placeUp(String blockId) {
        if (!checkOperations()) return "Operation limit exceeded";

        String result = onServerThread(() -> {
            Level level = resolveLevel();
            if (level == null) return "No world";

            BlockPos up = agentBlock.getAgentPosition().above();

            if (!level.isEmptyBlock(up)) {
                return "Position already occupied";
            }

            Block block = getBlockById(blockId);
            if (block == Blocks.AIR) {
                return "Unknown block: " + blockId;
            }

            level.setBlock(up, block.defaultBlockState(), 3);
            return "Placed " + blockId + " above";
        }, "No server");

        if (!"No server".equals(result)) {
            if (!agentBlock.waitForNextStep()) return "Execution interrupted";
        }
        return result;
    }

    public String remove() {
        if (!checkOperations()) return "Operation limit exceeded";

        String result = onServerThread(() -> {
            Level level = resolveLevel();
            if (level == null) return "No world";

            BlockPos forward = agentBlock.getAgentPosition().relative(agentBlock.getAgentDirection());

            if (level.isEmptyBlock(forward)) {
                return "No block to remove";
            }

            level.setBlock(forward, Blocks.AIR.defaultBlockState(), 3);
            return "Removed block at forward position";
        }, "No server");

        if (!"No server".equals(result)) {
            if (!agentBlock.waitForNextStep()) return "Execution interrupted";
        }
        return result;
    }

    public String removeDown() {
        if (!checkOperations()) return "Operation limit exceeded";

        String result = onServerThread(() -> {
            Level level = resolveLevel();
            if (level == null) return "No world";

            BlockPos down = agentBlock.getAgentPosition().below();

            if (level.isEmptyBlock(down)) {
                return "No block to remove";
            }

            level.setBlock(down, Blocks.AIR.defaultBlockState(), 3);
            return "Removed block below";
        }, "No server");

        if (!"No server".equals(result)) {
            if (!agentBlock.waitForNextStep()) return "Execution interrupted";
        }
        return result;
    }

    public Map<String, Object> getPosition() {
        return onServerThread(() -> {
            Map<String, Object> pos = new HashMap<>();
            BlockPos p = agentBlock.getAgentPosition();
            pos.put("x", p.getX());
            pos.put("y", p.getY());
            pos.put("z", p.getZ());
            return pos;
        }, new HashMap<>());
    }

    public String getDirection() {
        return onServerThread(() -> agentBlock.getAgentDirection().getName(), "unknown");
    }

    private Block getBlockById(String blockId) {
        if (blockId == null || blockId.isEmpty()) {
            return Blocks.AIR;
        }
        
        try {
            String fullId = blockId.contains(":") ? blockId : "minecraft:" + blockId;
            return BuiltInRegistries.BLOCK
                .getOptional(ResourceLocation.parse(fullId))
                .orElse(Blocks.AIR);
        } catch (Exception e) {
            return Blocks.AIR;
        }
    }
}
