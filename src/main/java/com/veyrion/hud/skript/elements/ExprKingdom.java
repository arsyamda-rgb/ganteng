package com.veyrion.hud.skript.elements;

import ch.njol.skript.classes.Changer.ChangeMode;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import ch.njol.util.coll.CollectionUtils;
import com.veyrion.hud.VeyrionHud;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

/** kingdom of player  (kosong kalau tidak punya). Set harus nama kingdom yang sudah dibuat. */
public class ExprKingdom extends SimplePropertyExpression<OfflinePlayer, String> {

    static {
        register(ExprKingdom.class, String.class, "veyrion kingdom", "offlineplayers");
    }

    @Override
    public @Nullable String convert(OfflinePlayer player) {
        return VeyrionHud.getPlugin(VeyrionHud.class).store().getKingdom(player.getUniqueId());
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
        String name = null;
        if (mode == ChangeMode.SET) {
            name = delta != null && delta.length > 0 && delta[0] instanceof String s ? s.strip() : null;
            if (name == null || !plugin.store().kingdomExists(name)) {
                plugin.getLogger().warning("Skript: kingdom tidak ada: " + name + " (buat dulu dengan /kd add atau efek create kingdom).");
                return;
            }
        }
        for (OfflinePlayer p : getExpr().getArray(event)) {
            plugin.store().setKingdom(p.getUniqueId(), name);
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
        return "veyrion kingdom";
    }
}
