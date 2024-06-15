package net.bitbylogic.hats;

import lombok.Getter;
import net.bitbylogic.hats.command.HatsCommand;
import net.bitbylogic.hats.listener.GrindstoneListener;
import net.bitbylogic.hats.listener.HatsListener;
import net.bitbylogic.hats.manager.HatManager;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;

@Getter
public class Hats extends JavaPlugin {

    private NamespacedKey hatKey;
    private NamespacedKey unlootableKey;
    private NamespacedKey combinedItemKey;

    private HatManager hatManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.hatKey = new NamespacedKey(this, "hat_id");
        this.unlootableKey = new NamespacedKey(this, "unlootable");
        this.combinedItemKey = new NamespacedKey(this, "combined_item");

        this.hatManager = new HatManager(this);

        getCommand("hats").setExecutor(new HatsCommand(this));

        getServer().getPluginManager().registerEvents(new HatsListener(this), this);
        getServer().getPluginManager().registerEvents(new GrindstoneListener(this), this);
    }

    @Override
    public void onDisable() {

    }

}
