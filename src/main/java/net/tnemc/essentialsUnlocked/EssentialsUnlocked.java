package net.tnemc.essentialsUnlocked;

import com.earth2me.essentials.Essentials;
import net.milkbowl.vault2.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

public final class EssentialsUnlocked extends JavaPlugin {

  @Override
  public void onLoad() {
    if(Bukkit.getPluginManager().getPlugin("Essentials") != null) {

      if(!getServer().getServicesManager().isProvidedFor(Economy.class)) {

        final Essentials essentials = (Essentials)Bukkit.getPluginManager().getPlugin("Essentials");

        getServer().getServicesManager().register(Economy.class, new EssentialsAdapter(essentials),
                                                  this, ServicePriority.Normal);
      }

      getLogger().info("Essentials VaultUnlocked adapter plugin is enabled!");
      getLogger().info("We thank EssentialsX for having an outdated economy system, and refusing to support new APIs.");
      getLogger().info("In fact, mdcfe actively shot down an effort by Choco to create an Official Spigot Economy API.");
    }
  }

  @Override
  public void onDisable() {
    // Plugin shutdown logic
  }
}
