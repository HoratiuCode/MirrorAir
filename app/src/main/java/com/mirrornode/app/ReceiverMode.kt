package com.mirrornode.app

enum class ReceiverMode(
    val storageValue: String,
) {
    AIRPLAY("airplay"),
    TV("tv"),
    ;

    companion object {
        fun fromStorageValue(value: String?): ReceiverMode {
            return entries.firstOrNull { it.storageValue.equals(value, ignoreCase = true) } ?: AIRPLAY
        }
    }
}
