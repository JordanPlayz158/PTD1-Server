package xyz.jordanplayz158.ptd.server.common.obfuscation


// Can't extend ObfuscationBuilder
class ObfuscationBuilder(private val builder: StringBuilder) {
    constructor(): this(StringBuilder())

    fun append(str: String?): ObfuscationBuilder {
        builder.append(str)
        return this
    }
    
    fun append(s: CharSequence?): ObfuscationBuilder {
        builder.append(s)
        return this
    }

    /**
     * @throws     IndexOutOfBoundsException {@inheritDoc}
     */
    fun append(s: CharSequence?, start: Int, end: Int): ObfuscationBuilder {
        builder.append(s, start, end)
        return this
    }

    fun append(str: CharArray?): ObfuscationBuilder {
        builder.append(str)
        return this
    }

    /**
     * @throws IndexOutOfBoundsException {@inheritDoc}
     */
    fun append(str: CharArray?, offset: Int, len: Int): ObfuscationBuilder {
        builder.append(str, offset, len)
        return this
    }

    fun append(b: Boolean): ObfuscationBuilder {
        builder.append(b)
        return this
    }

    fun append(c: Char): ObfuscationBuilder {
        builder.append(c)
        return this
    }

    fun append(b: Byte): ObfuscationBuilder {
        builder.append(b)
        return this
    }

    fun append(i: Int): ObfuscationBuilder {
        builder.append(i)
        return this
    }

    fun append(lng: Long): ObfuscationBuilder {
        builder.append(lng)
        return this
    }

    fun append(f: Float): ObfuscationBuilder {
        builder.append(f)
        return this
    }

    fun append(d: Double): ObfuscationBuilder {
        builder.append(d)
        return this
    }

    /**
     * @throws StringIndexOutOfBoundsException {@inheritDoc}
     */
    fun delete(start: Int, end: Int): ObfuscationBuilder {
        builder.delete(start, end)
        return this
    }

    /**
     * @throws StringIndexOutOfBoundsException {@inheritDoc}
     */
    fun insert(
        index: Int, str: CharArray?, offset: Int,
        len: Int
    ): ObfuscationBuilder {
        builder.insert(index, str, offset, len)
        return this
    }

    /**
     * @throws StringIndexOutOfBoundsException {@inheritDoc}
     */
    fun insert(offset: Int, obj: Any?): ObfuscationBuilder {
        builder.insert(offset, obj)
        return this
    }

    /**
     * @throws StringIndexOutOfBoundsException {@inheritDoc}
     */
    fun insert(offset: Int, str: String?): ObfuscationBuilder {
        builder.insert(offset, str)
        return this
    }

    /**
     * @throws StringIndexOutOfBoundsException {@inheritDoc}
     */
    fun insert(offset: Int, str: CharArray?): ObfuscationBuilder {
        builder.insert(offset, str)
        return this
    }

    /**
     * @throws IndexOutOfBoundsException {@inheritDoc}
     */
    fun insert(dstOffset: Int, s: CharSequence?): ObfuscationBuilder {
        builder.insert(dstOffset, s)
        return this
    }

    /**
     * @throws IndexOutOfBoundsException {@inheritDoc}
     */
    fun insert(
        dstOffset: Int, s: CharSequence?,
        start: Int, end: Int
    ): ObfuscationBuilder {
        builder.insert(dstOffset, s, start, end)
        return this
    }

    /**
     * @throws StringIndexOutOfBoundsException {@inheritDoc}
     */
    fun insert(offset: Int, b: Boolean): ObfuscationBuilder {
        builder.insert(offset, b)
        return this
    }

    /**
     * @throws IndexOutOfBoundsException {@inheritDoc}
     */
    fun insert(offset: Int, c: Char): ObfuscationBuilder {
        builder.insert(offset, c)
        return this
    }

    /**
     * @throws StringIndexOutOfBoundsException {@inheritDoc}
     */
    fun insert(offset: Int, i: Int): ObfuscationBuilder {
        builder.insert(offset, i)
        return this
    }

    /**
     * @throws StringIndexOutOfBoundsException {@inheritDoc}
     */
    fun insert(offset: Int, l: Long): ObfuscationBuilder {
        builder.insert(offset, l)
        return this
    }
    
    fun insert(offset: Int, f: Float): ObfuscationBuilder {
        builder.insert(offset, f)
        return this
    }
    
    fun insert(offset: Int, d: Double): ObfuscationBuilder {
        builder.insert(offset, d)
        return this
    }


    // PTD specific functions
    fun appendObfuscated(value: String): ObfuscationBuilder {
        builder.append(Obfuscation.convertIntToString(value))
        return this
    }

    fun appendObfuscated(value: Number): ObfuscationBuilder {
        appendObfuscated(value.toLong().toString())
        return this
    }

    fun appendObfuscatedLength(value: String, variableLength: Boolean = false): ObfuscationBuilder {
        val valueLength = value.length

        if (variableLength) {
            appendObfuscated(valueLength.toString().length)
        }

        appendObfuscated(valueLength)
        appendObfuscated(value)
        return this
    }

    fun appendObfuscatedLength(value: Number, variableLength: Boolean = false): ObfuscationBuilder {
        return appendObfuscatedLength(value.toLong().toString(), variableLength)
    }

    fun length(): Int {
        return builder.length
    }
    
    override fun toString(): String {
        return builder.toString()
    }
}