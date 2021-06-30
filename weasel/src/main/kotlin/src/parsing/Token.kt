package src.parsing

sealed class Token {

    override fun toString(): String {
        return this.javaClass.simpleName
    }

    // Keywords
    object IF : Token()
    object THEN : Token()
    object ELSE : Token()
    object LET : Token()
    object IN : Token()
    object FUN: Token()


    // Symbols
    object LPAREN : Token()
    object RPAREN : Token()
    object BACKSLASH : Token()
    object ARROW : Token()
    object EQUALS : Token()


    //
    object DELIMITER: Token()
    object CURLLEFT: Token()
    object CURLRIGHT: Token()
    /*
    fun yada(yada:String):JSONField {}
    MONOField = Number, String


    BLOCK ->
    CURLLEFT
        | IDENT : JSONField
        | IDENT : BLOCK
    CURLRIGHT


     */

    // Operatoren
    object PLUS : Token()
    object MINUS : Token()
    object MUL : Token()
    object DOUBLE_EQUALS : Token()

    data class IDENT(val ident: String) : Token()
    data class FIELDIDENT(val ident: String): Token()

    // Literals
    data class BOOLEAN_LIT(val b: Boolean) : Token()
    data class NUMBER_LIT(val n: Int) : Token()
    data class Str(val s: String) : Token()

    // Control Token
    object EOF : Token()
}
