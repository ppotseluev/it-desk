package com.github.ppotseluev.itdesk.storage

trait KeyValueDao[F[_], K, V] {
  def put(key: K, value: V): F[Unit]

  def get(key: K): F[Option[V]]
}
