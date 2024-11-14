package dev.redstudio.alfheim.utils;

import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;

/**
 * A queue implementation for long values that are deduplicated on addition.
 * <p>
 * This is achieved by storing the values in a {@link LongOpenHashSet} and a {@link LongArrayFIFOQueue}.
 *
 * @author Luna Lage (Desoroxxx)
 * @since 1.3
 */
public final class DeduplicatedLongQueue {

    private final LongArrayFIFOQueue queue;
    private LongOpenHashSet set;

    /**
     * Creates a new deduplicated queue with the given capacity.
     *
     * @param capacity The capacity of the deduplicated queue
     */
    public DeduplicatedLongQueue(final int capacity) {
        set = new LongOpenHashSet(capacity);
        queue = new LongArrayFIFOQueue(capacity);
    }

    /**
     * Adds a value to the queue.
     *
     * @param value The value to add to the queue
     */
    public void enqueue(final long value) {
        if (set.add(value))
            queue.enqueue(value);
    }

    /**
     * Removes and returns the first value in the queue.
     *
     * @return The first value in the queue
     */
    public long dequeue() {
        return queue.dequeueLong();
    }

    /**
     * Returns whether the queue is empty.
     *
     * @return {@code true} if the queue is empty, {@code false} otherwise
     */
    public boolean isEmpty() {
        return queue.isEmpty();
    }

    /**
     * Creates a new deduplication set.
     */
    public void newDeduplicationSet() {
        set = new LongOpenHashSet(queue.size());
    }
}
