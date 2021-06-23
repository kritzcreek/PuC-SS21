package src

import src.parsing.Lexer
import src.parsing.Parser
import src.parsing.Token

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
fun testBlock(input: String) {
    println("Parsing: $input")
    val lexer = Lexer(input)
    println(lexer.lex())
    // val parser = Parser(lexer)
    // println(parser.parseBlock())
}

fun main(){
    val ts = """ { foo: {
         bar: (\x => x) 10 
         foo: "bar"
         } 
         }""".trimIndent()
    testBlock(ts)

}