package ru.wqkcpf.moderationhelper.chat;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

public final class ChatNicknameParser {
    private static final Pattern COLOR_CODES = Pattern.compile("(?i)§[0-9A-FK-OR]");
    private static final Pattern VALID_NICK = Pattern.compile("^[A-Za-z0-9_]{3,16}$");

    private static final Set<String> RANKS = new HashSet<>(Arrays.asList(
            "HT5", "LT5", "HT4", "LT4", "HT3", "LT3", "HT2", "LT2", "HT1", "LT1",
            "RHT3", "RLT3", "RHT2", "RLT2", "RHT1", "RLT1",
            "XHT5", "XLT5", "XHT4", "XLT4", "XHT3", "XLT3", "XHT2", "XLT2", "XHT1", "XLT1",
            "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"
    ));

    private static final Set<String> SERVER_WORDS = new HashSet<>(Arrays.asList(
            "anarchy-alpha", "anarchy-beta", "anarchy-gamma", "anarchy-new", "duels"
    ));

    private ChatNicknameParser() {}

    public static Optional<String> parse(String rawMessage) {
        if (rawMessage == null || rawMessage.isBlank()) return Optional.empty();

        String message = COLOR_CODES.matcher(rawMessage).replaceAll("");
        message = message.replace('»', ' ')
                .replace('«', ' ')
                .replace('→', ' ')
                .replace('←', ' ')
                .replace(':', ' ')
                .replace('|', ' ');

        String[] tokens = message.split("\\s+");
        boolean forceNextValidNick = false;

        for (String rawToken : tokens) {
            String token = cleanToken(rawToken);
            if (token.isBlank()) continue;

            String upper = token.toUpperCase(Locale.ROOT);
            String lower = token.toLowerCase(Locale.ROOT);

            if (RANKS.contains(upper) || SERVER_WORDS.contains(lower)) {
                forceNextValidNick = true;
                continue;
            }

            if (VALID_NICK.matcher(token).matches()) {
                return Optional.of(token);
            }

            if (forceNextValidNick) {
                String onlyNickChars = token.replaceAll("[^A-Za-z0-9_]", "");
                if (VALID_NICK.matcher(onlyNickChars).matches()) {
                    return Optional.of(onlyNickChars);
                }
            }
        }
        return Optional.empty();
    }

    public static String cleanToken(String token) {
        if (token == null) return "";
        return token
                .replaceAll("^[\\[\\](){}/<>*_~.,;!?'\"`#@+=\\-]+", "")
                .replaceAll("[\\[\\](){}/<>*_~.,;!?'\"`#@+=\\-]+$", "");
    }

    public static boolean isValidNick(String nick) {
        return nick != null && VALID_NICK.matcher(nick).matches();
    }
}
