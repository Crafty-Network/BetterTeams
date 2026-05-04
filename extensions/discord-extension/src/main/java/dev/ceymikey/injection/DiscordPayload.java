
package dev.ceymikey.injection;

import com.booksaw.betterTeams.Main;
import dev.ceymikey.exceptions.FailedEndpointException;
import dev.ceymikey.exceptions.InjectionFailureException;
import dev.ceymikey.json.JsonArray;
import dev.ceymikey.json.JsonObject;
import org.jetbrains.annotations.NotNull;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class DiscordPayload {
	private DiscordPayload() {
	}

	public static void inject(@NotNull EmbedBuilder builder) {
		if (builder.getUrl() == null || builder.getUrl().isEmpty()) {
			throw new FailedEndpointException();
		}

		if ((builder.getTitle() == null || builder.getTitle().isEmpty())
				&& (builder.getDescription() == null || builder.getDescription().isEmpty())
				&& (builder.getFields() == null || builder.getFields().isEmpty())) {
			throw new InjectionFailureException();
		}

		HttpURLConnection connection = null;

		try {
			
			JsonObject payload = getPayload(builder);

			URL url = new URL(builder.getUrl());
			connection = (HttpURLConnection) url.openConnection();
			connection.setRequestMethod("POST");
			connection.setRequestProperty("Content-Type", "application/json");
			connection.setDoOutput(true);

			byte[] payloadBytes = payload.toString().getBytes(StandardCharsets.UTF_8);
			connection.setFixedLengthStreamingMode(payloadBytes.length);

			try (OutputStream os = connection.getOutputStream()) {
				os.write(payloadBytes);
				os.flush();
			}

			int responseCode = connection.getResponseCode();
			if (responseCode < 200 || responseCode >= 300) {
				Main.plugin.getLogger().severe("Could not send webhook. HTTP Error: " + responseCode);
			}

		} catch (Exception e) {
			Main.plugin.getLogger().severe("Could not send webhook. INJECTION FAILURE! | " + e.getMessage());
			e.printStackTrace();
		} finally {
			if (connection != null) {
				connection.disconnect();
			}
		}
	}

	private static @NotNull JsonObject getPayload(@NotNull EmbedBuilder builder) {
		JsonObject embed = new JsonObject();
		embed.put("title", builder.getTitle());
		embed.put("description", builder.getDescription());
		embed.put("color", builder.getColor());

		if (builder.getThumbnailUrl() != null && !builder.getThumbnailUrl().isEmpty()) {
			JsonObject thumbnail = new JsonObject();
			thumbnail.put("url", builder.getThumbnailUrl());
			embed.put("thumbnail", thumbnail);
		}

		JsonArray fieldsArray = new JsonArray();
		for (EmbedBuilder.Field field : builder.getFields()) {
			JsonObject fieldObject = new JsonObject();
			fieldObject.put("name", field.name);
			fieldObject.put("value", field.value);
			fieldsArray.put(fieldObject);
		}
		embed.put("fields", fieldsArray);

		if (builder.getFooterText() != null && !builder.getFooterText().isEmpty()) {
			JsonObject footer = new JsonObject();
			footer.put("text", builder.getFooterText());
			embed.put("footer", footer);
		}

		JsonObject payload = new JsonObject();
		payload.put("embeds", new JsonArray(embed));
		return payload;
	}
}