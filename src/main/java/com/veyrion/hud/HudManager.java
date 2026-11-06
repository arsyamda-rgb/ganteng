package com.veyrion.hud;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * HUD kanan-atas. Satu bossbar per baris (bar-nya transparan dari resource pack),
 * judul bossbar selalu di tengah layar, jadi tiap baris diberi spacer di kiri
 * supaya ujung kanannya menempel ke tepi kanan layar.
 *
 * Baris: 1 tanggal + jam + logo, 2 online, 3 coin, 4 ras, 5 kingdom.
 * Teks rata kanan, ikon di paling kanan (sama seperti HUD asli).
 * Ikon PNG putih, jadi warna ikon = warna teks di sebelahnya.
 */
public final class HudManager {

    private static final int ROWS = 5;

    private final VeyrionHud plugin;
    private final Map<UUID, BossBar[]> bars = new HashMap<>();
    private final Set<UUID> shown = new HashSet<>();
    /** Teks pengganti per player per baris (index 0-4), diatur dari Skript. Hanya di memori. */
    private final Map<UUID, String[]> overrides = new HashMap<>();

    private static final LegacyComponentSerializer RICH = LegacyComponentSerializer.builder()
            .character('&').hexColors().build();
    private static final Pattern COLOR_CODE = Pattern.compile("(?i)[&§](#[0-9a-f]{6}|[0-9a-fk-or])");

    private BukkitTask task;
    private DateTimeFormatter dateFormat;
    private DateTimeFormatter timeFormat;
    private ZoneId zone;

    public HudManager(VeyrionHud plugin) {
        this.plugin = plugin;
    }

