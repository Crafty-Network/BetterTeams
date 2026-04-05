
package dev.ceymikey.json;

import java.util.LinkedHashMap;
import java.util.Map;

public class JsonObject extends JsonElement {
	private final Map<String, Object> data = new LinkedHashMap<>();

	public JsonObject put(String key, Object value) {
		this.data.put(key, value);
		return this;
	}

	@Override
	public String toString() {
		return serializeObject(this.data);
	}

}