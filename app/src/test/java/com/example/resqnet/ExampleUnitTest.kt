package com.example.resqnet

import com.example.resqnet.data.model.MessageType
import com.example.resqnet.util.PriorityScorer
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun sos_and_medical_messages_score_higher_than_normal_messages() {
        val normalScore = PriorityScorer.score(MessageType.NORMAL, "Need update from sector 4")
        val medicalScore = PriorityScorer.score(MessageType.MEDICAL, "Urgent oxygen and bleeding support needed")
        val sosScore = PriorityScorer.score(MessageType.SOS, "Critical trapped family under collapsed roof")

        assertTrue(medicalScore > normalScore)
        assertTrue(sosScore > medicalScore)
    }
}
