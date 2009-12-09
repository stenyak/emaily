package com.google.wave.extensions.emaily.data;

import java.util.Calendar;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.appengine.api.memcache.Expiration;
import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.MemcacheServiceFactory;
import com.google.appengine.api.memcache.MemcacheService.SetPolicy;
import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * Class for exclusively locking entities.
 * 
 * @author dlux
 * 
 */
@Singleton
public class EntityLockManager {
  private final MemcacheService memcacheService = MemcacheServiceFactory.getMemcacheService();
  private final Logger logger;

  @Inject
  public EntityLockManager(Logger logger) {
    this.logger = logger;
  }

  /**
   * Run a Runnable in an exclusive lock of an entity.
   * 
   * @param entity The entity class.
   * @param entityId The entity id to be locked.
   * @param waitTimeout The timeout while we are waiting for the lock. In ms.
   * @param processingTimeout The maximum imaginable time while the <code>runnable</code> is running
   *          (once the lock is acquired). After that time, the lock is automatically released. In
   *          ms.
   * @param runAnyway If this is true, then we run the <code>runnable</code> even if we could not
   *          acquire the lock, but the <code>waitTimeout</code> exceeded.
   * @param runnable The function to run.
   */
  public void executeInLock(Class<? extends Object> entity, String entityId, long waitTimeout,
      long processingTimeout, boolean runAnyway, Runnable runnable) {
    executeInLock(buildCacheKey(entity, entityId), waitTimeout, processingTimeout, runAnyway,
        runnable);
  }

  private void executeInLock(String key, long waitTimeout, long processingTimeout,
      boolean runAnyway, Runnable runnable) {
    long startTime = Calendar.getInstance().getTimeInMillis();
    long lockWaitExpire = startTime + waitTimeout;
    long runnableStarted = 0;
    int retryNum = 0;
    try {
      // Try to acquire the lock
      while (!tryLockEntity(key, processingTimeout)) {
        if (Calendar.getInstance().getTimeInMillis() >= lockWaitExpire) {
          // If we timed out, then we log it:
          if (logger.isLoggable(Level.FINER))
            logger.finer(String.format("Acquiring lock timed out. Key: %s, timeout: %d"
                + ", time spent: %d, retries: %d, run anyway: %s", key, waitTimeout, Calendar
                .getInstance().getTimeInMillis()
                - startTime, retryNum, (runAnyway ? "yes" : "no")));
          if (runAnyway)
            break;
          else
            return;
        }
        // Retry to acquire the lock
        retryNum++;
        try {
          Thread.sleep(100);
        } catch (InterruptedException e) {
        }
      }
      if (logger.isLoggable(Level.FINER)) {
        logger.finer(String.format("Execution started: %s, wait delay: %d, lock retries: %d", key,
            Calendar.getInstance().getTimeInMillis() - startTime, retryNum));
      }
      // Start the execution of the runnable.
      runnableStarted = Calendar.getInstance().getTimeInMillis();
      runnable.run();
    } finally {
      unlockEntity(key);
    }
    if (runnableStarted > 0) {
      // Measure the time of execution.
      long elapsed = Calendar.getInstance().getTimeInMillis() - runnableStarted;
      if (logger.isLoggable(Level.FINER))
        logger.finer(String.format("Execution finished: %s, elapsed: %d, timeout: %d", key,
            elapsed, processingTimeout));
      if (elapsed > processingTimeout)
        logger.warning(String.format(
            "Execution in lock lasted longer than expected: %s, elapsed: %d, timeout: %d", key,
            elapsed, processingTimeout));
    }
  }

  /**
   * Build a cache key from an entit class and an entity id.
   * 
   * @param entity The class of the entity
   * @param entityId The id of the entity
   * @return The memcache key for the entity.
   */
  private String buildCacheKey(Class<? extends Object> entity, String entityId) {
    return entity.getSimpleName() + "/" + entityId;
  }

  /**
   * Tries to exclusively lock a memcache key for a specified timeout. If the lock was successful,
   * then it returns true, otherwise it returns false.
   * 
   * @param key The memcache key
   * @param timeout The number of seconds for the entity to be locked.
   * @return True, if the lock was successful.
   */
  private boolean tryLockEntity(String key, long timeout) {
    return memcacheService.put(key, "1", Expiration.byDeltaSeconds((int) timeout),
        SetPolicy.ADD_ONLY_IF_NOT_PRESENT);
  }

  /**
   * Unlocks an entity.
   * 
   * @param key The key to unlock.
   */
  private void unlockEntity(String key) {
    memcacheService.delete(key);
  }
}