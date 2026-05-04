package com.booksaw.betterTeams.message;

import com.booksaw.betterTeams.ConfigManager;
import com.booksaw.betterTeams.Main;
import com.booksaw.betterTeams.extension.BetterTeamsExtension;
import com.booksaw.betterTeams.util.StringUtil;
import lombok.Getter;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
* Creates a MessageConfig for the main plugin.
*
* @param language The language code to load
*/
public class MessageConfig {

    private static final String MISSING_MESSAGES_SUFFIX = "_missingmessages.txt";

    @Getter
    private final File dataFolder;
    private final Logger logger;

    @Getter
    private final String sourceName;

    @Getter
    private String language;

    private final Map<String, String> cache;

    @Getter
    private ConfigManager configManager;

    @Nullable
    private final BetterTeamsExtension extension;

    /**
    * Creates a MessageConfig for an extension.
    *
    * @param language  The language code to load
    * @param extension The extension this config belongs to
    */
    public MessageConfig(@NotNull String language) {
        this(language, Main.plugin.getDataFolder(), Main.plugin.getLogger(), "BetterTeams", null);
    }

    /**
    *
    * @param language   The language code to load
    * @param dataFolder The folder to load/save config files
    * @param logger     The logger to use
    * @param sourceName The name of the source (for logging)
    * @param extension  The extension (null for main plugin)
    */
    public MessageConfig(@NotNull String language, @NotNull BetterTeamsExtension extension) {
        this(language, extension.getDataFolder(), extension.getLogger().logger(), extension.getInfo().getName(), extension);
    }

    public MessageConfig(@NotNull String language, @NotNull File dataFolder, @NotNull Logger logger,
                         @NotNull String sourceName, @Nullable BetterTeamsExtension extension) {
        this.language = language;
        this.dataFolder = dataFolder;
        this.logger = logger;
        this.sourceName = sourceName;
        this.extension = extension;
        this.cache = new ConcurrentHashMap<>();

        reload();
    }

    public void reload() {
        reload(this.language);
    }

    public void reload(@NotNull String language) {
        if (language.isEmpty()) {
            throw new IllegalArgumentException("Language cannot be empty");
        }

        this.language = language;
        clearCache();

        this.configManager = new ConfigManager(language, true, extension);

        /**
        * Loads messages from a configuration file into the cache.
        *
        * @param file   The configuration file to load from
        * @param backup Whether this is a backup/fallback file
        */
        loadMessages(configManager.getConfig(), false);

        logger.info("[" + sourceName + "] Messages loaded from " + language + ".yml");
    }

    private void loadMessages(@NotNull FileConfiguration file, boolean backup) {
        List<String> missingMessages = new ArrayList<>();

        for (String key : file.getKeys(true)) {
            if (!cache.containsKey(key)) {
                String value = file.getString(key);
                if (value == null || file.get(key) instanceof ConfigurationSection) {
                    continue;
                }

                cache.put(key, value);

                if (backup) {
                    missingMessages.add(key);
                }
            }
        }

        if (!missingMessages.isEmpty()) {
            /**
            * Saves missing messages to a file for translation contribution.
            *
            * @param missingMessages List of missing message keys
            */
            saveMissingMessages(missingMessages);
            /**
            * Logs missing messages to the console.
            *
            * @param missingMessages List of missing message keys
            */
            logMissingMessages(missingMessages);
        }
    }

    /**
    * Loads backup/fallback messages from a YamlConfiguration.
    * Messages that are already loaded will not be overwritten.
    *
    * @param file The backup configuration file
    */
    public void loadBackupMessages(@NotNull YamlConfiguration file) {
        loadMessages(file, true);
    }

