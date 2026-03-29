package com.mirrornode.app

data class ReceiverState(
    val receiverName: String,
    val receiverCode: String,
    val statusText: String,
    val running: Boolean,
    val detailText: String,
)
