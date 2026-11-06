package com.veyrion.hud;

public enum Race {
    HUMAN("human", "Human"),
    BEAST("beast", "Beast"),
    ELF("elf", "Elf"),
    DRAGON("dragon", "Dragon");

    private final String id;
    private final String display;

    Race(String id, String display) {
        this.id = id;
        this.display = display;
    }

    public String id() {
        return id;
    }

    public String display() {
        return display;
    }

    public static Race from(String s) {
        if (s == null) return null;
        for (Race r : values()) {
            if (r.id.equalsIgnoreCase(s)) return r;
        }
        return null;
    }
}
