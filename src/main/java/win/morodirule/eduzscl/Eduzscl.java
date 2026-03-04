package win.morodirule.eduzscl;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.ServerChatEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.inventory.MenuType;
import win.morodirule.eduzscl.teaching.TeachingAgent;
import win.morodirule.eduzscl.teaching.CommandHandler;
import win.morodirule.eduzscl.teaching.AgentBlock;
import win.morodirule.eduzscl.teaching.AgentBlockEntity;
import win.morodirule.eduzscl.teaching.AgentBlockMenu;

import org.slf4j.Logger;

import java.util.Set;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(Eduzscl.MODID)
public class Eduzscl {
    // Define mod id in a common place for everything to reference
    public static final String MODID = "eduzscl";
    // Directly reference a slf4j logger
    private static final Logger LOGGER = LogUtils.getLogger();
    // Create a Deferred Register to hold Blocks which will all be registered under
    // the "eduzscl" namespace
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    // Create a Deferred Register to hold Items which will all be registered under
    // the "eduzscl" namespace
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    // Create a Deferred Register to hold CreativeModeTabs which will all be
    // registered under the "eduzscl" namespace
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister
            .create(Registries.CREATIVE_MODE_TAB, MODID);

    // Creates a new Block with the id "eduzscl:example_block", combining the
    // namespace and path
    public static final DeferredBlock<Block> EXAMPLE_BLOCK = BLOCKS.registerSimpleBlock("example_block",
            BlockBehaviour.Properties.of().mapColor(MapColor.STONE));
    // Creates a new BlockItem with the id "eduzscl:example_block", combining the
    // namespace and path
    public static final DeferredItem<BlockItem> EXAMPLE_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("example_block",
            EXAMPLE_BLOCK);

    // Agent Block
    public static final DeferredBlock<AgentBlock> AGENT_BLOCK = BLOCKS.registerBlock("agent_block", AgentBlock::new,
            BlockBehaviour.Properties.of());
    public static final DeferredItem<BlockItem> AGENT_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("agent_block",
            AGENT_BLOCK);

    // Block Entities
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister
            .create(Registries.BLOCK_ENTITY_TYPE, MODID);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AgentBlockEntity>> AGENT_BLOCK_ENTITY = BLOCK_ENTITIES
            .register("agent_block", () -> new BlockEntityType<>(AgentBlockEntity::new, Set.of(AGENT_BLOCK.get())));

    // Menus
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, MODID);
    public static final DeferredHolder<MenuType<?>, MenuType<AgentBlockMenu>> AGENT_BLOCK_MENU = MENUS.register(
            "agent_block",
            () -> new MenuType<AgentBlockMenu>((id, inv) -> new AgentBlockMenu(id, inv), FeatureFlags.DEFAULT_FLAGS)
    );

    // Creates a new food item with the id "eduzscl:example_id", nutrition 1 and
    // saturation 2
    public static final DeferredItem<Item> EXAMPLE_ITEM = ITEMS.registerSimpleItem("example_item", new Item.Properties()
            .food(new FoodProperties.Builder().alwaysEdible().nutrition(1).saturationModifier(2f).build()));

    // Creates a creative tab with the id "eduzscl:example_tab" for the example
    // item, that is placed after the combat tab
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> EXAMPLE_TAB = CREATIVE_MODE_TABS.register(
            "example_tab",
            () -> CreativeModeTab.builder().title(Component.translatable("itemGroup.eduzscl"))
                    .withTabsBefore(CreativeModeTabs.COMBAT).icon(() -> EXAMPLE_ITEM.get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        output.accept(EXAMPLE_ITEM.get()); // Add the example item to the tab. For your own tabs, this
                                                           // method is preferred over the event
                    }).build());

    // The constructor for the mod class is the first code that is run when your mod
    // is loaded.
    // FML will recognize some parameter types like IEventBus or ModContainer and
    // pass them in automatically.
    public Eduzscl(IEventBus modEventBus, ModContainer modContainer) {
        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);

        // Register the Deferred Register to the mod event bus so blocks get registered
        BLOCKS.register(modEventBus);
        // Register the Deferred Register to the mod event bus so items get registered
        ITEMS.register(modEventBus);
        // Register the Deferred Register to the mod event bus so tabs get registered
        CREATIVE_MODE_TABS.register(modEventBus);
        // Register Block Entities
        BLOCK_ENTITIES.register(modEventBus);
        // Register Menus
        MENUS.register(modEventBus);

        // Register ourselves for server and other game events we are interested in.
        // Note that this is necessary if and only if we want *this* class (Eduzscl) to
        // respond directly to events.
        // Do not add this line if there are no @SubscribeEvent-annotated functions in
        // this class, like onServerStarting() below.
        NeoForge.EVENT_BUS.register(this);

        // Register the item to a creative tab
        modEventBus.addListener(this::addCreative);

        // Register our mod's ModConfigSpec so that FML can create and load the config
        // file for us
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        // Some common setup code
        LOGGER.info("HELLO FROM COMMON SETUP");

        if (Config.logDirtBlock)
            LOGGER.info("DIRT BLOCK >> {}", BuiltInRegistries.BLOCK.getKey(Blocks.DIRT));

        LOGGER.info(Config.magicNumberIntroduction + Config.magicNumber);

        Config.items.forEach((item) -> LOGGER.info("ITEM >> {}", item.toString()));
    }

    // Add the example block item to the building blocks tab
    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS)
            event.accept(EXAMPLE_BLOCK_ITEM);
    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        // Do something when the server starts
        LOGGER.info("HELLO from server starting");

        // Register JS command
        CommandHandler.register(event.getServer().getCommands());
    }

    @SubscribeEvent
    public void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            TeachingAgent.onPlayerJoin(player);
        }
    }

    @SubscribeEvent
    public void onServerChat(ServerChatEvent event) {
        String message = event.getMessage().getString();
        LOGGER.info("Chat message: {}", message);

        if (message.startsWith("/js ") || message.equals("/js")) {
            event.setCanceled(true);
            ServerPlayer player = event.getPlayer();
            LOGGER.info("JS command detected from player: {}", player.getName().getString());
            TeachingAgent.handleCommand(player, message);
        }
    }

    // You can use EventBusSubscriber to automatically register all static methods
    // in the class annotated with @SubscribeEvent
    @EventBusSubscriber(modid = MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            LOGGER.info("HELLO FROM CLIENT SETUP");
            LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());
        }

        @SubscribeEvent
        public static void onRegisterMenuScreens(RegisterMenuScreensEvent event) {
            event.<AgentBlockMenu, win.morodirule.eduzscl.teaching.client.AgentBlockScreen>register(
                (MenuType<AgentBlockMenu>) (Object) Eduzscl.AGENT_BLOCK_MENU.get(), 
                (menu, inv, title) -> new win.morodirule.eduzscl.teaching.client.AgentBlockScreen(menu, inv, title));
        }
    }
}
