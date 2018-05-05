package com.ssplugins.rlstats;

public class APIReturn<U, V> {

	/**
	 * The id of this <code>Pair</code>
	 */
	private U id;

	/**
	 * The value of this <code>Pair</code>
	 */
	private V value;

	/**
	 * Constructs a new <code>Pair</code> with the given values.
	 * 
	 * @param id
	 *            message id
	 * @param value
	 *            return data;
	 */
	public APIReturn(U id, V value) {

		this.id = id;
		this.value = value;
	}

	public U getID() {
		return this.id;
	}
	
	public V getValue() {
		return this.value;
	}
}