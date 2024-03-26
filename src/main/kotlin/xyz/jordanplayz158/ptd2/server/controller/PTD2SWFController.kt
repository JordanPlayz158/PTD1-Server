package xyz.jordanplayz158.ptd2.server.controller

import at.favre.lib.crypto.bcrypt.BCrypt
import io.ktor.http.*
import io.ktor.server.util.*
import io.ktor.util.date.*
import org.jetbrains.exposed.sql.SizedIterable
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import xyz.jordanplayz158.ptd.ReasonsEnum
import xyz.jordanplayz158.ptd.ResultsEnum
import xyz.jordanplayz158.ptd2.server.*
import xyz.jordanplayz158.ptd2.server.orm.*
import java.time.Instant
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.first

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

            val stories = account.stories

            val response = mutableListOf("Result" to ResultsEnum.SUCCESS.id)
            if (stories === null) {
                response.add("extra" to "ycm")
                return true to response
            }


            // TODO: Can't find the reason for the nickname and color in the original source
            //  could be important though so leaving a note

//                story["profile$i"] = mapOf(
//                    "Nickname" to storyI.nickname,
//                    "Color" to storyI.color,
//                )

            // TODO: Never generated stories on creation or otherwise
            //  so need to check the code from KGM again tomorrow
            response.add("extra" to Obfuscation.encodeStory(stories))
            response.add("dw" to dayOfWeekToInt().toString())



            // Sadly no array read for object variables otherwise this could be generified into a for loop like the original
            if (account.dex1 !== null) {
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
            val stories: SizedIterable<PTD2Story>? = account.stories
            val profile = stories?.firstOrNull { it.num == whichProfile.toByte() }

            if (stories === null || profile === null) {
                // TODO: Turn enums into pairs of key and value
                //  so there is less redundancy in actual code
                return false to listOf("Result" to ResultsEnum.FAILURE.id, "Reason" to ReasonsEnum.FAILURE_NOT_FOUND.id)
            }

            val response = mutableListOf("Result" to ResultsEnum.SUCCESS.id)


            val encodedData = Obfuscation.encodeStoryProfile(profile)
            response.add("CS" to profile.currentSave)
            response.add("CT" to profile.currentTime.toString())
            response.add("Gender" to profile.gender.toString())
            response.add("extra" to encodedData[0].toString()) // Items
            response.add("extra2" to encodedData[1].toString())

            val pokemonData = encodedData[2] as Pair<String, String>
            response.add("extra3" to pokemonData.first // Pokemons data
                    + pokemonData.second) // Pokemon Nicknames
            response.add("extra4" to encodedData[3].toString())
            response.add("extra5" to encodedData[4].toString())

            return true to response
        }

        fun saveStory(account: PTD2Account, whichProfile: Int, parameters: Parameters) : Pair<Boolean, List<Pair<String, String>>> {
            val saveDataString = parameters["extra"]

            if (saveDataString === null) {
                return false to listOf("Result" to ResultsEnum.FAILURE.id, "Reason" to "NotAllParametersSupplied")
            }

            val save = saveDataString.parseUrlEncodedParameters(Charsets.UTF_8, 0)

            val response = mutableListOf("Result" to ResultsEnum.SUCCESS.id)
            transaction {
                val whichProfileByte = whichProfile.toByte()
                val profile = if (save["NewGameSave"] === null) {
                    account.stories.first { it.num == whichProfileByte }
                } else {
                    PTD2Story.new {
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
                    profile.currentSave = currentSave
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

                val pokes = Obfuscation.decodePokeInfo(parameters["extra3"]!!, account.email)

                for ((saveId, poke) in pokes) {
                    val reasons = poke["reason"] as ArrayList<Int>

                    var pokemon = PTD2Pokemon.findById(saveId)

                    val pokeNicknameNum = poke["needNickname"]
                    var nickname: String? = null
                    if (pokeNicknameNum !== null) {
                        nickname = save["PokeNick$pokeNicknameNum"]
                        // Unset may be needed but.... probably not?
                    }

                    for (reason in reasons) {
                        when (reason) {
                            1 -> { // Captured
                                pokemon = PTD2Pokemon.new(saveId) {
                                    this.story = profile.id

                                    // TODO: Nickname isn't set in the decode poke info as far as I can tell
                                    //   recheck source decode poke info
                                    //this.nickname = (poke["Nickname"] as String)
                                    this.nickname = nickname!!
                                    this.num = (poke["num"] as Short)
                                    this.xp = (poke["xp"] as Int)
                                    this.lvl = (poke["lvl"] as Short)
                                    this.move1 = (poke["move1"] as Short)
                                    this.move2 = (poke["move2"] as Short)
                                    this.move3 = (poke["move3"] as Short)
                                    this.move4 = (poke["move4"] as Short)
                                    this.targetType = (poke["targetingType"] as Byte)
                                    this.gender = (poke["gender"] as Byte)
                                    this.pos = (poke["pos"] as Int)
                                    this.extra = (poke["extra"] as Byte)
                                    this.item = (poke["item"] as Byte)
                                    this.tag = (poke["tag"] as String)
                                }
                            }
                            2 -> pokemon!!.lvl = poke["lvl"] as Short // Level up
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
                        response.add("PID$needSaveId" to poke["saveID"] as String)
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
            }

            return true to response
        }

        fun deleteStory(account: PTD2Account, whichProfile: Int): Pair<Boolean, List<Pair<String, String>>> {
            account.stories.first { it.num == whichProfile.toByte() }.delete()

            return true to listOf("Result" to ResultsEnum.SUCCESS.id)
        }

        fun load1v1(account: PTD2Account): Pair<Boolean, List<Pair<String, String>>> {
            //if (accountNull === null) {
                //return false to listOf("Result" to ResultsEnum.SUCCESS.id, "extra" to "ycm", "extra2" to "yqym")
            //}

            val oneV1s = account.oneV1
            val response = mutableListOf("Result" to ResultsEnum.SUCCESS.id, "extra2" to "yqym")

            if (oneV1s === null) {
                // Order doesn't matter but for KGM testing, it "does"
                // can probably change the test to parse the string as arrays
                // and compare array contents ignoring order of elements
                response.add(1, "extra" to "ycm")
                return true to response
            }


            // TODO: remember to put these in transactions, as exposed requires it for any db interaction
            val encodedData = Obfuscation.encode1v1(account.oneV1)
            response.add(1, "extra" to encodedData)
            return true to response
        }

        fun save1v1(account: PTD2Account, whichProfile: Int, parameters: Parameters): Pair<Boolean, List<Pair<String, String>>> {
            val newData = Obfuscation.decode1v1(parameters["extra"]!!)

            transaction {
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

            }

            return true to listOf("Result" to ResultsEnum.SUCCESS.id, "Reason" to ReasonsEnum.SUCCESS_LOADED_ACCOUNT.id)
        }

        fun delete1v1(account: PTD2Account, whichProfile: Int): Pair<Boolean, List<Pair<String, String>>> {
            transaction {
                account.oneV1.firstOrNull { it.num == whichProfile.toByte() }?.delete()
            }

            return true to listOf("Result" to ResultsEnum.SUCCESS.id)
        }

        fun maintenance(): List<Pair<String, String>> {
            return listOf("Result" to ResultsEnum.FAILURE.id, "Reason" to ReasonsEnum.FAILURE_MAINTENANCE.id)
        }

        fun userExists(email: String) : Boolean {
            return transaction { PTD2Account.find(Accounts.email eq email).firstOrNull() } !== null
        }
    }
}