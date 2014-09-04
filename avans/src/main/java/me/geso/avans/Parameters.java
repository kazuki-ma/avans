package me.geso.avans;

import java.util.Collection;
import java.util.Optional;

import org.apache.commons.collections4.MultiMap;

/**
 * The class represents path paremeters.
 * 
 * @author tokuhirom
 *
 */
public class Parameters {
	@Override
	public String toString() {
		return "Parameters [map=" + map + "]";
	}

	private final MultiMap<String, String> map;

	public Parameters(MultiMap<String, String> map) {
		this.map = map;
	}

	public String get(String name) {
		if (!map.containsKey(name)) {
			throw new RuntimeException("Missing mandatory path parameter: "
					+ name);
		}

		@SuppressWarnings("unchecked")
		Collection<String> collection = (Collection<String>) map.get(name);
		return collection.iterator().next();
	}

	/**
	 * Get a path parameter in long.
	 * 
	 * @param name
	 * @return
	 */
	public long getLong(String name) {
		String arg = this.get(name);
		return Long.parseLong(arg);
	}

	/**
	 * Get a path parameter in int.
	 * 
	 * @param name
	 * @return
	 */
	public int getInt(String name) {
		String arg = this.get(name);
		return Integer.parseInt(arg);
	}

	/**
	 * Get a path parameter in String. But this doesn't throws exception if the
	 * value doesn't exists.
	 * 
	 * @param name
	 * @return
	 */
	public Optional<String> getOptionalArg(String name) {
		@SuppressWarnings("unchecked")
		Collection<String> collection = (Collection<String>) map.get(name);
		return collection.stream().findFirst();
	}
}