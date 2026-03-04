package win.morodirule.eduzscl.teaching;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import dev.latvian.mods.rhino.*;

public class CommandHandler {
    public static void register(Commands registration) {
        registration.getDispatcher().register(
            Commands.literal("js")
                .requires(src -> src.hasPermission(0))
                .then(Commands.argument("code", StringArgumentType.greedyString())
                    .executes(CommandHandler::runCode))
                .executes(CommandHandler::showHelp)
        );

        registration.getDispatcher().register(
            Commands.literal("agent")
                .requires(src -> src.hasPermission(0))
                .then(Commands.literal("open")
                    .executes(CommandHandler::openAgentGui))
                .then(Commands.literal("debug")
                    .then(Commands.argument("x", com.mojang.brigadier.arguments.IntegerArgumentType.integer())
                        .then(Commands.argument("y", com.mojang.brigadier.arguments.IntegerArgumentType.integer())
                            .then(Commands.argument("z", com.mojang.brigadier.arguments.IntegerArgumentType.integer())
                                .executes(CommandHandler::openAgentGuiAtPos)))))
        );
    }
    
    private static int runCode(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        String code = StringArgumentType.getString(ctx, "code");
        
        if (source.getEntity() instanceof ServerPlayer player) {
            TeachingAgent.executeCode(player, code);
            return 1;
        }
        
        source.sendFailure(Component.literal("Must be a player to run JS code"));
        return 0;
    }
    
    private static int showHelp(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        source.sendSuccess(() -> Component.literal("§6=== JavaScript Teaching Agent ==="), false);
        source.sendSuccess(() -> Component.literal("§e/js <code> - Run JavaScript code"), false);
        source.sendSuccess(() -> Component.literal("§e/js help - Show this help"), false);
        source.sendSuccess(() -> Component.literal("§e/agent open - Open Agent Block GUI"), false);
        source.sendSuccess(() -> Component.literal("§e/agent debug <x> <y> <z> - Open GUI at position"), false);
        return 1;
    }

    private static int openAgentGui(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        
        if (source.getEntity() instanceof ServerPlayer player) {
            // Get block player is looking at
            Level level = player.level();
            net.minecraft.world.phys.HitResult hit = player.pick(5.0, 1.0f, false);
            if (hit instanceof net.minecraft.world.phys.BlockHitResult blockHit) {
                BlockPos pos = blockHit.getBlockPos();
                
                Block block = level.getBlockState(pos).getBlock();
                if (block instanceof win.morodirule.eduzscl.teaching.AgentBlock) {
                    win.morodirule.eduzscl.teaching.AgentBlockEntity blockEntity = (win.morodirule.eduzscl.teaching.AgentBlockEntity) level.getBlockEntity(pos);
                    if (blockEntity != null) {
                        player.openMenu(blockEntity);
                    } else {
                        source.sendFailure(Component.literal("No Agent Block at position"));
                    }
                } else {
                    source.sendFailure(Component.literal("Not looking at an Agent Block"));
                }
            } else {
                source.sendFailure(Component.literal("Not looking at a block"));
            }
            return 1;
        }
        
        source.sendFailure(Component.literal("Must be a player"));
        return 0;
    }

    private static int openAgentGuiAtPos(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        int x = com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(ctx, "x");
        int y = com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(ctx, "y");
        int z = com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(ctx, "z");
        BlockPos pos = new BlockPos(x, y, z);
        
        if (source.getEntity() instanceof ServerPlayer player) {
            Level level = player.level();
            Block block = level.getBlockState(pos).getBlock();
            if (block instanceof win.morodirule.eduzscl.teaching.AgentBlock) {
                win.morodirule.eduzscl.teaching.AgentBlockEntity blockEntity = (win.morodirule.eduzscl.teaching.AgentBlockEntity) level.getBlockEntity(pos);
                if (blockEntity != null) {
                    player.openMenu(blockEntity);
                    source.sendSuccess(() -> Component.literal("Opened Agent GUI at " + x + ", " + y + ", " + z), false);
                } else {
                    source.sendFailure(Component.literal("No Agent Block at position"));
                }
            } else {
                source.sendFailure(Component.literal("No Agent Block at " + x + ", " + y + ", " + z));
            }
            return 1;
        }
        
        source.sendFailure(Component.literal("Must be a player"));
        return 0;
    }
}
