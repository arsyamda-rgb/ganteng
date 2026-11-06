package com.veyrion.hud.skript.elements;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import com.veyrion.hud.VeyrionHud;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

/** all veyrion kingdoms  (daftar nama kingdom). */
public class ExprKingdoms extends SimpleExpression<String> {

    static {
        Skript.registerExpression(ExprKingdoms.class, String.class, ExpressionType.SIMPLE,
                "all [veyrion] kingdoms");
    }

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
        return true;
    }

    @Override
    protected String @Nullable [] get(Event event) {
        return VeyrionHud.getPlugin(VeyrionHud.class).store().kingdomNames().toArray(new String[0]);
    }

    @Override
    public boolean isSingle() {
        return false;
    }

    @Override
    public Class<? extends String> getReturnType() {
        return String.class;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "all veyrion kingdoms";
    }
}
