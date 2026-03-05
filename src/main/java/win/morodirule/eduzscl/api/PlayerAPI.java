package win.morodirule.eduzscl.api;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.resources.ResourceLocation;
import win.morodirule.eduzscl.teaching.TeachingAgent;

import java.util.HashMap;
import java.util.Map;

public class PlayerAPI {
    
    public Map<String, Double> getPosition() {
        ServerPlayer player = TeachingAgent.getCurrentPlayer();
        if (player == null) return null;
        
        Map<String, Double> pos = new HashMap<>();
        pos.put("x", player.getX());
        pos.put("y", player.getY());
        pos.put("z", player.getZ());
        
        return pos;
    }
    
    public Object[] getInventory() {
        ServerPlayer player = TeachingAgent.getCurrentPlayer();
        if (player == null) return new Object[0];
        
        int size = player.getInventory().getContainerSize();
        Object[] items = new Object[size];
        
        for (int i = 0; i < size; i++) {
            var itemStack = player.getInventory().getItem(i);
            if (!itemStack.isEmpty()) {
                Map<String, Object> itemData = new HashMap<>();
                Item item = itemStack.getItem();
                ResourceLocation key = BuiltInRegistries.ITEM.getKey(item);
                itemData.put("id", key.toString());
                itemData.put("count", itemStack.getCount());
                items[i] = itemData;
            }
        }
        
        return items;
    }
    
    public String getFacing() {
        ServerPlayer player = TeachingAgent.getCurrentPlayer();
        if (player == null) return "unknown";
        
        return player.getDirection().getName();
    }
    
    public String getName() {
        ServerPlayer player = TeachingAgent.getCurrentPlayer();
        if (player == null) return "unknown";
        
        return player.getName().getString();
    }
}
