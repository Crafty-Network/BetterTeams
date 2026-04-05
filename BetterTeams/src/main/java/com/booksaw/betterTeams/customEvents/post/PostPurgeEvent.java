package com.booksaw.betterTeams.customEvents.post;

import com.booksaw.betterTeams.customEvents.PurgeEvent;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class PostPurgeEvent extends Event {

	private static final HandlerList HANDLERS = new HandlerList();

	public static HandlerList getHandlerList() {
		return HANDLERS;
	}

	public PostPurgeEvent() {
		super(true);
	}

	@Override
	public @NotNull HandlerList getHandlers() {
		return HANDLERS;
	}
}
