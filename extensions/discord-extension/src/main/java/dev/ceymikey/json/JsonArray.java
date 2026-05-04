
package dev.ceymikey.json;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class JsonArray extends JsonElement {
	private final List<Object> items = new ArrayList<>();

	public JsonArray(Object... items) {
		this.items.addAll(Arrays.asList(items));
	}

	public JsonArray put(Object value) {
		this.items.add(value);
		return this;
	}

	@Override
	public String toString() {
		return serializeArray(items);
	}
}