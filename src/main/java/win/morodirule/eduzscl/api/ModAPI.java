package win.morodirule.eduzscl.api;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import win.morodirule.eduzscl.teaching.RhinoContext;
import win.morodirule.eduzscl.teaching.TeachingAgent;

public class ModAPI {
    private final RhinoContext rhinoContext;
    
    public ModAPI(RhinoContext rhinoContext) {
        this.rhinoContext = rhinoContext;
    }
    
    public String placeBlock(int x, int y, int z, String blockId) {
        if (!rhinoContext.incrementOperations()) {
            throw new RhinoContext.ScriptExecutionException("Operation limit exceeded (1000 blocks)");
        }
        
        if (!isValidPosition(x, y, z)) {
            return "Invalid position: (" + x + ", " + y + ", " + z + ")";
        }
        
        ServerPlayer player = TeachingAgent.getCurrentPlayer();
        if (player == null) return "No player context";
        
        Level world = player.serverLevel();
        Block block = getBlockById(blockId);
        
        if (block == Blocks.AIR) {
            return "Unknown block: " + blockId;
        }
        
        BlockPos pos = new BlockPos(x, y, z);
        if (world.setBlock(pos, block.defaultBlockState(), 3)) {
            return "Placed " + blockId + " at (" + x + ", " + y + ", " + z + ")";
        }
        
        return "Failed to place block";
    }
    
    public String getBlock(int x, int y, int z) {
        ServerPlayer player = TeachingAgent.getCurrentPlayer();
        if (player == null) return null;
        
        Level world = player.serverLevel();
        BlockPos pos = new BlockPos(x, y, z);
        BlockState state = world.getBlockState(pos);
        return state.getBlock().builtInRegistryHolder().key().location().toString();
    }
    
    public String setBlock(int x, int y, int z, String blockId) {
        return placeBlock(x, y, z, blockId);
    }
    
    private boolean isValidPosition(int x, int y, int z) {
        return y >= 0 && y <= 320 && Math.abs(x) <= 30000000 && Math.abs(z) <= 30000000;
    }
    
    private Block getBlockById(String blockId) {
        if (blockId == null || blockId.isEmpty()) {
            return Blocks.AIR;
        }
        
        try {
            String fullId = blockId.contains(":") ? blockId : "minecraft:" + blockId;
            return net.minecraft.core.registries.BuiltInRegistries.BLOCK
                .getOptional(net.minecraft.resources.ResourceLocation.parse(fullId))
                .orElse(Blocks.AIR);
        } catch (Exception e) {
            return Blocks.AIR;
        }
    }
}
