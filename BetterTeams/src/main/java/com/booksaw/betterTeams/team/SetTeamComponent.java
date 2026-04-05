package com.booksaw.betterTeams.team;

import com.booksaw.betterTeams.Team;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public abstract class SetTeamComponent<T> implements TeamComponent<Set<T>> {

	protected final Set<T> set;

	protected SetTeamComponent() {
		set = new HashSet<T>();
	}

	@Override
	public Set<T> get() {
		return set;
	}

	public Set<T> getClone() {
		return new HashSet<>(set);
	}

	@Override
	public void set(Set<T> newSet) {
		this.set.clear();
		this.set.addAll(newSet);
	}

	public int size() {
		return set.size();
	}

	public boolean isEmpty() {
		return set.isEmpty();
	}

	public void load(Iterable<String> strList) {
		set.clear();
		for (String str : strList) {
			set.add(fromString(str));
		}
	}

	public List<String> getConvertedList() {
		List<String> componentStrings = new ArrayList<>();

		for (T component : set) {
			String componentString = toString(component);
			if (componentString != null) {
				componentStrings.add(componentString);
			}
		}

		return componentStrings;
	}

	public void add(Team team, T component) {
		set.add(component);
	}

	public void remove(Team team, T component) {
		set.remove(component);
	}

	public boolean contains(T component) {
		return set.contains(component);
	}

	public void clear() {
		set.clear();
	}

	public abstract String getSectionHeading();

	public abstract T fromString(String str);

	public abstract String toString(T component);

}
