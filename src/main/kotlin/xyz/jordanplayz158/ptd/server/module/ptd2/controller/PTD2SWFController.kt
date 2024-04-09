package xyz.jordanplayz158.ptd.server.module.ptd2.controller

import io.ktor.http.*
import io.ktor.server.util.*
import io.ktor.util.date.*
import org.jetbrains.exposed.sql.SizedIterable
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import xyz.jordanplayz158.ptd.server.common.ReasonsEnum
import xyz.jordanplayz158.ptd.server.common.ResultsEnum
import xyz.jordanplayz158.ptd.server.common.data.Pokemon
import xyz.jordanplayz158.ptd.server.common.getOrCreateUser
import xyz.jordanplayz158.ptd.server.common.getUser
import xyz.jordanplayz158.ptd.server.common.obfuscation.Obfuscation
import xyz.jordanplayz158.ptd.server.common.obfuscation.ObfuscationBuilder
import xyz.jordanplayz158.ptd.server.common.obfuscation.ObfuscationIterator
import xyz.jordanplayz158.ptd.server.module.ptd2.orm.*
import java.time.Instant
import kotlin.collections.component1
import kotlin.collections.component2

class PTD2SWFController {
    companion object {
        fun createAccount(email: String, password: String) : List<Pair<String, String>> {
            if (userExists(email)) {
                return ReasonsEnum.FAILURE_TAKEN.fullReason()
            }

            return transaction {
                val user = getOrCreateUser(email, password)
                PTD2User.new {
                    this.user = user.id
                }

                return@transaction loadAccount()
            }
        }

        // Surprised PTD2 doesn't use any actual account elements in the loadAccount response
        //  Probably want to look at the SWF myself later to confirm
        fun loadAccount(/*account: PTD2Account*/) : List<Pair<String, String>> {
            val response = ReasonsEnum.SUCCESS_LOADED_ACCOUNT.fullReason()
            // Trainer Pass
            response.add("p" to "1")

            return response
        }

        fun loadStory(user: PTD2User): List<Pair<String, String>> {
            // Keeping this as not sure if the response from before this is called breaks it
            // Probably should be switched to a proper failure one if one exists
            //if (account === null) {
            //  return false to listOf("Result" to ResultsEnum.SUCCESS.id, "extra" to "ycm")
            //}


            val response = mutableListOf(ResultsEnum.SUCCESS.id)

            transaction {
                if (user.dex1 !== null) {
                    val saves = user.saves
                    response.add("extra" to encodeSaves(saves))
                    response.add("dw" to dayOfWeekToInt().toString())

                    // Sadly no array read for object variables otherwise this could be generified into a for loop like the original
                    response.addAll(addDexes(user.dex1!!, 1))
                    response.addAll(addDexes(user.dex2!!, 2))
                    response.addAll(addDexes(user.dex3!!, 3))
                    response.addAll(addDexes(user.dex4!!, 4))
                    response.addAll(addDexes(user.dex5!!, 5))
                    response.addAll(addDexes(user.dex6!!, 6))

                    for (i in 1..3) {
                        val save = saves.firstOrNull { it.number == i.toByte() }
                        if (save === null) continue

                        response.add("Nickname$i" to save.nickname)
                        response.add("Version$i" to save.version.toString())
                    }
                } else {
                    response.add("extra" to "ycm")
                }
            }

            return response
        }

        fun addDexes(dex: String, number: Int): List<Pair<String, String>> {
            val response = ArrayList<Pair<String, String>>(2)
            response.add("dextra$number" to dex)
            response.add("dcextra$number" to Obfuscation.convertIntToString
                (Obfuscation.createCheckSum(dex)))

            return response
        }

        // Probably define the Timezone somewhere and use that timezone instead of gmt
        fun dayOfWeekToInt(day: WeekDay = Instant.now().toGMTDate().dayOfWeek): Int {
            return when(day) {
                WeekDay.SUNDAY -> 0
                WeekDay.MONDAY -> 1
                WeekDay.TUESDAY -> 2
                WeekDay.WEDNESDAY -> 3
                WeekDay.THURSDAY -> 4
                WeekDay.FRIDAY -> 5
                WeekDay.SATURDAY -> 6
            }
        }

        // If not already mentioned, messy return type was fixed in spring boot, will be backported
        fun loadStoryProfile(save: PTD2Save) : List<Pair<String, String>> {
            return transaction {
                val response = mutableListOf(ResultsEnum.SUCCESS.id)

                val encodedData = encodeSave(save)
                response.add("CS" to "0")
                response.add("CT" to save.currentTime.toString())
                response.add("Gender" to save.gender.toString())
                response.add("extra" to encodedData[0]) // Items
                response.add("extra2" to encodedData[1])

                response.add("extra3" to encodedData[2]) // Pokemons data

                // Pokemon Nicknames
                val pokemonNicknames = encodedData[3]
                if(pokemonNicknames.isNotEmpty()) {
                    val pokemonNameParameters =
                        pokemonNicknames.substring(1).parseUrlEncodedParameters(Charsets.UTF_8, 0)
                    for (name in pokemonNameParameters.names()) {
                        response.add(name to pokemonNameParameters[name]!!)
                    }
                }

                response.add("extra4" to encodedData[4])
                response.add("extra5" to encodedData[5])

                return@transaction response
            }
        }

        fun saveStory(user: PTD2User, whichProfile: Byte, parameters: Parameters) : List<Pair<String, String>> {
            val saveDataString = parameters["extra"]

            if (saveDataString === null) {
                return ReasonsEnum.FAILURE_NOT_ALL_PARAMETERS_SUPPLIED.fullReason()
            }

            val saveData = saveDataString.parseUrlEncodedParameters(Charsets.UTF_8, 0)

            return transaction {
                val response = ArrayList<Pair<String, String>>(1)
                var save = user.saves.firstOrNull { it.number == whichProfile }

                if (saveData["NewGameSave"] !== null) {
                    if (save !== null) {
                        // Rather than updating existing entry
                        //  deleting works better due to cascading on delete
                        save.delete()
                    }

                    save = PTD2Save.new {
                        this.user = user.id
                        this.number = whichProfile
                        this.nickname = saveData["Nickname"]!!
                        this.version = saveData["Color"]!!.toByte()
                        this.gender = saveData["Gender"]!!.toByte()
                        // Can be set as default values for profile
                        this.money = 10
                        this.currentTime = 100
                    }
                }

                if (save === null) {
                    return@transaction ReasonsEnum.FAILURE_NOT_FOUND.fullReason()
                }

                // I think this can be done better, not the point though
                if (saveData["MapSave"] !== null) {
                    save.currentMap = saveData["MapLoc"]!!.toByte()
                    save.mapSpot = saveData["MapSpot"]!!.toByte()
                }

                // Money Save
                if (saveData["MSave"] !== null) {
                    save.money = Obfuscation.convertStringToInt(saveData["MA"]!!)
                }

                // CurrentSave
                /*
                val currentSave = save["CS"]
                if (currentSave !== null) {
                    // MySQLi coerces null strings/null values to 0
                    //  as type 'i' is provided
                    if (currentSave == "null") {
                        // Can add as a default and remove later
                        profile.currentSave = "0"
                    } else {
                        profile.currentSave = currentSave
                    }
                }*/

                if (saveData["TimeSave"] !== null) {
                    // I hope this block can be simplified
                    save.currentTime = saveData["CT"]!!.toByte()
                }

                // Need Dex
                if (parameters["needD"] !== null) {
                    user.dex1 = parameters["dextra1"]
                    user.dex2 = parameters["dextra2"]
                    user.dex3 = parameters["dextra3"]
                    user.dex4 = parameters["dextra4"]
                    user.dex5 = parameters["dextra5"]
                    user.dex6 = parameters["dextra6"]
                }

                val pokes = decodePokeInfo(parameters["extra3"]!!, save)

                for (poke in pokes) {
                    var pokemon = save.pokemon.firstOrNull { it.swfId == poke.saveId }

                    val pokeNicknameNum = poke.needNickname
                    var nickname: String? = null
                    if (pokeNicknameNum !== null) {
                        nickname = saveData["PokeNick$pokeNicknameNum"]
                    }

                    for (reason in poke.reasons) {
                        when (reason) {
                            1 -> { // Captured
                                pokemon = PTD2Pokemon.new {
                                    this.save = save.id
                                    this.swfId = poke.saveId
                                    this.nickname = nickname!!
                                    this.number = poke.number!!.toShort()
                                    this.experience = poke.experience!!
                                    this.level = poke.level!!.toByte()
                                    this.move1 = poke.moves!![0].toShort()
                                    this.move2 = poke.moves[1].toShort()
                                    this.move3 = poke.moves[2].toShort()
                                    this.move4 = poke.moves[3].toShort()
                                    this.targetType = poke.targetType!!.toByte()
                                    this.gender = poke.gender!!.toByte()
                                    this.position = poke.position!!
                                    this.extra = poke.extra!!.toByte()
                                    this.item = poke.item!!.toByte()
                                    this.tag = poke.tag!!
                                }
                            }
                            2 -> pokemon!!.level = poke.level!!.toByte() // Level up
                            3 -> pokemon!!.experience = poke.experience!! // XP up
                            4 -> { // Change moves
                                pokemon!!.move1 = poke.moves!![0].toShort()
                                pokemon.move2 = poke.moves[1].toShort()
                                pokemon.move3 = poke.moves[2].toShort()
                                pokemon.move4 = poke.moves[3].toShort()
                            }
                            5 -> pokemon!!.item = poke.item!!.toByte() // Change Item
                            6, 10 -> pokemon!!.number = poke.number!!.toShort() // 6 = Evolve, 10 = Need Trade
                            7 -> pokemon!!.nickname = nickname!! // Change Nickname
                            8 -> pokemon!!.position = poke.position!! // Pos change
                            9 -> pokemon!!.tag = poke.tag!! // Need Tag
                        }
                    }

                    val needSaveId = poke.needSaveId
                    if (needSaveId !== null) {
                        response.add("PID$needSaveId" to poke.saveId.toString())
                    }
                }

                val items = Obfuscation.decodeInventory(parameters["extra4"] as String)

                for ((num, quantity) in items) {
                    var item = save.items().firstOrNull { it.number == num }

                    if (item === null) item = PTD2Extra.new {
                        this.save = save.id
                        this.isItem = true
                        this.number = num
                    }

                    item.value = quantity
                }

                val extra = Obfuscation.decodeExtra(parameters["extra2"] as String)

                for ((num, value) in extra) {
                    // Can make a generic function for both
                    var extraDb = save.extras().firstOrNull { it.number == num }

                    if (extraDb === null) {
                        extraDb = PTD2Extra.new {
                            this.save = save.id
                            this.isItem = false
                            this.number = num
                        }
                    }

                    extraDb.value = value
                }

                response.add(ResultsEnum.SUCCESS.id)
                return@transaction response
            }
        }

        fun deleteStory(story: PTD2Save): List<Pair<String, String>> {
            story.delete()

            return listOf(ResultsEnum.SUCCESS.id)
        }

        fun load1v1(user: PTD2User): List<Pair<String, String>> {
            val response = mutableListOf(ResultsEnum.SUCCESS.id, "extra2" to "yqym")

            return transaction {
                val oneV1s = user.oneV1

                if (oneV1s.count() == 0L) {
                    // Order doesn't matter but for KGM testing, it "does"
                    // can probably change the test to parse the string as arrays
                    // and compare array contents ignoring order of elements
                    response.add(1, "extra" to "ycm")
                    return@transaction response
                }

                val encodedData = encode1v1(oneV1s)
                response.add(1, "extra" to encodedData)
                return@transaction response
            }
        }

        fun save1v1(user: PTD2User, whichProfile: Byte, parameters: Parameters): List<Pair<String, String>> {
            val newData = decode1v1(parameters["extra"]!!)

            return transaction {
                var oneV1 = user.oneV1.firstOrNull { it.number == whichProfile }

                if (oneV1 === null) {
                    oneV1 = PTD2OneV1.new {
                        this.user = user.id
                        this.number = whichProfile
                    }
                    // Regardless of if it actually saves something
                    //  KGM's still sends a success, check SWF to confirm
                }

                oneV1.money = newData["money"]!!
                oneV1.levelsUnlocked = newData["levelUnlocked"]!!.toByte()
                return@transaction ReasonsEnum.SUCCESS_LOADED_ACCOUNT.fullReason()
            }
        }

        fun delete1v1(oneV1: PTD2OneV1?): List<Pair<String, String>> {
            return transaction {
                oneV1?.delete()
                return@transaction listOf(ResultsEnum.SUCCESS.id)
            }
        }

        fun userExists(email: String) : Boolean {
            val user = getUser(email)

            if (user === null) return false

            return transaction { return@transaction PTD2User.find(PTD2Users.user eq user.id).firstOrNull() !== null }
        }

        fun getAvailableSaveId(save: PTD2Save): Int {
            return transaction { (save.pokemon.maxByOrNull { pokemon -> pokemon.swfId }?.swfId ?: 0) + 1 }
        }

        fun getLength(param1: Int, param2: Int): String {
            val stringLength = (param1 + param2 + 1).toString()
            val length = stringLength.length

            if (length != param1) {
                return getLength(length, param2)
            }

            return "$length$stringLength"
        }

        fun decodePokeInfo(encoded: String, save: PTD2Save): ArrayList<Pokemon> {
            return Obfuscation.decodePokeInfo(encoded, getAvailableSaveId(save))
        }

        fun encodePokemons(pokemons: SizedIterable<PTD2Pokemon>): Pair<String, String> {
            val pokemonsSize = pokemons.count().toInt()
            val pokes = Obfuscation.convertIntToString(pokemonsSize)
            val pokesLen = Obfuscation.convertIntToString(pokes.length)

            val encodedPokes = "$pokesLen$pokes"
            val pokemonNicknames = ObfuscationBuilder()
            val parts = HashMap<Int, String>()

            for (poke in pokemons) {
                /* this is to avoid suspect of hacking
                 For some reason sometimes the game
                 sends only the new pos of one poke */
                if (parts.containsKey(poke.position)) {
                    for (i in 0..<pokemonsSize) {
                        if (!parts.containsKey(i)) {
                            poke.position = i
                            break
                        }
                    }
                }

                val part = ObfuscationBuilder()

                part.appendObfuscatedLength(poke.number)
                part.appendObfuscatedLength(poke.experience, true)
                part.appendObfuscatedLength(poke.level)
                part.appendObfuscatedLength(poke.move1)
                part.appendObfuscatedLength(poke.move2)
                part.appendObfuscatedLength(poke.move3)
                part.appendObfuscatedLength(poke.move4)
                part.appendObfuscatedLength(poke.targetType)
                part.appendObfuscatedLength(poke.gender)
                part.appendObfuscatedLength(poke.swfId, true)

                val posValue = poke.position
                part.appendObfuscatedLength(posValue)
                part.appendObfuscatedLength(poke.extra)
                part.appendObfuscatedLength(poke.item)

                val tag = poke.tag
                part.appendObfuscated(tag.length)
                part.append(tag)

                pokemonNicknames.append("&PN${posValue + 1}=${poke.nickname}")

                parts[posValue] = part.toString()
            }

            // Might not be needed at the moment but why not
            parts.toSortedMap()

            // I think they use pos for sorting the array so only need the values
            val encodedPokesWithParts = encodedPokes + parts.values.joinToString("")
            val encodedLength = encodedPokesWithParts.length
            val encodedLengthLength = encodedLength.toString().length
            val finalLength = Obfuscation.convertIntToString(getLength(encodedLengthLength, encodedLength))
            val finalEncodedPokes = finalLength + encodedPokesWithParts

            return finalEncodedPokes to pokemonNicknames.toString()
        }

        fun encodeInventory(items: List<PTD2Extra>): String {
            val encodedItems = ObfuscationBuilder()
            var qnt = 0

            for (item in items) {
                val num = item.number
                val quantity = item.value

                if (quantity > 0) {
                    qnt++
                    encodedItems.appendObfuscatedLength(num).appendObfuscatedLength(quantity)
                }
            }

            val inventoryLength = Obfuscation.convertIntToString(qnt)
            val inventoryLengthLength = Obfuscation.convertIntToString(inventoryLength.length)

            val encodedItemsFinal = "$inventoryLengthLength$inventoryLength$encodedItems"
            val encodedLength = encodedItemsFinal.length
            val encodedLengthLength = encodedLength.toString().length
            val finalLength = Obfuscation.convertIntToString(getLength(encodedLengthLength, encodedLength))

            return "$finalLength$encodedItemsFinal"
        }

        fun encode1v1(profiles: SizedIterable<PTD2OneV1>): String {
            val encodedData = ObfuscationBuilder()

            for (profile in profiles) {
                // May be wrong?
                val whichProfile = Obfuscation.convertIntToString(profile.number)
                val encodedMoney = Obfuscation.convertIntToString(profile.money)
                val moneyLength = Obfuscation.convertIntToString(encodedMoney.length)

                val encodedLevels = Obfuscation.convertIntToString(profile.levelsUnlocked)
                val levelsLength = Obfuscation.convertIntToString(encodedLevels.length)

                encodedData.append(whichProfile)
                    .append(moneyLength).append(encodedMoney)
                    .append(levelsLength).append(encodedLevels)
            }

            val profileAmountString = Obfuscation.convertIntToString(profiles.count())
            encodedData.insert(0, profileAmountString)
            val dataLength = encodedData.length()
            val dataLengthLength = dataLength.toString().length
            val encodedLength = Obfuscation.convertIntToString(getLength(dataLengthLength, dataLength))

            encodedData.insert(0, encodedLength)

            return encodedData.toString()
        }

        fun encodeSaves(stories: SizedIterable<PTD2Save>?): String {
            val encodedData = ObfuscationBuilder()
            var profileSize = 0

            for (i in 1..3) {
                val profile = stories?.firstOrNull { it.number == i.toByte() }
                if (profile === null) continue

                profileSize++
                val whichProfile = Obfuscation.convertIntToString(i)
                // Can probably remove the prefix of encoded from many of these functions
                val encodedMoney = Obfuscation.convertIntToString(profile.money)
                val moneyLength = Obfuscation.convertIntToString(encodedMoney.length)

                val encodedBadges = Obfuscation.convertIntToString(getBadges(profile.extras))
                val badgesLength = Obfuscation.convertIntToString(encodedBadges.length)

                encodedData.append(whichProfile)
                    .append(moneyLength).append(encodedMoney)
                    .append(badgesLength).append(encodedBadges)
            }

            val currentProfileString = Obfuscation.convertIntToString(profileSize)
            encodedData.append(currentProfileString)
            val dataLength = encodedData.length()
            val dataLengthLength = dataLength.toString().length
            val encodedLength = Obfuscation.convertIntToString(getLength(dataLengthLength, dataLength))

            encodedData.insert(0, currentProfileString)
                .insert(0, encodedLength)

            return encodedData.toString()
        }

        fun encodeSave(save: PTD2Save): Array<String> {
            val encodedData = arrayOfNulls<String>(6)
            val extra = ObfuscationBuilder()

            val currentMap = Obfuscation.convertIntToString(save.currentMap)
            val mapLength = Obfuscation.convertIntToString(currentMap.length)
            val currentSpot = Obfuscation.convertIntToString(save.mapSpot)
            val spotLength = Obfuscation.convertIntToString(currentSpot.length)
            extra.append(mapLength).append(currentMap)
                .append(spotLength).append(currentSpot)

            val extraLength = extra.length()
            val extraLengthLength = extraLength.toString().length
            val encodedExtraLength =
                Obfuscation.convertIntToString(getLength(extraLengthLength, extraLength))

            extra.insert(0, encodedExtraLength)

            val extra2 = encodeInventory(save.extras()) // Extra info is compatible with items encoding method
            val extra3 = encodePokemons(save.pokemon)
            val extra4 = encodeInventory(save.items())

            val extra5 = Obfuscation.convertIntToString(
                Obfuscation.createCheckSum("${extra3.first}0")
            )

            encodedData[0] = extra.toString()
            encodedData[1] = extra2
            encodedData[2] = extra3.first
            encodedData[3] = extra3.second
            encodedData[4] = extra4
            encodedData[5] = extra5

            return encodedData.requireNoNulls()
        }

        fun getBadges(extra: SizedIterable<PTD2Extra>): Int {
            var badges = 0

            // This might be able to be an else if thing (or when) vs multiple ifs
            // or perhaps an array of key to badge number
            val badges1 = extra.firstOrNull { it.number == 48.toShort() }
            if (badges1 !== null && badges1.value == 2) badges = 1

            val badges2 = extra.firstOrNull { it.number == 59.toShort() }
            if (badges2 !== null && badges2.value == 2) badges = 2

            val moreBadges = extra.firstOrNull { it.number == 64.toShort() }
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

        fun decode1v1(encodedData: String): HashMap<String, Int> {
            val data = HashMap<String, Int>()

            val iterator = ObfuscationIterator(encodedData)

            // Getting the size of data
            val encodedLengthLength = Obfuscation.convertStringToInt(iterator.nextChar())
            val encodedLength = Obfuscation.convertStringToInt(
                iterator.nextXString(encodedLengthLength)
            )

            val money = iterator.nextVariable(false)
            val levelsUnlocked = iterator.nextVariable(false)

            data["money"] = money
            data["levelUnlocked"] = levelsUnlocked
            return data
        }
    }
}