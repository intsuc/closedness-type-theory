package dev.intsuc.ctt

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.core.subcommands
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.plus
import org.eclipse.lsp4j.Diagnostic
import org.eclipse.lsp4j.Position
import org.eclipse.lsp4j.Range

fun main(args: Array<String>) = Ctt.main(args)

private object Ctt : CliktCommand() {
    init {
        subcommands(Lsp)
    }

    override fun run() = Unit
}

private object Lsp : CliktCommand() {
    override fun run() = CttLanguageServer.launch()
}

sealed class Surface {
    abstract val range: Range

    data class Type(override val range: Range) : Surface()
    data class Func(val name: Name, val param: Surface, val result: Surface, override val range: Range) : Surface()
    data class FuncOf(val name: Name, val result: Surface, override val range: Range) : Surface()
    data class Call(val operator: Surface, val operand: Surface, override val range: Range) : Surface()
    data class Closed(val element: Surface, override val range: Range) : Surface()
    data class Close(val element: Surface, override val range: Range) : Surface()
    data class Open(val element: Surface, override val range: Range) : Surface()
    data class Let(val name: Name, val init: Surface, val anno: Surface, val body: Surface, override val range: Range) : Surface()
    data class Var(val name: Name, override val range: Range) : Surface()
    data class Hole(override val range: Range) : Surface()

    data class Name(val text: String, val range: Range)
}

sealed class Core {
    data object Type : Core()
    data class Func(val name: String, val param: Core, val result: Core) : Core()
    data class FuncOf(val name: String, val result: Core) : Core()
    data class Call(val operator: Core, val operand: Core) : Core()
    data class Closed(val element: Core) : Core()
    data class Close(val element: Core) : Core()
    data class Open(val element: Core) : Core()
    data class Let(val name: String, val init: Core, val body: Core) : Core()
    data class Var(val name: String, val index: Int) : Core()
    data object Hole : Core()
}

sealed class Value {
    data object Type : Value()
    data class Func(val name: String, val param: Lazy<Value>, val result: (Lazy<Value>) -> Value) : Value()
    data class FuncOf(val name: String, val result: (Lazy<Value>) -> Value) : Value()
    data class Call(val operator: Value, val operand: Lazy<Value>) : Value()
    data class Closed(val element: Lazy<Value>) : Value()
    data class Close(val element: Lazy<Value>) : Value()
    data class Open(val element: Lazy<Value>) : Value()
    data class Var(val name: String, val level: Int) : Value()
    data object Hole : Value()
}

class Reporter {
    private val diagnostics: MutableList<Diagnostic> = mutableListOf()
    fun report(range: Range, message: String) = diagnostics.add(Diagnostic(range, message))
}

class Parser(private val reporter: Reporter, private val text: String) {
    private val length: Int = text.length
    private var cursor: Int = 0
    private var line: Int = 0
    private var character: Int = 0

    fun parse(): Surface {
        val surface = parseSurface()
        skipWhitespace()
        if (cursor < length) {
            reporter.report(Range(here(), here()), "Expected end of file")
        }
        return surface
    }

    private fun parseSurface(): Surface {
        return if (cursor < length) {
            skipWhitespace()
            from {
                when (text[cursor]) {
                    '(' -> {
                        skipCharacters()
                        val surface = parseSurface()
                        expect(")")
                        surface
                    }
                    '{' -> {
                        skipCharacters()
                        val name = parseName()
                        expect("->")
                        val result = parseSurface()
                        expect("}")
                        Surface.FuncOf(name, result, until())
                    }
                    else -> {
                        val name = parseName()
                        when (name.text) {
                            "Type" -> Surface.Type(until())
                            "Func" -> {
                                expect("(")
                                val name = parseName()
                                val param = if (name.text.isEmpty()) {
                                    parseSurface()
                                } else {
                                    expect(":")
                                    parseSurface()
                                }
                                expect(")")
                                expect("->")
                                val result = parseSurface()
                                Surface.Func(name, param, result, until())
                            }
                            "Closed" -> {
                                expect("{")
                                val element = parseSurface()
                                expect("}")
                                Surface.Closed(element, until())
                            }
                            "close" -> {
                                expect("{")
                                val element = parseSurface()
                                expect("}")
                                Surface.Close(element, until())
                            }
                            "open" -> {
                                expect("{")
                                val element = parseSurface()
                                expect("}")
                                Surface.Open(element, until())
                            }
                            "let" -> {
                                val name = parseName()
                                expect(":")
                                val anno = parseSurface()
                                expect("=")
                                val init = parseSurface()
                                expect(";")
                                val body = parseSurface()
                                Surface.Let(name, anno, init, body, until())
                            }
                            else -> Surface.Var(name, until())
                        }
                    }
                }
            }
        } else {
            val range = Range(here(), here())
            reporter.report(range, "Unexpected end of file")
            Surface.Hole(range)
        }
    }

