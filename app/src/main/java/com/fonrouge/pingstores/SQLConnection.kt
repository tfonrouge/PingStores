package com.fonrouge.pingstores

import java.sql.Connection
import java.sql.DriverManager

fun getSQLConnection(hostname: String, port: Int, database: String, password: String, connectTimeout: Int): Connection? {
    return try {
        DriverManager.setLoginTimeout(connectTimeout)
        DriverManager.getConnection(
            "jdbc:jtds:sqlserver://$hostname:$port/$database",
            "sa",
            password
        )
    } catch (e: Exception) {
        null
    }
}
