package xyz.jordanplayz158.ptd.server.common.data

import com.google.gson.GsonBuilder
import com.google.gson.JsonSyntaxException
import io.ktor.http.parseUrlEncodedParameters
import io.ktor.serialization.ContentConverter
import io.ktor.serialization.JsonConvertException
import io.ktor.util.reflect.TypeInfo
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.charsets.Charset
import io.ktor.utils.io.jvm.javaio.toInputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import xyz.jordanplayz158.ptd.server.common.InetAddressNullableSerializer
import java.net.InetAddress

// Based off of https://github.com/ktorio/ktor/blob/main/ktor-shared/ktor-serialization/ktor-serialization-gson/jvm/src/GsonConverter.kt
class CustomFormUrlEncodedConverter(gsonBuilder: GsonBuilder = GsonBuilder()) : ContentConverter {
    private val gson = gsonBuilder.registerTypeAdapter(InetAddress::class.java, InetAddressNullableSerializer).create()

    override suspend fun deserialize(charset: Charset, typeInfo: TypeInfo, content: ByteReadChannel): Any? {
        try {

            return withContext(Dispatchers.IO) {
                // Says not to use for urlencoded forms because of "+"
                // but after inspection, seems to work fine
                val parameters = content.toInputStream().reader().readText()
                        .parseUrlEncodedParameters(Charsets.UTF_8, 0)

                val map = HashMap<String, Any?>()

                for ((key, value) in parameters.entries()) {
                    if (value.size == 1) {
                        val string = value[0]

                        if (string.isBlank()) {
                            map[key] = null
                            continue
                        }

                        if (string == "on") {
                            map[key] = true
                            continue
                        }

                        map[key] = string
                        continue
                    }

                    map[key] = value
                }


                val json = gson.toJson(map)

                gson.fromJson(json, typeInfo.reifiedType)
            }
        } catch (e: JsonSyntaxException) {
            throw JsonConvertException("Illegal json parameter found: ${e.message}", e)
        }
    }
}