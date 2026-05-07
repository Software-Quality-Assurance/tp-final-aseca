package com.austral.portfolio_tracker.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.util.UUID

@Entity
@Table(name = "portfolio")
data class Portfolio(
    @Id
    @NotBlank
    @Column(name = "id", nullable = false, unique = true)
    val id: String = UUID.randomUUID().toString(),
    @OneToOne
    @NotNull
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    val user: User,
    @Column(name = "history", nullable = false, unique = true)
    val history: List<String>,
) {
    init {
        require(id.isNotBlank()) { "Id must not be blank" }
    }
}
