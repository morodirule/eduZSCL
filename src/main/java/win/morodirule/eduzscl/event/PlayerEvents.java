package win.morodirule.eduzscl.event;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import win.morodirule.eduzscl.Eduzscl;
import win.morodirule.eduzscl.teaching.TeachingAgent;

public class PlayerEvents {

    public static void register() {
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.register(PlayerEvents.class);
    }

    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player) {
            TeachingAgent.onPlayerJoin(player);
        }
    }
}
