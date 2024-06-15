package net.bitbylogic.hats.manager;

import lombok.Getter;
import net.bitbylogic.hats.Hats;
import net.bitbylogic.hats.data.Hat;
import net.bitbylogic.hats.util.Utils;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class HatManager {

    private final Hats plugin;

    @Getter
    private final TreeMap<String, Hat> loadedHats = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

    public HatManager(Hats plugin) {
        this.plugin = plugin;

        loadHats();
    }

    public void loadHats() {
        loadedHats.clear();

        ConfigurationSection hatsSection = plugin.getConfig().getConfigurationSection("Hats");

        if (hatsSection == null) {
            plugin.getLogger().log(Level.WARNING, "Hats section missing, no hats will be loaded.");
            return;
        }

        for (String hatId : hatsSection.getKeys(false)) {
            ConfigurationSection hatSection = hatsSection.getConfigurationSection(hatId);

            if (hatSection == null) {
                continue;
            }

            int combineXPCost = hatSection.getInt("Combine-Level-Cost", 5);
            double wanderingTraderChance = hatSection.getDouble("Wandering-Trader-Chance", 0.17);
            ConfigurationSection itemSection = hatSection.getConfigurationSection("Item");
            ItemStack hatItem = itemSection == null ? null : Utils.getItemStackFromConfig(itemSection);
            HashMap<EntityType, Double> dropChances = new HashMap<>();

            hatSection.getStringList("Drop-Chances").forEach(dropData -> {
                String[] splitData = dropData.split(":");

                dropChances.put(EntityType.valueOf(splitData[0].toUpperCase()), Double.parseDouble(splitData[1]));
            });

            if (hatItem != null) {
                ItemMeta meta = hatItem.getItemMeta();

                if (meta != null) {
                    meta.getPersistentDataContainer().set(plugin.getHatKey(), PersistentDataType.STRING, hatId);
                    hatItem.setItemMeta(meta);
                }
            }

            loadedHats.put(hatId, new Hat(hatId, combineXPCost, wanderingTraderChance, hatItem, dropChances));
        }

        plugin.getLogger().log(Level.INFO, "Successfully loaded {0} hat(s)!", loadedHats.size());
    }

    public boolean isHatItem(ItemStack item) {
        if (item == null) {
            return false;
        }

        ItemMeta meta = item.getItemMeta();

        if (meta == null) {
            return false;
        }

        return meta.getPersistentDataContainer().has(plugin.getHatKey());
    }

    public Optional<Hat> getHatByItem(ItemStack item) {
        if (!isHatItem(item)) {
            return Optional.empty();
        }

        ItemMeta meta = item.getItemMeta();

        if (meta == null) {
            return Optional.empty();
        }

        return Optional.ofNullable(loadedHats.get(meta.getPersistentDataContainer().get(plugin.getHatKey(), PersistentDataType.STRING)));
    }

    public Optional<Hat> getHatByID(String id) {
        return Optional.ofNullable(loadedHats.get(id));
    }

    public List<Hat> getHatsByEntityType(EntityType entityType) {
        return loadedHats.values().stream().filter(hat -> hat.getDropChances().containsKey(entityType)).collect(Collectors.toList());
    }

}
