package net.bitbylogic.hats.util;

import com.google.common.collect.Lists;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public class Utils {

    /**
     * Create an ItemStack object from a configuration
     * section.
     *
     * @param section The configuration section.
     * @return New ItemStack instance.
     */
    // Borrowed from https://github.com/BitByLogics/APIByLogic
    public static ItemStack getItemStackFromConfig(ConfigurationSection section) {
        int amount = section.getInt("Amount", 1);
        ItemStack stack = new ItemStack(Material.valueOf(section.getString("Material", "BARRIER")), amount);
        ItemMeta meta = stack.getItemMeta();

        if (meta == null) {
            return null;
        }

        // Define the items name
        if (section.getString("Name") != null) {
            meta.setDisplayName(color(section.getString("Name")));
        }

        List<String> lore = Lists.newArrayList();

        // Define the items lore
        section.getStringList("Lore").forEach(string ->
                lore.add(color(string)));

        meta.setLore(lore);

        // Add flags to hide potion effects/attributes
        section.getStringList("Flags").forEach(flag -> {
            meta.addItemFlags(ItemFlag.valueOf(flag.toUpperCase()));
        });

        // Make the item glow
        if (section.getBoolean("Glow")) {
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }

        // If leather armor, apply dye color if defined
        if (stack.getType().name().startsWith("LEATHER_") && section.getString("Dye-Color") != null) {
            LeatherArmorMeta leatherArmorMeta = (LeatherArmorMeta) stack.getItemMeta();
            java.awt.Color color = net.md_5.bungee.api.ChatColor.of(section.getString("Dye-Color")).getColor();
            leatherArmorMeta.setColor(Color.fromRGB(color.getRed(), color.getGreen(), color.getBlue()));
            stack.setItemMeta(leatherArmorMeta);
        }

        stack.setItemMeta(meta);

        // If the item is a potion, apply potion data
        if (stack.getType() == Material.SPLASH_POTION || stack.getType() == Material.POTION) {
            ConfigurationSection potionSection = section.getConfigurationSection("Potion-Data");

            if (potionSection != null) {
                boolean vanilla = potionSection.getBoolean("Vanilla", false);
                PotionMeta potionMeta = (PotionMeta) stack.getItemMeta();
                String potionType = potionSection.getString("Type", "POISON");

                if (vanilla) {
                    potionMeta.setBasePotionType(PotionType.valueOf(potionSection.getString("Vanilla-Type", "WATER")));
                } else {
                    potionMeta.addCustomEffect(new PotionEffect(PotionEffectType.getByName(potionType), potionSection.getInt("Duration", 20), potionSection.getInt("Amplifier", 1) - 1), true);
                }

                stack.setItemMeta(potionMeta);
            }
        }

        if (stack.getType() == Material.TIPPED_ARROW) {
            ConfigurationSection potionSection = section.getConfigurationSection("Potion-Data");

            if (potionSection != null) {
                boolean vanilla = potionSection.getBoolean("Vanilla", false);
                PotionMeta potionMeta = (PotionMeta) stack.getItemMeta();
                String potionType = potionSection.getString("Type", "POISON");

                if (vanilla) {
                    potionMeta.setBasePotionType(PotionType.valueOf(potionSection.getString("Vanilla-Type", "WATER")));
                } else {
                    potionMeta.addCustomEffect(new PotionEffect(PotionEffectType.getByName(potionType), potionSection.getInt("Duration", 20), potionSection.getInt("Amplifier", 1) - 1), true);
                }

                stack.setItemMeta(potionMeta);
            }
        }

        // If the item is a player head, apply skin
        if (section.getString("Skull-Name") != null && stack.getType() == Material.PLAYER_HEAD) {
            SkullMeta skullMeta = (SkullMeta) stack.getItemMeta();
            skullMeta.setOwner(color(section.getString("Skull-Name", "Notch")));
            stack.setItemMeta(skullMeta);
        }

        // Used for resourcepacks, to display custom models
        if (section.getInt("Model-Data") != 0) {
            ItemMeta updatedMeta = stack.getItemMeta();
            updatedMeta.setCustomModelData(section.getInt("Model-Data"));
            stack.setItemMeta(updatedMeta);
        }

        ItemMeta updatedMeta = stack.getItemMeta();

        // Apply enchantments
        section.getStringList("Enchantments").forEach(enchant -> {
            String[] data = enchant.split(":");
            NamespacedKey key = NamespacedKey.minecraft(data[0].trim());
            Enchantment enchantment = Enchantment.getByKey(key);
            int level = 0;

            if (isNumber(data[1])) {
                level = Integer.parseInt(data[1]);
            }

            if (enchantment == null) {
                Bukkit.getLogger().warning(String.format("[APIByLogic] (ItemStackUtil): Skipped enchantment '%s', invalid enchant.", enchant));
                return;
            }

            updatedMeta.addEnchant(enchantment, level, true);
        });

        stack.setItemMeta(updatedMeta);

        return stack;
    }

    public static String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    /**
     * Check if a string is a number.
     *
     * @param string The string to check.
     * @return Whether the specified string is a number.
     */
    private static boolean isNumber(String string) {
        if (string == null) {
            return false;
        }

        int length = string.length();
        if (length == 0) {
            return false;
        }

        int i = 0;
        if (string.charAt(0) == '-') {
            if (length == 1) {
                return false;
            }
            i = 1;
        }

        for (; i < length; i++) {
            char c = string.charAt(i);
            if (c < '0' || c > '9') {
                return false;
            }
        }

        return true;
    }

    // Borrowed from https://github.com/BitByLogics/APIByLogic
    public static String itemStackToBase64(ItemStack item) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream);

            dataOutput.writeObject(item.serialize());
            dataOutput.close();

            return Base64Coder.encodeLines(outputStream.toByteArray());
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    // Borrowed from https://github.com/BitByLogics/APIByLogic
    public static ItemStack itemStackFromBase64(String data) {
        try {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64Coder.decodeLines(data));
            BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream);
            ItemStack itemStack = ItemStack.deserialize((Map<String, Object>) dataInput.readObject());

            dataInput.close();
            return itemStack;
        } catch (ClassNotFoundException | IOException e) {
            e.printStackTrace();
        }

        return null;
    }

}
