package com.austral.portfolio_tracker.portfolio

import com.austral.portfolio_tracker.entity.Portfolio
import com.austral.portfolio_tracker.entity.User
import jakarta.persistence.Column
import jakarta.persistence.JoinColumn
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.assertNotNull
import kotlin.test.Test

class PortfolioTests {
    private lateinit var history: MutableList<String>
    private lateinit var user: User

    @BeforeEach
    fun setup() {
        history = mutableListOf(("AMC"))
        user = User("1", "John", "Doe", "johndoe@gmail.com", "mysecretpassword")
    }

    @Test
    fun `001 should create a new portfolio`() {
        val portfolio = Portfolio("10928310", user, history)
        assertNotNull(portfolio)
    }

    @Test
    fun `002 new portfolio must be a JPA entity`() {
        val entityAnnotation = Portfolio::class.java.getAnnotation(jakarta.persistence.Entity::class.java)
        assertNotNull(entityAnnotation)
    }

    @Test
    fun `003 new portfolio must not have empty id field and should throw an exception`() {
        assertThrows(IllegalArgumentException::class.java) { Portfolio(" ", user, history) }
    }

    @Test
    fun `004 user must exist`() {
        val portfolio = Portfolio("10928310", user, history)
        assertNotNull(portfolio.user)
    }

    @Test
    fun `005 history must be empty by default`() {
        val portfolio = Portfolio(user = user)
        assertNotNull(portfolio.history)
    }

    @Test
    fun `006 id must be generated automatically`() {
        val portfolio = Portfolio(user = user, history = history)
        assertNotNull(portfolio.id)
    }

    @Test
    fun `007 portfolio must be an entity table`() {
        val tableAnnotation = Portfolio::class.java.getAnnotation(jakarta.persistence.Table::class.java)
        assertNotNull(tableAnnotation)
    }

    @Test
    fun `008 portfolio must be mapped to an entity table`() {
        val tableAnnotation = Portfolio::class.java.getAnnotation(jakarta.persistence.Table::class.java)
        assertNotNull(tableAnnotation)
        assertEquals("portfolio", tableAnnotation.name)
    }

    @Test
    fun `009 id must be a column`() {
        val column =
            Portfolio::class.java
                .getDeclaredField("id")
                .getAnnotation(Column::class.java)

        assertNotNull(column)
        assertEquals("id", column.name)
        assertEquals(false, column.nullable)
        assertEquals(true, column.unique)
    }

    @Test
    fun `010 user_id must be a column`() {
        val joinColumn =
            Portfolio::class.java
                .getDeclaredField("user")
                .getAnnotation(JoinColumn::class.java)

        assertNotNull(joinColumn)
        assertEquals("user_id", joinColumn.name)
        assertEquals(false, joinColumn.nullable)
        assertEquals(true, joinColumn.unique)
    }

    @Test
    fun `011 id must be a column`() {
        val column =
            Portfolio::class.java
                .getDeclaredField("history")
                .getAnnotation(Column::class.java)

        assertNotNull(column)
        assertEquals("history", column.name)
        assertEquals(false, column.nullable)
        assertEquals(true, column.unique)
    }

    @Test
    fun `010 user field must be accessible`() {
        val portfolio = Portfolio(user = user, history = history)
        assertNotNull(portfolio.user)
        assertEquals(user, portfolio.user)
    }

    @Test
    fun `011 history field must be accessible`() {
        val portfolio = Portfolio(user = user, history = history)
        assertNotNull(portfolio.history)
        assertEquals(history, portfolio.history)
    }
}
