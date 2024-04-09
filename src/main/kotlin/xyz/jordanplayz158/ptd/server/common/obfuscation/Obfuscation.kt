package xyz.jordanplayz158.ptd.server.common.obfuscation

import xyz.jordanplayz158.ptd.server.common.data.Pokemon

class Obfuscation {
    companion object {
        private val letterList = arrayOf('m', 'y', 'w', 'c', 'q', 'a', 'p', 'r', 'e', 'o')

        private fun convertToString(index: Int): Char {
            if (index < letterList.size) {
                return letterList[index]
            }

            return '1'
        }

        private fun convertToInt(char: Char): Int {
            for ((index, letter) in letterList.withIndex()) {
                if (char == letter) {
                    return index
                }
            }

            return -1
        }

        private fun convertStringToIntString(string: String): String {
            val stringBuilder = StringBuilder()

            for (char in string.toCharArray()) {
                val int = convertToInt(char)
                if (int == -1) {
                    return "-100"
                }

                stringBuilder.append(int)
            }

            return stringBuilder.toString()
        }

        fun convertStringToInt(string: String): Int {
            // PHP coerces an empty string to 0
            if (string.isEmpty()) return 0

            return convertStringToIntString(string).toInt()
        }

        fun convertStringToInt(char: Char): Int {
            return convertStringToInt(char.toString())
        }

        fun convertIntToString(num: String): String {
            val stringBuilder = StringBuilder()

            for (char in num.toCharArray()) {
                // PHP coerces strings that cannot be converted to ints
                //  to 0
                val convertedChar = convertToString(if (char.isDigit()) {
                    char.digitToInt()
                } else {
                    0
                })

                if (convertedChar == '1') {
                    return "-100"
                }

                stringBuilder.append(convertedChar)
            }

            return stringBuilder.toString()
        }

        fun convertIntToString(num: Number): String {
            return convertIntToString(num.toLong().toString())
        }

        fun createCheckSum(encodedInfo: String): Int {
            var checksum = 15

            for (char in encodedInfo.toCharArray()) {
                val digit = char.digitToIntOrNull()

                if (digit === null) {
                    // May not be equivalent to ord
                    checksum += char.code - 96
                    continue
                }

                checksum += digit
            }

            return checksum * 3
        }

        fun decodePokeInfo(encoded: String, id: Int): ArrayList<Pokemon> {
            var id = id
            val pokemons = ArrayList<Pokemon>()
            val iterator = ObfuscationIterator(encoded)

            // dataLength
            val dataLengthLength = convertStringToInt(iterator.nextChar())
            iterator.nextX(dataLengthLength)

            //val pokeLength = convertStringToInt(iterator.nextChar())
            //val pokes = convertStringToInt(iterator.nextXString(pokeLength))
            val pokes = iterator.nextVariable(false)

            for (i in 1..pokes) {
                if (!iterator.hasNext()) break

                val reasons = ArrayList<Int>()

                val pokeInfoLength = iterator.nextVariable(false)
                val saveId = iterator.nextVariable(true)

                val poke = Pokemon.Builder(saveId)

                for (j in 0..<pokeInfoLength) {
                    val infoTypeLength = convertStringToInt(iterator.nextChar())
                    val infoType = convertStringToInt(iterator.nextXString(infoTypeLength))


                    when (infoType) {
                        // Captured
                        1 -> {
                            poke.needNickname = i
                            poke.saveId = id
                            id++

                            val num = iterator.nextVariable(false)
                            poke.number = num
                            poke.experience = iterator.nextVariable(true)
                            poke.level = iterator.nextVariable(false)
                            poke.setMoves(iterator.nextVariable(false),
                                iterator.nextVariable(false),
                                iterator.nextVariable(false),
                                iterator.nextVariable(false))
                            poke.targetType = iterator.nextVariable(false)
                            poke.gender = iterator.nextVariable(false)
                            val pos = iterator.nextVariable(false)
                            poke.position = pos

                            var extra = iterator.nextVariable(false)
                            if (extra != 0) {
                                extra = if (extra == num) 1 else 2
                            }
                            poke.extra = extra

                            poke.item = iterator.nextVariable(false)

                            val tagLength = convertStringToInt(iterator.nextChar())
                            poke.tag = iterator.nextXString(tagLength)

                            poke.needSaveId = pos
                        }
                        // Level up
                        2 -> poke.level = iterator.nextVariable(false)
                        // XP up
                        3 -> poke.experience = iterator.nextVariable(true)
                        // Change moves
                        4 -> {
                            poke.setMoves(iterator.nextVariable(false),
                                iterator.nextVariable(false),
                                iterator.nextVariable(false),
                                iterator.nextVariable(false))
                        }
                        // Change Item
                        5 -> poke.item = iterator.nextVariable(false)
                        // Evolve
                        6 -> poke.number = iterator.nextVariable(false)
                        // Change Nickname
                        7 -> poke.needNickname = i
                        // Pos change
                        8 -> poke.position = iterator.nextVariable(false)
                        // Need Tag
                        9 -> {
                            val tagLength = convertStringToInt(iterator.nextXString(1))
                            val tag = iterator.nextXString(tagLength)
                            poke.tag = tag
                        }
                        // Need Trade
                        // This looks like? It can be merged with Evolve logic?
                        10 -> poke.number = iterator.nextVariable(false)
                    }
                    reasons.add(infoType)
                }
                poke.reasons = reasons.toTypedArray()
                pokemons.add(poke.build())
            }

            return pokemons
        }

        fun decodeInventory(encodedItems: String): HashMap<Short, Int> {
            val items = HashMap<Short, Int>()

            val iterator = ObfuscationIterator(encodedItems)
            val encodedLengthLength = convertStringToInt(iterator.nextChar())
            iterator.nextX(encodedLengthLength)
            val inventoryLengthLength = convertStringToInt(iterator.nextChar())
            val inventoryLength = convertStringToInt(iterator.nextXString(inventoryLengthLength))

            for (i in 0..<inventoryLength) {
                val num = iterator.nextVariable(false).toShort()
                val quantity = iterator.nextVariable(false)

                items[num] = quantity
            }

            return items
        }

        fun decodeExtra(encodedExtra: String): HashMap<Short, Int> {
            return decodeInventory(encodedExtra)
        }
    }
}