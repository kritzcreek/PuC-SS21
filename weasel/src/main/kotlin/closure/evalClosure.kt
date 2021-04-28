package closure

import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentHashMapOf

sealed class Expr {
    data class Var(val name: String) : Expr()
    data class Lambda(val binder: String, val body: Expr) : Expr()
    data class Application(val func: Expr, val arg: Expr) : Expr()
    data class Number(val n: Int) : Expr()
    data class Boolean(val b: kotlin.Boolean) : Expr()
    data class Addition(val summand1: Expr, val summand2: Expr) : Expr()
    data class Equals(val x: Expr, val y: Expr) : Expr()
    data class If(val condition: Expr, val thenBranch: Expr, val elseBranch: Expr) : Expr()
}

typealias Env = PersistentMap<String, Value>

sealed class Value {
    data class Number(val n: Int) : Value()
    data class Closure(val env: Env, val binder: String, val body: Expr) : Value()
    data class Boolean(val b: kotlin.Boolean) : Value()
}

fun eval(env: Env, expr: Expr): Value {
    return when (expr) {
        is Expr.Number -> Value.Number(expr.n)
        is Expr.Boolean -> Value.Boolean(expr.b)
        is Expr.Var -> env[expr.name] ?: throw Exception("${expr.name} is not defined.")
        is Expr.Lambda -> Value.Closure(env, expr.binder, expr.body)
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
        is Expr.Addition -> {
            plusValue(eval(env, expr.summand1), eval(env, expr.summand2))
        }
        is Expr.Equals -> {
            equalsValue(eval(env, expr.x), eval(env, expr.y))
        }
    }
}

fun equalsValue(x: Value, y: Value): Value {
    val v1n = x as? Value.Number ?: throw Exception("Can't compare $x, it's not a number")
    val v2n = y as? Value.Number ?: throw Exception("Can't compare $y, it's not a number")
    return Value.Boolean(v1n.n == v2n.n)
}

fun plusValue(v1: Value, v2: Value): Value {
    val v1n = v1 as? Value.Number ?: throw Exception("Can't add $v1, it's not a number")
    val v2n = v2 as? Value.Number ?: throw Exception("Can't add $v2, it's not a number")
    return Value.Number(v1n.n + v2n.n)
}

fun test(expr: Expr) {
    try {
        println(eval(persistentHashMapOf(), expr))
    } catch (ex: Exception) {
        println("Failed to eval with: ${ex.message}")
    }

}

// sum(0) == 0
// sum(n) == n + sum(n - 1)

fun sum(n: Int): Int =
    if (n == 0) {
        0
    } else {
        n + sum(n - 1)
    }

fun main() {
    val identity = Expr.Lambda("x", Expr.Var("x"))
    test(Expr.Number(10))
    test(Expr.Var("x"))
    test(identity)
    test(Expr.Application(identity, Expr.Number(20)))
    test(
        Expr.Application(
            Expr.Lambda("x", Expr.Lambda("y", Expr.Var("x"))),
            Expr.Number(20)
        )
    )
    test(
        Expr.Application(
            Expr.Application(
                Expr.Lambda("x", Expr.Lambda("y", Expr.Var("x"))), Expr.Number(20)
            ), Expr.Number(0)
        )
    )
    test(Expr.Application(Expr.Number(5), Expr.Number(20)))
    test(Expr.Addition(Expr.Number(5), Expr.Number(10)))
    test(Expr.Addition(identity, Expr.Number(10)))
    test(Expr.If(Expr.Boolean(true), Expr.Number(42), Expr.Number(10)))
    test(Expr.If(Expr.Boolean(false), Expr.Number(42), Expr.Number(10)))
    test(Expr.Equals(Expr.Number(10), Expr.Number(10)))
    test(Expr.Equals(Expr.Number(10), Expr.Number(11)))
    test(
        Expr.If(
            Expr.Equals(Expr.Number(10), Expr.Number(10)),
            Expr.Number(42),
            Expr.Number(10)
        )
    )

    println(sum(5)) // 1 + 2 + 3 + 4 + 5 == 15

    // (\x => f(\v => x x v))
    val innerZ = Expr.Lambda(
        "x",
        Expr.Application(
            Expr.Var("f"),
            Expr.Lambda(
                "v", Expr.Application(
                    Expr.Application(Expr.Var("x"), Expr.Var("x")), Expr.Var("v")
                )
            )
        )
    )
    // Z: \f => innerZ innerZ
    val z = Expr.Lambda("f", Expr.Application(innerZ, innerZ))

    // \f -> \n -> if n == 0 then 0 else n + f (n + -1)
    val sumSingle = Expr.Lambda(
        "sum", Expr.Lambda(
            "n", Expr.If(
                Expr.Equals(Expr.Var("n"), Expr.Number(0)),
                Expr.Number(0),
                Expr.Addition(
                    Expr.Var("n"),
                    Expr.Application(
                        Expr.Var("sum"),
                        Expr.Addition(Expr.Var("n"), Expr.Number(-1))
                    )
                )

            )
        )
    )

    val sumLambda = Expr.Application(z, sumSingle)

    test(Expr.Application(sumLambda, Expr.Number(5)))

    // Homework: Fibonacci
    // fib(0) = 1
    // fib(1) = 1
    // fib(n) = fib(n + -1) + fib(n + -2)
}