    private void saveMissingMessages(@NotNull List<String> missingMessages) {
        String fileName = language + MISSING_MESSAGES_SUFFIX;
        File file = new File(dataFolder, fileName);

        List<String> existingKeys = new ArrayList<>();

        if (file.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.isEmpty() || line.startsWith("#")) {
                        continue;
                    }
                    String[] split = line.split(": ", 2);
                    if (split.length == 2) {
                        existingKeys.add(split[0]);
                    }
                }
            } catch (IOException e) {
                logger.log(Level.WARNING, "Failed to read existing missing messages file", e);
                return;
            }
        } else {
            try {
                if (!file.getParentFile().exists()) {
                    file.getParentFile().mkdirs();
                }
                file.createNewFile();
            } catch (IOException e) {
                logger.log(Level.WARNING, "Failed to create missing messages file", e);
                return;
            }
        }

        try (PrintWriter writer = new PrintWriter(new FileWriter(file, !existingKeys.isEmpty()))) {
            if (existingKeys.isEmpty()) {
                writer.println("# Please translate these messages and submit them to the Booksaw Development Discord");
                writer.println("# (https://discord.gg/betterteams)");
                writer.println("# Your translations will be included in the next update");
                writer.println("# When done translating, run '/teama importmessages' to include the translated messages");
                writer.println();
            }

            for (String key : missingMessages) {
                if (!existingKeys.contains(key)) {
                    writer.println(key + ": " + cache.get(key));
                }
            }
        } catch (IOException e) {
            logger.log(Level.WARNING, "Failed to save missing messages", e);
        }
    }

    private void logMissingMessages(@NotNull List<String> missingMessages) {
        logger.info("==================================================================");
        logger.info("Messages are missing from your selected language (" + language + ").");
        logger.info("The following messages are using English fallback:");

        for (String key : missingMessages) {
            logger.info("- " + key + ": " + cache.get(key));
        }

        logger.info("");
        logger.info("If you can help with translation, please join the Discord server:");
        logger.info("https://discord.gg/betterteams");
        logger.info("");
        logger.info("A file called '" + language + MISSING_MESSAGES_SUFFIX + "' has been created");
        logger.info("in the " + sourceName + " folder. Translate the messages and submit to Discord!");
        logger.info("==================================================================");
    }

    @NotNull
    public String get(@NotNull String path) {
        String cached = cache.get(path);
        if (cached != null) {
            return cached;
        }

        logger.warning("[" + sourceName + "] Could not find message with key: " + path);
        return "";
    }

    @NotNull
    public String get(@NotNull String path, @NotNull Object... replacements) {
        return StringUtil.setPlaceholders(get(path), replacements);
    }

    @NotNull
    public String get(@Nullable OfflinePlayer player, @NotNull String path, @NotNull Object... replacements) {
        return StringUtil.setPlaceholders(player, get(path), replacements);
    }

    @NotNull
    public String getWithNamedPlaceholders(@NotNull String path, @NotNull Object... replacements) {
        String message = get(path);
        if (replacements.length >= 2) {
            for (int i = 0; i < replacements.length - 1; i += 2) {
                String key = String.valueOf(replacements[i]);
                String value = String.valueOf(replacements[i + 1]);
                message = message.replace("{" + key + "}", value);
            }
        }
        return message;
    }

    public void clearCache() {
        cache.clear();
    }

    public int getCacheSize() {
        return cache.size();
    }

    public boolean has(@NotNull String path) {
        return cache.containsKey(path);
    }

    @NotNull
    public Map<String, String> getAllMessages() {
        return Map.copyOf(cache);
    }

    @NotNull
    public File getFile() {
        return new File(dataFolder, language + ".yml");
    }

    @NotNull
    public FileConfiguration getConfig() {
        return configManager.getConfig();
    }

    @NotNull
    public MessageBuilder builder(@NotNull String path) {
        return new MessageBuilder(this, path);
    }

    public static class MessageBuilder {

        private final MessageConfig config;
        private String text;

        public MessageBuilder(@NotNull MessageConfig config, @NotNull String path) {
            this.config = config;
            this.text = config.get(path);
        }

        @NotNull
        public MessageBuilder with(@NotNull String key, @Nullable Object value) {
            this.text = this.text.replace("{" + key + "}", String.valueOf(value));
            return this;
        }

        @NotNull
        public MessageBuilder withAll(@NotNull Map<String, Object> placeholders) {
            for (Map.Entry<String, Object> entry : placeholders.entrySet()) {
                with(entry.getKey(), entry.getValue());
            }
            return this;
        }

        @NotNull
        public MessageBuilder withPAPI(@Nullable Player player) {
            if (player != null) {
                this.text = StringUtil.setPlaceholders(player, this.text);
            }
            return this;
        }

        @NotNull
        public String build() {
            return text;
        }

        @NotNull
        public MessageConfig getConfig() {
            return config;
        }
    }
}