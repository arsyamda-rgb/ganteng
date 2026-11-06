package com.veyrion.hud;

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

public final class Commands implements TabExecutor {

    private static final String ADMIN = "veyrionhud.admin";
    private static final String KD_ADD = "veyrionhud.kingdom.add";
    private static final Pattern KD_NAME = Pattern.compile("[A-Za-z0-9_-]{2,16}");

    private final VeyrionHud plugin;
    private final DataStore store;

    public Commands(VeyrionHud plugin) {
        this.plugin = plugin;
        this.store = plugin.store();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        switch (cmd.getName().toLowerCase(Locale.ROOT)) {
            case "coin" -> coin(sender, args);
            case "race" -> race(sender, args);
            case "kd" -> kd(sender, args);
            case "hud" -> hud(sender, args);
            default -> reload(sender, args);
        }
        return true;
    }

    // ================= /coin =================

    private void coin(CommandSender sender, String[] args) {
        if (args.length == 0) {
            if (!(sender instanceof Player p)) {
                msg(sender, "Pakai: /coin <player>");
                return;
            }
            msg(sender, "Coin kamu: &f" + fmt(store.getCoin(p.getUniqueId())));
            return;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "pay" -> {
                if (!(sender instanceof Player p)) {
                    msg(sender, "Hanya player yang bisa kirim coin.");
                    return;
                }
                if (args.length < 3) {
                    msg(sender, "Pakai: /coin pay <player> <jumlah>");
                    return;
                }
                Player target = Bukkit.getPlayerExact(args[1]);
                Long amount = parseLong(args[2]);
                if (target == null || target.equals(p)) {
                    msg(sender, "Player tujuan tidak valid / tidak online.");
                    return;
                }
                if (amount == null || amount <= 0) {
                    msg(sender, "Jumlah harus angka lebih dari 0.");
                    return;
                }
                if (store.getCoin(p.getUniqueId()) < amount) {
                    msg(sender, "Coin kamu tidak cukup.");
                    return;
                }
                store.addCoin(p.getUniqueId(), -amount);
                store.addCoin(target.getUniqueId(), amount);
                msg(p, "Kirim &f" + fmt(amount) + " &7coin ke &f" + target.getName() + "&7.");
                msg(target, "Terima &f" + fmt(amount) + " &7coin dari &f" + p.getName() + "&7.");
                refresh(p);
                refresh(target);
            }
            case "give", "take", "set" -> {
                if (!sender.hasPermission(ADMIN)) {
                    noPerm(sender);
                    return;
                }
                if (args.length < 3) {
                    msg(sender, "Pakai: /coin " + sub + " <player> <jumlah>");
                    return;
                }
                OfflinePlayer target = find(args[1]);
                Long amount = parseLong(args[2]);
                if (target == null) {
                    msg(sender, "Player tidak ditemukan.");
                    return;
                }
                if (amount == null || amount < 0) {
                    msg(sender, "Jumlah harus angka 0 atau lebih.");
                    return;
                }
                UUID id = target.getUniqueId();
                switch (sub) {
                    case "give" -> store.addCoin(id, amount);
                    case "take" -> store.addCoin(id, -amount);
                    default -> store.setCoin(id, amount);
                }
                msg(sender, "Coin &f" + name(target) + "&7 sekarang &f" + fmt(store.getCoin(id)) + "&7.");
                refresh(target);
            }
            default -> {
                OfflinePlayer target = find(args[0]);
                if (target == null) {
                    msg(sender, "Player tidak ditemukan.");
                    return;
                }
                msg(sender, "Coin &f" + name(target) + "&7: &f" + fmt(store.getCoin(target.getUniqueId())));
            }
        }
    }

    // ================= /race =================

