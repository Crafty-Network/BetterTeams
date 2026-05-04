
package dev.ceymikey.json;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

public class JsonElement {

	protected @NotNull String serializeObject(@NotNull Map<String, Object> map) {
		StringBuilder sb = new StringBuilder();
		sb.append("{");

		boolean first = true;
		for (Map.Entry<String, Object> entry : map.entrySet()) {
			if (!first) {
				sb.append(",");
			}
			first = false;

			sb.append("\"").append(entry.getKey()).append("\":");
			sb.append(serializeValue(entry.getValue()));
		}

		sb.append("}");
		return sb.toString();
	}

	protected String serializeValue(Object value) {
		if (value == null) {
			return "null";
		} else if (value instanceof String) {
			return "\"" + escapeString((String) value) + "\"";
		} else if (value instanceof Number || value instanceof Boolean) {
			return value.toString();
		} else if (value instanceof JsonObject) {
			return value.toString();
		} else if (value instanceof JsonArray) {
			return value.toString();
		} else if (value instanceof Map) {
			@SuppressWarnings("unchecked")
			Map<String, Object> map = (Map<String, Object>) value;
			return serializeObject(map);
		} else if (value instanceof List) {
			@SuppressWarnings("unchecked")
			List<Object> list = (List<Object>) value;
			return serializeArray(list);
		} else {
			return "\"" + escapeString(value.toString()) + "\"";
		}
	}

	protected @NotNull String serializeArray(@NotNull List<Object> list) {
		StringBuilder sb = new StringBuilder();
		sb.append("[");

		boolean first = true;
		for (Object item : list) {
			if (!first) {
				sb.append(",");
			}
			first = false;

			sb.append(serializeValue(item));
		}

		sb.append("]");
		return sb.toString();
	}

	protected @NotNull String escapeString(@NotNull String input) {
		return input.replace("\\", "\\\\")
				.replace("\"", "\\\"")
				.replace("\n", "\\n")
				.replace("\r", "\\r")
				.replace("\t", "\\t");
	}
}
