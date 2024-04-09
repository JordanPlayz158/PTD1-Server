package xyz.jordanplayz158.ptd.server.common.obfuscation

class ObfuscationIterator(private val charSequence: CharSequence) : CharIterator() {
    private var index = 0

    override fun nextChar(): Char = charSequence[index++]

    override fun hasNext(): Boolean = index < charSequence.length

    fun nextX(x: Int): Array<Char> {
        // Need to do it this way to mock PHP's substr function
        //  it returns "at most length characters" so the length
        //  can be over but must not fail
        val chars = arrayListOf<Char>()

        for (i in 0..<x) {
            if (!hasNext()) break

            chars.add(nextChar())
        }

        return chars.toTypedArray()
    }

    fun nextXString(x: Int): String {
        return nextX(x).joinToString("")
    }

    fun nextVariable(variableLength: Boolean = true) : Int {
        val lengthOfLength = if (!variableLength) {
            1
        } else Obfuscation.convertStringToInt(nextChar())
        val length = Obfuscation.convertStringToInt(nextXString(lengthOfLength))
        return Obfuscation.convertStringToInt(nextXString(length))
    }
}