    private void race(CommandSender sender, String[] args) {
        if (args.length == 0) {
            if (sender instanceof Player p) {
                Race r = store.getRace(p.getUniqueId());
                msg(sender, "Ras kamu: &f" + (r == null ? "belum dipilih" : r.display()));
            }
            msg(sender, "Pilihan: &fhuman, beast, elf, dragon &7(/race <ras>)");
            return;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        if (sub.equals("set") || sub.equals("reset")) {
            if (!sender.hasPermission(ADMIN)) {
                noPerm(sender);
                return;
            }
            boolean isSet = sub.equals("set");
            if (args.length < (isSet ? 3 : 2)) {
                msg(sender, isSet ? "Pakai: /race set <player> <ras>" : "Pakai: /race reset <player>");
                return;
            }
            OfflinePlayer target = find(args[1]);
            if (target == null) {
                msg(sender, "Player tidak ditemukan.");
                return;
            }
            Race r = null;
            if (isSet) {
                r = Race.from(args[2]);
                if (r == null) {
                    msg(sender, "Ras tidak ada. Pilihan: human, beast, elf, dragon.");
                    return;
                }
            }
            store.setRace(target.getUniqueId(), r);
            msg(sender, "Ras &f" + name(target) + "&7: &f" + (r == null ? "direset" : r.display()));
            refresh(target);
            return;
        }

        if (!(sender instanceof Player p)) {
            msg(sender, "Pakai: /race set <player> <ras>");
            return;
        }
        Race chosen = Race.from(args[0]);
        if (chosen == null) {
            msg(sender, "Ras tidak ada. Pilihan: human, beast, elf, dragon.");
            return;
        }
        Race current = store.getRace(p.getUniqueId());
        boolean allowChange = plugin.getConfig().getBoolean("allow-self-change-race", false);
        if (current != null && !allowChange && !p.hasPermission(ADMIN)) {
            msg(sender, "Ras sudah dipilih (&f" + current.display() + "&7). Minta admin untuk menggantinya.");
            return;
        }
        store.setRace(p.getUniqueId(), chosen);
        msg(sender, "Ras kamu sekarang: &f" + chosen.display());
        refresh(p);
    }

    // ================= /kd =================

    private void kd(CommandSender sender, String[] args) {
        if (args.length == 0) {
            msg(sender, "Pakai: /kd <add|remove|list|join|leave|info|set>");
            return;
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "add" -> {
                if (!sender.hasPermission(KD_ADD)) {
                    noPerm(sender);
                    return;
                }
                if (args.length < 2) {
                    msg(sender, "Pakai: /kd add <nama>");
                    return;
                }
                if (!KD_NAME.matcher(args[1]).matches()) {
                    msg(sender, "Nama kingdom 2-16 karakter: huruf, angka, _ atau - (tanpa spasi).");
                    return;
                }
                if (!store.addKingdom(args[1])) {
                    msg(sender, "Kingdom &f" + args[1] + "&7 sudah ada.");
                    return;
                }
                msg(sender, "Kingdom &f" + args[1] + "&7 dibuat. Player masuk dengan &f/kd join " + args[1]);
            }
            case "remove" -> {
                if (!sender.hasPermission(KD_ADD)) {
                    noPerm(sender);
                    return;
                }
                if (args.length < 2) {
                    msg(sender, "Pakai: /kd remove <nama>");
                    return;
                }
                if (!store.removeKingdom(args[1])) {
                    msg(sender, "Kingdom tidak ditemukan.");
                    return;
                }
                msg(sender, "Kingdom &f" + args[1] + "&7 dihapus.");
                refreshAll();
            }
            case "list" -> {
                if (store.kingdomNames().isEmpty()) {
                    msg(sender, "Belum ada kingdom.");
                    return;
                }
                msg(sender, "Daftar kingdom:");
                for (String n : store.kingdomNames()) {
                    msg(sender, "&f- " + n + " &7(" + store.memberCount(n) + " anggota)");
                }
            }
            case "join" -> {
                if (!(sender instanceof Player p)) {
                    msg(sender, "Hanya player yang bisa join.");
                    return;
                }
                if (args.length < 2) {
                    msg(sender, "Pakai: /kd join <nama>");
                    return;
                }
                if (!store.kingdomExists(args[1])) {
                    msg(sender, "Kingdom tidak ditemukan. Cek &f/kd list&7.");
                    return;
                }
                if (store.getKingdom(p.getUniqueId()) != null) {
                    msg(sender, "Kamu sudah punya kingdom. Keluar dulu dengan &f/kd leave&7.");
                    return;
                }
                store.setKingdom(p.getUniqueId(), args[1]);
                msg(sender, "Kamu masuk kingdom &f" + store.kingdomDisplay(args[1]) + "&7.");
                refresh(p);
            }
            case "leave" -> {
                if (!(sender instanceof Player p)) {
                    msg(sender, "Hanya player yang bisa keluar.");
                    return;
                }
                if (store.getKingdom(p.getUniqueId()) == null) {
                    msg(sender, "Kamu tidak punya kingdom.");
                    return;
                }
                store.setKingdom(p.getUniqueId(), null);
                msg(sender, "Kamu keluar dari kingdom.");
                refresh(p);
            }
            case "info" -> {
                if (!(sender instanceof Player p)) {
                    msg(sender, "Hanya player yang bisa pakai ini.");
                    return;
                }
                String k = store.getKingdom(p.getUniqueId());
                if (k == null) {
                    msg(sender, "Kamu tidak punya kingdom.");
                    return;
                }
                msg(sender, "Kingdom: &f" + k + " &7(" + store.memberCount(k) + " anggota)");
            }
            case "set" -> {
                if (!sender.hasPermission(ADMIN)) {
                    noPerm(sender);
                    return;
                }
                if (args.length < 3) {
                    msg(sender, "Pakai: /kd set <player> <nama|none>");
                    return;
                }
                OfflinePlayer target = find(args[1]);
                if (target == null) {
                    msg(sender, "Player tidak ditemukan.");
                    return;
                }
                String value = args[2].equalsIgnoreCase("none") ? null : args[2];
                if (!store.setKingdom(target.getUniqueId(), value)) {
                    msg(sender, "Kingdom tidak ditemukan.");
                    return;
                }
                msg(sender, "Kingdom &f" + name(target) + "&7: &f" + (value == null ? "dikosongkan" : store.kingdomDisplay(value)));
                refresh(target);
            }
            default -> msg(sender, "Pakai: /kd <add|remove|list|join|leave|info|set>");
        }
    }

    // ================= /hud =================

    private void hud(CommandSender sender, String[] args) {
        if (!(sender instanceof Player p)) {
            msg(sender, "Hanya player yang bisa pakai /hud.");
            return;
        }
        UUID id = p.getUniqueId();
        if (args.length == 0) {
            msg(sender, "Lebar layar: &f" + store.getGuiWidth(id) + " &7| geser: &f" + store.getShift(id)
                    + " &7| HUD: &f" + (store.isHudOff(id) ? "mati" : "hidup"));
            msg(sender, "Pakai: /hud <on|off|scale <1-4|auto>|res <w> <h>|width <px>|shift <px>>");
            return;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "on" -> {
                store.setHudOff(id, false);
                refresh(p);
                msg(sender, "HUD hidup.");
            }
            case "off" -> {
                store.setHudOff(id, true);
                refresh(p);
                msg(sender, "HUD mati.");
            }
            case "scale" -> {
                int s;
                if (args.length > 1 && args[1].equalsIgnoreCase("auto")) {
                    s = 0;
                } else {
                    Long v = args.length > 1 ? parseLong(args[1]) : null;
                    if (v == null || v < 1 || v > 4) {
                        msg(sender, "Pakai: /hud scale <1|2|3|4|auto>. Lihat di Options > Video Settings > GUI Scale.");
                        return;
                    }
                    s = v.intValue();
                }
                store.setGuiScale(id, s);
                refresh(p);
                msg(sender, "GUI scale diset &f" + (s == 0 ? "auto" : s) + "&7 (dianggap window &f"
                        + store.windowWidth(id) + "x" + store.windowHeight(id)
                        + "&7). Window beda? Pakai &f/hud res <lebar> <tinggi>&7.");
            }
            case "res" -> {
                Long w = args.length > 2 ? parseLong(args[1]) : null;
                Long h = args.length > 2 ? parseLong(args[2]) : null;
                if (w == null || h == null || w < 320 || w > 8000 || h < 240 || h > 5000) {
                    msg(sender, "Pakai: /hud res <lebar> <tinggi> (ukuran window game, contoh /hud res 1920 1080).");
                    return;
                }
                store.setWindow(id, w.intValue(), h.intValue());
                refresh(p);
                msg(sender, "Ukuran window diset &f" + w + "x" + h + "&7. Lebar HUD sekarang &f" + store.getGuiWidth(id) + "&7 GUI px.");
            }
            case "width" -> {
                Long v = args.length > 1 ? parseLong(args[1]) : null;
                if (v == null || v < 200 || v > 4000) {
                    msg(sender, "Pakai: /hud width <200-4000>. Isi = lebar window game / GUI scale (contoh 1920 / 3 = 640).");
                    return;
                }
                store.setGuiWidth(id, v.intValue());
                refresh(p);
                msg(sender, "Lebar layar diset &f" + v + "&7. Kalau HUD belum menempel kanan, ubah angka ini atau pakai /hud shift.");
            }
            case "shift" -> {
                Long v = args.length > 1 ? parseLong(args[1]) : null;
                if (v == null || v < -200 || v > 200) {
                    msg(sender, "Pakai: /hud shift <-200..200> (positif = geser ke kanan).");
                    return;
                }
                store.setShift(id, v.intValue());
                refresh(p);
                msg(sender, "Geser diset &f" + v + "&7.");
            }
            default -> msg(sender, "Pakai: /hud <on|off|scale <1-4|auto>|res <w> <h>|width <px>|shift <px>>");
        }
    }

    // ================= /veyrionhud =================

    private void reload(CommandSender sender, String[] args) {
        if (!sender.hasPermission(ADMIN)) {
            noPerm(sender);
            return;
        }
        if (args.length >= 1 && args[0].equalsIgnoreCase("reload")) {
            plugin.reloadAll();
            msg(sender, "Config & HUD di-reload. Script Skript di-reload dengan /sk reload.");
        } else {
            msg(sender, "Pakai: /veyrionhud reload");
        }
    }

    // ================= tab complete =================

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        boolean admin = sender.hasPermission(ADMIN);
        boolean kdAdd = sender.hasPermission(KD_ADD);
        String name = cmd.getName().toLowerCase(Locale.ROOT);

        switch (name) {
            case "coin" -> {
                if (args.length == 1) {
                    out.add("pay");
                    if (admin) out.addAll(List.of("give", "take", "set"));
                    out.addAll(onlineNames());
                } else if (args.length == 2) {
                    out.addAll(onlineNames());
                }
            }
            case "race" -> {
                if (args.length == 1) {
                    for (Race r : Race.values()) out.add(r.id());
                    if (admin) out.addAll(List.of("set", "reset"));
                } else if (args.length == 2 && admin) {
                    out.addAll(onlineNames());
                } else if (args.length == 3 && admin && args[0].equalsIgnoreCase("set")) {
                    for (Race r : Race.values()) out.add(r.id());
                }
            }
            case "kd" -> {
                if (args.length == 1) {
                    out.addAll(List.of("list", "join", "leave", "info"));
                    if (kdAdd) out.addAll(List.of("add", "remove"));
                    if (admin) out.add("set");
                } else if (args.length == 2) {
                    String sub = args[0].toLowerCase(Locale.ROOT);
                    if (sub.equals("join") || sub.equals("remove")) out.addAll(store.kingdomNames());
                    else if (sub.equals("set") && admin) out.addAll(onlineNames());
                } else if (args.length == 3 && args[0].equalsIgnoreCase("set") && admin) {
                    out.addAll(store.kingdomNames());
                    out.add("none");
                }
            }
            case "hud" -> {
                if (args.length == 1) out.addAll(List.of("on", "off", "scale", "res", "width", "shift"));
                else if (args.length == 2 && args[0].equalsIgnoreCase("scale")) out.addAll(List.of("1", "2", "3", "4", "auto"));
            }
            case "veyrionhud" -> {
                if (args.length == 1 && admin) out.add("reload");
            }
            default -> {
            }
        }

        String last = args.length == 0 ? "" : args[args.length - 1].toLowerCase(Locale.ROOT);
        out.removeIf(s -> !s.toLowerCase(Locale.ROOT).startsWith(last));
        return out;
    }

    // ================= helpers =================

    private List<String> onlineNames() {
        List<String> names = new ArrayList<>();
        for (Player p : Bukkit.getOnlinePlayers()) names.add(p.getName());
        return names;
    }

    private OfflinePlayer find(String name) {
        Player online = Bukkit.getPlayerExact(name);
        if (online != null) return online;
        return Bukkit.getOfflinePlayerIfCached(name);
    }

    private String name(OfflinePlayer p) {
        return p.getName() == null ? p.getUniqueId().toString() : p.getName();
    }

    private void refresh(OfflinePlayer target) {
        Player online = target.getPlayer();
        if (online != null) plugin.hud().update(online);
    }

    private void refreshAll() {
        for (Player p : Bukkit.getOnlinePlayers()) plugin.hud().update(p);
    }

    private Long parseLong(String s) {
        try {
            return Long.parseLong(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String fmt(long n) {
        return String.format(Locale.US, "%,d", n);
    }

    private void noPerm(CommandSender sender) {
        msg(sender, "&cKamu tidak punya izin.");
    }

    private void msg(CommandSender sender, String text) {
        sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize("&f[Veyrion] &7" + text));
    }
}
