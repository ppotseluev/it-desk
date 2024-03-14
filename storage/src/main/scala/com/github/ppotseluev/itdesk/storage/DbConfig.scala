package com.github.ppotseluev.itdesk.storage

case class DbConfig(
    host: String,
    port: Int,
    user: String,
    database: String,
    password: String
)
