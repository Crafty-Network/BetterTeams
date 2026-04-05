package com.booksaw.betterTeams.extension;

import com.booksaw.betterTeams.CommandResponse;
import com.booksaw.betterTeams.message.MessageConfig;
import com.booksaw.betterTeams.message.MessageService;
import com.booksaw.betterTeams.message.StaticMessage;
import lombok.Getter;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

@Getter
public class ExtensionMessages {

    private final MessageService messageService;
    private final BetterTeamsExtension extension;
    private String fileName;

    public ExtensionMessages(@NotNull BetterTeamsExtension extension, @NotNull String fileName) {
        this.extension = extension;
        this.fileName = fileName;

        MessageConfig config = new MessageConfig(fileName, extension);

        this.messageService = new MessageService(config);
        this.messageService.setupMessageSender();

        if (config.has("prefix")) {
            this.messageService.loadPrefix("prefix");
        } else {
            this.messageService.setPrefix(extension.getInfo().getName());
        }
    }

    public void reload(@NotNull String fileName) {
        if (fileName.isEmpty()) throw new IllegalArgumentException("File name cannot be empty");
        this.fileName = fileName;
        this.messageService.reload(fileName);

        if (has("prefix")) {
            this.messageService.loadPrefix("prefix");
        }
    }

    public void reload() {
        reload(this.fileName);
    }

    @NotNull
    public String get(@NotNull String path) {
        return messageService.getMessage(path);
    }

    @NotNull
    public String get(@NotNull String path, @NotNull Object... replacements) {
        return messageService.getMessage(path, replacements);
    }

    @NotNull
    public String getPrefix() {
        return messageService.getPrefix();
    }

    @NotNull
    public String getWithPrefix(@NotNull String path) {
        return getPrefix() + get(path);
    }

    @NotNull
    public String getWithPrefix(@NotNull String path, @NotNull Object... replacements) {
        return getPrefix() + get(path, replacements);
    }

    @NotNull
    public StaticMessage toStatic(@NotNull String path) {
        return new StaticMessage(getWithPrefix(path));
    }

    @NotNull
    public StaticMessage toStatic(@NotNull String path, @NotNull Object... replacements) {
        return new StaticMessage(getWithPrefix(path, replacements));
    }

    @NotNull
    public StaticMessage toStaticRaw(@NotNull String path) {
        return new StaticMessage(get(path));
    }

    @NotNull
    public StaticMessage toStaticRaw(@NotNull String path, @NotNull Object... replacements) {
        return new StaticMessage(get(path, replacements));
    }

    @NotNull
    public CommandResponse response(boolean success, @NotNull String path) {
        return new CommandResponse(success, toStatic(path));
    }

    @NotNull
    public CommandResponse response(boolean success, @NotNull String path, @NotNull Object... replacements) {
        return new CommandResponse(success, toStatic(path, replacements));
    }

    public void send(@NotNull CommandSender sender, @NotNull String path) {
        messageService.sendMessage(sender, true, path);
    }

    public void send(@NotNull CommandSender sender, @NotNull String path, @NotNull Object... replacements) {
        messageService.sendMessage(sender, true, path, replacements);
    }

    public void send(@NotNull Collection<? extends CommandSender> recipients, @NotNull String path) {
        messageService.sendMessage(recipients, true, path);
    }

    public void send(@NotNull Collection<? extends CommandSender> recipients, @NotNull String path, @NotNull Object... replacements) {
        messageService.sendMessage(recipients, true, path, replacements);
    }

    public void sendRaw(@NotNull CommandSender sender, @NotNull String path) {
        messageService.sendMessage(sender, false, path);
    }

    public void sendRaw(@NotNull CommandSender sender, @NotNull String path, @NotNull Object... replacements) {
        messageService.sendMessage(sender, false, path, replacements);
    }

    public boolean has(@NotNull String path) {
        return messageService.getMessageConfig().has(path);
    }

    public void clearCache() {
        messageService.getMessageConfig().clearCache();
    }

    public int getCacheSize() {
        return messageService.getMessageConfig().getCacheSize();
    }

    public MessageConfig.MessageBuilder builder(String path) {
        return messageService.getMessageConfig().builder(path);
    }
}