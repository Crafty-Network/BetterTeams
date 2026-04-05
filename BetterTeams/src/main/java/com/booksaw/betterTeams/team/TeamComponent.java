package com.booksaw.betterTeams.team;

import com.booksaw.betterTeams.team.storage.team.TeamStorage;

public interface TeamComponent<T> {

	T get();

	void set(T value);

	void load(TeamStorage section);

	void save(TeamStorage storage);

}
