package win.morodirule.eduzscl.network;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import win.morodirule.eduzscl.Eduzscl;
import win.morodirule.eduzscl.blockentity.CompletionBlockEntity;

@EventBusSubscriber(modid = Eduzscl.MODID, bus = EventBusSubscriber.Bus.MOD)
public final class ModNetworking {
    private ModNetworking() {}

    @SubscribeEvent
    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(Eduzscl.MODID).versioned("1");
        registrar.playToServer(
            CompletionBlockUpdatePayload.TYPE,
            CompletionBlockUpdatePayload.STREAM_CODEC,
            ModNetworking::handleCompletionBlockUpdate
        );
    }

    private static void handleCompletionBlockUpdate(CompletionBlockUpdatePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }

            BlockPos pos = payload.pos();
            if (pos == null || pos.equals(BlockPos.ZERO)) {
                return;
            }

            Level level = player.level();
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof CompletionBlockEntity completion) {
                completion.setSuccessCommand(payload.successCommand());
                completion.setFailureCommand(payload.failureCommand());
                completion.setChanged();
                level.sendBlockUpdated(pos, completion.getBlockState(), completion.getBlockState(), 3);
            }
        });
    }
}
