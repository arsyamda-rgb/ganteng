package com.veyrion.hud;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

/** Simpan coin, ras, kingdom, dan setelan HUD per player di plugins/VeyrionHud/data.yml */
public final class DataStore {

    private final VeyrionHud plugin;
    private final File file;

    private final Map<UUID, Long> coins = new HashMap<>();
    private final Map<UUID, Race> races = new HashMap<>();
    /** uuid -> id kingdom (lowercase) */
    private final Map<UUID, String> kingdomOf = new HashMap<>();
    /** id kingdom (lowercase) -> nama tampil */
    private final Map<String, String> kingdoms = new LinkedHashMap<>();

    private final Map<UUID, Integer> guiWidth = new HashMap<>();
    /** 0 = auto, 1-4 = GUI scale pilihan player */
    private final Map<UUID, Integer> guiScale = new HashMap<>();
    private final Map<UUID, Integer> winW = new HashMap<>();
    private final Map<UUID, Integer> winH = new HashMap<>();
    private final Map<UUID, Integer> shift = new HashMap<>();
    private final Set<UUID> hudOff = new HashSet<>();

    public DataStore(VeyrionHud plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "data.yml");
    }

    public void load() {
        coins.clear();
        races.clear();
        kingdomOf.clear();
        kingdoms.clear();
        guiWidth.clear();
        guiScale.clear();
        winW.clear();
        winH.clear();
        shift.clear();
        hudOff.clear();
        if (!file.exists()) return;

        YamlConfiguration y = YamlConfiguration.loadConfiguration(file);

        ConfigurationSection ks = y.getConfigurationSection("kingdoms");
        if (ks != null) {
            for (String key : ks.getKeys(false)) {
                kingdoms.put(key.toLowerCase(Locale.ROOT), ks.getString(key, key));
            }
        }

        ConfigurationSection ps = y.getConfigurationSection("players");
        if (ps != null) {
            for (String key : ps.getKeys(false)) {
                UUID uuid;
                try {
                    uuid = UUID.fromString(key);
                } catch (IllegalArgumentException e) {
                    continue;
                }
                if (ps.contains(key + ".coin")) coins.put(uuid, ps.getLong(key + ".coin", 0L));
                Race race = Race.from(ps.getString(key + ".race"));
                if (race != null) races.put(uuid, race);
                String kd = ps.getString(key + ".kingdom");
                if (kd != null && kingdoms.containsKey(kd.toLowerCase(Locale.ROOT))) {
                    kingdomOf.put(uuid, kd.toLowerCase(Locale.ROOT));
                }
                if (ps.contains(key + ".gui-width")) guiWidth.put(uuid, ps.getInt(key + ".gui-width"));
                if (ps.contains(key + ".gui-scale")) guiScale.put(uuid, ps.getInt(key + ".gui-scale"));
                if (ps.contains(key + ".win-w")) winW.put(uuid, ps.getInt(key + ".win-w"));
                if (ps.contains(key + ".win-h")) winH.put(uuid, ps.getInt(key + ".win-h"));
                if (ps.contains(key + ".shift")) shift.put(uuid, ps.getInt(key + ".shift"));
                if (ps.getBoolean(key + ".hud-off", false)) hudOff.add(uuid);
            }
        }
    }

    public void save() {
        YamlConfiguration y = new YamlConfiguration();
        for (Map.Entry<String, String> e : kingdoms.entrySet()) {
            y.set("kingdoms." + e.getKey(), e.getValue());
        }
        Set<UUID> all = new HashSet<>();
        all.addAll(coins.keySet());
        all.addAll(races.keySet());
        all.addAll(kingdomOf.keySet());
        all.addAll(guiWidth.keySet());
        all.addAll(guiScale.keySet());
        all.addAll(winW.keySet());
        all.addAll(winH.keySet());
        all.addAll(shift.keySet());
        all.addAll(hudOff);
        for (UUID u : all) {
            String base = "players." + u;
            if (coins.containsKey(u)) y.set(base + ".coin", coins.get(u));
            if (races.containsKey(u)) y.set(base + ".race", races.get(u).id());
            if (kingdomOf.containsKey(u)) y.set(base + ".kingdom", kingdomOf.get(u));
            if (guiWidth.containsKey(u)) y.set(base + ".gui-width", guiWidth.get(u));
            if (guiScale.containsKey(u)) y.set(base + ".gui-scale", guiScale.get(u));
            if (winW.containsKey(u)) y.set(base + ".win-w", winW.get(u));
            if (winH.containsKey(u)) y.set(base + ".win-h", winH.get(u));
            if (shift.containsKey(u)) y.set(base + ".shift", shift.get(u));
            if (hudOff.contains(u)) y.set(base + ".hud-off", true);
        }
        try {
            plugin.getDataFolder().mkdirs();
            y.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Gagal simpan data.yml: " + e.getMessage());
        }
    }

    // ---------- coin ----------

    public long getCoin(UUID u) {
        return coins.getOrDefault(u, plugin.getConfig().getLong("starting-coin", 0L));
    }

    public void setCoin(UUID u, long amount) {
        coins.put(u, Math.max(0L, amount));
    }

    public void addCoin(UUID u, long delta) {
        setCoin(u, getCoin(u) + delta);
    }

    // ---------- race ----------

    public Race getRace(UUID u) {
        return races.get(u);
    }

    public void setRace(UUID u, Race race) {
        if (race == null) races.remove(u);
        else races.put(u, race);
    }

    // ---------- kingdom ----------

    /** Nama tampil kingdom player, atau null kalau tidak punya. */
    public String getKingdom(UUID u) {
        String id = kingdomOf.get(u);
        return id == null ? null : kingdoms.get(id);
    }

    public boolean kingdomExists(String name) {
        return kingdoms.containsKey(name.toLowerCase(Locale.ROOT));
    }

    public String kingdomDisplay(String name) {
        return kingdoms.get(name.toLowerCase(Locale.ROOT));
    }

    public boolean addKingdom(String name) {
        String id = name.toLowerCase(Locale.ROOT);
        if (kingdoms.containsKey(id)) return false;
        kingdoms.put(id, name);
        return true;
    }

    public boolean removeKingdom(String name) {
        String id = name.toLowerCase(Locale.ROOT);
        if (kingdoms.remove(id) == null) return false;
        kingdomOf.values().removeIf(id::equals);
        return true;
    }

    /** name null = keluar dari kingdom. Return false kalau kingdom tidak ada. */
    public boolean setKingdom(UUID u, String name) {
        if (name == null) {
            kingdomOf.remove(u);
            return true;
        }
        String id = name.toLowerCase(Locale.ROOT);
        if (!kingdoms.containsKey(id)) return false;
        kingdomOf.put(u, id);
        return true;
    }

    public Collection<String> kingdomNames() {
        return new ArrayList<>(kingdoms.values());
    }

    public int memberCount(String name) {
        String id = name.toLowerCase(Locale.ROOT);
        int n = 0;
        for (String v : kingdomOf.values()) {
            if (v.equals(id)) n++;
        }
        return n;
    }

    // ---------- setelan HUD per player ----------

    /** Lebar layar dalam GUI pixel: angka manual > (scale + ukuran window) > default config. */
    public int getGuiWidth(UUID u) {
        Integer manual = guiWidth.get(u);
        if (manual != null) return manual;
        Integer scale = guiScale.get(u);
        if (scale != null) {
            int w = windowWidth(u);
            int s = scale > 0 ? scale : autoScale(w, windowHeight(u));
            return (int) Math.ceil(w / (double) s);
        }
        return plugin.getConfig().getInt("gui-width", 700);
    }

    /** Rumus "Auto" bawaan Minecraft: scale terbesar yang masih muat 320x240 GUI pixel. */
    public static int autoScale(int w, int h) {
        int s = 1;
        while (s < 32 && w / (s + 1) >= 320 && h / (s + 1) >= 240) s++;
        return s;
    }

    public void setGuiWidth(UUID u, int px) {
        guiWidth.put(u, px);
    }

    /** 0 = auto. Menghapus angka manual. */
    public void setGuiScale(UUID u, int scale) {
        guiScale.put(u, scale);
        guiWidth.remove(u);
    }

    public void setWindow(UUID u, int w, int h) {
        winW.put(u, w);
        winH.put(u, h);
    }

    public int windowWidth(UUID u) {
        return winW.getOrDefault(u, 1920);
    }

    public int windowHeight(UUID u) {
        return winH.getOrDefault(u, 1080);
    }

    public boolean hasGuiSetting(UUID u) {
        return guiWidth.containsKey(u) || guiScale.containsKey(u);
    }

    public int getShift(UUID u) {
        return shift.getOrDefault(u, 0);
    }

    public void setShift(UUID u, int px) {
        shift.put(u, px);
    }

    public boolean isHudOff(UUID u) {
        return hudOff.contains(u);
    }

    public void setHudOff(UUID u, boolean off) {
        if (off) hudOff.add(u);
        else hudOff.remove(u);
    }
}
