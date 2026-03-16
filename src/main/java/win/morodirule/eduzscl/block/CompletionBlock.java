package win.morodirule.eduzscl.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import win.morodirule.eduzscl.blockentity.CompletionBlockEntity;

public class CompletionBlock extends Block implements EntityBlock {
    public CompletionBlock(Properties properties) {
        super(properties);
    }

    @Override
    public boolean isSignalSource(BlockState state) {
        return true;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CompletionBlockEntity(pos, state);
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, net.minecraft.world.level.Level level, BlockPos pos, Player player,
            BlockHitResult hit) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof CompletionBlockEntity completion) {
                serverPlayer.openMenu(completion);
            }
        }
        return InteractionResult.SUCCESS;
    }
}
