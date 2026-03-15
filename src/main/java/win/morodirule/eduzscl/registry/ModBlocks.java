package win.morodirule.eduzscl.registry;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;
import win.morodirule.eduzscl.Eduzscl;
import win.morodirule.eduzscl.block.AgentBlock;

public class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Eduzscl.MODID);

    public static final DeferredBlock<Block> EXAMPLE_BLOCK = BLOCKS.registerSimpleBlock("example_block",
            BlockBehaviour.Properties.of().mapColor(MapColor.STONE));

    public static final DeferredBlock<AgentBlock> AGENT_BLOCK = BLOCKS.registerBlock("agent_block", AgentBlock::new,
            BlockBehaviour.Properties.of().noOcclusion());
}
