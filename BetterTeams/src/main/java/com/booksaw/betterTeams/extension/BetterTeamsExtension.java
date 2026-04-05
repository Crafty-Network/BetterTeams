package com.booksaw.betterTeams.extension;

import com.booksaw.betterTeams.ConfigManager;
import com.booksaw.betterTeams.Main;
import com.booksaw.betterTeams.message.MessageManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.logging.Level;

public abstract class BetterTeamsExtension {

	private ExtensionInfo info;
	private File dataFolder;
	private Main plugin;
	private ExtensionLogger extensionLogger;

	private ConfigManager configManager;
	private ExtensionMessages extensionMessages;

	public void onEnable() {}

	public void onDisable() {}

	public void onLoad() {}

	public final void selfDisable() {
		plugin.getExtensionManager().unloadExtension(this);
	}

	@NotNull
	public final Main getPlugin() {
		return this.plugin;
	}

	@NotNull
	public final ExtensionInfo getInfo() {
		return this.info;
	}

	@NotNull
	public final ExtensionLogger getLogger() {
		if (extensionLogger == null) {
			return new ExtensionLogger(plugin.getLogger(), info.getName());
		}
		return extensionLogger;
	}

	@NotNull
	public final File getDataFolder() {
		return this.dataFolder;
	}

	@NotNull
	public ConfigManager getConfig() {
		if (configManager == null) {
			reloadConfig();
		}
		return configManager;
	}

	@NotNull
	public ExtensionMessages getMessages()  {
		String lang = MessageManager.getLanguage();
		return getMessages(lang);
	}

	@NotNull
	public ExtensionMessages getMessages(@NotNull String fileName)  {
		if (extensionMessages == null) {
			extensionMessages = new ExtensionMessages(this, fileName);
		} else if (!extensionMessages.getFileName().equals(fileName)) {
			extensionMessages.reload(fileName);
		}
		return extensionMessages;
	}

	public void reloadConfig() {
		configManager = new ConfigManager("config", true, this);
	}

	public void reloadMessages() {
		if (extensionMessages != null) {
			extensionMessages.reload();
		}
	}

	public void reloadMessages(@NotNull String fileName) {
		if (extensionMessages != null) {
			extensionMessages.reload(fileName);
		} else {
			extensionMessages = new ExtensionMessages(this, fileName);
		}
	}

	public void saveConfig() {
		if (configManager != null) {
			configManager.save(false);
		}
	}

	public void saveResource(@NotNull String resourcePath, boolean replace) {
		if (resourcePath.isEmpty()) {
			throw new IllegalArgumentException("ResourcePath cannot be empty");

		}
		resourcePath = resourcePath.replace('\\', '/');
		String cleanPath = resourcePath.startsWith("/") ? resourcePath.substring(1) : resourcePath;

		try (InputStream in = getResource(cleanPath)) {
			if (in == null) {
				throw new IllegalArgumentException("Resource '" + cleanPath + "' not found in " + getInfo().getName());
			}

			File outFile = new File(dataFolder, cleanPath);
			File outDir = outFile.getParentFile();
			if (outDir != null && !outDir.exists()) {
				outDir.mkdirs();
			}
			if (!outFile.exists() || replace) {
				Files.copy(in, outFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
			}
		} catch (IOException ex) {
			getLogger().log(Level.SEVERE, "Could not save resource " + cleanPath, ex);
		}
	}

	@Nullable
	public InputStream getResource(@NotNull String filename) {
		if (filename.isEmpty()) {
			throw new IllegalArgumentException("ResourcePath cannot be empty");
		}
		String path = filename.startsWith("/") ? filename.substring(1) : filename;
		URL url = null;

		if (getClass().getClassLoader() instanceof ExtensionClassLoader extLoader) {
			url = extLoader.getLocalResource(path);
		}
		if (url == null) return null;

		try {
			URLConnection connection = url.openConnection();
			connection.setUseCaches(false);
			return connection.getInputStream();
		} catch (IOException e) {
			getLogger().log(Level.WARNING, "Could not load resource: " + path, e);
			return null;
		}
	}

	protected final void init(ExtensionInfo info, File dataFolder, Main plugin) {
		this.info = info;
		this.dataFolder = dataFolder;
		this.plugin = plugin;

		this.extensionLogger = new ExtensionLogger(plugin.getLogger(), info.getName());
	}
}

