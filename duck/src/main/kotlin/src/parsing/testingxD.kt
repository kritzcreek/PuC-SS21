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
    val parsedBlock = parser.parseBlock()
    val evaledBlock = evalBlock(persistentHashMapOf(),parsedBlock )
    println(evaledBlock)


}
// todo LEXER, check for fieldname
//
fun main(){
    val simpleTest = """
         {  
            foo: {
                thisShouldWork: (\x => x + 2) 10
                thisShouldNotWork: let a = 10 in
                                   let a = 9 in
                                   if a == 10 then 10 else 0
                x: let a = "AAAHHHHHHHHHH" in a
            } 
         }""".trimIndent()


    // let bindings
    val funTest = """
        let blockValue = { 
            color: "red" 
        }
        
        let fieldValue = "red"
        
        { 
            foo: blockValue
            bar: fieldValue
        }
        
    """.trimIndent()
    testBlock(simpleTest)

}