package win.morodirule.eduzscl.registry;

import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import win.morodirule.eduzscl.Eduzscl;
import win.morodirule.eduzscl.menu.AgentBlockMenu;
import win.morodirule.eduzscl.menu.CompletionBlockMenu;

public class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(net.minecraft.core.registries.Registries.MENU, Eduzscl.MODID);

    public static final DeferredHolder<MenuType<?>, MenuType<AgentBlockMenu>> AGENT_BLOCK_MENU = MENUS.register(
            "agent_block",
            () -> new MenuType<AgentBlockMenu>((id, inv) -> new AgentBlockMenu(id, inv), net.minecraft.world.flag.FeatureFlags.DEFAULT_FLAGS)
    );

    public static final DeferredHolder<MenuType<?>, MenuType<CompletionBlockMenu>> COMPLETION_BLOCK_MENU = MENUS.register(
            "completion_block",
            () -> new MenuType<CompletionBlockMenu>((id, inv) -> new CompletionBlockMenu(id, inv), net.minecraft.world.flag.FeatureFlags.DEFAULT_FLAGS)
    );
}
