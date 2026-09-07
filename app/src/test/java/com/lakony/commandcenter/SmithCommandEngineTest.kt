package com.lakony.commandcenter

import com.lakony.commandcenter.logic.SmithCommandEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SmithCommandEngineTest {
    @Test
    fun developerCommandRoutesHome() {
        val result = SmithCommandEngine.run("Smith, dev mode")
        assertEquals("Home", result.destination)
    }

    @Test
    fun addTaskReturnsTaskTitle() {
        val result = SmithCommandEngine.run("add task Finish database")
        assertEquals("Finish database", result.taskToAdd)
        assertEquals("Tasks", result.destination)
    }

    @Test
    fun blankCommandDoesNothing() {
        val result = SmithCommandEngine.run("   ")
        assertNull(result.destination)
        assertEquals("Enter a command first.", result.message)
    }
}
