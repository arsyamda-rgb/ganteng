package com.veyrion.hud;

import net.kyori.adventure.key.Key;

/** Karakter font veyrion_hud:hud. Harus sama dengan hud.json di resource pack. */
public final class Glyphs {

    public static final Key FONT = Key.key("veyrion_hud", "hud");

    public static final char LOGO = '';
    public static final char CALENDAR = '';
    public static final char CLOCK = '';
    public static final char ONLINE = '';
    public static final char COIN = '';
    public static final char KINGDOM = '';

    private static final char POS_BASE = ''; // +1, +2, +4 ... +512 px
    private static final char NEG_BASE = ''; // -1, -2, -4 ... -512 px

    private Glyphs() {
    }

    public static char race(Race race) {
        return switch (race) {
            case HUMAN -> '';
            case BEAST -> '';
            case ELF -> '';
            case DRAGON -> '';
        };
    }

    /** Teks kosong selebar px pixel (boleh negatif). */
    public static String spacer(int px) {
        if (px == 0) return "";
        char base = px < 0 ? NEG_BASE : POS_BASE;
        int n = Math.abs(px);
        StringBuilder sb = new StringBuilder();
        while (n >= 512) {
            sb.append((char) (base + 9));
            n -= 512;
        }
        for (int b = 8; b >= 0; b--) {
            if (((n >> b) & 1) == 1) sb.append((char) (base + b));
        }
        return sb.toString();
    }
}
