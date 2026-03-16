package win.morodirule.eduzscl.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import win.morodirule.eduzscl.Eduzscl;

public record CompletionBlockUpdatePayload(
    BlockPos pos,
    String successCommand,
    String failureCommand
) implements CustomPacketPayload {
    public static final Type<CompletionBlockUpdatePayload> TYPE =
        new Type<>(ResourceLocation.parse(Eduzscl.MODID + ":completion_block_update"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CompletionBlockUpdatePayload> STREAM_CODEC =
        StreamCodec.composite(
            BlockPos.STREAM_CODEC, CompletionBlockUpdatePayload::pos,
            ByteBufCodecs.STRING_UTF8, CompletionBlockUpdatePayload::successCommand,
            ByteBufCodecs.STRING_UTF8, CompletionBlockUpdatePayload::failureCommand,
            CompletionBlockUpdatePayload::new
        );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