    private fun parseName(): Surface.Name = from {
        val name = parseWord()
        Surface.Name(name, until())
    }

    private fun parseWord(): String {
        skipWhitespace()
        val start = cursor
        while (
            cursor < length && when (text[cursor]) {
                ' ', '\t', '\n', '\r', '-', '(', ')', ':', '{', '}', '=', ';' -> false
                else -> true
            }
        ) {
            skipCharacters()
        }
        return text.substring(start, cursor)
    }

    private fun expect(string: String) {
        skipWhitespace()
        if (cursor < length && text.startsWith(string, cursor)) {
            skipCharacters(string.length)
        } else {
            reporter.report(Range(here(), here().apply { character += string.length }), "Expected '$string'")
            skip()
        }
    }

    private fun skipWhitespace() {
        while (cursor < length) {
            skip {
                when (it) {
                    ' ', '\t' -> skipCharacters()
                    else -> return
                }
            }
        }
    }

    private inline fun skip(action: (Char) -> Unit = { skipCharacters() }) {
        when (val char = text[cursor]) {
            '\n' -> {
                cursor++
                line++
                character = 0
            }
            '\r' -> {
                cursor++
                if (cursor < length && text[cursor] == '\n') {
                    cursor++
                }
                line++
                character = 0
            }
            else -> action(char)
        }
    }

    private fun skipCharacters(size: Int = 1) {
        cursor += size
        character += size
    }

    private inner class RangeContext(val start: Position) {
        fun until(): Range = Range(start, here())
    }

    private inline fun <R> from(block: RangeContext.() -> R): R = RangeContext(here()).block()

    private fun here(): Position = Position(line, character)
}

fun Core.stringify(): String = when (this) {
    is Core.Type -> "Type"
    is Core.Func -> "Func ($name : ${param.stringify()}) -> ${result.stringify()}"
    is Core.FuncOf -> "{ $name -> ${result.stringify()} }"
    is Core.Call -> "${operator.stringify()}(${operand.stringify()})"
    is Core.Closed -> "Closed { ${element.stringify()} }"
    is Core.Close -> "close { ${element.stringify()} }"
    is Core.Open -> "open { ${element.stringify()} }"
    is Core.Let -> "let $name = ${init.stringify()}; ${body.stringify()})"
    is Core.Var -> name
    is Core.Hole -> "_"
}

typealias Env = PersistentList<Lazy<Value>>

fun Env.eval(core: Core): Value = when (core) {
    Core.Type -> Value.Type
    is Core.Func -> Value.Func(core.name, lazy { eval(core.param) }) { arg -> plus(arg).eval(core.result) }
    is Core.FuncOf -> Value.FuncOf(core.name) { arg -> plus(arg).eval(core.result) }
    is Core.Call -> when (val operator = eval(core.operator)) {
        is Value.FuncOf -> operator.result(lazy { eval(core.operand) })
        else -> Value.Call(operator, lazy { eval(core.operand) })
    }
    is Core.Closed -> Value.Closed(lazy { eval(core.element) })
    is Core.Close -> Value.Close(lazy { eval(core.element) })
    is Core.Open -> Value.Open(lazy { eval(core.element) })
    is Core.Let -> plus(lazy { eval(core.init) }).eval(core.body)
    is Core.Var -> get(size - core.index - 1).value
    Core.Hole -> Value.Hole
}

fun Int.quote(value: Value): Core = when (value) {
    Value.Type -> Core.Type
    is Value.Func -> Core.Func(value.name, quote(value.param.value), inc().quote(value.result(value.param)))
    is Value.FuncOf -> Core.FuncOf(value.name, inc().quote(value.result(lazyOf(Value.Var(value.name, this)))))
    is Value.Call -> Core.Call(quote(value.operator), quote(value.operand.value))
    is Value.Closed -> Core.Closed(quote(value.element.value))
    is Value.Close -> Core.Close(quote(value.element.value))
    is Value.Open -> Core.Open(quote(value.element.value))
    is Value.Var -> Core.Var(value.name, this - value.level - 1)
    Value.Hole -> Core.Hole
}

fun Int.conv(value1: Value, value2: Value): Boolean = when {
    value1 == Value.Hole || value2 == Value.Hole -> true
    value1 == Value.Type && value2 == Value.Type -> true
    value1 is Value.Func && value2 is Value.Func -> conv(value1.param.value, value2.param.value) && inc().conv(value1.result(value1.param), value2.result(value2.param))
    value1 is Value.FuncOf && value2 is Value.FuncOf -> lazyOf(Value.Var("_", this)).let { arg -> inc().conv(value1.result(arg), value2.result(arg)) }
    value1 is Value.Call && value2 is Value.Call -> conv(value1.operator, value2.operator) && conv(value1.operand.value, value2.operand.value)
    value1 is Value.Closed && value2 is Value.Closed -> conv(value1.element.value, value2.element.value)
    value1 is Value.Open && value2 is Value.Open -> conv(value1.element.value, value2.element.value)
    value1 is Value.Close && value2 is Value.Close -> conv(value1.element.value, value2.element.value)
    value1 is Value.Var && value2 is Value.Var -> value1.level == value2.level
    else -> false
}

