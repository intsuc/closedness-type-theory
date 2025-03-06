package dev.intsuc.ctt

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.core.subcommands
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.plus
import org.eclipse.lsp4j.Diagnostic
import org.eclipse.lsp4j.Range

fun main(args: Array<String>) = Ctt.main(args)

private object Ctt : CliktCommand() {
    init {
        subcommands()
    }

    override fun run() = Unit
}

sealed class Surface {
    abstract val range: Range

    data class Type(override val range: Range) : Surface()
    data class Func(val name: String, val param: Surface, val result: Surface, override val range: Range) : Surface()
    data class FuncOf(val name: String, val result: Surface, override val range: Range) : Surface()
    data class Call(val operator: Surface, val operand: Surface, override val range: Range) : Surface()
    data class Closed(val element: Surface, override val range: Range) : Surface()
    data class Close(val element: Surface, override val range: Range) : Surface()
    data class Open(val element: Surface, override val range: Range) : Surface()
    data class Let(val name: String, val init: Surface, val anno: Surface, val body: Surface, override val range: Range) : Surface()
    data class Var(val name: String, override val range: Range) : Surface()
    data class Hole(override val range: Range) : Surface()
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

fun Core.stringify(): String = when (this) {
    is Core.Type -> "Type"
    is Core.Func -> if (name.isEmpty()) "${param.stringify()} -> ${result.stringify()}" else "($name : ${param.stringify()}) -> ${result.stringify()}"
    is Core.FuncOf -> "{ $name -> ${result.stringify()} }"
    is Core.Call -> "${operator.stringify()}(${operand.stringify()})"
    is Core.Closed -> "Closed(${element.stringify()})"
    is Core.Close -> "close { ${element.stringify()} }"
    is Core.Open -> "open { ${element.stringify()} }"
    is Core.Let -> "let $name = ${init.stringify()} in ${body.stringify()})"
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

class Reporter {
    private val diagnostics: MutableList<Diagnostic> = mutableListOf()
    fun report(range: Range, message: String) = diagnostics.add(Diagnostic(range, message))
}

class Elaborator(private val reporter: Reporter) {
    fun Ctx.elaborate(surface: Surface, type: Value? = null): Pair<Core, Value> = when {
        surface is Surface.Type && type is Value.Type? -> {
            Core.Type to Value.Type
        }
        surface is Surface.Func && type is Value.Type? -> {
            val (param) = elaborate(surface.param, Value.Type)
            val (result) = extend(surface.name, lazy { env.eval(param) }).elaborate(surface.result, Value.Type)
            Core.Func(surface.name, param, result) to Value.Type
        }
        surface is Surface.FuncOf && type == null -> {
            val (result, _) = extend(surface.name, lazyOf(Value.Hole)).elaborate(surface.result)
            Core.FuncOf(surface.name, result).also {
                cannotSynthesizeTypeOf(it, surface.range)
            } to Value.Hole
        }
        surface is Surface.FuncOf && type is Value.Func -> {
            val (result) = extend(surface.name, type.param).elaborate(surface.result, type.result(lazyOf(Value.Var(type.name, env.size))))
            Core.FuncOf(surface.name, result) to type
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
            val (body, bodyType) = extend(surface.name, lazy { env.eval(init) }).elaborate(surface.body, type)
            Core.Let(surface.name, init, body) to bodyType
        }
        surface is Surface.Var && type == null -> {
            when (val level = types.indexOfLast { (name) -> name == surface.name }) {
                -1 -> {
                    variableNotFound(surface.name, surface.range)
                    Core.Hole to Value.Hole
                }
                else -> {
                    Core.Var(surface.name, env.size - level - 1) to types[level].second.value
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
