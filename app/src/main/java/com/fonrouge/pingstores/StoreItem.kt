package com.fonrouge.pingstores

class StoreItem(
    val hostname: String,
    val port: Int,
    val database: String,
    val password: String
) {
    var active = false
}
