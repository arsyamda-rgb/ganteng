package com.veyrion.hud.skript.elements;

import ch.njol.skript.classes.Changer.ChangeMode;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import ch.njol.util.coll.CollectionUtils;
import com.veyrion.hud.DataStore;
import com.veyrion.hud.VeyrionHud;
import org.bukkit.OfflinePlayer;
import org.bukkit.event.Event;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

/** coin of player  |  player's veyrion coin. Bisa set, add, remove, reset. */
public class ExprCoin extends SimplePropertyExpression<OfflinePlayer, Number> {

    static {
        register(ExprCoin.class, Number.class, "[veyrion] coin[s]", "offlineplayers");
    }

    @Override
    public @Nullable Number convert(OfflinePlayer player) {
        return VeyrionHud.getPlugin(VeyrionHud.class).store().getCoin(player.getUniqueId());
    }

    @Override
    public Class<?> @Nullable [] acceptChange(ChangeMode mode) {
        return switch (mode) {
            case SET, ADD, REMOVE -> CollectionUtils.array(Number.class);
            case RESET, DELETE -> CollectionUtils.array();
            default -> null;
        };
    }

    @Override
    public void change(Event event, Object @Nullable [] delta, ChangeMode mode) {
        VeyrionHud plugin = VeyrionHud.getPlugin(VeyrionHud.class);
        DataStore store = plugin.store();
        long value = delta != null && delta.length > 0 && delta[0] instanceof Number n ? n.longValue() : 0L;

        for (OfflinePlayer p : getExpr().getArray(event)) {
            switch (mode) {
                case SET -> store.setCoin(p.getUniqueId(), value);
                case ADD -> store.addCoin(p.getUniqueId(), value);
                case REMOVE -> store.addCoin(p.getUniqueId(), -value);
                default -> store.setCoin(p.getUniqueId(), 0L);
            }
            Player online = p.getPlayer();
            if (online != null) plugin.hud().update(online);
        }
    }

    @Override
    public Class<? extends Number> getReturnType() {
        return Number.class;
    }

    @Override
    protected String getPropertyName() {
        return "veyrion coin";
    }
}
