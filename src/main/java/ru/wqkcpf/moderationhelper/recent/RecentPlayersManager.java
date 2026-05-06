package ru.wqkcpf.moderationhelper.recent;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;
import ru.wqkcpf.moderationhelper.ModerationHelperClient;
import ru.wqkcpf.moderationhelper.config.ModConfig;

import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RecentPlayersManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type LIST_TYPE = new TypeToken<List<String>>() {}.getType();
    private final Path path = FabricLoader.getInstance().getConfigDir().resolve("moderation-helper-recent.json");
    private final ModConfig config;
    private final List<String> players = new ArrayList<>();

    public RecentPlayersManager(ModConfig config) {
        this.config = config;
    }

    public void load() {
        try {
            if (Files.notExists(path)) return;
            try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                List<String> loaded = GSON.fromJson(reader, LIST_TYPE);
                if (loaded != null) {
                    players.clear();
                    for (String nick : loaded) add(nick, false);
                }
            }
        } catch (Exception e) {
            ModerationHelperClient.LOGGER.warn("Cannot load recent players", e);
        }
    }

    public void add(String nick) {
        add(nick, true);
    }

    private void add(String nick, boolean save) {
        if (nick == null || nick.isBlank()) return;
        players.removeIf(p -> p.equalsIgnoreCase(nick));
        players.add(0, nick);
        while (players.size() > Math.max(1, config.recentPlayersLimit)) {
            players.remove(players.size() - 1);
        }
        if (save) save();
    }

    public List<String> getPlayers() {
        return Collections.unmodifiableList(players);
    }

    public void save() {
        try {
            Files.createDirectories(path.getParent());
            try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
                GSON.toJson(players, writer);
            }
        } catch (Exception e) {
            ModerationHelperClient.LOGGER.warn("Cannot save recent players", e);
        }
    }
}
