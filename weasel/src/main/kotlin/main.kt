sealed class Expr {
    data class Var(val name: String) : Expr()
    data class Lambda(val binder: String, val body: Expr) : Expr()
    data class Application(val func: Expr, val arg: Expr) : Expr()
    data class Number(val n: Int) : Expr()
}

fun eval(expr: Expr): Expr {
    return when (expr) {
        is Expr.Application -> {
            val evaledFunc = eval(expr.func)
            val evaledArg = eval(expr.arg)
            when (evaledFunc) {
                is Expr.Lambda -> eval(substitute(evaledFunc.binder, evaledArg, evaledFunc.body))
                else -> Expr.Application(evaledFunc, evaledArg)
            }
        }
        is Expr.Number, is Expr.Var, is Expr.Lambda -> expr
    }
}

fun substitute(binder: String, replacement: Expr, expr: Expr): Expr {
    return when (expr) {
        is Expr.Application -> Expr.Application(
            substitute(binder, replacement, expr.func),
            substitute(binder, replacement, expr.arg)
        )
        is Expr.Lambda -> {
            if (expr.binder == binder) {
                expr
            } else {
                Expr.Lambda(
                    expr.binder,
                    substitute(binder, replacement, expr.body)
                )
            }
        }
        is Expr.Number -> expr
        is Expr.Var -> if (expr.name == binder) replacement else expr
    }
}


fun main() {
//    val result = { x : Int -> { y : Int -> x }}(10)(2) == 10
//    val shadowed = { x : Int -> { x : Int -> x }}(10)(2) == 2
//    val complexExpr =
//        { f: (Int) -> Int ->
//            { y: Int -> f(f(y)) }
//        }({ x: Int -> x + 1 })(40)

//    val expr = Expr.Application(
//        Expr.Lambda("x", Expr.Var("x")),
//        Expr.Number(10)
//    )
//    println("${eval(expr)}")
//    val expr = Expr.Application(
//        Expr.Application(
//            Expr.Lambda("x", Expr.Lambda("y", Expr.Var("x"))),
//            Expr.Number(10)
//        ), Expr.Number(20)
//    )
//    println("${eval(expr)}")

//    val expr = Expr.Application(
//        Expr.Application(
//            Expr.Lambda(
//                "f", Expr.Lambda(
//                    "y",
//                    Expr.Application(
//                        Expr.Var("f"),
//                        Expr.Application(
//                            Expr.Var("f"),
//                            Expr.Var("y")
//                        )
//                    )
//                )
//            ),
//            Expr.Lambda("x", Expr.Var("x"))
//        ), Expr.Number(40)
//    )
//    println("${eval(expr)}")

//    val f = Expr.Lambda("x", Expr.Application(Expr.Var("x"), Expr.Var("x")))
//    val omega = Expr.Application(f, Expr.Application(f, f));
//    println("${eval(omega)}")

    // (λx ⇒ (λy ⇒ x y)) y
    // (λy ⇒ y y)
    val e = Expr.Application(
        Expr.Lambda(
            "x", Expr.Lambda(
                "y",
                Expr.Application(Expr.Var("x"), Expr.Var("y"))
            )
        ), Expr.Var("y")
    )
    println("${eval(e)}")

    val add10: () -> ((Int) -> Int) = {
        val x = 10;
        { y: Int -> x + y }
    }
    val x = 20
    println(add10()(5))
}
