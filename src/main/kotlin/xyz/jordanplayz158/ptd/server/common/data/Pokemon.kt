package xyz.jordanplayz158.ptd.server.common.data

data class Pokemon(
    val saveId: Int,
    val needNickname: Int?,
    val number: Int?,
    val experience: Int?,
    val level: Int?,
    val moves: Array<Int>?,
    val targetType: Int?,
    val gender: Int?,
    val position: Int?,
    val extra: Int?,
    val item: Int?,
    val tag: String?,
    val needSaveId: Int?,
    val reasons: Array<Int>
) {
    private constructor(builder: Builder)
            : this(builder.saveId, builder.needNickname, builder.number,
                builder.experience, builder.level, builder.moves,
                builder.targetType, builder.gender, builder.position,
                builder.extra, builder.item, builder.tag,
                builder.needSaveId, builder.reasons)

    class Builder(var saveId: Int) {
        var needNickname: Int? = null
        var number: Int? = null
        var experience: Int? = null
        var level: Int? = null
        val moves = arrayOf(0, 0, 0, 0)
        var targetType: Int? = null
        var gender: Int? = null
        var position: Int? = null
        var extra: Int? = null
        var item: Int? = null
        var tag: String? = null
        var needSaveId: Int? = null
        var reasons: Array<Int> = arrayOf()

        fun saveId(saveId: Int) = apply { this.saveId = saveId }
        fun needNickname(needNickname: Int) = apply { this.needNickname = needNickname }
        fun number(number: Int) = apply { this.number = number }
        fun experience(experience: Int) = apply { this.experience = experience }
        fun level(level: Int) = apply { this.level = level }

        fun setMove(index: Int, move: Int) {
            moves[index] = move
        }

        fun setMoves(vararg moves: Int) {
            for ((index, move) in moves.withIndex()) {
                setMove(index, move)
            }
        }

        fun targetType(targetType: Int) = apply { this.targetType = targetType }
        fun gender(gender: Int) = apply { this.gender = gender }
        fun position(position: Int) = apply { this.position = position }
        fun extra(extra: Int) = apply { this.extra = extra }
        fun item(item: Int) = apply { this.item = item }
        fun tag(tag: String) = apply { this.tag = tag }
        fun needSaveId(needSaveId: Int) = apply { this.needSaveId = needSaveId }
        fun reasons(reasons: Array<Int>) = apply { this.reasons = reasons }

        fun build() = Pokemon(this)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Pokemon

        if (saveId != other.saveId) return false
        if (needNickname != other.needNickname) return false
        if (number != other.number) return false
        if (experience != other.experience) return false
        if (level != other.level) return false
        if (moves != null) {
            if (other.moves == null) return false
            if (!moves.contentEquals(other.moves)) return false
        } else if (other.moves != null) return false
        if (targetType != other.targetType) return false
        if (gender != other.gender) return false
        if (position != other.position) return false
        if (extra != other.extra) return false
        if (item != other.item) return false
        if (tag != other.tag) return false
        if (needSaveId != other.needSaveId) return false

        return true
    }

    override fun hashCode(): Int {
        var result = saveId
        result = 31 * result + (needNickname ?: 0)
        result = 31 * result + (number ?: 0)
        result = 31 * result + (experience ?: 0)
        result = 31 * result + (level ?: 0)
        result = 31 * result + (moves?.contentHashCode() ?: 0)
        result = 31 * result + (targetType ?: 0)
        result = 31 * result + (gender ?: 0)
        result = 31 * result + (position ?: 0)
        result = 31 * result + (extra ?: 0)
        result = 31 * result + (item ?: 0)
        result = 31 * result + (tag?.hashCode() ?: 0)
        result = 31 * result + (needSaveId ?: 0)
        return result
    }
}