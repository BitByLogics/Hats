package net.bitbylogic.hats.listener;

import net.bitbylogic.hats.Hats;
import net.bitbylogic.hats.data.Hat;
import net.bitbylogic.hats.manager.HatManager;
import net.bitbylogic.hats.util.Utils;
import net.md_5.bungee.api.chat.TranslatableComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.damage.DamageSource;
import org.bukkit.enchantments.EnchantmentTarget;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.SpawnerSpawnEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

public class HatsListener implements Listener {

    private final Hats plugin;
    private final HatManager hatManager;
    private final Random random;
    private final List<Player> anvilViewers;

    public HatsListener(Hats plugin) {
        this.plugin = plugin;
        this.hatManager = plugin.getHatManager();

        this.random = new Random();
        this.anvilViewers = new ArrayList<>();

        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            Iterator<Player> playerIterator = anvilViewers.iterator();

            while (playerIterator.hasNext()) {
                Player player = playerIterator.next();

                if (player == null || player.getOpenInventory().getTopInventory().getType() != InventoryType.ANVIL) {
                    playerIterator.remove();
                    continue;
                }

                player.updateInventory();
            }
        }, 20, 0);
    }

    @EventHandler
    public void onAnvilOpen(InventoryOpenEvent event) {
        if (event.getInventory().getType() != InventoryType.ANVIL && event.getInventory().getType() != InventoryType.GRINDSTONE) {
            return;
        }

        anvilViewers.add((Player) event.getPlayer());
    }

    @SuppressWarnings("UnstableApiUsage")
    @EventHandler
    public void onCombine(PrepareAnvilEvent event) {
        AnvilInventory inventory = event.getInventory();
        ItemStack[] contents = inventory.getContents();

        if (contents[0] == null || contents[1] == null) {
            return;
        }

        ItemStack firstItem = contents[0];
        ItemStack secondItem = contents[1];

        if (!EnchantmentTarget.ARMOR_HEAD.includes(firstItem) && !EnchantmentTarget.ARMOR_HEAD.includes(secondItem)) {
            return;
        }

        if (!hatManager.isHatItem(firstItem) && !hatManager.isHatItem(secondItem) || hatManager.isHatItem(firstItem) && hatManager.isHatItem(secondItem)) {
            return;
        }

        ItemStack armorItem = EnchantmentTarget.ARMOR_HEAD.includes(firstItem) ? firstItem : secondItem;

        if (armorItem.getType() == Material.TURTLE_HELMET) {
            return;
        }

        ItemStack hatItem = hatManager.isHatItem(firstItem) ? firstItem : secondItem;
        Optional<Hat> optionalHat = hatManager.getHatByItem(hatItem);

        if (hatItem.getAmount() > 1) {
            return;
        }

        optionalHat.ifPresent(hat -> {
            ItemStack combinedItem = hatItem.clone();
            ItemMeta meta = combinedItem.getItemMeta();
            ItemMeta armorMeta = armorItem.getItemMeta();

            if (meta == null || armorMeta == null) {
                return;
            }

            List<String> newLore = new ArrayList<>();

            plugin.getConfig().getStringList("Combine-Settings.Lore").forEach(loreLine -> {
                if (loreLine.equalsIgnoreCase("%hat-lore%")) {
                    newLore.addAll(meta.getLore() == null ? new ArrayList<>() : meta.getLore());
                    return;
                }

                newLore.add(Utils.color(loreLine.replace("%armor-type%", ChatColor.stripColor(new TranslatableComponent(armorItem.getTranslationKey()).toLegacyText()))));
            });

            String serializedItem = Utils.itemStackToBase64(armorItem);

            if (serializedItem != null) {
                meta.getPersistentDataContainer().set(plugin.getCombinedItemKey(), PersistentDataType.STRING, serializedItem);
            }

            armorItem.getType().getDefaultAttributeModifiers(EquipmentSlot.HEAD).forEach((attribute, attributeModifier) -> {
                meta.removeAttributeModifier(attribute);
                meta.addAttributeModifier(attribute, new AttributeModifier(new NamespacedKey(plugin, attribute.name()), attributeModifier.getAmount(), attributeModifier.getOperation(), EquipmentSlotGroup.HEAD));
            });

            meta.setLore(newLore.isEmpty() ? meta.getLore() : newLore);
            meta.setEnchantmentGlintOverride(false);
            armorMeta.getEnchants().forEach((enchantment, level) -> meta.addEnchant(enchantment, level, true));
            combinedItem.setItemMeta(meta);
            event.setResult(combinedItem);

            Bukkit.getScheduler().runTask(plugin, () -> {
                inventory.setRepairCost(hat.getCombineXPCost());
                inventory.setRepairCostAmount(1);
                inventory.setMaximumRepairCost(hat.getCombineXPCost() + 1);
            });
        });
    }

    @EventHandler
    public void onSpawn(SpawnerSpawnEvent event) {
        event.getEntity().getPersistentDataContainer().set(plugin.getUnlootableKey(), PersistentDataType.BYTE, (byte) 0);
    }

    @SuppressWarnings("UnstableApiUsage")
    @EventHandler
    public void onDeath(EntityDeathEvent event) {
        Entity entity = event.getEntity();
        DamageSource cause = event.getDamageSource();

        if (cause.getCausingEntity() == null || cause.getCausingEntity().getType() != EntityType.PLAYER
                || entity.getPersistentDataContainer().has(plugin.getUnlootableKey())) {
            return;
        }

        EntityType entityType = entity.getType();

        hatManager.getHatsByEntityType(entityType).forEach(hat -> {
            if (random.nextDouble(101) > hat.getDropChances().get(entityType)) {
                return;
            }

            event.getDrops().add(hat.getItem());
        });
    }

}
