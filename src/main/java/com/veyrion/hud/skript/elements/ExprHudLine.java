package com.veyrion.hud.skript.elements;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.Changer.ChangeMode;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import ch.njol.util.coll.CollectionUtils;
import com.veyrion.hud.VeyrionHud;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

/**
 * veyrion hud line 1-5 of player. Baris 1 = tanggal/jam, 2 = online, 3 = coin, 4 = ras, 5 = kingdom.
 * set = ganti teks (boleh kode warna &a atau &#RRGGBB), reset/delete = kembali ke teks bawaan.
 */
public class ExprHudLine extends SimpleExpression<String> {

    static {
        Skript.registerExpression(ExprHudLine.class, String.class, ExpressionType.COMBINED,
                "[veyrion] hud line %number% of %player%");
    }

    private Expression<Number> line;
    private Expression<Player> player;

    @Override
    @SuppressWarnings("unchecked")
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
        line = (Expression<Number>) exprs[0];
        player = (Expression<Player>) exprs[1];
        return true;
    }

    @Override
    protected String @Nullable [] get(Event event) {
        Number n = line.getSingle(event);
        Player p = player.getSingle(event);
        if (n == null || p == null) return new String[0];
        String text = VeyrionHud.getPlugin(VeyrionHud.class).hud().getOverride(p.getUniqueId(), n.intValue());
        return text == null ? new String[0] : new String[]{text};
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
        Number n = line.getSingle(event);
        Player p = player.getSingle(event);
        if (n == null || p == null) return;
        int row = n.intValue();
        if (row < 1 || row > 5) {
            plugin.getLogger().warning("Skript: baris HUD harus 1-5, bukan " + row + ".");
            return;
        }
        String text = null;
        if (mode == ChangeMode.SET && delta != null && delta.length > 0 && delta[0] instanceof String s) {
            text = s;
        }
        plugin.hud().setOverride(p.getUniqueId(), row, text);
        plugin.hud().update(p);
    }

    @Override
    public boolean isSingle() {
        return true;
    }

    @Override
    public Class<? extends String> getReturnType() {
        return String.class;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "veyrion hud line " + line.toString(event, debug) + " of " + player.toString(event, debug);
    }
}
