package com.gameiq

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.transaction.annotation.EnableTransactionManagement
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableJpaRepositories
@EnableTransactionManagement
@EnableScheduling
class GameIQApplication

fun main(args: Array<String>) {
    runApplication<GameIQApplication>(*args)
}
