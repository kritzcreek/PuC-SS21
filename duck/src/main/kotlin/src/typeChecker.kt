package src

import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.persistentHashMapOf
import kotlinx.collections.immutable.persistentSetOf
import src.parsing.Lexer
import src.parsing.Parser

sealed class Monotype {
    object Number : Monotype()
    object Str : Monotype()
    object Bool : Monotype()
    data class Fun(val arg: Monotype, val res: Monotype) : Monotype() {
        override fun toString(): String = "($arg -> $res)"
    }

    data class Unknown(val u: Int) : Monotype() {
        override fun toString(): String = "u$u"
    }

    data class Var(val name: String) : Monotype() {
        override fun toString(): String = name
    }

    override fun toString(): String {
        return when (this) {
            Bool -> "Bool"
            Number -> "Number"
            is Fun -> "($arg -> $res)"
            is Unknown -> "u$u"
            is Var -> "$name"
            is Str -> "String"
        }
    }

    fun unknowns(): PersistentSet<Int> {
        return when (this) {
            Str, Bool, Number, is Var -> persistentSetOf()
            is Fun -> this.arg.unknowns().addAll(this.res.unknowns())
            is Unknown -> persistentSetOf(this.u)
        }
    }

    fun substitute(v: String, replacement: Monotype): Monotype {
        return when (this) {
            Str, Bool, Number, is Unknown -> this
            is Fun -> Fun(arg.substitute(v, replacement), res.substitute(v, replacement))
            is Var -> if (name == v) {
                replacement
            } else {
                this
            }
        }
    }
}

data class Polytype(val vars: List<String>, val ty: Monotype) {
    fun unknowns(): PersistentSet<Int> = ty.unknowns()
    override fun toString(): String = if (vars.isEmpty()) {
        ty.toString()
    } else {
        "forall ${vars.joinToString(" ")}. $ty"
    }

    companion object {
        fun fromMono(ty: Monotype) = Polytype(emptyList(), ty)
    }
}

fun instantiate(ty: Polytype): Monotype {
    return ty.vars.fold(ty.ty) { t, v -> t.substitute(v, freshUnknown()) }
}

fun generalize(ctx: Context, ty: Monotype): Polytype {
    val ty = applySolution(ty, solution)
    val freeUnknowns = ty.unknowns()
        .removeAll(ctx.flatMap { it.value.unknowns() })
    val localSolution: Solution = hashMapOf()
    val vars = mutableListOf<String>()
    for ((u, name) in freeUnknowns.zip('a'..'z')) {
        localSolution[u] = Monotype.Var(name.toString())
        vars += name.toString()
    }
    return Polytype(vars, applySolution(ty, localSolution))
}

var supply = 0
fun freshUnknown(): Monotype {
    supply += 1
    return Monotype.Unknown(supply)
}

typealias Context = PersistentMap<String, Polytype>

val emptyContext: Context = persistentHashMapOf(
    "fix" to Polytype(
        listOf("a"),
        // forall a. (a -> a) -> a
        Monotype.Fun(
            Monotype.Fun(Monotype.Var("a"), Monotype.Var("a")),
            Monotype.Var("a")
        )
    )
)

typealias Solution = HashMap<Int, Monotype>

var solution: Solution = HashMap()

fun applySolution(ty: Monotype, solution: Solution): Monotype {
    return when (ty) {
       Monotype.Str, Monotype.Bool, Monotype.Number, is Monotype.Var -> ty
        is Monotype.Fun -> Monotype.Fun(applySolution(ty.arg, solution), applySolution(ty.res, solution))
        is Monotype.Unknown ->
            solution[ty.u]?.let { applySolution(it, solution) } ?: ty
    }
}

fun unify(ty1: Monotype, ty2: Monotype) {
    val ty1 = applySolution(ty1, solution)
    val ty2 = applySolution(ty2, solution)
    if (ty1 is Monotype.Number && ty2 is Monotype.Number) {
        return
    } else if (ty1 is Monotype.Bool && ty2 is Monotype.Bool) {
        return
    } else if (ty1 is Monotype.Fun && ty2 is Monotype.Fun) {
        unify(ty1.arg, ty2.arg)
        unify(ty1.res, ty2.res)
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
        is Expr.Str -> Monotype.Str
        is Expr.Lambda -> {
            val tyArg = freshUnknown()
            val tyBody = infer(ctx.put(expr.binder, Polytype.fromMono(tyArg)), expr.body)
            Monotype.Fun(tyArg, tyBody)
        }
        is Expr.Var -> {
            ctx[expr.name]?.let { instantiate(it) } ?: throw Exception("Unbound variable ${expr.name}")
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
            when (expr.operator) {
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
                Operator.Minus -> {
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
            val tmpCtx = ctx.put(expr.binder, generalize(ctx, tyExpr))
            val tyBody = infer(tmpCtx, expr.body)
            tyBody
        }
    }
}

fun testInfer(expr: String, ctx: Context = emptyContext) {
    solution = HashMap()
    val parsed = Parser(Lexer((expr)).lexTokens()).parseExpr()
    try {
        val inferred = applySolution(infer(ctx, parsed), solution)
        println("$expr : ${generalize(emptyContext, inferred)}")
    } catch (e: Exception) {
        println("Failed to infer type of $expr with: ${e.message}")
    }
}

fun main() {
    testInfer("1")
    testInfer("true")
    testInfer("\\x => true")
    testInfer("x")
    testInfer("\\x => x")
    testInfer("""(\f => \x => x) (\x => x + 1) 10""")

    testInfer("""(\f => f (f 10))""")
    testInfer("\\f => \\x => f x + 10") // (Number -> Number) -> Number -> Number
    testInfer("""(\f => f f 10) (\x => x)""")
    testInfer(
        """
        let add3 = \x => x + 3 in
        let twice = \f => \x => f (f x) in
        twice add3 10
    """.trimIndent()
    )
    testInfer(
        """
        let identity = \x => x in
        let x = identity 10 in
        let y = identity true in
        identity
    """.trimIndent()
    )
    testInfer(
        """
        let const = \x => \y => x in
        const 10
    """.trimIndent()
    )
    testInfer(
        """
        let identity = \x => x in
        identity identity 10
    """.trimIndent()
    )
    testInfer(
        """
        (\x => x x) (\x => x x)
    """.trimIndent()
    )
    testInfer(
        """
        let fib = fix \f => \n => 
          if n == 0 then 
            1
          else if n == 1 then 
            1 
          else 
            f (n - 1) + f (n - 2) in
        fib 5
        """.trimIndent()
    )
}