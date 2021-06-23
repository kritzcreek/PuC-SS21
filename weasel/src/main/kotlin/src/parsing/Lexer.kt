package src.parsing

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
            '{' -> Token.CURLLEFT
            '}' -> Token.CURLRIGHT
            ';' -> Token.DELIMITER
            ':' -> Token.DOUBLEDOT
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
                else -> Token.EQUALS
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
            "let" -> Token.LET
            "in" -> Token.IN
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