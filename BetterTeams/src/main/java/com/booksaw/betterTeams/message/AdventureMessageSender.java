package com.booksaw.betterTeams.message;

import java.util.Collection;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;

final class AdventureMessageSender implements MessageSender {

    AdventureMessageSender() {}

    @Override
    public void sendMessage(@NotNull CommandSender recipient, @NotNull Component message) {
        recipient.sendMessage(message);
    }

    @Override
    public void sendMessage(@NotNull Collection<? extends CommandSender> recipients, @NotNull Component message) {
        recipients.forEach(r -> r.sendMessage(message));
    }

    @Override
    public void sendTitle(@NotNull Player recipient, @NotNull Component title) {
        recipient.showTitle(Title.title(title, Component.empty()));
    }

    @Override
    public void sendTitle(@NotNull Collection<Player> recipients, @NotNull Component title) {
        Title t = Title.title(title, Component.empty());
        recipients.forEach(p -> p.showTitle(t));
    }

    @Override
    public void sendSubTitle(@NotNull Player recipient, @NotNull Component subtitle) {
        recipient.showTitle(Title.title(Component.empty(), subtitle));
    }

    @Override
    public void sendSubTitle(@NotNull Collection<Player> recipients, @NotNull Component subtitle) {
        Title t = Title.title(Component.empty(), subtitle);
        recipients.forEach(p -> p.showTitle(t));
    }

    @Override
    public void sendTitleAndSub(@NotNull Player recipient, @NotNull Component title, @NotNull Component subtitle) {
        recipient.showTitle(Title.title(title, subtitle));
    }

    @Override
    public void sendTitleAndSub(@NotNull Collection<Player> recipients, @NotNull Component title, @NotNull Component subtitle) {
        Title t = Title.title(title, subtitle);
        recipients.forEach(p -> p.showTitle(t));
    }

    public void sendActionBar(@NotNull Player recipient, @NotNull Component message) {
        recipient.sendActionBar(message);
    }

    public void sendActionBar(@NotNull Collection<Player> recipients, @NotNull Component message) {
        recipients.forEach(p -> p.sendActionBar(message));
    }
}
