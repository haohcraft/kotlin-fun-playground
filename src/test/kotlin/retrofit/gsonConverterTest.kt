package retrofit

import com.google.gson.GsonBuilder
import com.google.gson.JsonSyntaxException
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Rule
import org.junit.jupiter.api.Test
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.DisplayName
import org.junit.rules.ExpectedException

data class Value(val name: String, val isTrue: Boolean, val age: Int, val pay: Float, val nickname: String? = "Hi")

interface Service {
    @GET("/")
    fun value(): Call<Value>
}

val server = MockWebServer()

class gsonConverterTest {
    val gson = GsonBuilder()
        .create();
    val service = Retrofit.Builder()
        .baseUrl(server.url("/"))
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()
        .create(Service::class.java)

    @DisplayName("Given a raw response with right corresponding json string, When deserialize/serialize, Then return the right POJO")
    @Test
    fun testCorrectResponse() {
        server.enqueue(MockResponse().setBody("{\"name\":\"value\"}"))
        val call = service.value()
        val response = call.execute()
        val body = response.body()
        assertEquals("value", body?.name)
    }

    @DisplayName("Given a raw response with the wrong type of json string value, When deserialize/serialize, Then throw JsonSyntaxException")
    @Test
    fun testResponseWithWrongType() {
        server.enqueue(MockResponse().setBody("{\"age\":\"true\"}"))
        val call = service.value()
        assertThrows(JsonSyntaxException::class.java) {
            call.execute()
        }

    }

    @DisplayName("Given a raw response with a null value for a String type, When deserialize/serialize, Then return string null")
    @Test
    fun testResponseWithNullField() {
        server.enqueue(MockResponse().setBody("{\"name\":\"null\"}"))
        val call = service.value()
        val response = call.execute()
        val body = response.body()
        assertEquals("null", body?.name)

    }

    @DisplayName("Given a raw response with a missing fields, When deserialize/serialize, Then return the default value based on Gson")
    @Test
    fun testResponseWithMissingField() {
        server.enqueue(MockResponse().setBody("{}"))
        val call = service.value()
        val response = call.execute()
        val body = response.body()
        assertEquals(null, body?.name)
        assertEquals(false, body?.isTrue)
        assertEquals(0, body?.age)
        assertEquals(0.toFloat(), body?.pay)
        assertEquals(null, body?.nickname)

    }
}
