package com.incidenthub.util;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.IdentifierGenerator;

import java.io.Serializable;

/**
 * Twitter Snowflake ID Generator
 *
 * 64-bit ID structure:
 * | 1 bit (unused) | 41 bits (timestamp) | 10 bits (machine/node) | 12 bits
 * (sequence) |
 *
 * - Timestamp: milliseconds since custom epoch (2024-01-01) → ~69 years
 * - Machine ID (10 bits): supports 1024 nodes (datacenter + worker)
 * - Sequence (12 bits): 4096 IDs per millisecond per node
 *
 * Total capacity: ~4 million unique IDs/second/node, time-sorted, no
 * coordination needed.
 */
public class SnowflakeIdGenerator implements IdentifierGenerator {

  private static final long CUSTOM_EPOCH = 1704067200000L; // 2024-01-01 00:00:00 UTC

  private static final long NODE_ID_BITS = 10L;
  private static final long SEQUENCE_BITS = 12L;

  private static final long MAX_NODE_ID = (1L << NODE_ID_BITS) - 1;
  private static final long MAX_SEQUENCE = (1L << SEQUENCE_BITS) - 1;

  private static final long NODE_ID_SHIFT = SEQUENCE_BITS;
  private static final long TIMESTAMP_SHIFT = SEQUENCE_BITS + NODE_ID_BITS;

  private final long nodeId;
  private long lastTimestamp = -1L;
  private long sequence = 0L;

  private static SnowflakeIdGenerator instance;

  public SnowflakeIdGenerator() {
    this(1L); // Default node ID = 1
  }

  public SnowflakeIdGenerator(long nodeId) {
    if (nodeId < 0 || nodeId > MAX_NODE_ID) {
      throw new IllegalArgumentException("Node ID must be between 0 and " + MAX_NODE_ID);
    }
    this.nodeId = nodeId;
  }

  public static synchronized SnowflakeIdGenerator getInstance() {
    if (instance == null) {
      instance = new SnowflakeIdGenerator(1L);
    }
    return instance;
  }

  public synchronized long nextId() {
    long currentTimestamp = currentTimeMillis();

    if (currentTimestamp < lastTimestamp) {
      throw new IllegalStateException("Clock moved backwards. Refusing to generate ID for "
          + (lastTimestamp - currentTimestamp) + " milliseconds.");
    }

    if (currentTimestamp == lastTimestamp) {
      sequence = (sequence + 1) & MAX_SEQUENCE;
      if (sequence == 0) {
        // Sequence exhausted for this millisecond, wait for next
        currentTimestamp = waitNextMillis(lastTimestamp);
      }
    } else {
      sequence = 0;
    }

    lastTimestamp = currentTimestamp;

    return ((currentTimestamp - CUSTOM_EPOCH) << TIMESTAMP_SHIFT)
        | (nodeId << NODE_ID_SHIFT)
        | sequence;
  }

  @Override
  public Serializable generate(SharedSessionContractImplementor session, Object object) {
    return nextId();
  }

  private long waitNextMillis(long lastTimestamp) {
    long timestamp = currentTimeMillis();
    while (timestamp <= lastTimestamp) {
      timestamp = currentTimeMillis();
    }
    return timestamp;
  }

  private long currentTimeMillis() {
    return System.currentTimeMillis();
  }

  /**
   * Extract timestamp from a Snowflake ID (useful for debugging/sorting)
   */
  public static long extractTimestamp(long id) {
    return (id >> TIMESTAMP_SHIFT) + CUSTOM_EPOCH;
  }

  /**
   * Extract node ID from a Snowflake ID
   */
  public static long extractNodeId(long id) {
    return (id >> NODE_ID_SHIFT) & MAX_NODE_ID;
  }

  /**
   * Extract sequence from a Snowflake ID
   */
  public static long extractSequence(long id) {
    return id & MAX_SEQUENCE;
  }
}
