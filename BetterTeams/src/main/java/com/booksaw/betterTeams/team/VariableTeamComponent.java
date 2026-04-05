package com.booksaw.betterTeams.team;

public interface VariableTeamComponent<T> {

	void add(T amount);

	void sub(T amount);

	void mult(T amount);

	void div(T amount);

}
