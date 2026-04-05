package com.booksaw.betterTeams.extension;
import lombok.*;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

@Getter
@ToString
@EqualsAndHashCode
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class ExtensionInfo {
	
	private final String name;

	private final String mainClass;

	private final String version;

	private final String author;

	private final String description;

	private final String website;

	private final List<String> extensionDepend;

	private final List<String> extensionSoftDepend;

	private final List<String> pluginDepend;

	private final List<String> pluginSoftDepend;

	private final File jarFile;

	public String getDisplayName() {
		String trimmedAuthor = (author != null) ? author.trim() : "";
		return name.trim() + " v" + version.trim() +
				(trimmedAuthor.isEmpty() ? "" : " (author: " + trimmedAuthor + ")");
	}

	public static ExtensionInfo fromYaml(File file) throws IOException {
		if (file == null || !file.exists()) {
			throw new IOException("JAR file not found or invalid: " + (file != null ? file.getAbsolutePath() : "null"));
		}

		try (JarFile jarFile = new JarFile(file)) {
			JarEntry entry = jarFile.getJarEntry("extension.yml");
			if (entry == null) {
				throw new IOException("extension.yml not found in JAR: " + file.getName());
			}

			try (InputStream yamlStream = jarFile.getInputStream(entry);
				 InputStreamReader reader = new InputStreamReader(yamlStream, StandardCharsets.UTF_8)) {
				YamlConfiguration yml = YamlConfiguration.loadConfiguration(reader);

				String name = yml.getString("name", "").trim();
				String main = yml.getString("main", "").trim();
				String ver = yml.getString("version", "1.0").trim();
				String author = yml.getString("author", "").trim();
				String desc = yml.getString("description", "").trim();
				String site = yml.getString("website", "").trim();

				List<String> eHard = yml.getStringList("depend");
				List<String> eSoft = yml.getStringList("softdepend");
				List<String> pHard = yml.getStringList("plugin-depend");
				List<String> pSoft = yml.getStringList("plugin-softdepend");

				if (main.isEmpty()) {
					throw new IllegalArgumentException("No 'main' specified in extension.yml");
				}
				if (name.isEmpty()) {
					throw new IllegalArgumentException("No 'name' specified in extension.yml");
				}
				if (jarFile.getJarEntry(main.replace('.', '/') + ".class") == null) {
					throw new IllegalArgumentException("Main class not found in JAR: " + main);
				}
				return new ExtensionInfo(name, main, ver, author, desc, site, eHard, eSoft, pHard, pSoft, file);
			}
		}
	}
}
