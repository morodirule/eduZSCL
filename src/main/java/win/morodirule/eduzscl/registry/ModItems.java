package win.morodirule.eduzscl.registry;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.food.FoodProperties;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import win.morodirule.eduzscl.Eduzscl;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Eduzscl.MODID);

    public static final DeferredItem<BlockItem> EXAMPLE_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("example_block",
            ModBlocks.EXAMPLE_BLOCK);

    public static final DeferredItem<BlockItem> AGENT_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("agent_block",
            ModBlocks.AGENT_BLOCK);

    public static final DeferredItem<BlockItem> COMPLETION_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("completion_block",
            ModBlocks.COMPLETION_BLOCK);

    public static final DeferredItem<Item> EXAMPLE_ITEM = ITEMS.registerSimpleItem("example_item", new Item.Properties()
            .food(new FoodProperties.Builder().alwaysEdible().nutrition(1).saturationModifier(2f).build()));

    public static final DeferredItem<Item> CONNECTOR_ITEM = ITEMS.registerItem("connector", properties -> new win.morodirule.eduzscl.item.ConnectorItem(properties));
}
