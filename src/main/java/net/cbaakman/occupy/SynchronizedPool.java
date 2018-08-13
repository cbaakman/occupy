package net.cbaakman.occupy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class SynchronizedPool<K, V> {

	private Map<K, V> map = new HashMap<K, V>();
	
	public V get(K key) {
		synchronized(map) {
			return map.get(key);
		}
	}
	
	public void put(K key, V value) {
		synchronized(map) {
			map.put(key, value);
		}
	}
	
	public Collection<V> getAll() {
		synchronized(map) {
			return new ArrayList<V>(map.values());
		}
	}
	
	public Collection<K> getKeys() {
		synchronized(map) {
			return new ArrayList<K>(map.keySet());
		}
	}

	public void clear() {
		synchronized(map) {
			map.clear();
		}
	}
	
	public boolean hasKey(K key) {
		synchronized(map) {
			return map.containsKey(key);
		}
	}
	
	public void remove(K key) {
		synchronized(map) {
			map.remove(key);
		}
	}
}
