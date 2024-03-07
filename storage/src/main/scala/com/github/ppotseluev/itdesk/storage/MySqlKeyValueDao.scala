package com.github.ppotseluev.itdesk.storage

import cats.effect.MonadCancelThrow
import cats.instances.string._
import cats.syntax.functor._
import doobie.implicits._
import doobie.util.fragment.Fragment
import doobie.util.transactor.Transactor
import doobie.{Get, Put}
import io.circe.parser

import scala.reflect.runtime.universe.TypeTag

class MySqlKeyValueDao[F[_], K, V: TypeTag](tableName: String, transactor: Transactor[F])(implicit
    keySchema: Schema.String[K],
    valueSchema: Schema[V],
    F: MonadCancelThrow[F]
) extends KeyValueDao[F, K, V] {

  import MySqlKeyValueDao._

  private def putSql(key: K, value: V): doobie.Update0 =
    (Fragment.const(s"INSERT INTO $tableName (id, value)") ++
      fr"""|VALUES (
           |  $key,
           |  $value
           |) ON DUPLICATE KEY UPDATE
           |value = $value;
        """.stripMargin).update

  private def getSql(key: K) =
    (Fragment.const(s"SELECT value FROM $tableName") ++ fr"WHERE id = $key;").query[V]

  override def put(key: K, value: V): F[Unit] =
    putSql(key, value).run.transact(transactor).void

  override def get(key: K): F[Option[V]] =
    getSql(key).option
      .transact(transactor)
}

object MySqlKeyValueDao {
  private implicit def derivePut[T](implicit schema: Schema[T]): Put[T] = schema match {
    case Schema.Json(codec)   => Put[String].contramap(codec.apply(_).noSpaces)
    case Schema.String(codec) => Put[String].contramap(codec.write)
  }

  private implicit def deriveGet[T: TypeTag](implicit schema: Schema[T]): Get[T] = schema match {
    case Schema.Json(codec) =>
      Get[String]
        .temap(parser.parse(_).left.map(_.getMessage))
        .temap(codec.decodeJson(_).left.map(_.getMessage))
    case Schema.String(codec) => Get[String].temap(codec.read)
  }
}
