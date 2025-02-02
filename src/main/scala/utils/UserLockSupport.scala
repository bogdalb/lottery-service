package utils

import com.github.benmanes.caffeine.cache.{Cache, Caffeine}

import java.time.Duration
import java.util.UUID
import java.util.concurrent.locks.ReentrantLock
import scala.concurrent.Future

trait UserLockSupport {

  protected def userLocks: Cache[UUID, ReentrantLock]

  protected def getUserLock(userId: UUID): ReentrantLock = {
    userLocks.get(userId, _ => new ReentrantLock())
  }

  protected def withUserLock[T](userId: UUID)(block: => Future[T]): Future[T] = {
    val userLock = getUserLock(userId)
    userLock.lock()
    try {
      block
    } finally {
      userLock.unlock()
    }
  }

}

object UserLockSupport{
  def createUserLockCache(ttlInSeconds: Int) = Caffeine.newBuilder()
    .expireAfterAccess(Duration.ofSeconds(ttlInSeconds))
    .build[UUID, ReentrantLock]()
}
