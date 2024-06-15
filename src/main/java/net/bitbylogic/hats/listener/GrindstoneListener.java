package net.bitbylogic.hats.listener;

import net.bitbylogic.hats.Hats;
import net.bitbylogic.hats.manager.HatManager;
import net.bitbylogic.hats.util.Utils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public class GrindstoneListener implements Listener {

    private final Hats plugin;
    private final HatManager hatManager;

    public GrindstoneListener(Hats plugin) {
        this.plugin = plugin;
        this.hatManager = plugin.getHatManager();
    }

    @EventHandler
    public void handleAddition(InventoryClickEvent event) {
        Inventory inventory = event.getClickedInventory();

        if (inventory == null || inventory.getType() != InventoryType.GRINDSTONE || event.getSlot() == 2) {
            return;
        }

        ItemStack currentItem = event.getCursor();

        if (!(hatManager.isHatItem(inventory.getContents()[0]) || hatManager.isHatItem(inventory.getContents()[1])) && !hatManager.isHatItem(currentItem)) {
            return;
        }

        if (hatManager.isHatItem(currentItem)) {
            event.setCancelled(true);

            if (!currentItem.getItemMeta().getPersistentDataContainer().has(plugin.getCombinedItemKey())) {
                return;
            }
        }

        if (currentItem != null && currentItem.getType() != Material.AIR) {
            ItemStack firstItem = inventory.getItem(0);
            ItemStack secondItem = inventory.getItem(1);

            inventory.setItem(0, null);
            inventory.setItem(1, null);

            if (firstItem != null) {
                event.getWhoClicked().getInventory().addItem(firstItem);
            }

            if (secondItem != null) {
                event.getWhoClicked().getInventory().addItem(secondItem);
            }
        }

        Bukkit.getScheduler().runTask(plugin, () -> {
            hatManager.getHatByItem(currentItem).ifPresent(hat -> {
                event.getWhoClicked().setItemOnCursor(null);
                inventory.setItem(event.getSlot(), currentItem);

                ItemStack newHatItem = hat.getItem();
                newHatItem.setAmount(currentItem.getAmount());
                inventory.setItem(2, newHatItem);
            });
        });
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        Inventory inventory = event.getView().getTopInventory();

        if (inventory.getType() != InventoryType.GRINDSTONE) {
            return;
        }

        ItemStack currentItem = event.getCursor();

        if (!hatManager.isHatItem(currentItem)) {
            return;
        }

        event.setCancelled(true);
    }

    @EventHandler
    public void onSeparateConfirm(InventoryClickEvent event) {
        if (!plugin.getConfig().getBoolean("Combine-Settings.Return-On-Separate")) {
            return;
        }

        Inventory inventory = event.getClickedInventory();

        if (inventory == null || inventory.getType() != InventoryType.GRINDSTONE || event.getSlot() != 2) {
            return;
        }

        if (!hatManager.isHatItem(inventory.getItem(event.getSlot()))) {
            return;
        }

        ItemStack hatItem = inventory.getItem(0) == null ? inventory.getItem(1) : inventory.getItem(0);

        if (hatItem == null || hatItem.getItemMeta() == null) {
            return;
        }

        ItemMeta meta = hatItem.getItemMeta();
        PersistentDataContainer dataContainer = meta.getPersistentDataContainer();

        if (!dataContainer.has(plugin.getCombinedItemKey())) {
            return;
        }

        ItemStack deserializedItem = Utils.itemStackFromBase64(dataContainer.get(plugin.getCombinedItemKey(), PersistentDataType.STRING));

        if (deserializedItem == null) {
            return;
        }

        event.setCancelled(true);

        hatManager.getHatByItem(hatItem).ifPresent(hat -> {
            inventory.clear();

            ItemStack newHat = hat.getItem();
            newHat.setAmount(hatItem.getAmount());
            deserializedItem.setAmount(hatItem.getAmount());

            HumanEntity clicker = event.getWhoClicked();
            ((Player) clicker).playSound(clicker.getLocation(), Sound.BLOCK_GRINDSTONE_USE, 1f, 1f);
            clicker.getInventory().addItem(deserializedItem, newHat).forEach((integer, itemStack) -> clicker.getWorld().dropItemNaturally(clicker.getLocation(), itemStack));
        });
    }

}
