package src.parsing

import src.Expr
import src.Field
import src.Operator
import src.Value


class Parser(val tokens: ArrayList<Token>) {
    private var index = 0

    private fun peek(n: Int = 0): Token {
        return tokens.getOrElse(index + n){ Token.EOF }
    }

    private fun next(n: Int = 1): Token {
        val t = tokens.getOrElse(index){ Token.EOF }
        index += n
        return t
    }

    fun parse(): Field.Block{
        when(val t = next()) {
            is Token.CURLLEFT -> return parseBlock()
            else -> throw Exception("Unexpected token: $t")
        }
    }

    fun parseField(): Field{
        if (peek() == Token.CURLLEFT) return parseBlock()
        val result = parseExpr()
        if (result is Expr.Lambda) throw Exception("Fields shouldn't just consist of lambdas like this: \n$result" )
        return Field.Monofield(result)
    }

    fun parseBlock(): Field.Block {
        val result = hashMapOf<String,Field>()
        expectNext<Token.CURLLEFT>("{")
        while (peek() != Token.CURLRIGHT){
            val i = expectNext<Token.FIELDIDENT>("Field Identifier").ident

            val value = parseField()
            result[i] = value
        }
        expectNext<Token.CURLRIGHT>("}")
        return Field.Block(result)
    }

    fun parseExpr(): Expr {
        return when{
            peek() is Token.Str -> Expr.Str((next() as Token.Str).s)
            else -> parseBinary(0)
        }
    }

    fun parseBinary(minBP: Int): Expr {
        var lhs: Expr = parseApplication()
        while (true) {
            val op = parseOperator() ?: break
            val (leftBP, rightBP) = bindingPower(op)
            if (leftBP < minBP) {
                break
            }
            next()
            val rhs = parseBinary(rightBP)
            lhs = Expr.Binary(op, lhs, rhs)
        }

        return lhs
    }

    /*
    {
        yada: "yada"
        asd :{
            abc : (\x => x) 10
            yeet : "abc"
        }
    }
     */

    private fun parseOperator(): Operator? {
        return when(peek()) {
            Token.PLUS -> Operator.Plus
            Token.MINUS -> Operator.Minus
            Token.MUL -> Operator.Multiply
            Token.DOUBLE_EQUALS -> Operator.Equals
            else -> null
        }
    }

    fun bindingPower(op: Operator): Pair<Int, Int> {
        return when(op) {
            Operator.Equals -> 1 to 2
            Operator.Plus, Operator.Minus -> 3 to 4
            Operator.Multiply -> 5 to 6
        }
    }

    fun parseApplication(): Expr {
        val func = parseAtom()
        val args: MutableList<Expr> = mutableListOf()
        while (true) {
            args += tryParseAtom() ?: break
        }
        return args.fold(func) { acc, arg -> Expr.Application(acc, arg) }
    }

    fun parseAtom(): Expr {
        return tryParseAtom() ?: throw Exception("Expected expression, but saw unexpected token: ${peek()}")
    }

    fun tryParseAtom(): Expr? {
        return when (val t = peek()) {
            is Token.BOOLEAN_LIT -> parseBoolean()
            is Token.NUMBER_LIT -> parseNumber()
            is Token.IDENT ->  parseVar()
            is Token.IF -> parseIf()
            is Token.BACKSLASH -> parseLambda()
            is Token.LPAREN -> parseParenthesized()
            is Token.LET -> parseLet()
            else -> null
        }
    }

    private fun parseLet(): Expr {
        expectNext<Token.LET>("let")
        val binder = expectNext<Token.IDENT>("binder").ident
        expectNext<Token.EQUALS>("equals")
        val expr = parseExpr()
        expectNext<Token.IN>("in")
        val body = parseExpr()
        return Expr.Let(binder, expr, body)
    }

    private fun parseBoolean(): Expr {
        val t = expectNext<Token.BOOLEAN_LIT>("boolean literal")
        return Expr.Boolean(t.b)
    }

    private fun parseNumber(): Expr {
        val t = expectNext<Token.NUMBER_LIT>("number literal")
        return Expr.Number(t.n)
    }

    private fun parseVar(): Expr {
        val t = expectNext<Token.IDENT>("identifier")
        return Expr.Var(t.ident)
    }

    private fun parseParenthesized(): Expr {
        expectNext<Token.LPAREN>("(")
        val inner = parseExpr()
        expectNext<Token.RPAREN>(")")
        return inner
    }

    private fun parseLambda(): Expr {
        // \binder => body
        expectNext<Token.BACKSLASH>("\\")
        val binder = expectNext<Token.IDENT>("ident").ident
        expectNext<Token.ARROW>("=>")
        val body = parseExpr()
        return Expr.Lambda(binder, body)
    }

    // if true then 3 else 4
    private fun parseIf(): Expr.If {
        expectNext<Token.IF>("if")
        val condition = parseExpr()
        expectNext<Token.THEN>("then")
        val thenBranch = parseExpr()
        expectNext<Token.ELSE>("else")
        val elseBranch = parseExpr()
        return Expr.If(condition, thenBranch, elseBranch)
    }

    private inline fun <reified A>expectNext(msg: String): A {
        val next = next()
        if (next !is A) {
            throw Exception("Unexpected token: expected $msg, but saw $next")
        }
        return next
    }
}

