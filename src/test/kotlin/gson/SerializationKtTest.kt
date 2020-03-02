package gson

import com.google.gson.*
import com.google.gson.annotations.SerializedName
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.lang.reflect.Type
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

data class SerializationTestData(
    @SerializedName("expire_date")
    val expireDate: Date,
    @SerializedName("start_date")
    val startDate: Date
)

const val targetFormat = "yyyy-MM-dd"

val testJsonString = """
            {
              "expire_date": "2020-01-23 21:52:43",
              "start_date": "2020-03-23"
            }
        """.trimIndent()

val designatedFormat = SimpleDateFormat(targetFormat, Locale.US)


object MyDateTypeAdapter : JsonDeserializer<Date>, JsonSerializer<Date> {

    private val DATE_FORMATS = arrayOf("yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd")
    private val dateFormatters: List<SimpleDateFormat> = DATE_FORMATS.map { SimpleDateFormat(it, Locale.US) }
    override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): Date {
        for (formatter in dateFormatters) {
            try {
                return formatter.parse(json?.asString)
            } catch (ignore: ParseException) {
            }
        }

        throw JsonParseException("DateParseException: " + json.toString())
    }

    override fun serialize(src: Date?, typeOfSrc: Type?, context: JsonSerializationContext?): JsonElement {
        synchronized(designatedFormat) {
            val dateFormatAsString: String = designatedFormat.format(src)
            return JsonPrimitive(dateFormatAsString)
        }
    }
}

class SerializationKtTest {

    @DisplayName("Given a value with two data format, When use setDateFormat on GsonBuilder, Then return as expected")
    @Test
    fun testSetDateFormat() {


        val gson = GsonBuilder().apply {
            setDateFormat("yyyy-MM-dd HH:mm:ss")
        }.create()


        val result = gson.fromJson(testJsonString, SerializationTestData::class.java)
        assertNotEquals("2020-01-24", result.expireDate)

    }

    @DisplayName("Given a value with two data format, When use setDateFormat on GsonBuilder and a simpleDateFormat, Then return as expected")
    @Test
    fun testSetDateFormatWithSimpleDateFormat() {


        val gson = GsonBuilder().apply {
            setDateFormat("yyyy-MM-dd HH:mm:ss")
        }.create()


        val result = gson.fromJson(testJsonString, SerializationTestData::class.java)
        assertEquals("2020-03-23", designatedFormat.format(result.startDate))

    }

    @DisplayName("Given a value with two data format, When use registerTypeAdapter on GsonBuilder, Then return as expected")
    @Test
    fun testRegisterTypeAdapter() {
        val gson = GsonBuilder().apply {
            registerTypeAdapter(Date::class.java, MyDateTypeAdapter)
        }.create()

        val result = gson.fromJson(testJsonString, SerializationTestData::class.java)
        assertEquals("Thu Jan 23 21:52:43 EST 2020", result.expireDate.toString())
        assertEquals("\"2020-01-23\"", gson.toJson(result.expireDate))
    }
}
