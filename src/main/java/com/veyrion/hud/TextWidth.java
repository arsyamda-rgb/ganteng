package com.veyrion.hud;

/** Lebar maju (advance) karakter font default Minecraft, dalam GUI pixel (sudah termasuk 1px jarak). */
public final class TextWidth {

    private TextWidth() {
    }

    public static int of(String s) {
        int w = 0;
        for (int i = 0; i < s.length(); i++) {
            w += advance(s.charAt(i));
        }
        return w;
    }

    public static int advance(char c) {
        switch (c) {
            case ' ':
                return 4;
            case '!':
            case ',':
            case '.':
            case ':':
            case ';':
            case 'i':
            case '|':
                return 2;
            case '\'':
            case 'l':
            case '`':
                return 3;
            case 'I':
            case 't':
            case '[':
            case ']':
                return 4;
            case '"':
            case '(':
            case ')':
            case '*':
            case '<':
            case '>':
            case 'f':
            case 'k':
            case '{':
            case '}':
                return 5;
            case '@':
            case '~':
                return 7;
            default:
                return 6;
        }
    }
}
