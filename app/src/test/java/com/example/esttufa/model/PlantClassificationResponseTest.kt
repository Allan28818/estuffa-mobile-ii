package com.example.esttufa.model

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Test

class PlantClassificationResponseTest {

    @Test
    fun `resolves prediction returned by production api`() {
        val response = Gson().fromJson(
            """{"model":"decision_tree","prediction":"tomato","confidence":null}""",
            PlantClassificationResponse::class.java
        )

        assertEquals("tomato", response.resolvedClassName())
    }

    @Test
    fun `keeps compatibility with legacy class fields`() {
        val response = PlantClassificationResponse(
            model = null,
            prediction = null,
            predicted_class = " lettuce ",
            confidence = null,
            class_name = "arugula"
        )

        assertEquals("lettuce", response.resolvedClassName())
    }
}
