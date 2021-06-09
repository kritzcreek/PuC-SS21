package closure

import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentHashMapOf

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
        println(eval(persistentHashMapOf("fix" to z), Parser(Lexer(expr)).parseExpr()))
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

val z = eval(
    persistentHashMapOf(),
    Parser(Lexer("""\f => (\x => f(\v => x x v)) (\x => f(\v => x x v))""")).parseExpr()
)

fun main() {
//    val identity = Expr.Lambda("x", Expr.Var("x"))
//    test(Expr.Number(10))
//    test(Expr.Var("x"))
//    test(identity)
//    test(Expr.Application(identity, Expr.Number(20)))
//    test(
//        Expr.Application(
//            Expr.Lambda("x", Expr.Lambda("y", Expr.Var("x"))),
//            Expr.Number(20)
//        )
//    )
//    test(
//        Expr.Application(
//            Expr.Application(
//                Expr.Lambda("x", Expr.Lambda("y", Expr.Var("x"))), Expr.Number(20)
//            ), Expr.Number(0)
//        )
//    )
//    test(Expr.Application(Expr.Number(5), Expr.Number(20)))
//    test(Expr.Binary(Operator.Plus, Expr.Number(5), Expr.Number(10)))
//    test(Expr.Binary(Operator.Plus, identity, Expr.Number(10)))
//    test(Expr.If(Expr.Boolean(true), Expr.Number(42), Expr.Number(10)))
//    test(Expr.If(Expr.Boolean(false), Expr.Number(42), Expr.Number(10)))
//    test(Expr.Binary(Operator.Equals, Expr.Number(10), Expr.Number(10)))
//    test(Expr.Binary(Operator.Equals, Expr.Number(10), Expr.Number(11)))
//    test(
//        Expr.If(
//            Expr.Binary(Operator.Equals, Expr.Number(10), Expr.Number(10)),
//            Expr.Number(42),
//            Expr.Number(10)
//        )
//    )
//
//    println(sum(5)) // 1 + 2 + 3 + 4 + 5 == 15
//
//    val z = Parser(Lexer("""\f => (\x => f(\v => x x v)) (\x => f(\v => x x v))""")).parseExpr()
//    val sumSingle = Parser(Lexer("""\f => \n => if n == 0 then 0 else n + f (n - 1)""")).parseExpr()
//
//    val sumLambda = Expr.Application(z, sumSingle)
//
//    test(Expr.Application(sumLambda, Expr.Number(5)))
    testEval(
        """
        let add3 = \x => x + 3 in
        let twice = \f => \x => f (f x) in
        twice add3 10
    """.trimIndent()
    )

    testEval(
        """
        let fib = fix \fib => \n => 
          if n == 0 then 
            1 
          else if n == 1 then 
            1 
          else 
            fib (n - 1) + fib (n - 2) in
        fib 5
        """.trimIndent()
    )

    // Homework: Fibonacci
    // fib(0) = 1
    // fib(1) = 1
    // fib(n) = fib(n + -1) + fib(n + -2)
}
