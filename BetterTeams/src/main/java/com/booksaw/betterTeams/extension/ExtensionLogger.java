package com.booksaw.betterTeams.extension;

import org.jetbrains.annotations.NotNull;

import java.util.logging.Level;
import java.util.logging.Logger;

public record ExtensionLogger(Logger logger, String prefix) {
	public ExtensionLogger(@NotNull Logger logger, @NotNull String prefix) {
		this.logger = logger;
		this.prefix = "[" + prefix + "] ";
	}

	public void info(@NotNull String msg) {
		logger.info(prefix + msg);
	}

	public void warning(@NotNull String msg) {
		logger.warning(prefix + msg);
	}

	public void severe(@NotNull String msg) {
		logger.severe(prefix + msg);
	}

	public void log(@NotNull Level level, @NotNull String msg) {
		logger.log(level, prefix + msg);
	}

	public void log(@NotNull Level level, @NotNull String msg, @NotNull Throwable thrown) {
		logger.log(level, prefix + msg, thrown);
	}

}
