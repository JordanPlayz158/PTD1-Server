package xyz.jordanplayz158.ptd.server.module.ptd2.controller

import io.ktor.http.*
import io.ktor.server.util.*
import io.ktor.util.date.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import xyz.jordanplayz158.ptd.server.common.*
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
                    response.add("extra" to Obfuscation.encodeStory(saves))
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

                val encodedData = Obfuscation.encodeStoryProfile(save)
                response.add("CS" to "0")
                response.add("CT" to save.currentTime.toString())
                response.add("Gender" to save.gender.toString())
                response.add("extra" to encodedData[0].toString()) // Items
                response.add("extra2" to encodedData[1].toString())

                val pokemonData = encodedData[2] as Pair<String, String>
                response.add(
                    "extra3" to pokemonData.first // Pokemons data
                ) // Pokemon Nicknames

                if(pokemonData.second.isNotEmpty()) {
                    val pokemonNameParameters =
                        pokemonData.second.substring(1).parseUrlEncodedParameters(Charsets.UTF_8, 0)
                    for (name in pokemonNameParameters.names()) {
                        response.add(name to pokemonNameParameters[name]!!)
                    }
                }

                response.add("extra4" to encodedData[3].toString())
                response.add("extra5" to encodedData[4].toString())

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

                val pokes = Obfuscation.decodePokeInfo(parameters["extra3"]!!, save)

                for ((saveId, poke) in pokes) {
                    val reasons = poke["reason"] as ArrayList<Int>

                    var pokemon = save.pokemon.firstOrNull { it.swfId == saveId }

                    val pokeNicknameNum = poke["needNickname"]
                    var nickname: String? = null
                    if (pokeNicknameNum !== null) {
                        nickname = saveData["PokeNick$pokeNicknameNum"]
                        // Unset may be needed but.... probably not?
                    }

                    for (reason in reasons) {
                        when (reason) {
                            1 -> { // Captured
                                pokemon = PTD2Pokemon.new {
                                    this.save = save.id
                                    this.swfId = (poke["saveID"] as Int)
                                    this.nickname = nickname!!
                                    this.number = (poke["num"] as Int).toShort()
                                    this.experience = (poke["xp"] as Int)
                                    this.level = (poke["lvl"] as Int).toByte()
                                    this.move1 = (poke["move1"] as Int).toShort()
                                    this.move2 = (poke["move2"] as Int).toShort()
                                    this.move3 = (poke["move3"] as Int).toShort()
                                    this.move4 = (poke["move4"] as Int).toShort()
                                    this.targetType = (poke["targetingType"] as Int).toByte()
                                    this.gender = (poke["gender"] as Int).toByte()
                                    this.position = (poke["pos"] as Int)
                                    this.extra = (poke["extra"] as Int).toByte()
                                    this.item = (poke["item"] as Int).toByte()
                                    this.tag = (poke["tag"] as String)
                                }
                            }
                            2 -> pokemon!!.level = (poke["lvl"] as Int).toByte() // Level up
                            3 -> pokemon!!.experience = poke["xp"] as Int // XP up
                            4 -> { // Change moves
                                pokemon!!.move1 = (poke["move1"] as Short)
                                pokemon.move2 = (poke["move2"] as Short)
                                pokemon.move3 = (poke["move3"] as Short)
                                pokemon.move4 = (poke["move4"] as Short)
                            }
                            5 -> pokemon!!.item = poke["item"] as Byte // Change Item
                            6, 10 -> pokemon!!.number = poke["num"] as Short // 6 = Evolve, 10 = Need Trade
                            7 -> pokemon!!.nickname = nickname!! // Change Nickname
                            8 -> pokemon!!.position = poke["pos"] as Int // Pos change
                            9 -> pokemon!!.tag = poke["tag"] as String // Need Tag
                        }
                    }

                    val needSaveId = poke["needSaveID"]
                    if (needSaveId !== null) {
                        response.add("PID$needSaveId" to saveId.toString())
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

                val encodedData = Obfuscation.encode1v1(oneV1s)
                response.add(1, "extra" to encodedData)
                return@transaction response
            }
        }

        fun save1v1(user: PTD2User, whichProfile: Byte, parameters: Parameters): List<Pair<String, String>> {
            val newData = Obfuscation.decode1v1(parameters["extra"]!!)

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
    }
}