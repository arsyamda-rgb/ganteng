package com.veyrion.hud.skript.elements;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.util.Kleenean;
import com.veyrion.hud.VeyrionHud;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

import java.util.regex.Pattern;

/** create veyrion kingdom "Nama"  |  delete veyrion kingdom "Nama" */
public class EffKingdom extends Effect {

    private static final Pattern NAME = Pattern.compile("[A-Za-z0-9_-]{2,16}");

    static {
        Skript.registerEffect(EffKingdom.class,
                "create [veyrion] kingdom %string%",
                "(delete|remove) [veyrion] kingdom %string%");
    }

    private Expression<String> name;
    private boolean create;

    @Override
    @SuppressWarnings("unchecked")
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
        name = (Expression<String>) exprs[0];
        create = matchedPattern == 0;
        return true;
    }

    @Override
    protected void execute(Event event) {
        String n = name.getSingle(event);
        if (n == null) return;
        n = n.strip();
        VeyrionHud plugin = VeyrionHud.getPlugin(VeyrionHud.class);

        if (create) {
            if (!NAME.matcher(n).matches()) {
                plugin.getLogger().warning("Skript: nama kingdom 2-16 karakter huruf/angka/_/- tanpa spasi: " + n);
                return;
            }
            plugin.store().addKingdom(n);
        } else if (plugin.store().removeKingdom(n)) {
            for (Player p : Bukkit.getOnlinePlayers()) plugin.hud().update(p);
        }
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return (create ? "create" : "delete") + " veyrion kingdom " + name.toString(event, debug);
    }
}
