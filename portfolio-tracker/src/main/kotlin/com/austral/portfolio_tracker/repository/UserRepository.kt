package com.austral.portfolio_tracker.repository

import com.austral.portfolio_tracker.entity.User
import org.springframework.data.jpa.repository.JpaRepository

interface UserRepository : JpaRepository<User, Long> {
    fun findByMail(mail: String): User?
}
