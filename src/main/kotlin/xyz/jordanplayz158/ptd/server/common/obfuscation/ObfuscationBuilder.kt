package xyz.jordanplayz158.ptd.server.common.obfuscation


// Can't extend StringBuilder
class ObfuscationBuilder(private val builder: StringBuilder) {
    constructor(): this(StringBuilder())

    fun append(str: String?) = apply { builder.append(str) }
    
    fun append(s: CharSequence?) = apply { builder.append(s) }

    /**
     * @throws     IndexOutOfBoundsException {@inheritDoc}
     */
    fun append(s: CharSequence?, start: Int, end: Int) = apply { builder.append(s, start, end) }

    fun append(str: CharArray?) = apply { builder.append(str) }

    /**
     * @throws IndexOutOfBoundsException {@inheritDoc}
     */
    fun append(str: CharArray?, offset: Int, len: Int) = apply { builder.append(str, offset, len) }

    fun append(b: Boolean) = apply { builder.append(b) }

    fun append(c: Char) = apply { builder.append(c) }

    fun append(b: Byte) = apply { builder.append(b) }

    fun append(i: Int) = apply { builder.append(i) }

    fun append(lng: Long) = apply { builder.append(lng) }

    fun append(f: Float) = apply { builder.append(f) }

    fun append(d: Double) = apply { builder.append(d) }

    /**
     * @throws StringIndexOutOfBoundsException {@inheritDoc}
     */
    fun delete(start: Int, end: Int) = apply { builder.delete(start, end) }

    /**
     * @throws StringIndexOutOfBoundsException {@inheritDoc}
     */
    fun insert(
        index: Int, str: CharArray?, offset: Int,
        len: Int
    ) = apply { builder.insert(index, str, offset, len) }

    /**
     * @throws StringIndexOutOfBoundsException {@inheritDoc}
     */
    fun insert(offset: Int, obj: Any?) = apply { builder.insert(offset, obj) }

    /**
     * @throws StringIndexOutOfBoundsException {@inheritDoc}
     */
    fun insert(offset: Int, str: String?) = apply { builder.insert(offset, str) }

    /**
     * @throws StringIndexOutOfBoundsException {@inheritDoc}
     */
    fun insert(offset: Int, str: CharArray?) = apply { builder.insert(offset, str) }

    /**
     * @throws IndexOutOfBoundsException {@inheritDoc}
     */
    fun insert(dstOffset: Int, s: CharSequence?) = apply { builder.insert(dstOffset, s) }

    /**
     * @throws IndexOutOfBoundsException {@inheritDoc}
     */
    fun insert(
        dstOffset: Int, s: CharSequence?,
        start: Int, end: Int
    ) = apply { builder.insert(dstOffset, s, start, end) }

    /**
     * @throws StringIndexOutOfBoundsException {@inheritDoc}
     */
    fun insert(offset: Int, b: Boolean) = apply { builder.insert(offset, b) }

    /**
     * @throws IndexOutOfBoundsException {@inheritDoc}
     */
    fun insert(offset: Int, c: Char) = apply { builder.insert(offset, c) }

    /**
     * @throws StringIndexOutOfBoundsException {@inheritDoc}
     */
    fun insert(offset: Int, i: Int) = apply { builder.insert(offset, i) }

    /**
     * @throws StringIndexOutOfBoundsException {@inheritDoc}
     */
    fun insert(offset: Int, l: Long) = apply { builder.insert(offset, l) }
    
    fun insert(offset: Int, f: Float) = apply { builder.insert(offset, f) }
    
    fun insert(offset: Int, d: Double) = apply { builder.insert(offset, d) }


    // PTD specific functions
    fun appendObfuscated(value: String) = apply { builder.append(Obfuscation.convertIntToString(value)) }

    fun appendObfuscated(value: Number) = apply { appendObfuscated(value.toLong().toString()) }

    fun appendObfuscatedLength(value: String, variableLength: Boolean = false) = apply {
        val valueLength = value.length

        if (variableLength) {
            appendObfuscated(valueLength.toString().length)
        }

        appendObfuscated(valueLength)
        appendObfuscated(value)
    }

    fun appendObfuscatedLength(value: Number, variableLength: Boolean = false) = apply {
        appendObfuscatedLength(value.toLong().toString(), variableLength)
    }

    fun length(): Int {
        return builder.length
    }
    
    override fun toString(): String {
        return builder.toString()
    }
}