package com.github.pseudoresonance.resonantbot.gameutils.rocketleague.entities;

public class APIReturn<U, V> {

	/**
	 * The id of this {@link APIReturn}
	 */
	private U id;

	/**
	 * The value of this {@link APIReturn}
	 */
	private V value;

	/**
	 * Constructs a new {@link APIReturn} with the given values.
	 * 
	 * @param id message id
	 * @param value return data;
	 */
	public APIReturn(U id, V value) {

		this.id = id;
		this.value = value;
	}

	/**
	 * @return Id of this {@link APIReturn}
	 */
	public U getID() {
		return this.id;
	}
	
	/**
	 * @return Value of this {@link APIReturn}
	 */
	public V getValue() {
		return this.value;
	}
}