package xyz.jordanplayz158.ptd2.server.controller

import at.favre.lib.crypto.bcrypt.BCrypt
import io.ktor.http.*
import io.ktor.server.util.*
import io.ktor.util.date.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import xyz.jordanplayz158.ptd.ReasonsEnum
import xyz.jordanplayz158.ptd.ResultsEnum
import xyz.jordanplayz158.ptd2.server.Accounts
import xyz.jordanplayz158.ptd2.server.Obfuscation
import xyz.jordanplayz158.ptd2.server.orm.*
import java.time.Instant
import kotlin.collections.component1
import kotlin.collections.component2

class PTD2SWFController {
    companion object {
        fun createAccount(email: String, password: String) : Pair<Boolean, List<Pair<String, String>>> {
            if (userExists(email)) {
                return false to listOf("Result" to ResultsEnum.FAILURE.id, "Reason" to ReasonsEnum.FAILURE_TAKEN.id)
            }

            transaction {
                PTD2Account.new {
                    this.email = email
                    this.pass = BCrypt.withDefaults().hashToString(12, password.toCharArray())
                }
            }

            return loadAccount()
        }

        // Surprised PTD2 doesn't use any actual account elements in the loadAccount response
        //  Probably want to look at the SWF myself later to confirm
        fun loadAccount(/*account: PTD2Account*/) : Pair<Boolean, List<Pair<String, String>>> {
            return true to listOf("Result" to ResultsEnum.SUCCESS.id,
                "Reason" to ReasonsEnum.SUCCESS_LOADED_ACCOUNT.id,
                // Trainer Pass
                "p" to "1")
        }

        fun loadStory(account: PTD2Account): Pair<Boolean, List<Pair<String, String>>> {
            // Keeping this as not sure if the response from before this is called breaks it
            // Probably should be switched to a proper failure one if one exists
            //if (account === null) {
            //  return false to listOf("Result" to ResultsEnum.SUCCESS.id, "extra" to "ycm")
            //}


            val response = mutableListOf("Result" to ResultsEnum.SUCCESS.id)

            transaction {
                if (account.dex1 !== null) {
                    val stories = account.stories
                    response.add("extra" to Obfuscation.encodeStory(stories))
                    response.add("dw" to dayOfWeekToInt().toString())

                    // Sadly no array read for object variables otherwise this could be generified into a for loop like the original
                    response.addAll(addDexes(account.dex1!!, 1))
                    response.addAll(addDexes(account.dex2!!, 2))
                    response.addAll(addDexes(account.dex3!!, 3))
                    response.addAll(addDexes(account.dex4!!, 4))
                    response.addAll(addDexes(account.dex5!!, 5))
                    response.addAll(addDexes(account.dex6!!, 6))

                    for (i in 1..3) {
                        val profile = stories.firstOrNull { it.num == i.toByte() }
                        if (profile === null) continue

                        response.add("Nickname$i" to profile.nickname)
                        response.add("Version$i" to profile.color.toString())
                    }
                } else {
                    response.add("extra" to "ycm")
                }
            }

            return true to response
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
        fun loadStoryProfile(account: PTD2Account, whichProfile: Int) : Pair<Boolean, List<Pair<String, String>>> {
            return transaction {
                val stories = account.stories
                val profile = stories?.firstOrNull { it.num == whichProfile.toByte() }

                if (stories === null || profile === null) {
                    // TODO: Turn enums into pairs of key and value
                    //  so there is less redundancy in actual code
                    return@transaction false to listOf(
                        "Result" to ResultsEnum.FAILURE.id,
                        "Reason" to ReasonsEnum.FAILURE_NOT_FOUND.id
                    )
                }

                val response = mutableListOf("Result" to ResultsEnum.SUCCESS.id)


                val encodedData = Obfuscation.encodeStoryProfile(profile)
                response.add("CS" to profile.currentSave)
                response.add("CT" to profile.currentTime.toString())
                response.add("Gender" to profile.gender.toString())
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

                return@transaction true to response
            }
        }

        fun saveStory(account: PTD2Account, whichProfile: Int, parameters: Parameters) : Pair<Boolean, List<Pair<String, String>>> {
            val saveDataString = parameters["extra"]

            if (saveDataString === null) {
                return false to listOf("Result" to ResultsEnum.FAILURE.id, "Reason" to "NotAllParametersSupplied")
            }

            val save = saveDataString.parseUrlEncodedParameters(Charsets.UTF_8, 0)

            return transaction {
                val response = ArrayList<Pair<String, String>>(1)
                val whichProfileByte = whichProfile.toByte()
                var profile = account.stories.firstOrNull { it.num == whichProfileByte }

                if (save["NewGameSave"] !== null) {
                    if (profile !== null) {
                        // Rather than updating existing entry
                        //  deleting works better due to cascading on delete
                        profile.delete()
                    }

                    profile = PTD2Story.new {
                        this.account = account.id
                        this.num = whichProfileByte
                        this.nickname = save["Nickname"]!!
                        this.color = save["Color"]!!.toByte()
                        this.gender = save["Gender"]!!.toByte()
                        // Can be set as default values for profile
                        this.money = 10
                        this.currentTime = 100
                    }
                }

                if (profile === null) {
                    return@transaction false to listOf("Result" to ResultsEnum.FAILURE.id, "Reason" to ReasonsEnum.FAILURE_NOT_FOUND.id)
                }

                // I think this can be done better, not the point though
                if (save["MapSave"] !== null) {
                    profile.mapLoc = save["MapLoc"]!!.toShort()
                    profile.mapSpot = save["MapSpot"]!!.toShort()
                }

                // Money Save
                if (save["MSave"] !== null) {
                    profile.money = Obfuscation.convertStringToInt(save["MA"]!!)
                }

                // CurrentSave
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
                }

                if (save["TimeSave"] !== null) {
                    // I hope this block can be simplified
                    profile.currentTime = save["CT"]!!.toShort()
                }

                // Need Dex
                if (parameters["needD"] !== null) {
                    account.dex1 = parameters["dextra1"]
                    account.dex2 = parameters["dextra2"]
                    account.dex3 = parameters["dextra3"]
                    account.dex4 = parameters["dextra4"]
                    account.dex5 = parameters["dextra5"]
                    account.dex6 = parameters["dextra6"]
                }

                val pokes = Obfuscation.decodePokeInfo(parameters["extra3"]!!, profile)

                for ((saveId, poke) in pokes) {
                    val reasons = poke["reason"] as ArrayList<Int>

                    var pokemon = profile.pokes.firstOrNull { it.swfId == saveId }

                    val pokeNicknameNum = poke["needNickname"]
                    var nickname: String? = null
                    if (pokeNicknameNum !== null) {
                        nickname = save["PokeNick$pokeNicknameNum"]
                        // Unset may be needed but.... probably not?
                    }

                    for (reason in reasons) {
                        when (reason) {
                            1 -> { // Captured
                                pokemon = PTD2Pokemon.new {
                                    this.story = profile.id
                                    this.swfId = (poke["saveID"] as Int)
                                    this.nickname = nickname!!
                                    this.num = (poke["num"] as Int).toShort()
                                    this.xp = (poke["xp"] as Int)
                                    this.lvl = (poke["lvl"] as Int).toShort()
                                    this.move1 = (poke["move1"] as Int).toShort()
                                    this.move2 = (poke["move2"] as Int).toShort()
                                    this.move3 = (poke["move3"] as Int).toShort()
                                    this.move4 = (poke["move4"] as Int).toShort()
                                    this.targetType = (poke["targetingType"] as Int).toByte()
                                    this.gender = (poke["gender"] as Int).toByte()
                                    this.pos = (poke["pos"] as Int)
                                    this.extra = (poke["extra"] as Int).toByte()
                                    this.item = (poke["item"] as Int).toByte()
                                    this.tag = (poke["tag"] as String)
                                }
                            }
                            2 -> pokemon!!.lvl = (poke["lvl"] as Int).toShort() // Level up
                            3 -> pokemon!!.xp = poke["xp"] as Int // XP up
                            4 -> { // Change moves
                                pokemon!!.move1 = (poke["move1"] as Short)
                                pokemon.move2 = (poke["move2"] as Short)
                                pokemon.move3 = (poke["move3"] as Short)
                                pokemon.move4 = (poke["move4"] as Short)
                            }
                            5 -> pokemon!!.item = poke["item"] as Byte // Change Item
                            6 -> pokemon!!.num = poke["num"] as Short // Evolve
                            7 -> pokemon!!.nickname = nickname!! // Change Nickname
                            8 -> pokemon!!.pos = poke["pos"] as Int // Pos change
                            9 -> pokemon!!.tag = poke["tag"] as String // Need Tag
                            // TODO: Again, can probably merge evolve and trade
                            10 -> pokemon!!.num = poke["num"] as Short // Need Trade
                        }
                    }

                    val needSaveId = poke["needSaveID"]
                    if (needSaveId !== null) {
                        response.add("PID$needSaveId" to saveId.toString())
                    }
                }

                val items = Obfuscation.decodeInventory(parameters["extra4"] as String)

                for ((num, quantity) in items) {
                    var item = profile.items.firstOrNull { it.num == num.toByte() }

                    if (item === null) item = PTD2Item.new {
                        this.story = profile.id
                        this.num = num.toByte()
                    }

                    item.value = quantity
                }

                val extra = Obfuscation.decodeExtra(parameters["extra2"] as String)

                for ((num, value) in extra) {
                    // Can make a generic function for both
                    var extraDb = profile.extras.firstOrNull { it.num == num.toByte() }

                    if (extraDb === null) {
                        extraDb = PTD2Extra.new {
                            this.story = profile.id
                            this.num = num.toByte()
                        }
                    }

                    extraDb.value = value
                }

                response.add("Result" to ResultsEnum.SUCCESS.id)
                return@transaction true to response
            }
        }

        fun deleteStory(account: PTD2Account, whichProfile: Int): Pair<Boolean, List<Pair<String, String>>> {
            transaction { account.stories.first { it.num == whichProfile.toByte() }.delete() }

            return true to listOf("Result" to ResultsEnum.SUCCESS.id)
        }

        fun load1v1(account: PTD2Account): Pair<Boolean, List<Pair<String, String>>> {
            //if (accountNull === null) {
                //return false to listOf("Result" to ResultsEnum.SUCCESS.id, "extra" to "ycm", "extra2" to "yqym")
            //}

            val response = mutableListOf("Result" to ResultsEnum.SUCCESS.id, "extra2" to "yqym")

            // Somehow a SizedIterator can somehow be null
            //  even when it is not null but this should be safe

            return transaction {
                // Everything even the most insignificant
                //  innocuous thing, put it in a transaction
                val oneV1s = account.oneV1

                if (oneV1s.count() == 0L) {
                    // Order doesn't matter but for KGM testing, it "does"
                    // can probably change the test to parse the string as arrays
                    // and compare array contents ignoring order of elements
                    response.add(1, "extra" to "ycm")
                    return@transaction true to response
                }

                val encodedData = Obfuscation.encode1v1(oneV1s)
                response.add(1, "extra" to encodedData)
                return@transaction true to response
            }
        }

        fun save1v1(account: PTD2Account, whichProfile: Int, parameters: Parameters): Pair<Boolean, List<Pair<String, String>>> {
            val newData = Obfuscation.decode1v1(parameters["extra"]!!)

            return transaction {
                var oneV1 = account.oneV1.firstOrNull { it.num == whichProfile.toByte() }

                if (oneV1 === null) {
                    oneV1 = PTD2OneV1.new {
                        this.account = account.id
                        this.num = whichProfile.toByte()
                    }
                    // Regardless of if it actually saves something
                    //  KGM's still sends a success, check SWF to confirm
                }

                oneV1.money = newData["money"]!!
                oneV1.levelUnlocked = newData["levelUnlocked"]!!.toByte()
                return@transaction true to listOf("Result" to ResultsEnum.SUCCESS.id, "Reason" to ReasonsEnum.SUCCESS_LOADED_ACCOUNT.id)
            }
        }

        fun delete1v1(account: PTD2Account, whichProfile: Int): Pair<Boolean, List<Pair<String, String>>> {
            return transaction {
                account.oneV1.firstOrNull { it.num == whichProfile.toByte() }?.delete()
                return@transaction true to listOf("Result" to ResultsEnum.SUCCESS.id)
            }
        }

        fun maintenance(): Pair<Boolean, List<Pair<String, String>>> {
            return false to listOf("Result" to ResultsEnum.FAILURE.id, "Reason" to ReasonsEnum.FAILURE_MAINTENANCE.id)
        }

        fun userExists(email: String) : Boolean {
            return transaction { return@transaction PTD2Account.find(Accounts.email eq email).firstOrNull() !== null }
        }
    }
}