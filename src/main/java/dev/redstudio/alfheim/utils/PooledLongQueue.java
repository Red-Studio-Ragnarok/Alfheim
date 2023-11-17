package dev.redstudio.alfheim.utils;

import lombok.Getter;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Implement own queue with pooled segments to reduce allocation costs and reduce idle memory footprint
 *
 * @author Luna Lage (Desoroxxx)
 * @author Angeline (@jellysquid)
 * @since 1.0
 */
@Deprecated // Todo: This is cool when memory is the problem but raw CPU speed would be better
public final class PooledLongQueue {

    private static final int CACHED_QUEUE_SEGMENTS_COUNT = 4096;
    private static final int QUEUE_SEGMENT_SIZE = 1024;

    /**
     * The number of encoded values present in this queue
     * <p>
     * Not thread-safe! If you must know whether the queue is empty, please use {@link PooledLongQueue#isEmpty()}.
     */
    @Getter private int size = 0;

    private final Pool pool;

    private Segment currentSegment, lastSegment;

    /**
     * Stores whether the queue is empty.
     * <p>
     * All threads will see updates to this field immediately.
     * <p>
     * Writes to volatile fields are generally quite a bit more expensive, so we avoid repeatedly setting this flag to true.
     */
    @Getter private volatile boolean empty;

    public PooledLongQueue(final Pool pool) {
        this.pool = pool;
    }

    /**
     * Not thread-safe! Adds an encoded long value into this queue.
     *
     * @param val The encoded value to add
     */
    public void add(final long val) {
        if (currentSegment == null) {
            empty = false;
            currentSegment = lastSegment = pool.acquire();
        }

        if (lastSegment.index == QUEUE_SEGMENT_SIZE) {
            final Segment ret = lastSegment.next = lastSegment.pool.acquire();
            ret.longArray[ret.index++] = val;

            lastSegment = ret;
        } else {
            lastSegment.longArray[lastSegment.index++] = val;
        }

        ++size;
    }

    /**
     * Not thread safe! Creates an iterator over the values in this queue. Values will be returned in a FIFO fashion.
     *
     * @return The iterator
     */
    public LongQueueIterator iterator() {
        return new LongQueueIterator(currentSegment);
    }

    public final class LongQueueIterator {

        private int index, capacity;

        private long[] currentArray;
        private Segment currentSegment;

        private LongQueueIterator(final Segment currentSegment) {
            this.currentSegment = currentSegment;

            if (this.currentSegment != null) {
                currentArray = currentSegment.longArray;
                capacity = currentSegment.index;
            }
        }

        public boolean hasNext() {
            return currentSegment != null;
        }

        public long next() {
            final long next = currentArray[index++];

            if (index == capacity) {
                index = 0;

                currentSegment = currentSegment.next;

                if (currentSegment != null) {
                    currentArray = currentSegment.longArray;
                    capacity = currentSegment.index;
                }
            }

            return next;
        }

        public void finish() {
            final PooledLongQueue queue = PooledLongQueue.this;

            Segment segment = queue.currentSegment;

            while (segment != null) {
                final Segment next = segment.next;

                segment.release();

                segment = next;
            }

            queue.size = 0;
            queue.currentSegment = null;
            queue.lastSegment = null;
            queue.empty = true;
        }
    }

    public static final class Pool {

        private final Deque<Segment> segmentPool = new ArrayDeque<>();

        private Segment acquire() {
            if (segmentPool.isEmpty())
                return new Segment(this);

            return segmentPool.pop();
        }

        private void release(final Segment segment) {
            if (segmentPool.size() < CACHED_QUEUE_SEGMENTS_COUNT)
                segmentPool.push(segment);
        }
    }

    private static final class Segment {

        private final long[] longArray = new long[QUEUE_SEGMENT_SIZE];
        private final Pool pool;
        private int index = 0;
        private Segment next;

        private Segment(final Pool pool) {
            this.pool = pool;
        }

        private void release() {
            index = 0;
            next = null;

            pool.release(this);
        }
    }
}
