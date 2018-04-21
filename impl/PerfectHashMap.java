package impl;

import java.util.Iterator;
import java.util.NoSuchElementException;
import adt.Map;
import adt.Set;
import impl.ListSet;
import java.util.Collections;

/**
 * PerfectHashMap
 * 
 * Implementation of perfect hashing, that is, when the keys are known ahead of
 * time. Note that containsKey and get will work as expected if used with a key
 * that doesn't exist. However, we assume put will never be called using a key
 * that isn't supplied to the constructor; behavior is unspecified otherwise.
 * 
 * @author Thomas VanDrunen CSCI 345, Wheaton College March 17, 2015
 * @param <K>
 *            The key-type of the map
 * @param <V>The
 *            value-type of the map
 */

public class PerfectHashMap<K, V> implements Map<K, V> {

	/**
	 * Secondary maps for the buckets
	 */
	private class SecondaryMap implements Map<K, V> {

		/**
		 * The keys in this secondary map. This is necessary to check when get and
		 * containsKey are called on spurious keys and also for the iterator.
		 */
		K[] keys;

		/**
		 * The values in the secondary map.
		 */
		V[] values;

		/**
		 * The number of slots in the arrays, computed as the square of the number of
		 * keys that can go here.
		 */
		int m;

		/**
		 * The hash function, drawn from class Hpm
		 */
		HashFunction<Object> h;

		/**
		 * Constructor. Given a set of keys, make appropriately size arrays and a hash
		 * set that makes no collisions.
		 * 
		 * @param givenKeys
		 */
		@SuppressWarnings("unchecked")
		SecondaryMap(Set<K> givenKeys) {

			// size of the secondary array
			m = givenKeys.size();
			
			// if there are no keys hashed to this secondary array return
			if (m == 0)
				return;

			// tempKeys holds the keys before the secondary hash function is called on them
			K[] tempKeys = (K[]) new Object[m];

			// square of the size of the given keys
			m *= m;

			keys = (K[]) new Object[m];
			values = (V[]) new Object[m];

			// transfer the keys from the set to the tempKeys array
			int i = 0;
			Iterator<K> it = givenKeys.iterator();
			while (it.hasNext())
				tempKeys[i++] = (K) it.next();

			// p is the greatest prime number of tempKeys. Used in the secondary hash function
			p = findMaskAndGreatestKey(tempKeys);
			p = findGreatestPrime(p);
			boolean collisions = true;
			
			// repeat until there are no collisions in the secondary hash table
			while (collisions) {
				// make a new hash function
				h = HashFactory.universalHashFunction(p, m);
				collisions = false;
				
				// hash the keys. if there is a collision break and repeat
				for (int j = 0; j < tempKeys.length; j++) {
					int secHash = h.hash(tempKeys[j]);
					if (keys[secHash] != null) {
						collisions = true;
						break;
					}
					keys[secHash] = tempKeys[j];
				}

				// reset the  key array
				keys = (K[]) new Object[m];
			}
		}

		/**
		 * Add an association to the map. We assume the given key was known ahead of
		 * time.
		 * 
		 * @param key
		 *            The key to this association
		 * @param val
		 *            The value to which this key is associated
		 */
		public void put(K key, V val) {
			int pos = h.hash(key);
			keys[pos] = key;
			values[pos] = val;
		}

		/**
		 * Get the value for a key.
		 * 
		 * @param key
		 *            The key whose value we're retrieving.
		 * @return The value associated with this key, null if none exists
		 */
		public V get(K key) {
			if (!containsKey(key))
				return null;
			return values[h.hash(key)];
		}

		/**
		 * Test if this map contains an association for this key.
		 * 
		 * @param key
		 *            The key to test.
		 * @return true if there is an association for this key, false otherwise
		 */
		public boolean containsKey(K key) {
			if (m == 0)
				return false;
			int pos = h.hash(key);
			return keys[pos] != null
					// next part necessary only if we assume
					// keys that can't be put may be tested
					&& keys[pos].equals(key);
		}

		/**
		 * Remove the association for this key, if it exists.
		 * 
		 * @param key
		 *            The key to remove
		 */
		public void remove(K key) {
			if (containsKey(key))
				keys[h.hash(key)] = null;
		}

		/**
		 * The iterator for this portion of the map.
		 */
		public Iterator<K> iterator() {
			
			// if the keys are null return null
			if(keys == null)
				return null;
			
			// go to the first key in this secondary hash-map
			int start = 0;
			while(start < m && keys[start] == null)
				start++;
			
			// if there are no more keys in this secondary hash-map return null
			if(start == m)
				return null;
			
			// final variable for the iterator
			int finalStart = start;
			
			
			return new Iterator<K>() {
				// starting position
				int current = finalStart;

				public boolean hasNext() {
					return current < m;
				}

				public K next() {
					int next = current++;
					
					// find next key
					while (current < m && keys[current] == null) {
						current++;
					}
					
					return keys[next];
				}
			};
		}

		/**
		 * Find greatest prime number after a given index
		 * 
		 * @param the
		 *            index from which to start searching
		 * @return the greatest prime number after index
		 */

		public int findGreatestPrime(int index) {
			int nonPrime = index;
			nonPrime++;
			for (int i = 2; i < nonPrime; i++) {
				if (nonPrime % i == 0) {
					nonPrime++;
					i = 2;
				} else {
					continue;
				}
			}
			return nonPrime;
		}
	}

	/**
	 * Secondary maps
	 */
	private SecondaryMap[] secondaries;

