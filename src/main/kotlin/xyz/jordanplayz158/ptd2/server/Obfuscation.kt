package xyz.jordanplayz158.ptd2.server

import org.jetbrains.exposed.sql.SizedIterable
import xyz.jordanplayz158.ptd2.server.data.GenericKeyValue
import xyz.jordanplayz158.ptd2.server.orm.PTD2Extra
import xyz.jordanplayz158.ptd2.server.orm.PTD2OneV1
import xyz.jordanplayz158.ptd2.server.orm.PTD2Pokemon
import xyz.jordanplayz158.ptd2.server.orm.PTD2Story

class Obfuscation {
    companion object {
        private val letterList = arrayOf('m', 'y', 'w', 'c', 'q', 'a', 'p', 'r', 'e', 'o')

        fun convertToString(index: Int): Char {
            if (index < letterList.size) {
                return letterList[index]
            }

            return '1'
        }

        fun convertToInt(char: Char): Int {
            for ((index, letter) in letterList.withIndex()) {
                if (char == letter) {
                    return index
                }
            }

            return -1
        }

        fun convertStringToIntString(string: String): String {
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

        fun convertIntToString(num: Long): String {
            return convertIntToString(num.toString())
        }

        fun convertIntToString(num: Int): String {
            return convertIntToString(num.toLong())
        }

        fun convertIntToString(num: Short): String {
            return convertIntToString(num.toInt())
        }

        fun convertIntToString(num: Byte): String {
            return convertIntToString(num.toShort())
        }

        fun getLength(param1: Int, param2: Int): String {
            val stringLength = (param1 + param2 + 1).toString()
            val length = stringLength.length

            if (length != param1) {
                return getLength(length, param2)
            }

            return "$length$stringLength"
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

        fun decodePokeInfo(encoded: String, story: PTD2Story): HashMap<Int, HashMap<String, Any>> {
            val pokemons = HashMap<Int, HashMap<String, Any>>()
            var availableSaveId = getAvailableSaveId(story)

            val iterator = encoded.iterator()

            // dataLength
            val dataLengthLength = convertStringToInt(iterator.nextChar())
            iterator.nextX(dataLengthLength)

            //val pokeLength = convertStringToInt(iterator.nextChar())
            //val pokes = convertStringToInt(iterator.nextXString(pokeLength))
            val pokes = iterator.nextVariable(false)

            for (i in 1..pokes) {
                if (!iterator.hasNext()) break

                val poke = HashMap<String, Any>()
                val reasons = ArrayList<Int>()

                val pokeInfoLength = iterator.nextVariable(false)
                val saveId = iterator.nextVariable(true)

                poke["saveID"] = saveId

                for (j in 0..<pokeInfoLength) {
                    val infoTypeLength = convertStringToInt(iterator.nextChar())
                    val infoType = convertStringToInt(iterator.nextXString(infoTypeLength))


                    when (infoType) {
                        // Captured
                        1 -> {
                            poke["needNickname"] = i
                            poke["saveID"] = availableSaveId
                            availableSaveId++

                            val num = iterator.nextVariable(false)
                            poke["num"] = num
                            poke["xp"] = iterator.nextVariable(true)
                            poke["lvl"] = iterator.nextVariable(false)
                            poke["move1"] = iterator.nextVariable(false)
                            poke["move2"] = iterator.nextVariable(false)
                            poke["move3"] = iterator.nextVariable(false)
                            poke["move4"] = iterator.nextVariable(false)
                            poke["targetingType"] = iterator.nextVariable(false)
                            poke["gender"] = iterator.nextVariable(false)
                            val pos = iterator.nextVariable(false)
                            poke["pos"] = pos

                            var extra = iterator.nextVariable(false)
                            if (extra != 0) {
                                extra = if (extra == num) 1 else 2
                            }
                            poke["extra"] = extra

                            poke["item"] = iterator.nextVariable(false)

                            val tagLength = convertStringToInt(iterator.nextChar())
                            poke["tag"] = iterator.nextXString(tagLength)

                            poke["needSaveID"] = pos
                        }
                        // Level up
                        2 -> poke["lvl"] = iterator.nextVariable(false)
                        // XP up
                        3 -> poke["xp"] = iterator.nextVariable(true)
                        // Change moves
                        4 -> {
                            poke["move1"] = iterator.nextVariable(false)
                            poke["move2"] = iterator.nextVariable(false)
                            poke["move3"] = iterator.nextVariable(false)
                            poke["move4"] = iterator.nextVariable(false)
                        }
                        // Change Item
                        5 -> poke["item"] = iterator.nextVariable(false)
                        // Evolve
                        6 -> poke["num"] = iterator.nextVariable(false)
                        // Change Nickname
                        7 -> poke["needNickname"] = i
                        // Pos change
                        8 -> poke["pos"] = iterator.nextVariable(false)
                        // Need Tag
                        9 -> {
                            val tagLength = convertStringToInt(iterator.nextXString(1))
                            val tag = iterator.nextXString(tagLength)
                            poke["tag"] = tag
                        }
                        // Need Trade
                        // This looks like? It can be merged with Evolve logic?
                        10 -> poke["num"] = iterator.nextVariable(false)
                    }
                    reasons.add(infoType)
                }
                poke["reason"] = reasons
                pokemons[poke["saveID"] as Int] = poke
            }

            return pokemons
        }

        fun encodePokemons(pokemons: SizedIterable<PTD2Pokemon>): Pair<String, String> {
            val pokemonsSize = pokemons.count().toInt()
            val pokes = convertIntToString(pokemonsSize)
            val pokesLen = convertIntToString(pokes.length)

            val encodedPokes = "$pokesLen$pokes"
            val pokemonNicknames = StringBuilder()
            val parts = HashMap<Int, String>()

            for (poke in pokemons) {
                /* this is to avoid suspect of hacking
                 For some reason sometimes the game
                 sends only the new pos of one poke */
                if (parts.containsKey(poke.pos)) {
                    for (i in 0..<pokemonsSize) {
                        if (!parts.containsKey(i)) {
                            poke.pos = i
                            break
                        }
                    }
                }

                // Probably convert the hashmap to an arraylist of object
                //   Then can also remove the !!'s
                val (num, numLength) = convertValue(poke.num)
                val (xp, xpLength, xpLengthLength) = convertValueVariableLength(poke.xp)
                val (lvl, lvlLength) = convertValue(poke.lvl)
                val (move1, move1Length) = convertValue(poke.move1)
                val (move2, move2Length) = convertValue(poke.move2)
                val (move3, move3Length) = convertValue(poke.move3)
                val (move4, move4Length) = convertValue(poke.move4)
                val (tt, ttLength) = convertValue(poke.targetType)
                val (gender, genderLength) = convertValue(poke.gender)
                val (saveId, saveIdLength, saveIdLengthLength) = convertValueVariableLength(poke.swfId)
                val posValue = poke.pos
                val (pos, posLength) = convertValue(poke.pos)
                val (extra, extraLength) = convertValue(poke.extra)
                val (item, itemLength) = convertValue(poke.item)
                val tag = poke.tag
                val tagLength = convertIntToString(tag.length)

                pokemonNicknames.append("&PN${posValue + 1}=${poke.nickname}")

                parts[posValue] = "$numLength$num" +
                        "$xpLengthLength$xpLength$xp" +
                        "$lvlLength$lvl" +
                        "$move1Length$move1" +
                        "$move2Length$move2" +
                        "$move3Length$move3" +
                        "$move4Length$move4" +
                        "$ttLength$tt" +
                        "$genderLength$gender" +
                        "$saveIdLengthLength$saveIdLength$saveId" +
                        "$posLength$pos" +
                        "$extraLength$extra" +
                        "$itemLength$item" +
                        "$tagLength$tag"
            }

            // Might not be needed at the moment but why not
            parts.toSortedMap()

            // I think they use pos for sorting the array so only need the values
            val encodedPokesWithParts = encodedPokes + parts.values.joinToString("")
            val encodedLength = encodedPokesWithParts.length
            val encodedLengthLength = encodedLength.toString().length
            val finalLength = convertIntToString(getLength(encodedLengthLength, encodedLength))
            val finalEncodedPokes = finalLength + encodedPokesWithParts

            return finalEncodedPokes to pokemonNicknames.toString()
            // Gonna use Quercus for Unit Testing
        }

        private fun convertValue(long: Long): Pair<String, String> {
            val valueConverted = convertIntToString(long)
            return valueConverted to convertIntToString(valueConverted.length)
        }

        private fun convertValue(int: Int): Pair<String, String> {
            return convertValue(int.toLong())
        }

        private fun convertValue(short: Short): Pair<String, String> {
            return convertValue(short.toLong())
        }

        private fun convertValue(byte: Byte): Pair<String, String> {
            return convertValue(byte.toLong())
        }

        private fun convertValueVariableLength(long: Long): Triple<String, String, String> {
            val convertedValAndLength = convertValue(long)
            val (convertedValue, length) = convertedValAndLength
            return Triple(convertedValue, length, convertIntToString(length.length))
        }

        private fun convertValueVariableLength(int: Int): Triple<String, String, String> {
            return convertValueVariableLength(int.toLong())
        }

        fun encodeInventory(items: List<GenericKeyValue>): String {
            val encodedItems = StringBuilder()
            var qnt = 0

            for ((num, quantity) in items) {
                if (quantity > 0) {
                    qnt++
                    val (encodedNum, numLength) = convertValue(num)
                    val (encodedQuantity, quantityLength) = convertValue(quantity)

                    encodedItems.append(numLength).append(encodedNum)
                        .append(quantityLength).append(encodedQuantity)
                }
            }

            val inventoryLength = convertIntToString(qnt)
            val inventoryLengthLength = convertIntToString(inventoryLength.length)

            val encodedItemsFinal = "$inventoryLengthLength$inventoryLength$encodedItems"
            val encodedLength = encodedItemsFinal.length
            val encodedLengthLength = encodedLength.toString().length
            val finalLength = convertIntToString(getLength(encodedLengthLength, encodedLength))

            return "$finalLength$encodedItemsFinal"
        }

        fun decodeInventory(encodedItems: String): HashMap<Int, Int> {
            val items = HashMap<Int, Int>()

            val iterator = encodedItems.iterator()
            val encodedLengthLength = convertStringToInt(iterator.nextChar())
            iterator.nextX(encodedLengthLength)
            val inventoryLengthLength = convertStringToInt(iterator.nextChar())
            val inventoryLength = convertStringToInt(iterator.nextXString(inventoryLengthLength))

            for (i in 0..<inventoryLength) {
                val num = iterator.nextVariable(false)
                val quantity = iterator.nextVariable(false)

                items[num] = quantity
            }

            return items
        }

        fun decodeExtra(encodedExtra: String): HashMap<Int, Int> {
            return decodeInventory(encodedExtra)
        }

        fun decode1v1(encodedData: String): HashMap<String, Int> {
            val data = HashMap<String, Int>()

            val iterator = encodedData.iterator()

            // Getting the size of data
            val encodedLengthLength = convertStringToInt(iterator.nextChar())
            val encodedLength = convertStringToInt(
                iterator.nextXString(encodedLengthLength))

            val money = iterator.nextVariable(false)
            val levelsUnlocked = iterator.nextVariable(false)

            data["money"] = money
            data["levelUnlocked"] = levelsUnlocked
            return data
        }

        fun encode1v1(profiles: SizedIterable<PTD2OneV1>): String {
            val encodedData = StringBuilder()

            for (profile in profiles) {
                // May be wrong?
                val whichProfile = convertIntToString(profile.num)
                val encodedMoney = convertIntToString(profile.money)
                val moneyLength = convertIntToString(encodedMoney.length)

                val encodedLevels = convertIntToString(profile.levelUnlocked)
                val levelsLength = convertIntToString(encodedLevels.length)

                encodedData.append(whichProfile)
                    .append(moneyLength).append(encodedMoney)
                    .append(levelsLength).append(encodedLevels)
            }

            val profileAmountString = convertIntToString(profiles.count())
            encodedData.insert(0, profileAmountString)
            val dataLength = encodedData.length
            val dataLengthLength = dataLength.toString().length
            val encodedLength = convertIntToString(getLength(dataLengthLength, dataLength))

            encodedData.insert(0, encodedLength)

            return encodedData.toString()
        }

        fun encodeStory(stories: SizedIterable<PTD2Story>?): String {
            val encodedData = StringBuilder()
            var profileSize = 0

            for (i in 1..3) {
                val profile = stories?.firstOrNull { it.num == i.toByte() }
                if (profile === null) continue

                profileSize++
                val whichProfile = convertIntToString(i)
                // Can probably remove the prefix of encoded from many of these functions
                val encodedMoney = convertIntToString(profile.money)
                val moneyLength = convertIntToString(encodedMoney.length)

                val encodedBadges = convertIntToString(getBadges(profile.extras))
                val badgesLength = convertIntToString(encodedBadges.length)

                encodedData.append(whichProfile)
                    .append(moneyLength).append(encodedMoney)
                    .append(badgesLength).append(encodedBadges)
            }

            val currentProfileString = convertIntToString(profileSize)
            encodedData.append(currentProfileString)
            val dataLength = encodedData.length
            val dataLengthLength = dataLength.toString().length
            val encodedLength = convertIntToString(getLength(dataLengthLength, dataLength))

            encodedData.insert(0, currentProfileString)
                .insert(0, encodedLength)

            return encodedData.toString()
        }

        // I'm sure I can do better than Any type
        fun encodeStoryProfile(profile: PTD2Story): ArrayList<Any> {
            val encodedData = ArrayList<Any>(5)
            val extra = StringBuilder()

            val currentMap = convertIntToString(profile.mapLoc)
            val mapLength = convertIntToString(currentMap.length)
            val currentSpot = convertIntToString(profile.mapSpot)
            val spotLength = convertIntToString(currentSpot.length)
            extra.append(mapLength).append(currentMap)
                .append(spotLength).append(currentSpot)

            val extraLength = extra.length
            val extraLengthLength = extraLength.toString().length
            val encodedExtraLength = convertIntToString(getLength(extraLengthLength, extraLength))

            extra.insert(0, encodedExtraLength)

            val extra2 = encodeInventory(GenericKeyValue.convertExtra(profile.extras)) // Extra info is compatible with items encoding method
            val extra3 = encodePokemons(profile.pokes)
            val extra4 = encodeInventory(GenericKeyValue.convertItems(profile.items))

            val extra5 = convertIntToString(
                //                           extra3[0], probably not identical to it
                createCheckSum("${extra3.first}${profile.currentSave}"))

            encodedData.add(extra.toString())
            encodedData.add(extra2)
            encodedData.add(extra3)
            encodedData.add(extra4)
            encodedData.add(extra5)

            return encodedData
        }

        fun getBadges(extra: SizedIterable<PTD2Extra>): Int {
            var badges = 0

            // This might be able to be an else if thing (or when) vs multiple ifs
            // or perhaps an array of key to badge number
            val badges1 = extra.firstOrNull { it.num == 48.toByte() }
            if (badges1 !== null && badges1.value == 2) badges = 1

            val badges2 = extra.firstOrNull { it.num == 59.toByte() }
            if (badges2 !== null && badges2.value == 2) badges = 2

            val moreBadges = extra.firstOrNull { it.num == 64.toByte() }
            if (moreBadges !== null) {
                val badgesValue = moreBadges.value

                if (badgesValue >= 12) badges = 8
                else if (badgesValue >= 11) badges = 7
                // Where is 6?
                else if (badgesValue >= 9) badges = 5
                else if (badgesValue >= 7) badges = 4
                else if (badgesValue >= 1) badges = 3
            }

            return badges
        }
    }
}

private fun CharIterator.nextX(x: Int): Array<Char> {
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

private fun CharIterator.nextXString(x: Int): String {
    return nextX(x).joinToString("")
}

// Probably want to make new Iterator class later as this is obfuscation specific
private fun CharIterator.nextVariable(variableLength: Boolean = true) : Int {
    val lengthOfLength = if (!variableLength) {
        1
    } else Obfuscation.convertStringToInt(nextChar())
    val length = Obfuscation.convertStringToInt(nextXString(lengthOfLength))
    return Obfuscation.convertStringToInt(nextXString(length))
}