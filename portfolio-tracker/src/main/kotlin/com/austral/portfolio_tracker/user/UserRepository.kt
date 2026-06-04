package com.austral.portfolio_tracker.user

import com.austral.portfolio_tracker.entities.User
import org.springframework.data.jpa.repository.JpaRepository

interface UserRepository : JpaRepository<User, Long> {
    fun findByMail(mail: String): User?

    fun findByMailIgnoreCase(mail: String): User?

    fun existsByMailIgnoreCase(mail: String): Boolean
}
