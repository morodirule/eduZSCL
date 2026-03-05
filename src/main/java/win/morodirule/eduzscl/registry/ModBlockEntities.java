package win.morodirule.eduzscl.registry;

import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import win.morodirule.eduzscl.Eduzscl;
import win.morodirule.eduzscl.blockentity.AgentBlockEntity;

import java.util.Set;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister
            .create(net.minecraft.core.registries.Registries.BLOCK_ENTITY_TYPE, Eduzscl.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AgentBlockEntity>> AGENT_BLOCK_ENTITY = BLOCK_ENTITIES
            .register("agent_block", () -> new BlockEntityType<>(AgentBlockEntity::new, Set.of(ModBlocks.AGENT_BLOCK.get())));
}
