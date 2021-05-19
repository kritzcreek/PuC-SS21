package closure

import kotlinx.collections.immutable.persistentHashMapOf

sealed class Token {

    override fun toString(): String {
        return this.javaClass.simpleName
    }

    // Keywords
    object IF : Token()
    object THEN : Token()
    object ELSE : Token()

    // Symbols
    object LPAREN : Token()
    object RPAREN : Token()
    object BACKSLASH : Token()
    object ARROW : Token()

    // Operatoren
    object PLUS : Token()
    object MINUS : Token()
    object MUL : Token()
    object DOUBLE_EQUALS : Token()

    data class IDENT(val ident: String) : Token()

    // Literals
    data class BOOLEAN_LIT(val b: Boolean) : Token()
    data class NUMBER_LIT(val n: Int) : Token()

    // Control Token
    object EOF : Token()
}

class PeekableIterator<A>(private val iter: Iterator<A>) {
    private var lookahead: A? = null
    fun next(): A {
        lookahead?.let { lookahead = null; return it }
        return iter.next()
    }

    fun peek(): A {
        val token = next()
        lookahead = token
        return token
    }

    fun hasNext(): Boolean {
        return lookahead != null || iter.hasNext()
    }
}

class Lexer(input: String) {

    private val iter: PeekableIterator<Char> = PeekableIterator(input.iterator())
    private var lookahead: Token? = null

    public fun next(): Token {
        lookahead?.let { lookahead = null; return it }
        consumeWhitespace()
        if (!iter.hasNext()) {
            return Token.EOF
        }
        return when (val c = iter.next()) {
            '(' -> Token.LPAREN
            ')' -> Token.RPAREN
            '+' -> Token.PLUS
            '-' -> Token.MINUS
            '*' -> Token.MUL
            '\\' -> Token.BACKSLASH
            '=' -> when (iter.peek()) {
                '>' -> {
                    iter.next()
                    Token.ARROW
                }
                '=' -> {
                    iter.next()
                    Token.DOUBLE_EQUALS
                }
                else -> {
                    throw Exception("Expected '>' or '=' but saw '${iter.next()}'")
                }
            }
            else -> when {
                c.isJavaIdentifierStart() -> ident(c)
                c.isDigit() -> number(c)
                else -> throw Exception("Unexpected char: '$c'")
            }
        }
    }

    public fun peek(): Token {
        val token = next()
        lookahead = token
        return token
    }

    private fun number(c: Char): Token {
        var result = c.toString()
        while (iter.hasNext() && iter.peek().isDigit()) {
            result += iter.next()
        }
        return Token.NUMBER_LIT(result.toInt())
    }

    private fun ident(c: Char): Token {
        var result = c.toString()
        while (iter.hasNext() && iter.peek().isJavaIdentifierPart()) {
            result += iter.next()
        }
        return when (result) {
            "true" -> Token.BOOLEAN_LIT(true)
            "false" -> Token.BOOLEAN_LIT(false)
            "if" -> Token.IF
            "then" -> Token.THEN
            "else" -> Token.ELSE
            else -> Token.IDENT(result)
        }
    }

    private fun consumeWhitespace() {
        while (iter.hasNext()) {
            val c = iter.peek()
            if (!c.isWhitespace()) break
            iter.next()
        }
    }
}

class Parser(val tokens: Lexer) {

    fun parseExpr(): Expr {
        return parseBinary(0)
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
            else -> null
        }
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


fun test(input: String) {
    println("Lexing: $input")
    val lexer = Lexer(input)
    while (lexer.peek() != Token.EOF) {
        println(lexer.next())
    }
    println(lexer.next())
}

fun testParser(input: String) {
    println("Parsing: $input")
    val lexer = Lexer(input)
    val parser = Parser(lexer)
    println(parser.parseExpr())
}

fun testEval(input: String) {
    println("Evaluating: $input")
    println(eval(persistentHashMapOf(), Parser(Lexer(input)).parseExpr()))
}

fun main() {
    test("""
        if (\x => equals 25 x) 20
        then true
        else add 3 (add 4 5)
    """.trimIndent())

    // Uebung:
    // 1. Zeilenkommentare (Tipp: Kommentare als Whitespace behandeln)
    // 2. Bonus(schwer): Blockkommentare
    // 3. BonusBonus(schwer!): Geschachtelte Blockkommentare
//    test("""
//        if (\x => equals 25 x) 20
//        // Huge if true
//        then true
//        // smol
//        /* ich
//        bin auch
//        ein Kommentar
//        */
//        /* Ich /* bin geschachtelt */ */
//        else add 3 (add 4 5)
//    """.trimIndent())


    testParser("42")
    testParser("true")
    testParser("imAVar")
    testParser("if true then 3 else 4")
    testParser("""\x => \y => 10""")
    testParser("5")


    val input = """(\x => if x then 3 else 4) false"""
    println(eval(persistentHashMapOf(), Parser(Lexer(input)).parseExpr()))

    testParser("1 + 2")
    testEval("2 * 2 + 3 == 7")
}

