package de.crafttogether.ctcommands.text;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class JoinMessagesConfig {

    private static final Pattern KEY_LINE = Pattern.compile("^(\\s*)([A-Za-z0-9_]+)(\\s*:\\s*)(.*)$");

    private final Path configPath;

    // Original layout (alle Zeilen inkl. Banner/Kommentare/Leerzeilen)
    private List<String> lines = new ArrayList<>();

    // Parsed values (ohne Quotes) + typisierte Defaults
    private final Map<String, Object> values = new HashMap<>();

    public JoinMessagesConfig(Path configPath) throws IOException {
        this.configPath = configPath;
        reload(); // lädt + parst + behält Layout
    }

    /* ===================== GETTER ===================== */

    public boolean isShowJoin() {
        return getBoolean("showjoin", true);
    }

    public boolean isShowLeave() {
        return getBoolean("showleave", true);
    }

    public String getServerJoinMessage() {
        return getString("serverjoin", "");
    }

    public String getSilentJoinMessage() {
        return getString("silentjoin", "");
    }

    public String getServerLeaveMessage() {
        return getString("serverleave", "");
    }

    public String getSilentLeaveMessage() {
        return getString("silentleave", "");
    }

    public boolean isPrivateServerJoin() {
        return getBoolean("private_serverjoin", true);
    }

    public String getPrivateServerJoinFile() {
        return getString("private_serverjoin_file", "");
    }

    public boolean isWelcomeEnabled() {
        return getBoolean("welcome", true);
    }

    public String getWelcomeMessage() {
        return getString("welcome_message", "");
    }

    public boolean isPrivateWelcome() {
        return getBoolean("private_welcome", false);
    }

    public String getPrivateWelcomeMessage() {
        return getString("private_welcome_message", "");
    }
    public boolean isVelocityOverridesEnabled() {
        return getBoolean("velocity_overrides", true);
    }


    /* ===================== SETTER ===================== */

    public void setShowJoin(boolean value) {
        setUnchecked("showjoin", value);
    }

    public void setShowLeave(boolean value) {
        setUnchecked("showleave", value);
    }

    public void setServerJoinMessage(String value) {
        setUnchecked("serverjoin", value);
    }

    public void setSilentJoinMessage(String value) {
        setUnchecked("silentjoin", value);
    }

    public void setServerLeaveMessage(String value) {
        setUnchecked("serverleave", value);
    }

    public void setSilentLeaveMessage(String value) {
        setUnchecked("silentleave", value);
    }

    public void setPrivateServerJoin(boolean value) {
        setUnchecked("private_serverjoin", value);
    }

    public void setPrivateServerJoinFile(String value) {
        setUnchecked("private_serverjoin_file", value);
    }

    public void setWelcomeEnabled(boolean value) {
        setUnchecked("welcome", value);
    }

    public void setWelcomeMessage(String value) {
        setUnchecked("welcome_message", value);
    }

    public void setPrivateWelcome(boolean value) {
        setUnchecked("private_welcome", value);
    }

    public void setPrivateWelcomeMessage(String value) {
        setUnchecked("private_welcome_message", value);
    }

    private void setUnchecked(String key, Object value) {
        if (value == null) {
            throw new IllegalArgumentException("Config-Wert darf nicht null sein: " + key);
        }
        values.put(key, value);
    }

    /* ===================== SAVE / RELOAD ===================== */

    /**
     * Speichert, ohne das Layout zu verändern:
     * - Banner/Kommentare/Leerzeilen bleiben exakt gleich
     * - Nur die Werte rechts von "key:" werden ersetzt
     * - Falls ein Key noch nicht existiert, wird er am Ende angehängt (ohne Layout zu zerstören)
     */
    public void save() throws IOException {
        // Ersetze existierende Keys in den vorhandenen Zeilen
        Set<String> written = new HashSet<>();
        List<String> out = new ArrayList<>(lines.size());

        for (String line : lines) {
            Matcher m = KEY_LINE.matcher(line);
            if (!m.matches()) {
                out.add(line);
                continue;
            }

            String key = m.group(2);
            if (!values.containsKey(key)) {
                out.add(line);
                continue;
            }

            // Layoutteile
            String leadingWs = m.group(1);
            String sep = m.group(3);
            String rest = m.group(4);

            SplitValueComment split = splitValueAndComment(rest);
            String existingValueToken = split.valueToken;          // ohne trailing spaces
            String spacingBeforeComment = split.spacingToComment;  // originale spaces/tabs zwischen value und comment
            String comment = split.comment;                        // inkl. '#...'

            Object newVal = values.get(key);
            String newValueText = formatValueLike(existingValueToken, newVal);

            out.add(leadingWs + key + sep + newValueText + spacingBeforeComment + comment);
            written.add(key);
        }

        // Keys, die noch nicht existieren -> ans Ende anhängen (Layout bleibt ansonsten identisch)
        List<String> missing = new ArrayList<>();
        for (String key : values.keySet()) {
            if (!written.contains(key) && !containsKeyInLines(key)) {
                missing.add(key);
            }
        }
        // deterministische Reihenfolge
        missing.sort(String::compareToIgnoreCase);

        if (!missing.isEmpty()) {
            if (!out.isEmpty() && !out.get(out.size() - 1).isEmpty()) {
                // Keine extra Formatierung erzwingen – nur eine Leerzeile, falls Datei nicht ohnehin endet
                out.add("");
            }
            for (String key : missing) {
                Object v = values.get(key);
                out.add(key + ": " + formatValueLike(null, v));
            }
        }

        Files.write(
                configPath,
                out,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE
        );

        // Keep in-memory layout aligned with disk
        this.lines = out;
    }

    public void reload() throws IOException {
        if (!Files.exists(configPath)) {
            // Datei existiert nicht -> leeres Layout + leere values
            this.lines = new ArrayList<>();
            this.values.clear();
            return;
        }

        this.lines = Files.readAllLines(configPath, StandardCharsets.UTF_8);
        this.values.clear();
        parseIntoValues(this.lines);
    }

    /* ===================== INTERNAL PARSING ===================== */

    private void parseIntoValues(List<String> srcLines) {
        for (String line : srcLines) {
            Matcher m = KEY_LINE.matcher(line);
            if (!m.matches()) continue;

            String key = m.group(2);
            String rest = m.group(4);

            SplitValueComment split = splitValueAndComment(rest);
            String valueToken = split.valueToken.trim();

            if (valueToken.isEmpty()) continue;

            // boolean?
            if (isBooleanToken(valueToken)) {
                values.put(key, Boolean.parseBoolean(valueToken.toLowerCase(Locale.ROOT)));
                continue;
            }

            // string
            values.put(key, parseStringToken(valueToken));
        }
    }

    private boolean getBoolean(String key, boolean def) {
        Object v = values.get(key);
        if (v instanceof Boolean b) return b;
        if (v instanceof String s && isBooleanToken(s.trim())) {
            return Boolean.parseBoolean(s.trim().toLowerCase(Locale.ROOT));
        }
        return def;
    }

    private String getString(String key, String def) {
        Object v = values.get(key);
        if (v instanceof String s) return s;
        if (v instanceof Boolean b) return String.valueOf(b);
        return def;
    }

    private boolean containsKeyInLines(String key) {
        for (String line : lines) {
            Matcher m = KEY_LINE.matcher(line);
            if (m.matches() && key.equals(m.group(2))) return true;
        }
        return false;
    }

    /* ===================== VALUE HANDLING (FORMAT-STABIL) ===================== */

    /**
     * Trennt den "rest"-Teil (rechts von "key:") in valueToken + spacingToComment + comment.
     * Kommentare werden nur erkannt, wenn ein '#' außerhalb von Quotes vorkommt.
     */
    private static SplitValueComment splitValueAndComment(String rest) {
        if (rest == null) rest = "";

        int idx = findCommentStart(rest);
        if (idx < 0) {
            // kein Kommentar
            String valuePart = rest;
            String valueToken = rstrip(valuePart);
            String spacing = valuePart.substring(valueToken.length()); // original spaces am Ende
            return new SplitValueComment(valueToken, spacing, "");
        }

        String valuePart = rest.substring(0, idx);
        String comment = rest.substring(idx);

        String valueToken = rstrip(valuePart);
        String spacing = valuePart.substring(valueToken.length());

        return new SplitValueComment(valueToken, spacing, comment);
    }

    private static int findCommentStart(String s) {
        boolean inSingle = false;
        boolean inDouble = false;
        boolean escaped = false;

        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);

            if (escaped) {
                escaped = false;
                continue;
            }

            if (inDouble && c == '\\') {
                escaped = true;
                continue;
            }

            if (!inDouble && c == '\'' ) {
                inSingle = !inSingle;
                continue;
            }

            if (!inSingle && c == '"') {
                inDouble = !inDouble;
                continue;
            }

            if (!inSingle && !inDouble && c == '#') {
                return i;
            }
        }
        return -1;
    }

    private static boolean isBooleanToken(String t) {
        String x = t.trim().toLowerCase(Locale.ROOT);
        return "true".equals(x) || "false".equals(x);
    }

    /**
     * Wenn in der Datei schon Quotes verwendet wurden, behalten wir das Prinzip bei.
     * - existingValueToken != null: wir richten uns nach dem bisherigen Stil (quoted/unquoted)
     * - existingValueToken == null: default: Strings quoted, Booleans unquoted
     */
    private static String formatValueLike(String existingValueToken, Object newVal) {
        if (newVal instanceof Boolean b) {
            return b ? "true" : "false";
        }

        String s = String.valueOf(newVal);

        boolean keepQuoted = true; // default: strings quoted (sauber bei Sonderzeichen)
        if (existingValueToken != null) {
            String t = existingValueToken.trim();
            if (t.startsWith("\"") && t.endsWith("\"") && t.length() >= 2) keepQuoted = true;
            else if (t.startsWith("'") && t.endsWith("'") && t.length() >= 2) keepQuoted = true;
            else keepQuoted = false;
        }

        if (!keepQuoted) {
            return s;
        }
        return "\"" + escapeForDoubleQuotes(s) + "\"";
    }

    private static String parseStringToken(String token) {
        String t = token.trim();
        if (t.length() >= 2 && t.startsWith("\"") && t.endsWith("\"")) {
            String inner = t.substring(1, t.length() - 1);
            return unescapeDoubleQuoted(inner);
        }
        if (t.length() >= 2 && t.startsWith("'") && t.endsWith("'")) {
            // Single quotes: minimal parsing ('' -> ')
            String inner = t.substring(1, t.length() - 1);
            return inner.replace("''", "'");
        }
        return t;
    }

    private static String escapeForDoubleQuotes(String s) {
        StringBuilder sb = new StringBuilder(s.length() + 8);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '\\' -> sb.append("\\\\");
                case '"'  -> sb.append("\\\"");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> sb.append(c);
            }
        }
        return sb.toString();
    }

    private static String unescapeDoubleQuoted(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        boolean esc = false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (!esc) {
                if (c == '\\') {
                    esc = true;
                } else {
                    sb.append(c);
                }
                continue;
            }
            // esc == true
            esc = false;
            switch (c) {
                case 'n' -> sb.append('\n');
                case 'r' -> sb.append('\r');
                case 't' -> sb.append('\t');
                case '\\' -> sb.append('\\');
                case '"' -> sb.append('"');
                default -> sb.append(c); // unknown escape -> keep char
            }
        }
        // trailing '\' -> keep it
        if (esc) sb.append('\\');
        return sb.toString();
    }

    private static String rstrip(String s) {
        int end = s.length();
        while (end > 0) {
            char c = s.charAt(end - 1);
            if (c == ' ' || c == '\t') end--;
            else break;
        }
        return s.substring(0, end);
    }

    private record SplitValueComment(String valueToken, String spacingToComment, String comment) {}
}
