package de.crafttogether.ctcommands.text;

import com.velocitypowered.api.proxy.Player;
import de.themoep.minedown.adventure.MineDown;
import net.kyori.adventure.text.Component;

import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

/**
 * Zentrale Text-Engine für CTCommands.
 * Verarbeitet MineDown-Markup und Legacy-Farbcodes (&c, &e, &l, ...).
 */
public final class Texts {
    private Texts() {}
    private static final PlainTextComponentSerializer PLAIN = PlainTextComponentSerializer.plainText();


    public static Component parse(String raw) {
        if (raw == null || raw.isEmpty()) return Component.empty();
        return MineDown.parse(raw);
    }

    public static Component parse(String raw, Player player) {
        if (raw == null || raw.isEmpty()) return Component.empty();
        String name = player != null ? player.getUsername() : "";
        return MineDown.parse(raw.replace("%NAME%", name));
    }

    //&c = Rot
    public static Component error(String msg) {
        return MineDown.parse("&c" + msg);
    }
    //&7 = Grau
    public static Component info(String msg) {
        return MineDown.parse("&7" + msg);
    }

    public static String toPlain(Component component) {
        if (component == null) return "";
        return PLAIN.serialize(component);
    }
}