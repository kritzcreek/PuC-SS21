package src

import kotlinx.collections.immutable.persistentHashMapOf
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
    val parser = Parser(lexer.lexTokens())
    println(parser.parseExpr())
}
fun testBlock(input: String) {
    println("Parsing: $input")
    val lexer = Lexer(input)
    val parser = Parser(lexer.lexTokens())
    val block = parser.parseBlock()
    println(block)

    println(evalToJson(persistentHashMapOf(), block))

}

fun main(){
    val ts = """
         {  
            foo: {
                bar: (\x => x) 10 
                foo: "Bar"
            } 
         }""".trimIndent()
    testBlock(ts)

}