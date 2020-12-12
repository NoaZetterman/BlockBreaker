package blockbreaker.bedrockbreaker;

import org.bukkit.plugin.java.JavaPlugin;


public final class BedrockBreaker extends JavaPlugin {




    @Override
    public void onEnable() {
        this.saveDefaultConfig();
        
        getServer().getPluginManager().registerEvents(new BlockEvents(this), this);

    }

    @Override
    public void onDisable() {
        // Nothing here I believe?
    }
}
