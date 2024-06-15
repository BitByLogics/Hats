package net.bitbylogic.hats.data;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;

@Getter
@RequiredArgsConstructor
public class Hat {

    private final String id;
    private final int combineXPCost;
    private final double wanderingTraderChance;
    private final ItemStack item;
    private final HashMap<EntityType, Double> dropChances;

    public ItemStack getItem() {
        return item.clone();
    }

}
