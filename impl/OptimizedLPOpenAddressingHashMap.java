package impl;

import java.util.Iterator;

/**
 * OptimizedLPOpenAddressingHashMap
 * 
 * An extension to open addressing that avoids using sentinel deleted values
 * when using the linear probing strategy.
 * 
 * @author Thomas VanDrunen CSCI 345, Wheaton College May 18, 2017
 * @param <K>
 *            The key-type of the map
 * @param <V>
 *            The value-type of the map
 */
public class OptimizedLPOpenAddressingHashMap<K, V> extends OpenAddressingHashMap<K, V> {

	/**
	 * Actually unnecessary since the default constructor would have the same
	 * effect, but this shows intentionality.
	 */
	public OptimizedLPOpenAddressingHashMap() {
		super(1);
	}

	/**
	 * Remove the association for this key, if it exists.
	 * 
	 * @param key
	 *            The key to remove
	 */
	@Override // now that's a REAL override
	public void remove(K key) {
		
		// return if the key doesn't exist
		if(find(key) == -1)
			return;

		Iterator<Integer> probe = prober.probe(key);
		int gap = -1; // gap that needs to be filled


		while (probe.hasNext()) {
			int current = probe.next(); // current key

			// return if current is a null
			if (table[current] == null) {
				break;
			}

			// if the key is equal to the current key, set gap as this key to remove it
			if (table[current].key.equals(key)) {
				gap = current;
				continue;
			}

			// the ideal hash of the current key
			int ideal = h.hash(table[current].key);

			// if ideal is less than gap than we can move the current to gap
			if ((ideal <= gap)
					|| (current < ideal && ideal <= gap)
					|| (gap < current && current < ideal)
					&& gap >= 0) {
				table[gap] = table[current];
				gap = current;
			}

		}

		// fill in the gap with null
		table[gap] = null;
	}

}
