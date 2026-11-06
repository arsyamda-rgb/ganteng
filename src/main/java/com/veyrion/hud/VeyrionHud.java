package com.veyrion.hud;

import ch.njol.skript.Skript;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.util.List;

public final class VeyrionHud extends JavaPlugin implements Listener {

    private DataStore store;
    private HudManager hud;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        store = new DataStore(this);
        store.load();
        hud = new HudManager(this);
        hud.start();

        Commands commands = new Commands(this);
        for (String name : List.of("coin", "race", "kd", "hud", "veyrionhud")) {
            PluginCommand pc = getCommand(name);
            if (pc != null) {
                pc.setExecutor(commands);
                pc.setTabCompleter(commands);
            }
        }

        getServer().getPluginManager().registerEvents(this, this);

        try {
            Skript.registerAddon(this).loadClasses("com.veyrion.hud.skript", "elements");
        } catch (IOException e) {
            getLogger().severe("Gagal memuat syntax Skript: " + e.getMessage());
        }

        // autosave tiap 5 menit
        Bukkit.getScheduler().runTaskTimer(this, store::save, 6000L, 6000L);
    }

    @Override
    public void onDisable() {
        if (hud != null) hud.stop();
        if (store != null) store.save();
    }

    public void reloadAll() {
        reloadConfig();
        hud.reload();
    }

    public DataStore store() {
        return store;
    }

    public HudManager hud() {
        return hud;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        Bukkit.getScheduler().runTaskLater(this, () -> {
            if (!p.isOnline()) return;
            hud.update(p);
            if (!store.hasGuiSetting(p.getUniqueId())) {
                p.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(
                        "&f[Veyrion] &7HUD tidak pas di layarmu? Atur: &f/hud scale <1-4|auto> &7(sesuai GUI Scale di Video Settings)."));
            }
        }, 20L);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        hud.remove(e.getPlayer());
    }
}