	/**
	 * A prime number greater than the greatest hash value
	 */
	private int p;

	/**
	 * A parameter to the hash function; here, the number of keys known ahead of
	 * time.
	 */
	private int m;

	/**
	 * The primary hash function
	 */
	private HashFunction<Object> h;

	/**
	 * A bit mask to grab certain bits from the result of a call to hashCode().
	 * Recommended that this is one less than a power of 2, and hence we will grab a
	 * certain number of lower ordered bits. This is used to reduce the range of the
	 * integer keys and hence allow for a smaller prime p.
	 */
	private int mask;

	/**
	 * Constructor. Takes the keys (all known ahead of time) to set things up to
	 * guarantee no collisions.
	 * 
	 * @param keys
	 */
	@SuppressWarnings("unchecked")
	public PerfectHashMap(K[] keys) {

		m = keys.length;
		// prime number for the hash function
		p = findMaskAndGreatestKey(keys);

		// set to record how many keys hash to each position in the secondaries array
		Set<K>[] counts = new ListSet[keys.length];
		secondaries = (SecondaryMap[]) new PerfectHashMap.SecondaryMap[counts.length];

		// primary hash function to hash keys to positions in secondaries
		h = HashFactory.universalHashFunction(p, m);

		// initialize counts
		for (int i = 0; i < keys.length; i++)
			counts[i] = new ListSet<K>();
		
		// add keys to counts
		for (int i = 0; i < keys.length; i++)
			counts[h.hash(keys[i])].add(keys[i]);

		// make new secondary hash-maps for each set in counts
		for (int i = 0; i < counts.length; i++)
			secondaries[i] = new SecondaryMap(counts[i]);
	}

	/**
	 * Helper function (intended to be used by the constructor) to find an
	 * appropriate mask for the keys and the greatest integer key using that mask.
	 * The mask, stored as an instance variable, is the greatest value one less than
	 * a power of two which will produce unique integers when bitwise-anded with
	 * each key's hashcode.
	 * 
	 * @param keys
	 *            The set of keys, given to the constructor.
	 * @return The greatest value of any key's hashcode bitwise-anded by the mask.
	 */
	private int findMaskAndGreatestKey(K[] keys) {
		// The greatest code found so for the current mask
		int greatestCode;
		// Our current guess for the least mask
		mask = 31;
		// Do any keys' hashcodes have identical bits when
		// bitwise-anded with the current mask?
		boolean doubleHit;

		// Repeatedly guess a mask until we find one
		// that gives no double hits.
		do {
			// Increase our mask guess to the next integer
			// that is one less than a power of two.
			// (Effectively 63 is our first guess.)
			mask = (mask << 1) + 1;
			// We have not found any double hits so far on this mask
			doubleHit = false;
			// hit[i] is true iff we have inspected a key whose
			// hashcode bitwise-anded with the mask equals i.
			// Note that mask itself is the greatest possible
			// result of bitwise-anding a value with mask.
			boolean[] hits = new boolean[mask + 1];
			greatestCode = 0;
			for (K key : keys) {
				int code = (key.hashCode() & mask);
				if (code > greatestCode)
					greatestCode = code;
				if (hits[code])
					doubleHit = true;
				else
					hits[code] = true;
			}
		} while (doubleHit);
		return greatestCode;
	}

	/**
	 * Add an association to the map. We assume the given key was known ahead of
	 * time.
	 * 
	 * @param key
	 *            The key to this association
	 * @param val
	 *            The value to which this key is associated
	 */
	public void put(K key, V val) {
		secondaries[h.hash(key)].put(key, val);
	}

	/**
	 * Get the value for a key.
	 * 
	 * @param key
	 *            The key whose value we're retrieving.
	 * @return The value associated with this key, null if none exists
	 */
	public V get(K key) {
		return secondaries[h.hash(key)].get(key);
	}

	/**
	 * Test if this map contains an association for this key.
	 * 
	 * @param key
	 *            The key to test.
	 * @return true if there is an association for this key, false otherwise
	 */
	public boolean containsKey(K key) {
		return secondaries[h.hash(key)].containsKey(key);
	}

	/**
	 * Remove the association for this key, if it exists.
	 * 
	 * @param key
	 *            The key to remove
	 */
	public void remove(K key) {
		secondaries[h.hash(key)].remove(key);
	}

	/**
	 * Return an iterator over this map
	 */
	public Iterator<K> iterator() {
		
		int start = 0;

		// move start to the first non null secondary iterator
		while(start < m && secondaries[start].iterator() == null)
			start++;
		
		// if there is nothing to iterate in the secondary array, return empty iterator
		if(start == m)
			return Collections.emptyIterator();
		
		// final variable for the iterator
		final int finalStart = start;
		
		
		return new Iterator<K>() {
		
			int current = finalStart;
			// secondary iterator
			Iterator<K> it = secondaries[current].iterator(); 
			
			public boolean hasNext() {
				return current < m && it != null && it.hasNext();
			}
			
			public K next() {
				K ret = it.next();

				// if there are no more values in the current secondary array which
				// is being iterated by it, move to the next valid secondary array iterator
				if(!it.hasNext()) {


					// move to the next valid secondary array iterator
					current++;
					while((current < m) && (secondaries[current].iterator() == null)) {
						current++;
					}
					
					// if start == m it means there are no more items in the hashtable
					if(current == m)
						it = null;
					else
						it = secondaries[current].iterator();
				}
				
				return ret;
			}
		};
	}
}
