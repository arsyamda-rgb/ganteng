package com.veyrion.hud.skript.elements;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.util.Kleenean;
import com.veyrion.hud.VeyrionHud;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

/** refresh veyrion hud of player  (HUD juga refresh sendiri tiap detik, ini untuk update instan). */
public class EffRefreshHud extends Effect {

    static {
        Skript.registerEffect(EffRefreshHud.class, "refresh [veyrion] hud of %players%");
    }

    private Expression<Player> players;

    @Override
    @SuppressWarnings("unchecked")
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
        players = (Expression<Player>) exprs[0];
        return true;
    }

    @Override
    protected void execute(Event event) {
        VeyrionHud plugin = VeyrionHud.getPlugin(VeyrionHud.class);
        for (Player p : players.getArray(event)) plugin.hud().update(p);
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "refresh veyrion hud of " + players.toString(event, debug);
    }
}