    public void start() {
        loadFormats();
        long interval = Math.max(5L, plugin.getConfig().getLong("update-interval-ticks", 20L));
        task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player p : Bukkit.getOnlinePlayers()) update(p);
        }, 20L, interval);
    }

    public void stop() {
        if (task != null) task.cancel();
        task = null;
        for (Player p : Bukkit.getOnlinePlayers()) hide(p);
        bars.clear();
        shown.clear();
    }

    public void reload() {
        stop();
        start();
    }

    public void remove(Player p) {
        hide(p);
        bars.remove(p.getUniqueId());
        overrides.remove(p.getUniqueId());
    }

    /** row 1-5 (1 = tanggal/jam, 2 = online, 3 = coin, 4 = ras, 5 = kingdom). text null = kembali normal. */
    public void setOverride(UUID id, int row, String text) {
        if (row < 1 || row > ROWS) return;
        String[] arr = overrides.computeIfAbsent(id, k -> new String[ROWS]);
        arr[row - 1] = text;
    }

    public String getOverride(UUID id, int row) {
        String[] arr = overrides.get(id);
        return arr == null || row < 1 || row > ROWS ? null : arr[row - 1];
    }

    private String ov(UUID id, int row, String fallback) {
        String o = getOverride(id, row);
        return o == null ? fallback : o;
    }

    private boolean hasOv(UUID id, int row) {
        return getOverride(id, row) != null;
    }

    private void hide(Player p) {
        BossBar[] bs = bars.get(p.getUniqueId());
        if (bs != null && shown.remove(p.getUniqueId())) {
            for (BossBar b : bs) p.hideBossBar(b);
        }
    }

    private void loadFormats() {
        FileConfiguration cfg = plugin.getConfig();
        try {
            zone = ZoneId.of(cfg.getString("timezone", "Asia/Jakarta"));
        } catch (Exception e) {
            zone = ZoneId.systemDefault();
            plugin.getLogger().warning("timezone di config tidak valid, pakai timezone server.");
        }
        dateFormat = safeFormatter(cfg.getString("date-format", "dd/MM/yyyy"), "dd/MM/yyyy");
        timeFormat = safeFormatter(cfg.getString("time-format", "HH:mm"), "HH:mm");
    }

    private DateTimeFormatter safeFormatter(String pattern, String fallback) {
        try {
            return DateTimeFormatter.ofPattern(pattern, Locale.ENGLISH);
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Format waktu tidak valid: " + pattern);
            return DateTimeFormatter.ofPattern(fallback, Locale.ENGLISH);
        }
    }

    private static BossBar[] makeBars() {
        BossBar[] bs = new BossBar[ROWS];
        for (int i = 0; i < ROWS; i++) {
            bs[i] = BossBar.bossBar(Component.empty(), 1.0f, BossBar.Color.WHITE, BossBar.Overlay.PROGRESS);
        }
        return bs;
    }

    private TextColor color(String path, TextColor fallback) {
        String s = plugin.getConfig().getString("colors." + path);
        if (s == null) return fallback;
        TextColor c = TextColor.fromHexString(s);
        return c == null ? fallback : c;
    }

    public void update(Player p) {
        UUID id = p.getUniqueId();
        DataStore store = plugin.store();

        if (store.isHudOff(id)) {
            hide(p);
            return;
        }

        FileConfiguration cfg = plugin.getConfig();
        int icon = cfg.getInt("glyph-advance.icon", 10);
        int logo = cfg.getInt("glyph-advance.logo", 25);
        int guiWidth = store.getGuiWidth(id);
        int margin = cfg.getInt("margin", 6);
        int shift = store.getShift(id);

        ZonedDateTime now = ZonedDateTime.now(zone);

        String online = cfg.getString("format.online", "{online} Online")
                .replace("{online}", String.valueOf(Bukkit.getOnlinePlayers().size()));

        String coin = cfg.getString("format.coin", "{coin}")
                .replace("{coin}", String.format(Locale.US, "%,d", store.getCoin(id)));

        Race race = store.getRace(id);
        String raceName = race == null ? cfg.getString("no-race", "Belum Pilih Ras") : race.display();
        String raceText = cfg.getString("format.race", "{race}").replace("{race}", raceName);
        // Belum pilih ras: pakai ikon human (abu-abu) supaya baris tetap rata.
        char raceGlyph = Glyphs.race(race == null ? Race.HUMAN : race);

        String kd = store.getKingdom(id);
        String kingdomName = kd == null ? cfg.getString("no-kingdom", "Tanpa Kingdom") : kd;
        String kingdomText = cfg.getString("format.kingdom", "{kingdom}").replace("{kingdom}", kingdomName);

        TextColor cDate = color("date", NamedTextColor.GOLD);
        TextColor cTime = color("time", NamedTextColor.AQUA);
        TextColor cOnline = color("online", NamedTextColor.GREEN);
        TextColor cCoin = color("coin", NamedTextColor.YELLOW);
        TextColor cKingdom = color("kingdom", NamedTextColor.LIGHT_PURPLE);
        TextColor cLogo = color("logo", NamedTextColor.WHITE);
        TextColor cRace = race == null
                ? color("race.none", NamedTextColor.GRAY)
                : color("race." + race.id(), NamedTextColor.WHITE);

        Line[] lines = new Line[ROWS];
        if (hasOv(id, 1)) {
            // Teks baris 1 diganti dari Skript: ikon kalender + teks + logo.
            lines[0] = new Line()
                    .glyph(Glyphs.CALENDAR, icon, cDate).gap(2).rich(getOverride(id, 1), cDate)
                    .gap(8).glyph(Glyphs.LOGO, logo, cLogo);
        } else {
            lines[0] = new Line()
                    .glyph(Glyphs.CALENDAR, icon, cDate).gap(2).text(now.format(dateFormat), cDate)
                    .gap(6).glyph(Glyphs.CLOCK, icon, cTime).gap(2).text(now.format(timeFormat), cTime)
                    .gap(8).glyph(Glyphs.LOGO, logo, cLogo);
        }
        lines[1] = row(id, 2, online, cOnline, Glyphs.ONLINE, icon);
        lines[2] = row(id, 3, coin, cCoin, Glyphs.COIN, icon);
        lines[3] = row(id, 4, raceText, cRace, raceGlyph, icon);
        lines[4] = row(id, 5, kingdomText, cKingdom, Glyphs.KINGDOM, icon);

        BossBar[] bs = bars.computeIfAbsent(id, k -> makeBars());
        for (int i = 0; i < ROWS; i++) {
            bs[i].name(render(lines[i], guiWidth, margin, shift));
        }
        if (shown.add(id)) {
            for (BossBar b : bs) p.showBossBar(b);
        }
    }

    private Line row(UUID id, int row, String defaultText, TextColor color, char glyph, int icon) {
        Line line = new Line();
        String custom = getOverride(id, row);
        if (custom != null) line.rich(custom, color);
        else line.text(defaultText, color);
        return line.gap(3).glyph(glyph, icon, color);
    }

    /**
     * Judul bossbar dipusatkan klien: lebar total T = spacer + isi, tengah di G/2.
     * Ujung kanan = G/2 + T/2. Mau ujung kanan di G - margin + shift -> T = G - 2*margin + 2*shift.
     */
    private static Component render(Line line, int guiWidth, int margin, int shift) {
        int spacer = guiWidth - 2 * margin + 2 * shift - line.width;
        return Component.text()
                .font(Glyphs.FONT)
                .append(Component.text(Glyphs.spacer(spacer)))
                .append(line.parts)
                .build();
    }

    private static final class Line {
        final List<Component> parts = new ArrayList<>();
        int width;

        Line text(String s, TextColor color) {
            parts.add(Component.text(s, color));
            width += TextWidth.of(s);
            return this;
        }

        /** Teks dengan kode warna &a / &#RRGGBB dari Skript. Lebar dihitung tanpa kode warna. */
        Line rich(String s, TextColor color) {
            parts.add(RICH.deserialize(s).colorIfAbsent(color));
            width += TextWidth.of(COLOR_CODE.matcher(s).replaceAll(""));
            return this;
        }

        Line glyph(char c, int advance, TextColor color) {
            parts.add(Component.text(String.valueOf(c), color));
            width += advance;
            return this;
        }

        Line gap(int px) {
            parts.add(Component.text(Glyphs.spacer(px)));
            width += px;
            return this;
        }
    }
}
