sealed class Expression {
    data class Number(val number: Int) : Expression()
    data class Addition(val summand1: Expression, val summand2: Expression) : Expression()
    data class Multiplication(val faktor1: Expression, val faktor2: Expression) : Expression()
    data class Negation(val inner: Expression) : Expression()
}

sealed class SmallerExpression {
    data class Number(val number: Int) : SmallerExpression()
    data class Addition(val summand1: SmallerExpression, val summand2: SmallerExpression) : SmallerExpression()
    data class Multiplication(val faktor1: SmallerExpression, val faktor2: SmallerExpression) : SmallerExpression()
}

fun evalSmall(expression: SmallerExpression): Int {
    return when (expression) {
        is SmallerExpression.Addition ->
            evalSmall(expression.summand1) + evalSmall(expression.summand2)
        is SmallerExpression.Number ->
            expression.number
        is SmallerExpression.Multiplication ->
            evalSmall(expression.faktor1) * evalSmall(expression.faktor2)
    }
}

fun eval(expression: Expression): Int {
    return evalSmall(lower(expression))
}

fun lower(expression: Expression): SmallerExpression {
    return when(expression) {
        is Expression.Addition -> SmallerExpression.Addition(
            lower(expression.summand1),
            lower(expression.summand2)
        )
        is Expression.Multiplication -> SmallerExpression.Multiplication(
            lower(expression.faktor1),
            lower(expression.faktor2)
        )
        is Expression.Negation -> SmallerExpression.Multiplication(
            SmallerExpression.Number(-1),
            lower(expression.inner)
        )
        is Expression.Number -> SmallerExpression.Number(expression.number)
    }
}

fun main(args: Array<String>) {
    // 1 + -(2 + 2)
    val expr =
        Expression.Addition(
            Expression.Number(1),
            Expression.Negation(
                Expression.Addition(
                    Expression.Number(2),
                    Expression.Number(2)
                )
            )
        )
    // 1 + (-1 * (2 + 2))
    val lowered = lower(expr)

    println("${expr} => ${lowered} = ${eval(expr)}")
}