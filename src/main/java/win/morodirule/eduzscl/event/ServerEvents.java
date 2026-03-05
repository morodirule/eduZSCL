package win.morodirule.eduzscl.event;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.ServerChatEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import win.morodirule.eduzscl.Eduzscl;
import win.morodirule.eduzscl.command.CommandHandler;
import win.morodirule.eduzscl.teaching.TeachingAgent;

public class ServerEvents {

    public static void register() {
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.register(ServerEvents.class);
    }

    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event) {
        Eduzscl.LOGGER.info("HELLO from server starting");
        CommandHandler.register(event.getServer().getCommands());
    }

    @SubscribeEvent
    public static void onServerChat(ServerChatEvent event) {
        String message = event.getMessage().getString();
        Eduzscl.LOGGER.info("Chat message: {}", message);

        if (message.startsWith("/js ") || message.equals("/js")) {
            event.setCanceled(true);
            var player = event.getPlayer();
            Eduzscl.LOGGER.info("JS command detected from player: {}", player.getName().getString());
            TeachingAgent.handleCommand(player, message);
        }
    }
}
