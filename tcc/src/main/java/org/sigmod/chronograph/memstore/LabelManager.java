package org.sigmod.chronograph.memstore;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Manages the integer representation of labels
 */
public class LabelManager {
	ArrayList<String> intIds;
	// Contains the integer representation of string values
	HashMap<String, Integer> stringIds;

	/**
	 * Initializes the collection of integer ids, string ids, and freed ids
	 */
	public LabelManager() {
		this.intIds = new ArrayList<>();
		this.stringIds = new HashMap<>();
	}

	/**
	 * Sets the integer value of a string id
	 * 
	 * @param id the string id to be represented
	 * @return the integer id
	 */
	public Integer add(String id) {
		// Check existence of id in stringIds
		if (stringIds.containsKey(id))
			return stringIds.get(id);

		Integer intId = intIds.size();
		intIds.add(id);
		stringIds.put(id, intId);

		return intId;
	}

	/**
	 * Returns the string id of the integer id
	 * 
	 * @param id the integer id to be retrieved
	 * 
	 * @return the string id representation or {@code null} if not mapped
	 */
	public String getId(Integer id) {
		if (intIds.size() <= id)
			return null;

		return intIds.get(id);
	}

	/**
	 * Returns the integer representation of a string id if present
	 * 
	 * @param id the id to be retrieved
	 * @return the integer representation of the id, or {@code null} if not mapped
	 */
	public Integer getId(String id) {
		if (!stringIds.containsKey(id))
			return null;
		return stringIds.get(id);
	}

	public void printInfo() {
		System.out.println("Ints: " + intIds);
		System.out.println("Strings: " + stringIds);
		System.out.println();
	}
}