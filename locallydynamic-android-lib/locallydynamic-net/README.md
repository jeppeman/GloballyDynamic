# net

Provides a type safe http client and DSL builders for requests.

Usage
---

```groovy
dependencies {
    implementation 'com.jeppeman.locallydynamic:net:1.X.X'
}

```

### Creating a client
```kotlin
import com.jeppeman.locallydynamic.net.*

val httpClient: HttpClient = httpClient { // DSL builder for client
    connectTimeout = 60_000,
    readTimeout = 60_000
}
```

### Building a request
```kotlin
import com.jeppeman.locallydynamic.net.*

val request = request { // DSL builder for request
    url = HttpUrl.parse("https://github.com/jeppeman/LocallyDynamic"),
    method = HttpMethod.POST
    body = "{ body: \"body\" }",
    headers { // DSL builder for headers
        header {
            name = "Authorization"
            value = "Bearer $token"
        }
        header {
            name = "Content-Type"
            value = "application/json; charset=utf8"
        }
    }
}
```

### Making a request
```kotlin
import com.jeppeman.locallydynamic.net.*

// Generic type response
val response: Response<MyResponseType> = httpClient.executeRequest(request, MyResponseType::class)
if (response.isSuccessful) {
    val body: MyResponseType = response.body
    Log.d("LocallyDynamic", "Yaay, body: $body")
}

// String response
val response: Response<String> = httpClient.executeRequest(request)
if (response.isSuccessful) {
    val body: String = response.body
    Log.d("LocallyDynamic", "Yaay, body: $body")
}

// Asynchronous
httpClient.executeRequestAsync(
    request = request,
    responseBodyType = MyResponseType::class,
    onResponse = { response: Response<MyResponseType> ->
        if (response.isSuccessful) {
            Log.d("LocallyDynamic", "Yaay, body: $response.body")
        }
    },
    onFailure = { throwable: Throwable ->
        Log.e("LocallyDynamic", ":(", throwable)
    }
)
```
