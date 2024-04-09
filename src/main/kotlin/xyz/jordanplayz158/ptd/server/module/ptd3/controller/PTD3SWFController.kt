package xyz.jordanplayz158.ptd.server.module.ptd3.controller

import io.ktor.http.*
import org.jetbrains.exposed.sql.SizedIterable
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import xyz.jordanplayz158.ptd.server.common.ReasonsEnum
import xyz.jordanplayz158.ptd.server.common.ResultsEnum
import xyz.jordanplayz158.ptd.server.common.data.Pokemon
import xyz.jordanplayz158.ptd.server.common.getOrCreateUser
import xyz.jordanplayz158.ptd.server.common.getUser
import xyz.jordanplayz158.ptd.server.common.obfuscation.Obfuscation
import xyz.jordanplayz158.ptd.server.common.orm.User
import xyz.jordanplayz158.ptd.server.module.ptd3.orm.PTD3Extra
import xyz.jordanplayz158.ptd.server.module.ptd3.orm.PTD3Pokemon
import xyz.jordanplayz158.ptd.server.module.ptd3.orm.PTD3Save
import xyz.jordanplayz158.ptd.server.module.ptd3.orm.PTD3Saves
import kotlin.random.Random
import kotlin.random.nextInt

class PTD3SWFController {
    companion object {
        fun createAccount(email: String, password: String) : List<Pair<String, String>> {
            if (userExists(email)) {
                return ReasonsEnum.FAILURE_TAKEN.fullReason()
            }

            return transaction {
                getOrCreateUser(email, password)
                return@transaction loadAccount()
            }
        }

        fun loadAccount() : List<Pair<String, String>> {
            val response = ReasonsEnum.SUCCESS_LOADED_ACCOUNT.fullReason()
            response.add(1, "UID" to Random.nextInt(100..1000).toString())

            return response
        }

        fun loadStory(user: User): List<Pair<String, String>> {
            val response = mutableListOf(ResultsEnum.SUCCESS.id)

            val extra = StringBuilder()

            transaction {
                val saves = getSaves(user)
                extra.append(saves.count())

                for (save in saves) {
                    val num = save.number
                    val money = save.money
                    val levelsCompleted = save.levelsCompleted
                    val levelsAccomplished = save.levelsAccomplished

                    extra.append(num)
                    extra.append(money.toString().length).append(money)
                    extra.append(levelsCompleted.toString().length).append(levelsCompleted)
                    extra.append(levelsAccomplished.toString().length).append(levelsAccomplished)
                    response.add("Nickname$num" to save.nickname)
                    response.add("Version$num" to save.version.toString())
                }
            }

            response.add("extra" to encodeDataWithLength(extra.toString()))


            return response
        }

        // If not already mentioned, messy return type was fixed in spring boot, will be backported
        fun loadStoryProfile(user: User, whichProfile: Byte) : List<Pair<String, String>> {
            return transaction {
                val save = getSaves(user).firstOrNull { it.number == whichProfile }

                if (save === null) {
                    return@transaction ReasonsEnum.FAILURE_NOT_FOUND.fullReason()
                }

                val response = mutableListOf(ResultsEnum.SUCCESS.id)

                response.add("extra" to encodeDataWithLength(
                    "${writeNumber(save.levelsCompleted)}" +
                            "${writeNumber(save.levelsAccomplished)}"))

                val extra = save.extras()
                val extras = StringBuilder(writeNumber(extra.count()))

                for (info in extra) {
                    extras.append(writeNumber(info.number))
                    extras.append(writeNumber(info.value))
                }

                response.add("extra2" to encodeDataWithLength(extras.toString()))

                val pokemons = save.pokemons.sortedBy { it.position }
                val pokemonData = StringBuilder(writeNumber(pokemons.size))

                pokemons.forEachIndexed { index, pokemon ->
                    response.add("PN${index + 1}" to pokemon.nickname)

                    pokemonData.append(writeNumber(pokemon.number))
                    pokemonData.append(writeDoubleNumber(pokemon.experience))
                    pokemonData.append(writeNumber(pokemon.level))
                    pokemonData.append(writeNumber(pokemon.move1))
                    pokemonData.append(writeNumber(pokemon.move2))
                    pokemonData.append(writeNumber(pokemon.move3))
                    pokemonData.append(writeNumber(pokemon.move4))
                    pokemonData.append(writeNumber(pokemon.targetType))
                    pokemonData.append(writeNumber(pokemon.gender))
                    pokemonData.append(writeDoubleNumber(pokemon.swfId))
                    pokemonData.append(writeNumber(pokemon.position))
                    pokemonData.append(writeNumber(pokemon.extra))
                    pokemonData.append(writeNumber(pokemon.item))
                    pokemonData.append(writeString(pokemon.tag))
                    pokemonData.append(writeNumber(0)) // Unused
                    pokemonData.append(writeNumber(pokemon.moveSelected))
                    pokemonData.append(writeNumber(pokemon.ability))
                    pokemonData.append(writeNumber(0)) // Unused
                    pokemonData.append(writeNumber(0)) // Unused

                }

                val extra3 = encodeDataWithLength(pokemonData.toString())

                response.add("extra3" to extra3) // Pokemons data

                val items = save.items()
                val extra4 = StringBuilder(writeNumber(items.count()))

                for (item in items) {
                    extra4.append(writeNumber(item.number))
                    extra4.append(writeNumber(item.value))
                }

                response.add("extra4" to encodeDataWithLength(extra4.toString()))

                response.add("CS" to getCS())

                val extra5 = Obfuscation.convertIntToString(
                    Obfuscation.createCheckSum("$extra3${getCS()}"))


                response.add("extra5" to extra5)
                return@transaction response
            }
        }

        fun saveStory(user: User, whichProfile: Byte, parameters: Parameters) : List<Pair<String, String>> {
            val saveDataString = parameters["extra"]

            if (saveDataString === null) {
                return ReasonsEnum.FAILURE_NOT_ALL_PARAMETERS_SUPPLIED.fullReason()
            }

            val saveData = saveDataString.parseUrlEncodedParameters(Charsets.UTF_8, 0)

            return transaction {
                val response = ArrayList<Pair<String, String>>(1)
                var save = getSaves(user).firstOrNull { it.number == whichProfile }

                if (saveData["NewGameSave"] !== null) {
                    if (save !== null) {
                        // Rather than updating existing entry
                        //  deleting works better due to cascading on delete
                        save.delete()
                    }

                    save = PTD3Save.new {
                        this.user = user.id
                        this.number = whichProfile
                        this.nickname = saveData["Nickname"]!!
                        this.version = saveData["Color"]!!.toByte()
                        this.gender = saveData["Gender"]!!.toByte()
                        // Can be set as default values for profile
                        this.money = 10
                    }
                }

                if (save === null) {
                    return@transaction ReasonsEnum.FAILURE_NOT_FOUND.fullReason()
                }

                // I think this can be done better, not the point though
                if (saveData["LevelSave"] !== null) {
                    save.levelsAccomplished = saveData["LevelA"]!!.toByte()
                    save.levelsCompleted = saveData["LevelC"]!!.toByte()
                }

                // Money Save
                if (saveData["MSave"] !== null) {
                    save.money = Obfuscation.convertStringToInt(saveData["MA"]!!)
                }

                val pokes = decodePokeInfo(parameters["extra3"]!!, save)

                for (poke in pokes) {
                    var pokemon = save.pokemons.firstOrNull { it.swfId == poke.saveId }

                    val pokeNicknameNum = poke.needNickname
                    var nickname: String? = null
                    if (pokeNicknameNum !== null) {
                        nickname = saveData["PokeNick$pokeNicknameNum"]
                    }

                    for (reason in poke.reasons) {
                        when (reason) {
                            1 -> { // Captured
                                pokemon = PTD3Pokemon.new {
                                    this.save = save.id
                                    this.swfId = poke.saveId
                                    this.nickname = nickname!!
                                    this.number = poke.number!!.toShort()
                                    this.experience = poke.experience!!
                                    this.level = poke.level!!.toShort()
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
                            2 -> pokemon!!.level = poke.level!!.toShort() // Level up
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

                    if (item === null) item = PTD3Extra.new {
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
                        extraDb = PTD3Extra.new {
                            this.save = save.id
                            this.isItem = false
                            this.number = num
                        }
                    }

                    extraDb.value = value
                }

                response.add(0, "CS" to getCS())
                response.add(0, ResultsEnum.SUCCESS.id)
                return@transaction response
            }
        }

        fun deleteStory(user: User, whichProfile: Byte): List<Pair<String, String>> {
            transaction { getSaves(user).first { it.number == whichProfile }.delete() }

            return listOf(ResultsEnum.SUCCESS.id)
        }

        fun maintenance(): List<Pair<String, String>> {
            return ReasonsEnum.FAILURE_MAINTENANCE.fullReason()
        }

        fun userExists(email: String) : Boolean {
            return getUser(email) !== null
        }

        fun getSaves(id: Long): SizedIterable<PTD3Save> {
            return transaction { PTD3Save.find(PTD3Saves.user eq id) }
        }

        fun getSaves(user: User): SizedIterable<PTD3Save> {
            return getSaves(user.id.value)
        }

        fun getAvailableSaveId(save: PTD3Save): Int {
            return transaction { (save.pokemons.maxByOrNull { pokemon -> pokemon.swfId }?.swfId ?: 0) + 1 }
        }

        fun decodePokeInfo(encoded: String, save: PTD3Save): ArrayList<Pokemon> {
            return Obfuscation.decodePokeInfo(encoded, getAvailableSaveId(save))
        }


        fun getCS(): String {
            return Obfuscation.convertIntToString("12345")
        }

        fun getDataLength(data: String): String {
            val length = data.length
            var lengthOfLength = length.toString().length
            var finalLength = lengthOfLength + length + 1

            while (finalLength.toString().length > lengthOfLength) {
                lengthOfLength++
                finalLength++
            }

            val signatureLength = finalLength.toString().length
            val finalFinalLength = signatureLength.toString() + finalLength.toString()

            return "$finalFinalLength$data"
        }

        fun encodeDataWithLength(data: String): String {
            return Obfuscation.convertIntToString(getDataLength(data))
        }

        fun writeNumber(data: Number): String {
            return "${data.toString().length}$data"
        }

        // I can probably make an iterator for both ways
        fun writeDoubleNumber(data: Number): String {
            return "${writeNumber(data.toString().length)}$data"
        }

        fun writeString(data: String): String {
            return "${data.length}$data"
        }
    }
}