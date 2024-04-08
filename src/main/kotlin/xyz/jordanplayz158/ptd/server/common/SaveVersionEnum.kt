package xyz.jordanplayz158.ptd.server.common

enum class SaveVersionEnum(val id: Byte) {
    RED(1),
    BLUE(2);

    companion object {
        fun get(id: Byte): SaveVersionEnum {
            return entries.first { it.id == id }
        }
    }
}