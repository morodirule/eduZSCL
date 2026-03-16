package win.morodirule.eduzscl.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import win.morodirule.eduzscl.block.AgentBlock;
import win.morodirule.eduzscl.blockentity.AgentBlockEntity;
import win.morodirule.eduzscl.teaching.TeachingAgent;
import win.morodirule.eduzscl.teaching.TeachingAgent.Lesson;

import java.util.stream.Collectors;

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
                .then(Commands.literal("lesson")
                    .then(Commands.argument("lessonId", StringArgumentType.string())
                        .executes(CommandHandler::setAgentLesson)))
                .then(Commands.literal("lessons")
                    .executes(CommandHandler::listLessons))
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
        
        source.sendFailure(Component.translatable("command.eduzscl.js.player_only"));
        return 0;
    }
    
    private static int showHelp(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        source.sendSuccess(() -> Component.translatable("command.eduzscl.js.help.header"), false);
        source.sendSuccess(() -> Component.translatable("command.eduzscl.js.help.js"), false);
        source.sendSuccess(() -> Component.translatable("command.eduzscl.js.help.help"), false);
        source.sendSuccess(() -> Component.translatable("command.eduzscl.js.help.open"), false);
        source.sendSuccess(() -> Component.literal("/agent lessons - List lesson IDs"), false);
        source.sendSuccess(() -> Component.literal("/agent lesson <id> - Set lesson on looked-at agent block"), false);
        source.sendSuccess(() -> Component.translatable("command.eduzscl.js.help.debug"), false);
        return 1;
    }

    private static int openAgentGui(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        
        if (source.getEntity() instanceof ServerPlayer player) {
            AgentBlockEntity blockEntity = resolveLookedAtAgent(player);
            if (blockEntity == null) {
                source.sendFailure(Component.translatable("command.eduzscl.agent.not_looking_at_agent"));
                return 0;
            }
            player.openMenu(blockEntity);
            return 1;
        }
        
        source.sendFailure(Component.translatable("command.eduzscl.agent.player_only"));
        return 0;
    }

    private static int setAgentLesson(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        String lessonId = StringArgumentType.getString(ctx, "lessonId");
        Lesson lesson = TeachingAgent.LessonManager.getLesson(lessonId);
        if (lesson == null) {
            source.sendFailure(Component.literal("Unknown lesson id: " + lessonId + ". Use /agent lessons"));
            return 0;
        }

        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.translatable("command.eduzscl.agent.player_only"));
            return 0;
        }

        AgentBlockEntity blockEntity = resolveLookedAtAgent(player);
        if (blockEntity == null) {
            source.sendFailure(Component.translatable("command.eduzscl.agent.not_looking_at_agent"));
            return 0;
        }

        blockEntity.setCurrentLessonId(lessonId);
        source.sendSuccess(() -> Component.literal("Set agent lesson to " + lessonId + " (" + lesson.getTitle() + ")"), false);
        return 1;
    }

    private static int listLessons(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        var lessons = TeachingAgent.LessonManager.getAllLessons();
        if (lessons.isEmpty()) {
            source.sendFailure(Component.literal("No lessons found"));
            return 0;
        }

        String ids = lessons.stream()
            .map(Lesson::getId)
            .collect(Collectors.joining(", "));
        source.sendSuccess(() -> Component.literal("Available lessons: " + ids), false);
        return 1;
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
            if (block instanceof AgentBlock) {
                AgentBlockEntity blockEntity = (AgentBlockEntity) level.getBlockEntity(pos);
                if (blockEntity != null) {
                    player.openMenu(blockEntity);
                    source.sendSuccess(() -> Component.translatable("command.eduzscl.agent.debug_success", x, y, z), false);
                } else {
                    source.sendFailure(Component.translatable("command.eduzscl.agent.not_found"));
                }
            } else {
                source.sendFailure(Component.translatable("command.eduzscl.agent.not_found"));
            }
            return 1;
        }
        
        source.sendFailure(Component.translatable("command.eduzscl.agent.player_only"));
        return 0;
    }

    private static AgentBlockEntity resolveLookedAtAgent(ServerPlayer player) {
        Level level = player.level();
        net.minecraft.world.phys.HitResult hit = player.pick(5.0, 1.0f, false);
        if (!(hit instanceof net.minecraft.world.phys.BlockHitResult blockHit)) {
            return null;
        }

        BlockPos pos = blockHit.getBlockPos();
        Block block = level.getBlockState(pos).getBlock();
        if (!(block instanceof AgentBlock)) {
            return null;
        }

        if (!(level.getBlockEntity(pos) instanceof AgentBlockEntity blockEntity)) {
            return null;
        }
        return blockEntity;
    }

}
