package it.solphivia.mobile;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Model-independent parsing only. Never sends messages or resolves recipients. */
final class ShareBridge {
    private static final Pattern COMMAND = Pattern.compile(
            "^condividi\\s+(.+?)\\s+su\\s+whats\\s*app[.!?]*$",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    static String preview(String command) {
        if (command == null) return null;
        Matcher match = COMMAND.matcher(command.trim());
        if (!match.matches()) return null;
        String text = match.group(1).trim();
        return text.isEmpty() || text.length() > 2000 ? null : text;
    }
}
