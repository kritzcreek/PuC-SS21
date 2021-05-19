package closure

import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentHashMapOf

sealed class Monotype {
    object Number: Monotype()
    object Bool: Monotype()
    data class Fun(val arg: Monotype, val res: Monotype): Monotype() {
        override fun toString(): String = "($arg -> $res)"
    }
    data class Unknown(val u: Int): Monotype() {
        override fun toString(): String = "u$u"
    }

    override fun toString(): String {
        return when(this) {
            Bool -> "Bool"
            Number -> "Number"
            is Fun -> "($arg -> $res)"
            is Unknown -> "u$u"
        }
    }
}

var supply = 0
fun freshUnknown(): Monotype {
    supply += 1
    return Monotype.Unknown(supply)
}

typealias Context = PersistentMap<String, Monotype>
val emptyContext: Context  = persistentHashMapOf()

val equalities: MutableList<Pair<Monotype, Monotype>> = mutableListOf()

fun infer(ctx: Context, expr: Expr): Monotype {
    when (expr) {
        is Expr.Boolean -> return Monotype.Bool
        is Expr.Number -> return Monotype.Number
        is Expr.Lambda -> {
            val tyArg = freshUnknown()
            val tyBody = infer(ctx.put(expr.binder, tyArg), expr.body)
            return Monotype.Fun(tyArg, tyBody)
        }
        is Expr.Var -> {
            return ctx[expr.name] ?: throw Exception("Unbound variable ${expr.name}")
        }
        is Expr.Application -> {
            val tyArg = infer(ctx, expr.arg)
            val tyFun = infer(ctx, expr.func)
            // tyFun = tyRes -> tyArg
            val tyRes = freshUnknown()
            equalities += tyFun to Monotype.Fun(tyArg, tyRes)
            return tyRes
        }
        is Expr.Binary -> {
            when(expr.operator) {
                Operator.Equals -> {
                    equalities += infer(ctx, expr.x) to Monotype.Number
                    equalities += infer(ctx, expr.y) to Monotype.Number
                    return Monotype.Bool
                }
                Operator.Plus -> {
                    equalities += infer(ctx, expr.x) to Monotype.Number
                    equalities += infer(ctx, expr.y) to Monotype.Number
                    return Monotype.Number
                }
                Operator.Minus ->{
                    equalities += infer(ctx, expr.x) to Monotype.Number
                    equalities += infer(ctx, expr.y) to Monotype.Number
                    return Monotype.Number
                }
                Operator.Multiply -> {
                    equalities += infer(ctx, expr.x) to Monotype.Number
                    equalities += infer(ctx, expr.y) to Monotype.Number
                    return Monotype.Number
                }
            }
        }
        is Expr.If -> TODO()
    }
}

fun printEqualities() {
    for ((t1, t2) in equalities) {
        println("$t1 == $t2")
    }
}

fun testInfer(expr: String, ctx: Context = emptyContext) {
    val parsed = Parser(Lexer((expr))).parseExpr()
    try {
        val inferred = infer(ctx, parsed)
        printEqualities()
        println("$expr : $inferred")
    } catch (e: Exception) {
        println("Failed to infer type of $expr with: ${e.message}")
    }
}

fun main() {
//    testInfer("1")
//    testInfer("true")
//    testInfer("\\x => true")
//    testInfer("x")
//    testInfer("x", persistentHashMapOf("x" to Monotype.Number))
//    testInfer("\\x => x")
    testInfer("\\f => \\x => f (f x) + 10") // (Number -> Number) -> Number -> Number
}

//\f => \x => f (f x) + 10 : (Number -> Number) -> Number -> Number
// Naechste Woche: Unifikation