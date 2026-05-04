package com.booksaw.betterTeams.extension;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.net.URLClassLoader;

@RequiredArgsConstructor
@Getter
public class ExtensionWrapper {

 /**
 * Holds metadata about the extension.
 */
	private final ExtensionInfo info;

 /**
 * The actual instance of the extension's main class.
 */
	private final BetterTeamsExtension instance;

 /**
 * The classloader used to load this extension, isolating its dependencies.
 */
	private final URLClassLoader classLoader;

	@Setter
	private boolean enabled = false;
}

