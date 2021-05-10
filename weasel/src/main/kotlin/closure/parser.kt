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
            '\\' -> Token.BACKSLASH
            '=' -> if (iter.peek() == '>') {
                iter.next()
                Token.ARROW
            } else {
                throw Exception("Expected '>' but saw '${iter.next()}'")
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
        return when (val t = tokens.next()) {
            is Token.BOOLEAN_LIT -> Expr.Boolean(t.b)
            is Token.NUMBER_LIT -> Expr.Number(t.n)
            is Token.IDENT -> Expr.Var(t.ident)
            is Token.IF -> parseIf()
            is Token.BACKSLASH -> parseLambda()
            is Token.LPAREN -> parseParenthesized()
            else -> throw Exception("Unexpected token: $t")
        }
    }

    private fun parseParenthesized(): Expr {
        val inner = parseExpr()
        expectNext<Token.RPAREN>(")")
        return inner
    }

    private fun parseLambda(): Expr {
        // \binder => body
        val binder = expectNext<Token.IDENT>("ident").ident
        expectNext<Token.ARROW>("=>")
        val body = parseExpr()
        return Expr.Lambda(binder, body)
    }

    // if true then 3 else 4
    private fun parseIf(): Expr.If {
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


    val input = "if true then 3 else 4"
    println(eval(persistentHashMapOf(), Parser(Lexer(input)).parseExpr()))
}

