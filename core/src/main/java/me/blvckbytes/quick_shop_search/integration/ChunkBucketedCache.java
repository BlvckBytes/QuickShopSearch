package me.blvckbytes.quick_shop_search.integration;

import it.unimi.dsi.fastutil.longs.Long2ObjectAVLTreeMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import org.bukkit.Location;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public abstract class ChunkBucketedCache<T> {

  private final Map<UUID, Long2ObjectMap<List<T>>> itemsByChunkHashByWorldId;

  protected ChunkBucketedCache() {
    this.itemsByChunkHashByWorldId = new ConcurrentHashMap<>();
  }

  protected abstract @Nullable Location extractItemLocation(T item);

  protected abstract boolean doItemsEqual(T a, T b);

  protected void clear() {
    itemsByChunkHashByWorldId.clear();
  }

  protected void registerItem(T item) {
    var bukkitLocation = extractItemLocation(item);

    if (bukkitLocation == null)
      return;

    var itemWorld = bukkitLocation.getWorld();

    if (itemWorld == null)
      return;

    var itemBucket = itemsByChunkHashByWorldId
      .computeIfAbsent(itemWorld.getUID(), k -> new Long2ObjectAVLTreeMap<>())
      .computeIfAbsent(fastChunkHash(bukkitLocation), k -> new ArrayList<>());

    // Ensure "set-like" behaviour, based on whatever criteria the equality-checker implements
    for (var iterator = itemBucket.iterator(); iterator.hasNext();) {
      if (doItemsEqual(item, iterator.next())) {
        iterator.remove();
        return;
      }
    }

    itemBucket.add(item);
  }

  protected void unregisterItem(T item) {
    var bukkitLocation = extractItemLocation(item);

    if (bukkitLocation == null)
      return;

    var itemWorld = bukkitLocation.getWorld();

    if (itemWorld == null)
      return;

    var itemsByChunkHash = itemsByChunkHashByWorldId.get(itemWorld.getUID());

    if (itemsByChunkHash == null)
      return;

    var items = itemsByChunkHash.get(fastChunkHash(bukkitLocation));

    if (items == null)
      return;

    for (var iterator = items.iterator(); iterator.hasNext();) {
      if (doItemsEqual(item, iterator.next())) {
        iterator.remove();
        return;
      }
    }
  }

  private class ClosestItemSession {
    Location origin;
    int originChunkX;
    int originChunkY;
    int originChunkZ;
    Long2ObjectMap<List<T>> buckets;

    T closestItem = null;
    int closestBlockDistanceSquared = 0;
    int closestChunkDistanceSquared = 0;

    ClosestItemSession(Location origin, Long2ObjectMap<List<T>> buckets) {
      this.origin = origin;
      this.buckets = buckets;

      this.originChunkX = origin.getBlockX() >> 4;
      this.originChunkY = origin.getBlockY() >> 4;
      this.originChunkZ = origin.getBlockZ() >> 4;
    }

    void visitItems(int deltaChunkX, int deltaChunkY, int deltaChunkZ) {
      var chunkHash = fastChunkHash(originChunkX + deltaChunkX, originChunkY + deltaChunkY, originChunkZ + deltaChunkZ);
      var bucket = buckets.get(chunkHash);

      if (bucket == null)
        return;

      for (var item : bucket) {
        var distanceSquared = computeDistanceSquared(item, origin);

        if (closestItem == null || closestBlockDistanceSquared > distanceSquared) {
          closestBlockDistanceSquared = distanceSquared;
          closestChunkDistanceSquared = distanceSquared >> 8;
          closestItem = item;
        }
      }
    }
  }

  protected @Nullable T findClosestItem(Location origin, int blockRadius) {
    var world = origin.getWorld();

    if (world == null)
      return null;

    var itemsByChunkHash = itemsByChunkHashByWorldId.get(world.getUID());

    if (itemsByChunkHash == null)
      return null;

    var closestSession = new ClosestItemSession(origin, itemsByChunkHash);

    var radiusInChunks = (blockRadius + 15) >> 4;
    var maxDistanceSquared = square(radiusInChunks) * 2;
    var yDistanceSquared = 0;

    // [0, 1, -1, 2, -2, 3, -3, ...]
    for (int iY = 0; iY <= radiusInChunks * 2; ++iY) {
      int dChunkY = (iY & 1) != 0 ? iY - iY / 2 : iY / -2;

      if (dChunkY > 0)
        yDistanceSquared += 2 * dChunkY - 1;

      var yzDistanceSquared = yDistanceSquared;

      sliceLoop:
      for (var dChunkZ = 0; dChunkZ <= radiusInChunks; ++dChunkZ) {
        if (dChunkZ != 0)
          yzDistanceSquared += 2 * dChunkZ - 1;

        var yzxDistanceSquared = yzDistanceSquared;
        T priorGroupClosest = null;

        for (var dChunkX = 0; dChunkX <= dChunkZ; ++dChunkX) {
          if (dChunkX != 0)
            yzxDistanceSquared += 2 * dChunkX - 1;

          if (yzxDistanceSquared > maxDistanceSquared)
            break sliceLoop;

          if (closestSession.closestItem != null && yzxDistanceSquared > closestSession.closestChunkDistanceSquared + 2)
            break sliceLoop;

          closestSession.visitItems(dChunkX, dChunkY, dChunkZ);

          if (dChunkZ != 0)
            closestSession.visitItems(dChunkX, dChunkY, -dChunkZ);

          if (dChunkX != 0) {
            closestSession.visitItems(-dChunkX, dChunkY, dChunkZ);
            closestSession.visitItems(-dChunkX, dChunkY, -dChunkZ);

            if (dChunkX != dChunkZ) {
              closestSession.visitItems(dChunkZ, dChunkY, -dChunkX);
              closestSession.visitItems(-dChunkZ, dChunkY, -dChunkX);
            }
          }

          if (dChunkX != dChunkZ) {
            closestSession.visitItems(dChunkZ, dChunkY, dChunkX);
            closestSession.visitItems(-dChunkZ, dChunkY, dChunkX);
          }

          if (closestSession.closestItem == null)
            continue;

          if (priorGroupClosest == closestSession.closestItem)
            break;

          priorGroupClosest = closestSession.closestItem;
        }
      }
    }

    return closestSession.closestItem;
  }

  private int computeDistanceSquared(T item, Location origin) {
    var itemLocation = extractItemLocation(item);

    if (itemLocation == null)
      return Integer.MAX_VALUE;

    return (
      square(origin.getBlockX() - itemLocation.getBlockX()) +
      square(origin.getBlockY() - itemLocation.getBlockY()) +
      square(origin.getBlockZ() - itemLocation.getBlockZ())
    );
  }

  private static int square(int value) {
    return value * value;
  }

  private static long fastChunkHash(Location location) {
    return fastChunkHash(
      location.getBlockX() >> 4,
      location.getBlockY() >> 4,
      location.getBlockZ() >> 4
    );
  }

  private static long fastChunkHash(int chunkX, int chunkY, int chunkZ) {
    // Assuming an axis is limited to roughly +- 30,000,000, that would be +- 1,875,000 chunks/axis
    // 2^(22-1) = 2,097,152 => 22 bits per axis should suffice
    // The y-axis does by far not exhaust this range, so it will be compromised to 20 bits

    //  ---------- 22b ----------   ---------- 22b ----------   ---------- 20b ----------
    // 64.......................43 42.......................21 20.........................1
    // <1b sign chunkX><21b chunkX><1b sign chunkZ><21b chunkZ><1b sign chunkY><19b chunkY>
    return (
      (((((long) chunkX) & 0x3FFFFF) << (20 + 22)) | ((chunkX < 0 ? 1L : 0L) << (20 + 22 + 21))) |
      (((((long) chunkZ) & 0x3FFFFF) << 20) | ((chunkZ < 0 ? 1L : 0L) << (20 + 21))) |
      ((((long) chunkY) & 0x7FFFF) | ((chunkY < 0 ? 1L : 0L) << 19))
    );
  }
}
