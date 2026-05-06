package ru.wqkcpf.moderationhelper.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import ru.wqkcpf.moderationhelper.ModerationHelperClient;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ModConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("moderation-helper-gui.json");
    private static final int CURRENT_RULES_PRESET_VERSION = 3;

    public int rulesPresetVersion = CURRENT_RULES_PRESET_VERSION;

    public boolean obsEnabled = true;
    public String obsHost = "localhost";
    public int obsPort = 4455;
    public String obsPassword = "";

    public int recentPlayersLimit = 15;
    public String screenshotCleanupMode = "DELETE"; // DELETE / ARCHIVE / OFF
    public int screenshotRetentionDays = 30;
    public String screenshotFolder = "moderation_screenshots";

    public String checkCommandTemplate = "/check {nick}";
    public String checkTellTemplate = "/tell {nick} Здравствуйте, проверка на читы. В течении 5 минут жду ваш Anydesk(наилучший вариант, скачать можно в любом браузере)/Discord. Также сообщаю, что в случае признания на наличие чит-клиентов срок бана составит 20 дней, вместо 30.";

    public List<ReasonOption> quickReasons = defaultReasons();

    public static ModConfig load() {
        try {
            if (Files.notExists(CONFIG_PATH)) {
                ModConfig created = new ModConfig();
                created.save();
                return created;
            }
            try (Reader reader = Files.newBufferedReader(CONFIG_PATH, StandardCharsets.UTF_8)) {
                ModConfig config = GSON.fromJson(reader, ModConfig.class);
                if (config == null) {
                    config = new ModConfig();
                }

                // Если у игрока уже был старый конфиг из прошлой версии, обновляем набор правил.
                if (config.quickReasons == null || config.quickReasons.isEmpty()
                        || config.rulesPresetVersion < CURRENT_RULES_PRESET_VERSION) {
                    config.quickReasons = defaultReasons();
                    config.rulesPresetVersion = CURRENT_RULES_PRESET_VERSION;
                    config.save();
                }
                return config;
            }
        } catch (Exception e) {
            ModerationHelperClient.LOGGER.error("Cannot load config, using defaults", e);
            return new ModConfig();
        }
    }

    public void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(CONFIG_PATH, StandardCharsets.UTF_8)) {
                GSON.toJson(this, writer);
            }
        } catch (IOException e) {
            ModerationHelperClient.LOGGER.error("Cannot save config", e);
        }
    }

    public static List<ReasonOption> defaultReasons() {
        List<ReasonOption> list = new ArrayList<>();

        // Предупреждения
        list.add(rule("2.1", "предупреждение", List.of(), "warn"));

        // Муты: отдельная категория причин для /mute {nick} {duration} {reason}
        list.add(rule("2.2", "Запрещено оскорблять кого-либо/что-либо", List.of("1h", "12h", "1d", "7d"), "mute"));
        list.add(rule("2.3", "Запрещено оскорблять родных", List.of("1d", "5d", "15d"), "mute"));
        list.add(rule("2.4", "Запрещены сообщения сексуального характера", List.of("2h"), "mute"));
        list.add(rule("2.5", "Запрещено неадекватное поведение", List.of("2h"), "mute"));
        list.add(rule("2.6", "Запрещена реклама любых серверов и сторонних ресурсов", List.of("1d"), "mute"));
        list.add(rule("2.7", "Запрещена пропаганда или агитация, возбуждающая социальную, половую, расовую, национальную или религиозную ненависть и вражду", List.of("9h"), "mute"));
        list.add(rule("2.8", "Запрещено отправлять сообщения с наличием сторонних ссылок/рекламой стримов и видео", List.of("8h", "3d"), "mute"));
        list.add(rule("2.9", "Запрещена выдача себя за игрового администратора сервера", List.of("12h"), "mute"));
        list.add(rule("2.10", "Запрещены угрозы, не относящиеся к игровому процессу", List.of("12h"), "mute"));
        list.add(rule("2.11", "Запрещено угрожать наказанием без реальной причины", List.of("6h"), "mute"));
        list.add(rule("2.12", "Запрещено обсуждать политику", List.of("12h", "7d"), "mute"));
        list.add(rule("2.13", "Запрещено введение игроков в заблуждение", List.of("2h"), "mute"));
        list.add(rule("2.14", "Запрещено попрошайничество ресурсов/привилегий/чего-либо у администрации", List.of("6h"), "mute"));
        list.add(rule("2.15", "Запрещены помехи в голосовом чате", List.of("4h"), "mute"));

        // Баны: отдельная категория причин для /ban {nick} {duration} {reason}
        list.add(rule("2.2", "Запрещено оскорблять кого-либо/что-либо", List.of("1d", "7d", "2d"), "ban"));
        list.add(rule("2.3", "Запрещено оскорблять родных", List.of("3d", "7d"), "ban"));
        list.add(rule("2.6", "Запрещена реклама любых серверов и сторонних ресурсов", List.of("1d", "14d"), "ban"));
        list.add(rule("2.7", "Запрещена пропаганда или агитация, возбуждающая социальную, половую, расовую, национальную или религиозную ненависть и вражду", List.of("3d"), "ban"));
        list.add(rule("3.1", "Запрещены никнеймы, содержащие слова, нарушающие другие пункты правил, оскорбительного/нецензурного характера, названия других проектов и читов", List.of("perm"), "ban"));
        list.add(rule("4.1", "Донатеры обязаны иметь доказательства при выдаче наказания", List.of("20d", "30d", "15d", "5d"), "ban"));

        // IP-баны: отдельная категория причин для /ipban {nick} {duration} {reason}
        list.add(rule("3.3", "Запрещена любая торговля за реальные деньги", List.of("30d"), "ipban"));
        list.add(rule("3.6", "Запрещено использование багов сервера, постройка лаг-машин", List.of("1d", "15d"), "ipban"));
        list.add(rule("3.7", "Запрещено использование/хранение стороннего ПО", List.of("30d", "20d", "15d", "7d"), "ipban"));
        list.add(rule("3.8", "Запрещено обходить бан за читы, играя на другом аккаунте, когда один из аккаунтов забанен менее 30 дней назад", List.of("30d"), "ipban"));
        list.add(rule("3.9", "Запрещено подстрекательство игроков на нарушение правил", List.of("3d"), "ipban"));
        list.add(rule("3.10", "Тим с игроком, использующим стороннее ПО", List.of("15d"), "ipban"));

        return list;
    }

    private static ReasonOption rule(String code, String description, List<String> durations, String punishment) {
        return new ReasonOption(code, description, durations, List.of(punishment));
    }

    public record ReasonOption(String code, String description, List<String> durationSuggestions, List<String> punishments) {
        public String buttonText() {
            if (durationSuggestions == null || durationSuggestions.isEmpty()) {
                return code + " — " + description;
            }
            return code + " — " + description + " [" + String.join(", ", durationSuggestions) + "]";
        }

        public String firstDuration() {
            if (durationSuggestions == null || durationSuggestions.isEmpty()) return "";
            return durationSuggestions.get(0);
        }

        public boolean supports(String punishment) {
            if (punishments == null || punishment == null) return false;
            String normalized = punishment.toLowerCase(Locale.ROOT);
            for (String value : punishments) {
                if (normalized.equals(value.toLowerCase(Locale.ROOT))) {
                    return true;
                }
            }
            return false;
        }
    }
}
