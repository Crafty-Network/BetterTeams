package com.booksaw.betterTeams.extension;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.net.URLClassLoader;

@RequiredArgsConstructor
@Getter
public class ExtensionWrapper {

	private final ExtensionInfo info;

	private final BetterTeamsExtension instance;

	private final URLClassLoader classLoader;

	@Setter
	private boolean enabled = false;
}

