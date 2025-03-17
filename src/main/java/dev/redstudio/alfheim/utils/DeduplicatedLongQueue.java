package dev.redstudio.alfheim.utils;

import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;

/// A queue implementation for long values that are deduplicated on addition.
///
/// This is achieved by storing the values in a [LongOpenHashSet] and a [LongArrayFIFOQueue].
///
/// @author Luna Mira Lage (Desoroxxx)
/// @version 2024-12-18
/// @since 1.3
public final class DeduplicatedLongQueue {

	// TODO: Fully Implement my own implementation to get rid of the downsides of reduce etc...

	private final LongArrayFIFOQueue queue;
	private LongOpenHashSet set;

	/// Creates a new deduplicated queue with the given capacity.
	///
	/// @param capacity The capacity of the deduplicated queue
	public DeduplicatedLongQueue(final int capacity) {
		set = new LongOpenHashSet(capacity);
		queue = new LongArrayFIFOQueue(capacity);
	}

	/// Adds a value to the queue.
	///
	/// @param value The value to add to the queue
	public void enqueue(final long value) {
		if (set.add(value)) {
			queue.enqueue(value);
		}
	}

	/// Removes and returns the first value in the queue.
	///
	/// @return The first value in the queue
	public long dequeue() {
		return queue.dequeueLong();
	}

	/// Returns whether the queue is empty.
	///
	/// @return `true` if the queue is empty,`false` otherwise
	public boolean isEmpty() {
		return queue.isEmpty();
	}

	/// Creates a new deduplication set.
	public void newDeduplicationSet() {
		set = new LongOpenHashSet(queue.size());
	}
}
