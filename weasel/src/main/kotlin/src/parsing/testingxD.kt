package src

import kotlinx.collections.immutable.persistentHashMapOf
import netscape.javascript.JSObject
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
    val simpleTest = """
         {  
            foo: {
                bar: (\x => x) 10 
                foo: "Bar"
                x: let a = "AAAHHHHHHHHHH" in a
            } 
         }""".trimIndent()


    // let bindings
    val funTest = """
        let a = { 
            color: red 
        }
        
        { 
            foo: 10
            bar: a
        }
    """.trimIndent()
    testBlock(simpleTest)

}