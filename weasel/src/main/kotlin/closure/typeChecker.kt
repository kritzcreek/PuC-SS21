package closure

import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.persistentHashMapOf
import kotlinx.collections.immutable.persistentSetOf

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

    fun unknowns(): PersistentSet<Int> {
        return when(this){
            Bool, Number -> persistentSetOf()
            is Fun -> this.arg.unknowns().addAll(this.res.unknowns())
            is Unknown -> persistentSetOf(this.u)
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

typealias Solution = HashMap<Int, Monotype>
var solution: Solution = HashMap()

fun applySolution(ty: Monotype): Monotype {
    return when(ty){
        Monotype.Bool, Monotype.Number -> ty
        is Monotype.Fun -> Monotype.Fun(applySolution(ty.arg), applySolution(ty.res))
        is Monotype.Unknown ->
            solution[ty.u]?.let { applySolution(it) } ?: ty
    }
}

fun unify(ty1: Monotype, ty2: Monotype) {
    val ty1 = applySolution(ty1)
    val ty2 = applySolution(ty2)
    if (ty1 is Monotype.Number && ty2 is Monotype.Number) {
        return
    } else if (ty1 is Monotype.Bool && ty2 is Monotype.Bool) {
        return
    } else if (ty1 is Monotype.Fun && ty2 is Monotype.Fun) {
        unify(ty1.arg, ty2.arg)
        unify(applySolution(ty1.res), applySolution(ty2.res))
    } else if (ty1 is Monotype.Unknown) {
        solveUnknown(ty1.u, ty2)
    } else if (ty2 is Monotype.Unknown) {
        solveUnknown(ty2.u, ty1)
    } else {
        throw Exception("Can't unify $ty1 with $ty2")
    }
}

fun solveUnknown(u: Int, ty: Monotype) {
    if (ty is Monotype.Unknown && ty.u == u) {
        return
    } else if (ty.unknowns().contains(u)) {
        throw Exception("Occurs check failed for u$u = $ty")
    } else {
        solution[u] = ty
    }
}

fun infer(ctx: Context, expr: Expr): Monotype {
    return when (expr) {
        is Expr.Boolean -> Monotype.Bool
        is Expr.Number -> Monotype.Number
        is Expr.Lambda -> {
            val tyArg = freshUnknown()
            val tyBody = infer(ctx.put(expr.binder, tyArg), expr.body)
            Monotype.Fun(tyArg, tyBody)
        }
        is Expr.Var -> {
            ctx[expr.name] ?: throw Exception("Unbound variable ${expr.name}")
        }
        is Expr.Application -> {
            val tyRes = freshUnknown()

            val tyFun = infer(ctx, expr.func)
            val tyArg = infer(ctx, expr.arg)
            // tyFun = tyArg -> tyRes
            unify(tyFun, Monotype.Fun(tyArg, tyRes))
            tyRes
        }
        is Expr.Binary -> {
            when(expr.operator) {
                Operator.Equals -> {
                    unify(infer(ctx, expr.x), Monotype.Number)
                    unify(infer(ctx, expr.y), Monotype.Number)
                    Monotype.Bool
                }
                Operator.Plus -> {
                    unify(infer(ctx, expr.x), Monotype.Number)
                    unify(infer(ctx, expr.y), Monotype.Number)
                    Monotype.Number
                }
                Operator.Minus ->{
                    unify(infer(ctx, expr.x), Monotype.Number)
                    unify(infer(ctx, expr.y), Monotype.Number)
                    Monotype.Number
                }
                Operator.Multiply -> {
                    unify(infer(ctx, expr.x), Monotype.Number)
                    unify(infer(ctx, expr.y), Monotype.Number)
                    Monotype.Number
                }
            }
        }
        is Expr.If -> {
            unify(infer(ctx, expr.condition), Monotype.Bool)
            val tyReturn = infer(ctx, expr.thenBranch)
            unify(infer(ctx, expr.elseBranch), tyReturn)
            tyReturn
        }
        is Expr.Let -> {
            val tyExpr = infer(ctx, expr.expr)
            val tmpCtx = ctx.put(expr.binder, tyExpr)
            val tyBody = infer(tmpCtx, expr.body)
            tyBody
        }
    }
}

fun testInfer(expr: String, ctx: Context = emptyContext) {
    solution = HashMap()
    val parsed = Parser(Lexer((expr))).parseExpr()
    try {
        val inferred = infer(ctx, parsed)
        println("$expr : ${applySolution(inferred)}")
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
//    testInfer("""(\f => \x => x) (\x => x + 1) 10""")

//    testInfer("""(\f => f (f 10))""")
//    testInfer("\\f => \\x => f x + 10") // (Number -> Number) -> Number -> Number
//    testInfer("""(\f => f f 10) (\x => x)""")
    testInfer("""
        let add3 = \x => x + 3 in
        let twice = \f => \x => f (f x) in
        twice add3 10
    """.trimIndent())
    testInfer("""
        let identity = \x => x in
        let y = identity 10 in
        let z = identity true in
        identity
    """.trimIndent())
}

//\f => \x => f (f x) + 10 : (Number -> Number) -> Number -> Number
// Naechste Woche: Unifikation