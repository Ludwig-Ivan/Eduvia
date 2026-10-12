package com.ludwig.eduvia

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform