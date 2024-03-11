package com.github.ppotseluev.itdesk.storage

import cats.effect.Sync
import java.util.concurrent.ConcurrentHashMap

class InMemoryKeyValueDao[F[_]: Sync, K, V] extends KeyValueDao[F, K, V] {

  private val map = new ConcurrentHashMap[K, V]()

  override def put(key: K, value: V): F[Unit] = Sync[F].delay {
    map.put(key, value)
  }

  override def get(key: K): F[Option[V]] = Sync[F].delay {
    Option(map.get(key))
  }
}
