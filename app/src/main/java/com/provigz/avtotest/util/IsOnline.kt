package com.provigz.avtotest.util

import java.net.InetAddress

fun isOnline(): Boolean {
    return try {
        val address = InetAddress.getByName("https://avtoizpit.com")
        address.isReachable(5000)
    } catch (e: Exception) {
        false
    }
}