data class Ctx(val env: Env, val types: PersistentList<Pair<String, Lazy<Value>>>) {
    fun extend(name: String, type: Lazy<Value>, value: Lazy<Value>? = null): Ctx = Ctx(
        env + (value ?: lazyOf(Value.Var(name, env.size))),
        types + (name to type),
    )
}

class Elaborator(private val reporter: Reporter) {
    fun Ctx.elaborate(surface: Surface, type: Value? = null): Pair<Core, Value> = when {
        surface is Surface.Type && type is Value.Type? -> {
            Core.Type to Value.Type
        }
        surface is Surface.Func && type is Value.Type? -> {
            val (param) = elaborate(surface.param, Value.Type)
            val (result) = extend(surface.name.text, lazy { env.eval(param) }).elaborate(surface.result, Value.Type)
            Core.Func(surface.name.text, param, result) to Value.Type
        }
        surface is Surface.FuncOf && type == null -> {
            val (result, _) = extend(surface.name.text, lazyOf(Value.Hole)).elaborate(surface.result)
            Core.FuncOf(surface.name.text, result).also {
                cannotSynthesizeTypeOf(it, surface.range)
            } to Value.Hole
        }
        surface is Surface.FuncOf && type is Value.Func -> {
            val (result) = extend(surface.name.text, type.param).elaborate(surface.result, type.result(lazyOf(Value.Var(type.name, env.size))))
            Core.FuncOf(surface.name.text, result) to type
        }
        surface is Surface.Call && type == null -> {
            val (operator, operatorType) = elaborate(surface.operator)
            when (operatorType) {
                is Value.Func -> {
                    val (operand) = elaborate(surface.operand, operatorType.param.value)
                    val type = operatorType.result(lazy { env.eval(operand) })
                    Core.Call(operator, operand) to type
                }
                else -> {
                    typeMismatch(Core.Func("", Core.Hole, Core.Hole), env.size.quote(operatorType), surface.operator.range)
                    val (operand) = elaborate(surface.operand, Value.Hole)
                    Core.Call(operator, operand) to Value.Hole
                }
            }
        }
        surface is Surface.Closed && type is Value.Type? -> {
            val (element) = elaborate(surface.element, Value.Type)
            Core.Closed(element) to Value.Type
        }
        surface is Surface.Close && type is Value.Closed? -> {
            val (element, elementType) = elaborate(surface.element, type?.element?.value)
            Core.Close(element) to Value.Closed(lazyOf(elementType))
        }
        surface is Surface.Open && type == null -> {
            val (element, elementType) = elaborate(surface.element)
            when (elementType) {
                is Value.Closed -> Core.Open(element) to elementType.element.value
                else -> {
                    typeMismatch(Core.Closed(Core.Hole), env.size.quote(elementType), surface.element.range)
                    Core.Open(element) to Value.Hole
                }
            }
        }
        surface is Surface.Let -> {
            val (init) = elaborate(surface.init, Value.Type)
            val (body, bodyType) = extend(surface.name.text, lazy { env.eval(init) }).elaborate(surface.body, type)
            Core.Let(surface.name.text, init, body) to bodyType
        }
        surface is Surface.Var && type == null -> {
            when (val level = types.indexOfLast { (name) -> name == surface.name.text }) {
                -1 -> {
                    variableNotFound(surface.name.text, surface.range)
                    Core.Hole to Value.Hole
                }
                else -> {
                    Core.Var(surface.name.text, env.size - level - 1) to types[level].second.value
                }
            }
        }
        surface is Surface.Hole -> {
            Core.Hole to (type ?: Value.Hole)
        }
        type != null -> {
            val (core, coreType) = elaborate(surface)
            if (!env.size.conv(coreType, type)) {
                typeMismatch(env.size.quote(type), env.size.quote(coreType), surface.range)
            }
            core to type
        }
        else -> error("Unreachable: surface=$surface, type=$type")
    }

    private fun cannotSynthesizeTypeOf(core: Core, range: Range) = reporter.report(range, "Cannot synthesize type of ${core.stringify()}")

    private fun typeMismatch(expected: Core, actual: Core, range: Range) = reporter.report(range, "Type mismatch: expected ${expected.stringify()}, actual ${actual.stringify()}")

    private fun variableNotFound(name: String, range: Range) = reporter.report(range, "Variable not found: $name")
}
