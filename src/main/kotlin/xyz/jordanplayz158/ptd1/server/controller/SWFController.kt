package xyz.jordanplayz158.ptd1.server.controller

import at.favre.lib.crypto.bcrypt.BCrypt
import io.ktor.http.Parameters
import io.ktor.http.parseUrlEncodedParameters
import org.jetbrains.exposed.dao.with
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.Logger
import xyz.jordanplayz158.ptd1.server.Keygen
import xyz.jordanplayz158.ptd.ReasonsEnum
import xyz.jordanplayz158.ptd.ResultsEnum
import xyz.jordanplayz158.ptd.SaveVersionEnum
import xyz.jordanplayz158.ptd1.server.Users
import xyz.jordanplayz158.ptd1.server.orm.Achievement
import xyz.jordanplayz158.ptd1.server.orm.Pokemon
import xyz.jordanplayz158.ptd1.server.orm.Save
import xyz.jordanplayz158.ptd1.server.orm.SaveItem
import xyz.jordanplayz158.ptd1.server.orm.User
import java.time.Instant
import kotlin.collections.ArrayList

class SWFController {
    companion object {
        fun createAccount(email: String, password: String) : Pair<Boolean, List<Pair<String, String>>> {
            if(userExists(email)) {
                return false to listOf("Result" to ResultsEnum.FAILURE.id, "Reason" to ReasonsEnum.FAILURE_TAKEN.id)
            }

            transaction {
                val user = User.new {
                    this.email = email
                    this.password = BCrypt.withDefaults().hashToString(12, password.toCharArray())
                }

                Achievement.new {
                    this.user = user.id
                }

                for(i in 0..<3) {
                    Save.new {
                        this.user = user.id
                        number = i.toShort()
                        version = SaveVersionEnum.RED.id.toShort()
                    }
                }
            }

            return loadAccount(email, password)
        }

        fun loadAccount(email: String, password: String) : Pair<Boolean, List<Pair<String, String>>> {
            if(!validCredentials(email, password)) {
                return false to listOf("Result" to ResultsEnum.FAILURE.id, "Reason" to ReasonsEnum.FAILURE_NOT_FOUND.id)
            }

            val response = ArrayList<Pair<String, String>>()
            transaction {
                val user = User.find(Users.email eq email).with(User::saves, Save::pokemon, Save::items).first()

                response.add("Result" to ResultsEnum.SUCCESS.id)
                response.add("Reason" to ReasonsEnum.SUCCESS_LOGGED_IN.id)

                val currentSave = (10000000000000..99999999999999).random().toString()
                val trainerId = (333..99999).random()
                response.add("CurrentSave" to currentSave)
                response.add("TrainerID" to trainerId.toString())
                response.add("ProfileID" to Keygen.generateProfileId(currentSave, trainerId))
                //response.add("accNickname" to )

                user.saves.forEach { save ->
                    val number = save.number + 1
                    response.add("Advanced$number" to save.levelsStarted.toString())
                    response.add("Advanced${number}_a" to save.levelsCompleted.toString())
                    response.add("Nickname$number" to save.nickname)
                    response.add("Badges$number" to save.badges.toString())
                    response.add("avatar$number" to save.avatar)
                    response.add("Classic$number" to save.hasFlash.toString())
                    response.add("Classic${number}_a" to "")
                    response.add("Challenge$number" to save.challenge.toString())
                    response.add("Money$number" to save.money.toString())
                    response.add("NPCTrade$number" to save.npcTrade.toString())
                    response.add("Version$number" to save.version.toString())

                    var i = 1
                    save.items.forEach {item ->
                        for (count in 0..item.count) {
                            response.add("p${number}_item_${i}_num" to item.item.toString())
                            i++
                        }
                    }

                    response.add("p${number}_numItem" to (i - 1).toString())

                    var shinyPokemonCount = 0
                    save.pokemon.forEachIndexed {pokeI, pokemon ->
                        if(pokemon.rarity.toInt() == 1) shinyPokemonCount++

                        val prefix = "p${number}_poke_${pokeI + 1}_"
                        response.add(prefix + "nickname" to pokemon.nickname)
                        response.add(prefix + "num" to pokemon.number.toString())
                        response.add(prefix + "lvl" to pokemon.level.toString())
                        response.add(prefix + "exp" to pokemon.experience.toString())
                        //response.add(prefix + "owner" to "0")
                        response.add(prefix + "targetType" to pokemon.targetType.toString())
                        response.add(prefix + "tag" to pokemon.tag)
                        response.add(prefix + "myID" to pokemon.swfId.toString())
                        response.add(prefix + "pos" to pokemon.position.toString())
                        response.add(prefix + "noWay" to pokemon.rarity.toString())
                        response.add(prefix + "m1" to pokemon.move1.toString())
                        response.add(prefix + "m2" to pokemon.move2.toString())
                        response.add(prefix + "m3" to pokemon.move3.toString())
                        response.add(prefix + "m4" to pokemon.move4.toString())
                        response.add(prefix + "mSel" to pokemon.moveSelected.toString())
                    }

                    response.add("p${number}_numPoke" to save.pokemon.count().toString())
                    response.add("p${number}_hs" to shinyPokemonCount.toString())
                }

                response.add("dex1" to user.dex.padEnd(151, '0'))
                response.add("dex1Shiny" to user.shinyDex.padEnd(151, '0'))
                response.add("dex1Shadow" to user.shadowDex.padEnd(151, '0'))

            }

            return true to response
        }

        fun saveAccount(log: Logger, email: String, password: String, parameters: Parameters) : Pair<Boolean, List<Pair<String, String>>> {
            if(!validCredentials(email, password)) {
                return false to listOf("Result" to ResultsEnum.FAILURE.id, "Reason" to ReasonsEnum.FAILURE_NOT_FOUND.id)
            }

            val saveString = parameters["saveString"]

            if(saveString === null) {
                return false to listOf("Result" to ResultsEnum.FAILURE.id, "Reason" to "saveStringEmpty")
            }

            // Says not to use for urlencoded forms because of "+"
            // but after inspection, seems to work fine
            val save = saveString.parseUrlEncodedParameters(Charsets.UTF_8, 0)

            val response = ArrayList<Pair<String, String>>()
            response.add("newSave" to (10000000000000..99999999999999).random().toString())

            try {
                transaction {
                    val user = User.find(Users.email eq email).with(User::saves, Save::pokemon, Save::items).first()

                    user.dex = save["dex1"]!!.trimEnd('0')
                    user.shinyDex = save["dex1Shiny"]!!.trimEnd('0')
                    user.shadowDex = save["dex1Shadow"]!!.trimEnd('0')
                    user.updatedAt = Instant.now()

                    val saveNumber = save["whichProfile"]!!.toInt() - 1
                    var userSave = user.saves.filter { save -> save.number.toInt() == saveNumber }[0]
                    val releasePokes: List<Int>? = save["releasePoke"]?.split('|')?.distinct()?.map { id -> id.toInt() }?.filter { id -> id != 0 }

                    if(save["newGame"] == "yes") {
                        userSave.delete()
                        userSave = Save.new {
                            this.user = user.id
                            this.number = saveNumber.toShort()
                            this.version = save["Version"]!!.toShort()
                        }
                    } else {
                        releasePokes?.forEach {id ->
                            userSave.pokemon.find { poke -> poke.swfId == id }?.delete()
                        }

                        userSave.updatedAt = Instant.now()
                    }

                    val saveId = userSave.id

                    userSave.levelsCompleted = save["a_story"]!!.toShort()
                    userSave.levelsStarted = save["a_story_a"]!!.toShort()
                    userSave.hasFlash = save["c_story"]!!.toBoolean()
                    userSave.badges = save["badges"]!!.toShort()
                    userSave.challenge = save["challenge"]!!.toShort()
                    userSave.npcTrade = save["NPCTrade"]!!.toBoolean()
                    userSave.money = save["Money"]!!.toInt()
                    userSave.nickname = save["Nickname"]!!
                    userSave.avatar = save["Avatar"]!!

                    for(i in 1..save["HMP"]!!.toInt()) {
                        val pokePrefix = "poke${i}_"
                        val swfId = save[pokePrefix + "myID"]?.toInt()

                        var pokemon = userSave.pokemon.find { pokemon -> pokemon.swfId == swfId }

                        if(pokemon === null) {
                            pokemon = Pokemon.new {
                                this.save = saveId
                                this.swfId = (userSave.pokemon.maxByOrNull { pokemon -> pokemon.swfId }?.swfId ?: 0) + 1
                            }
                        } else pokemon.updatedAt = Instant.now()

                        if(releasePokes?.contains(swfId) == true) {
                            continue
                        }

                        // This value can sometimes be set and lets
                        // the server know what the rarity of the
                        // pokemon is in a less direct way
                        // (likely was curveballs for hackers)
                        val extraRarityString = save[pokePrefix + "extra"]
                        if(extraRarityString != null) {
                            val extraRarity = extraRarityString.toInt()

                            pokemon.rarity = when(extraRarity) {
                                /* Shiny
                                 * Geodude & Graveler = 1
                                 * Magnemite & Magneton = 2
                                 * Tentacool & Tentacruel = 3
                                 * Onix = 4
                                 * Staryu & Starmie = 5
                                 * Voltorb & Electrode = 6
                                 * Hitmonlee & Hitmonchan = 153
                                 * Omanyte & Omastar & Kabuto & Kabutops = 168
                                 * Missing No. = 182
                                 * Articuno & Zapdos & Moltres = 854
                                 * Generic = 151
                                 */
                                1, 2, 3, 4, 5, 6, 151, 153, 168, 182, 854 -> 1
                                /* Shadow
                                 * Lickitung = 180
                                 * Articuno & Zapdos & Moltres = 855
                                 * Generic = 555
                                 */
                                180, 555, 855 -> 2
                                /* Normal
                                 * Hitmonlee & Hitmonchan = 152
                                 * Missing No. = 181
                                 * Mew = 201
                                 * Omanyte & Omastar & Kabuto & Kabutops = 154
                                 * Articuno & Zapdos & Moltres = 857
                                 * Generic = 0
                                 *
                                 * Assuming anything else is normal at the moment
                                 */
                                else -> 0
                            }
                        }

                        val reasons = save[pokePrefix + "reason"]!!.split("|")
                        reasons.forEach { reason ->
                            if(reason == "cap") {
                                val rarity = save[pokePrefix + "shiny"]
                                if(rarity != null) pokemon.rarity = rarity.toShort()
                            }

                            if(reason == "cap" || reason == "trade") {
                                pokemon.number = save[pokePrefix + "num"]!!.toShort()
                                pokemon.nickname = save[pokePrefix + "nickname"]!!
                                pokemon.experience = save[pokePrefix + "exp"]!!.toInt()
                                pokemon.level = save[pokePrefix + "lvl"]!!.toShort()
                                pokemon.move1 = save[pokePrefix + "m1"]!!.toShort()
                                pokemon.move2 = save[pokePrefix + "m2"]!!.toShort()
                                pokemon.move3 = save[pokePrefix + "m3"]!!.toShort()
                                pokemon.move4 = save[pokePrefix + "m4"]!!.toShort()
                                pokemon.moveSelected = save[pokePrefix + "mSel"]!!.toShort()
                                pokemon.targetType = save[pokePrefix + "targetType"]!!.toShort()
                                pokemon.ability = save[pokePrefix + "ability"]!!.toShort()
                                pokemon.tag = save[pokePrefix + "tag"]!!
                                pokemon.position = save[pokePrefix + "pos"]!!.toInt()
                                return@forEach
                            }

                            when(reason) {
                                "evolve" -> {
                                    pokemon.number = save[pokePrefix + "num"]!!.toShort()
                                    pokemon.nickname = save[pokePrefix + "nickname"]!!
                                }
                                "exp" -> pokemon.experience = save[pokePrefix + "exp"]!!.toInt()
                                "pos" -> pokemon.position = save[pokePrefix + "pos"]!!.toInt()
                                "lvl" -> pokemon.level = save[pokePrefix + "lvl"]!!.toShort()
                                "moves" -> {
                                    pokemon.move1 = save[pokePrefix + "m1"]!!.toShort()
                                    pokemon.move2 = save[pokePrefix + "m2"]!!.toShort()
                                    pokemon.move3 = save[pokePrefix + "m3"]!!.toShort()
                                    pokemon.move4 = save[pokePrefix + "m4"]!!.toShort()
                                }
                                "tag" -> pokemon.tag = save[pokePrefix + "tag"]!!
                                "target" -> pokemon.targetType = save[pokePrefix + "targetType"]!!.toShort()
                                "mSel" -> pokemon.moveSelected = save[pokePrefix + "mSel"]!!.toShort()
                            }
                        }

                        // If newly created pokemon
                        // (updated_at only set for pokemon found in db)
                        if(pokemon.updatedAt == null) {
                            response.add("newPokePos_${pokemon.position}" to pokemon.swfId.toString())
                        }
                    }

                    val items = HashMap<Short, Short>()
                    for(i in 1..save["HMI"]!!.toInt()) {
                        val item = save["item${i}_num"]!!.toShort()
                        items[item] = (items.getOrDefault(item, 0) + 1).toShort()
                    }

                    items.forEach { (item, count) ->
                        val userItem = userSave.items.find { userItem -> userItem.item == item }

                        if(userItem == null) {
                            SaveItem.new {
                                this.save = saveId
                                this.item = item
                                this.count = count
                            }
                            return@forEach
                        }

                        userItem.count = count
                        userItem.updatedAt = Instant.now()
                    }
                }

                response.add("Result" to ResultsEnum.SUCCESS.id)
                return true to response
            } catch (e: Exception) {
                response.add("Result" to ResultsEnum.FAILURE.id)
                log.info("Unknown error occurred while saving $save", e)
                return false to response
            }
        }

        /**
         * @return true if user exists AND user's password matches
         */
        fun validCredentials(email: String, password: String) : Boolean {
            var user: User? = null
            transaction { user = User.find(Users.email eq email).firstOrNull() }

            if(user === null) return false

            return BCrypt.verifyer().verify(password.toCharArray(), user!!.password).verified
        }

        fun userExists(email: String) : Boolean {
            var user: User? = null
            transaction { user = User.find(Users.email eq email).firstOrNull() }
            return user !== null
        }
    }
}