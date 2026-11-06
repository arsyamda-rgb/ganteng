package com.veyrion.hud.skript.elements;

import ch.njol.skript.classes.Changer.ChangeMode;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import ch.njol.util.coll.CollectionUtils;
import com.veyrion.hud.Race;
import com.veyrion.hud.VeyrionHud;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

/** race of player  (human / beast / elf / dragon, kosong kalau belum pilih). Bisa set dan reset. */
public class ExprRace extends SimplePropertyExpression<OfflinePlayer, String> {

    static {
        register(ExprRace.class, String.class, "veyrion race", "offlineplayers");
    }

    @Override
    public @Nullable String convert(OfflinePlayer player) {
        Race race = VeyrionHud.getPlugin(VeyrionHud.class).store().getRace(player.getUniqueId());
        return race == null ? null : race.display();
    }

    @Override
    public Class<?> @Nullable [] acceptChange(ChangeMode mode) {
        return switch (mode) {
            case SET -> CollectionUtils.array(String.class);
            case RESET, DELETE -> CollectionUtils.array();
            default -> null;
        };
    }

    @Override
    public void change(Event event, Object @Nullable [] delta, ChangeMode mode) {
        VeyrionHud plugin = VeyrionHud.getPlugin(VeyrionHud.class);
        Race race = null;
        if (mode == ChangeMode.SET) {
            race = delta != null && delta.length > 0 && delta[0] instanceof String s ? Race.from(s.strip()) : null;
            if (race == null) {
                plugin.getLogger().warning("Skript: ras tidak valid (human, beast, elf, dragon).");
                return;
            }
        }
        for (OfflinePlayer p : getExpr().getArray(event)) {
            plugin.store().setRace(p.getUniqueId(), race);
            Player online = p.getPlayer();
            if (online != null) plugin.hud().update(online);
        }
    }

    @Override
    public Class<? extends String> getReturnType() {
        return String.class;
    }

    @Override
    protected String getPropertyName() {
        return "veyrion race";
    }
}
