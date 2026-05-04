package com.austral.portfolio_tracker

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import java.util.TimeZone

@SpringBootApplication
class PortfolioTrackerApplication {
	companion object {
		init {
			TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
		}
	}
}

fun main(args: Array<String>) {
	runApplication<PortfolioTrackerApplication>(*args)
}
