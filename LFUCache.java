import java.util.Map;
import java.util.HashMap;
import java.util.LinkedHashSet;

// O(1) implementation of Least Frequently Used cache
// Based on: http://dhruvbird.com/lfu.pdf
public class LFUCache<K, V> {
    // The mapping between a cache key and its corresponding cache value.
    private Map<K, V> cache;

    // The mapping between a cache key and its usage count (frequency).
    private Map<K, Integer> usage;

    // The mapping between a frequency and the set of keys which share the same
    // frequency. We use LinkedHashSet to retain insertion order, can switch to
    // normal HashSet if least recently used deletion is not required.
    private Map<Integer, LinkedHashSet<K>> frequency;

    // The cache capacity.
    private int capacity;

    // The frequency of the least frequently used key.
    // aka the smallest frequency.
    private int minFrequency;


    public LFUCache(int capacity) {
        this.capacity = capacity;
        this.minFrequency = -1;
        this.cache = new HashMap<>();
        this.usage = new HashMap<>();
        this.frequency = new HashMap<>();

        // Pre-add an empty keys set for frequency 1 as it will always get
        // created with new insertion (fresh key have 1 usage frequency)
        this.frequency.put(1, new LinkedHashSet<>());
    }


    public V get(K key) {
        if (!cache.containsKey(key)) return null;

        // 1. Get the key usage frequency
        // 2. Increase the key usage frequency counter
        // 3. Remove the key from its current frequency keys set as we're 
        //    preparing to move it to the next (+1) frequency keys set.
        int count = usage.get(key);
        usage.put(key, count + 1);
        frequency.get(count).remove(key);

        // 4. If the key we're removing belongs to the least frequently used
        //    keys set and if the keys set is empty after removal. We need to
        //    update the smallest frequency tracker accordingly.
        //    We can optionally delete / untrack the empty frequency keys set
        //    to save spaces;
        if (count == minFrequency && frequency.get(count).size() == 0) {
            // Delete the empty frequency keys set except for the frequency 1
            // keys set as it's a very common frequency
            if (count != 1) frequency.remove(count);
            minFrequency++;
        }
            
        // 5. Add the key to the new increased frequency keys set. Creating the
        //    keys set if necessary
        LinkedHashSet<K> keys = frequency.getOrDefault(count + 1, new LinkedHashSet<>());
        keys.add(key);
        frequency.put(count + 1, keys);

        // 6. Return the cache value
        return cache.get(key);
    }

    public void put(K key, V value) {
        if (capacity <= 0) return;
        
        // If key does exist
        // 1. Update the key with new value
        // 2. Call get() to increase frequency
        // 3. Return
        if (cache.containsKey(key)) {
            cache.put(key, value);
            get(key);
            return;
        }

        // Cache is full, evict least frequently used key to make space
        // 1. Use the smallest frequency tracker to get the keys set which are
        //    least frequently used. If we used LinkedHashSet to store the keys
        //    we'll be able to extract the first inserted key for removal as
        //    well
        // 2. Extract the evict key from the keys set in step 1
        // 3. Remove the evict key from the keys set in step 1
        // 3. Remove the evict key from the value cache
        // 4. Remove the evict key from the usage frequency
        if (cache.size() >= capacity) {
            K evict = frequency.get(minFrequency).iterator().next();
            frequency.get(minFrequency).remove(evict);
            cache.remove(evict);
            usage.remove(evict);
        }

        // The key is new
        // 1. Put the provided value into the cache key
        // 2. Set the usage frequency of this key to 1
        // 3. Set the smallest frequency (LF) back to 1
        // 4. Add the key to the keys set of frequency 1
        cache.put(key, value);
        usage.put(key, 1);
        minFrequency = 1;
        frequency.get(1).add(key);
    }
}
