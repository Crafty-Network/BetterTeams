package com.booksaw.betterTeams.team.meta;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class TeamMeta {

	private final Map<String, String> metaData;

	public TeamMeta() {
		this.metaData = new HashMap<>();
	}

	public void set(String key, String value) {
		this.metaData.put(key, value);
	}

	public Optional<String> get(String key) {
		return Optional.ofNullable(this.metaData.get(key));
	}

	public boolean has(String key) {
		return this.metaData.containsKey(key);
	}

	public void remove(String key) {
		this.metaData.remove(key);
	}

	public Map<String, String> getAll() {
		return Collections.unmodifiableMap(this.metaData);
	}

	public Map<String, String> getSerialized() {
		return new HashMap<>(this.metaData);
	}

	public void load(Map<String, String> rawMeta) {
		this.metaData.clear();
		if (rawMeta != null) {
			this.metaData.putAll(rawMeta);
		}

	}
}
