package com.example.gymtracker

import com.example.gymtracker.data.CATEGORIES
import com.example.gymtracker.data.WeightAction.DECREASE
import com.example.gymtracker.data.WeightAction.HOLD
import com.example.gymtracker.data.WeightAction.INCREASE
import com.example.gymtracker.data.groupSuggestion
import com.example.gymtracker.ui.BackView
import com.example.gymtracker.ui.FrontView
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BodyDiagramTest {
    @Test
    fun groupFollowsTheMoreCommonCall() {
        assertEquals(INCREASE, groupSuggestion(listOf(INCREASE, INCREASE, DECREASE, HOLD)))
        assertEquals(DECREASE, groupSuggestion(listOf(DECREASE, HOLD, HOLD)))
        assertNull(groupSuggestion(listOf(INCREASE, DECREASE)))
        assertNull(groupSuggestion(listOf(HOLD)))
        assertNull(groupSuggestion(emptyList()))
    }

    @Test
    fun everyBodyPartIsARealCategory() {
        val groups = (FrontView.parts + BackView.parts).map { it.group }.filter { it.isNotEmpty() }.toSet()
        assertTrue("unknown groups: ${groups - CATEGORIES.toSet()}", CATEGORIES.containsAll(groups))
        // Every category except "All" shows up somewhere on the body.
        assertEquals(CATEGORIES.drop(1).toSet(), groups)
        (FrontView.glyphAt.keys + BackView.glyphAt.keys).forEach { assertTrue(it in groups) }
    }
}
