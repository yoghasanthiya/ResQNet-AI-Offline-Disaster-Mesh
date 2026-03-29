package com.example.resqnet.util

import com.example.resqnet.data.model.MessageType

object PriorityScorer {
    fun score(type: MessageType, content: String): Int {
        val normalized = content.lowercase()
        val keywordBonus = listOf("critical", "urgent", "bleeding", "trapped", "collapsed", "oxygen")
            .count { normalized.contains(it) } * 8

        val typeScore = when (type) {
            MessageType.SOS -> 100
            MessageType.MEDICAL -> 90
            MessageType.RESOURCE -> 70
            MessageType.NORMAL -> 40
        }

        return typeScore + keywordBonus + normalized.length.coerceAtMost(40) / 10
    }
}
