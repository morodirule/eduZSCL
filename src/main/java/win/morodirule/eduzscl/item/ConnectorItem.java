package win.morodirule.eduzscl.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import win.morodirule.eduzscl.block.AgentBlock;
import win.morodirule.eduzscl.block.CompletionBlock;
import win.morodirule.eduzscl.blockentity.AgentBlockEntity;
import win.morodirule.eduzscl.blockentity.CompletionBlockEntity;

public class ConnectorItem extends Item {
    private static final String PENDING_AGENT_X = "ConnectorPendingAgentX";
    private static final String PENDING_AGENT_Y = "ConnectorPendingAgentY";
    private static final String PENDING_AGENT_Z = "ConnectorPendingAgentZ";
    private static final String PENDING_COMPLETION_X = "ConnectorPendingCompletionX";
    private static final String PENDING_COMPLETION_Y = "ConnectorPendingCompletionY";
    private static final String PENDING_COMPLETION_Z = "ConnectorPendingCompletionZ";

    public ConnectorItem(Properties properties) {
        super(properties);
    }

    private boolean hasCustomData(ItemStack stack, String key) {
        var customData = stack.get(DataComponents.CUSTOM_DATA);
        return customData != null && customData.copyTag().contains(key);
    }

    private int getCustomDataInt(ItemStack stack, String key) {
        var customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            return customData.copyTag().getInt(key);
        }
        return 0;
    }

    private void setCustomDataInt(ItemStack stack, String key, int value) {
        var customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        var tag = customData.copyTag();
        tag.putInt(key, value);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    private void removeCustomData(ItemStack stack) {
        stack.remove(DataComponents.CUSTOM_DATA);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        ItemStack stack = context.getItemInHand();

        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        if (!(context.getPlayer() instanceof ServerPlayer player)) {
            return InteractionResult.PASS;
        }

        BlockState clickedState = level.getBlockState(pos);
        BlockEntity clickedEntity = level.getBlockEntity(pos);

        // If player clicked on a completion block
        if (clickedState.getBlock() instanceof CompletionBlock && clickedEntity instanceof CompletionBlockEntity) {
            if (hasCustomData(stack, PENDING_AGENT_X) && hasCustomData(stack, PENDING_AGENT_Y) && hasCustomData(stack, PENDING_AGENT_Z)) {
                BlockPos agentPos = new BlockPos(
                        getCustomDataInt(stack, PENDING_AGENT_X),
                        getCustomDataInt(stack, PENDING_AGENT_Y),
                        getCustomDataInt(stack, PENDING_AGENT_Z));

                if (level.getBlockState(agentPos).getBlock() instanceof AgentBlock) {
                    BlockEntity agentBe = level.getBlockEntity(agentPos);
                    if (agentBe instanceof AgentBlockEntity agentBlockEntity) {
                        agentBlockEntity.setLinkedCompletionBlockPos(pos);
                        player.sendSystemMessage(Component.literal("Connector: Agent linked to completion block at " + pos));
                        removeCustomData(stack);
                        return InteractionResult.SUCCESS;
                    }
                }

                player.sendSystemMessage(Component.literal("Connector: Stored agent target no longer valid. Please re-select an agent."));
                removeCustomData(stack);
                return InteractionResult.SUCCESS;
            }

            setCustomDataInt(stack, PENDING_COMPLETION_X, pos.getX());
            setCustomDataInt(stack, PENDING_COMPLETION_Y, pos.getY());
            setCustomDataInt(stack, PENDING_COMPLETION_Z, pos.getZ());
            player.sendSystemMessage(Component.literal("Connector: Completion block selected at " + pos + ". Now click an agent block to link."));
            return InteractionResult.SUCCESS;
        }

        // If player clicked on an agent block
        if (clickedState.getBlock() instanceof AgentBlock && clickedEntity instanceof AgentBlockEntity) {
            if (hasCustomData(stack, PENDING_COMPLETION_X) && hasCustomData(stack, PENDING_COMPLETION_Y) && hasCustomData(stack, PENDING_COMPLETION_Z)) {
                BlockPos completionPos = new BlockPos(
                        getCustomDataInt(stack, PENDING_COMPLETION_X),
                        getCustomDataInt(stack, PENDING_COMPLETION_Y),
                        getCustomDataInt(stack, PENDING_COMPLETION_Z));

                if (level.getBlockState(completionPos).getBlock() instanceof CompletionBlock) {
                    AgentBlockEntity agentBlockEntity = (AgentBlockEntity) clickedEntity;
                    agentBlockEntity.setLinkedCompletionBlockPos(completionPos);
                    player.sendSystemMessage(Component.literal("Connector: Agent linked to completion block at " + completionPos));
                    removeCustomData(stack);
                    return InteractionResult.SUCCESS;
                }

                player.sendSystemMessage(Component.literal("Connector: Stored completion block target no longer valid. Please re-select a completion block."));
                removeCustomData(stack);
                return InteractionResult.SUCCESS;
            }

            setCustomDataInt(stack, PENDING_AGENT_X, pos.getX());
            setCustomDataInt(stack, PENDING_AGENT_Y, pos.getY());
            setCustomDataInt(stack, PENDING_AGENT_Z, pos.getZ());
            player.sendSystemMessage(Component.literal("Connector: Agent selected at " + pos + ". Now click a completion block to link."));
            return InteractionResult.SUCCESS;
        }

        player.sendSystemMessage(Component.literal("Connector: Please use on an Agent block or a Completion block."));
        return InteractionResult.PASS;
    }
}
