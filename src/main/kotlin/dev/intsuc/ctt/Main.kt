package dev.intsuc.ctt

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.core.subcommands

fun main(args: Array<String>) = Ctt.main(args)

private object Ctt : CliktCommand() {
    init {
        subcommands()
    }

    override fun run() = Unit
}
