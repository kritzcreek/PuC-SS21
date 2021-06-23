package src

import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentHashMapOf
import src.parsing.Lexer
import src.parsing.Parser

typealias Env = PersistentMap<String, Value>

sealed class Value {
    data class Number(val n: Int) : Value()
    data class Str(val s: kotlin.String) : Value()
    data class Closure(val env: Env, val binder: String, val body: Expr) : Value()
    data class Boolean(val b: kotlin.Boolean) : Value()
}

fun eval(env: Env, expr: Expr): Value {
    return when (expr) {
        is Expr.Str -> Value.Str(expr.s)
        is Expr.Number -> Value.Number(expr.n)
        is Expr.Boolean -> Value.Boolean(expr.b)
        is Expr.Var -> env[expr.name] ?: throw Exception("${expr.name} is not defined.")
        is Expr.Lambda -> Value.Closure(env, expr.binder, expr.body)
        is Expr.Let -> {
            val evaledExpr = eval(env, expr.expr)
            val nestedEnv = env.put(expr.binder, evaledExpr)
            eval(nestedEnv, expr.body)
        }
        is Expr.Application -> {
            val evaledFunc = eval(env, expr.func)
            val evaledArg = eval(env, expr.arg)
            when (evaledFunc) {
                is Value.Closure -> {
                    val newEnv = evaledFunc.env.put(evaledFunc.binder, evaledArg)
                    eval(newEnv, evaledFunc.body)
                }
                else -> throw Exception("$evaledFunc is not a function")
            }
        }
        is Expr.If -> {
            val cond = eval(env, expr.condition) as? Value.Boolean ?: throw Exception("Not a boolean")
            if (cond.b) {
                eval(env, expr.thenBranch)
            } else {
                eval(env, expr.elseBranch)
            }
        }
        is Expr.Binary -> {
            when (expr.operator) {
                Operator.Equals -> equalsValue(eval(env, expr.x), eval(env, expr.y))
                Operator.Multiply ->
                    evalBinaryNumber(eval(env, expr.x), eval(env, expr.y)) { x, y -> x * y }
                Operator.Plus ->
                    evalBinaryNumber(eval(env, expr.x), eval(env, expr.y)) { x, y -> x + y }
                Operator.Minus ->
                    evalBinaryNumber(eval(env, expr.x), eval(env, expr.y)) { x, y -> x - y }
            }
        }
    }
}

fun equalsValue(x: Value, y: Value): Value {
    val v1n = x as? Value.Number ?: throw Exception("Can't compare $x, it's not a number")
    val v2n = y as? Value.Number ?: throw Exception("Can't compare $y, it's not a number")
    return Value.Boolean(v1n.n == v2n.n)
}

fun evalBinaryNumber(v1: Value, v2: Value, f: (Int, Int) -> Int): Value {
    val v1n = v1 as? Value.Number ?: throw Exception("Can't use a binary operation on $v1, it's not a number")
    val v2n = v2 as? Value.Number ?: throw Exception("Can't use a binary operation on $v2, it's not a number")
    return Value.Number(f(v1n.n, v2n.n))
}

fun testEval(expr: String) {
    try {
        println(
            eval(
                persistentHashMapOf()
                ,
                Parser(
                    Lexer(
                        expr
                    )
                )
                    .
                    parseExpr(
                    )
            )
        )

    }
    catch
        (
        ex
                     :
                     Exception) {
        println("Failed to eval with: ${ex.message}")
    }

}

fun main() {
"""
    fun myFunction(input: String): JSONObject{
        return input+=" ist cool"
    }
    let x = "Basti"
    {
        foo: "bar"
        yeet: myFunction(x)
    }
""".trimIndent()
}
