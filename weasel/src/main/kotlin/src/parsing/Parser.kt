package src.parsing

import src.Expr
import src.Field
import src.Operator


class Parser(val tokens: Lexer) {

    fun parseField(): Field{
        if (tokens.peek() == Token.CURLLEFT) return parseBlock()
        return Field.Monofield(parseExpr())
    }

    fun parseBlock(): Field {
        val result = hashMapOf<String,Field>()
        expectNext<Token.CURLLEFT>("{")
        while (tokens.peek() != Token.CURLRIGHT){
            val i = expectNext<Token.IDENT>("Identifier").ident
            expectNext<Token.DOUBLEDOT>(":")

            val value = parseField()
            result[i] = value
        }
        expectNext<Token.CURLRIGHT>("}")
        return Field.Block(result)
    }

    fun parseExpr(): Expr {
        return when(tokens.peek()){
            is Token.Str -> Expr.Str((tokens.next() as Token.Str).s)
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
            tokens.next()
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
        return when(tokens.peek()) {
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
        return tryParseAtom() ?: throw Exception("Expected expression, but saw unexpected token: ${tokens.peek()}")
    }

    fun tryParseAtom(): Expr? {
        return when (val t = tokens.peek()) {
            is Token.BOOLEAN_LIT -> parseBoolean()
            is Token.NUMBER_LIT -> parseNumber()
            is Token.IDENT -> parseVar()
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
        val next = tokens.next()
        if (next !is A) {
            throw Exception("Unexpected token: expected $msg, but saw $next")
        }
        return next
    }
}

