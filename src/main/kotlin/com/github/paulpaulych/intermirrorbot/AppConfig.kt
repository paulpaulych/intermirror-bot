package com.github.paulpaulych.intermirrorbot

import org.ktorm.database.Database
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import javax.sql.DataSource

@Configuration
class AppConfig {

    @Bean
    fun database(dataSource: DataSource): Database {
        return Database.Companion.connectWithSpringSupport(dataSource)
    }
}