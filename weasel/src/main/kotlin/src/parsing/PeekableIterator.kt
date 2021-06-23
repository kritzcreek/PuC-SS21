package src.parsing

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