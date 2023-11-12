package com.github.paulpaulych.intermirrorbot

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class IntermirrorBotApplication

fun main(args: Array<String>) {
	runApplication<IntermirrorBotApplication>(*args)
}
