[previous](query-dsl.md) | [parent](index.md)
---

# Co-routines

The RestHighLevelClient exposes asynchronous versions of most APIs that take a call back to process
the response when it comes back. Using this is kind of boiler plate heavy. 

Luckily, Kotlin has co-routines for asynchronous programming and this library provides co-routine 
friendly versions of these functions. They each work pretty much the same way as their synchronous 
version except they are marked as suspend and use a `SuspendingActionListener` that uses Kotlin's
`suspendCancellableCoroutine` to wrap the callback that the rest high level client expects.

If you use an asynchronous server framework such as ktor or Spring Boot (in reactive mode), you'll
want to use these.

To support co-routines, this project is using a 
[code generation plugin](https://github.com/jillesvangurp/es-kotlin-codegen-plugin) 
to generate the co-routine friendly versions of each of the
Rest High Level async functions. At this point most of them are covered. There are more than a hundred 
of these. 

As an example, here are three ways to use the reloadAnalyzers API:

```kotlin
// the synchronous version as provided by the RestHighLevel client
val indicesClient = esClient.indices()
val response = indicesClient.reloadAnalyzers(ReloadAnalyzersRequest("myindex"), RequestOptions.DEFAULT)
// the asynchronous version with a callback as provided by the RestHighLevel client
indicesClient.reloadAnalyzersAsync(ReloadAnalyzersRequest("myindex"), RequestOptions.DEFAULT, object : ActionListener<ReloadAnalyzersResponse> {
    override fun onFailure(e: Exception) {
        println("it failed")
    }

    override fun onResponse(response: ReloadAnalyzersResponse) {
        println("it worked")
    }
})

runBlocking {
    // the coroutine friendly version generated by the code generator plugin
    // this is a suspend version so we put it in a runBlocking to get a coroutine scope
    // use a more appropriate scope in your own application of course.
    val response = indicesClient.reloadAnalyzersAsync(ReloadAnalyzersRequest("myindex"), RequestOptions.DEFAULT)
}
```

This works the same for all the async functions in the Java client. 

## IndexDAO Async

Of course, [`IndexDAO`](https://github.com/jillesvangurp/es-kotlin-wrapper-client/tree/master/src/main/kotlin/io/inbot/eskotlinwrapper/IndexDAO.kt) has async functions as well and using that is 
exactly the same as the synchronous version.

```kotlin
runBlocking {
    // simply use async versions the same way as you would use the regular versions
    thingDao.bulkAsync(refreshPolicy = WriteRequest.RefreshPolicy.IMMEDIATE) {
        1.rangeTo(50).forEach {
            index("$it",Thing("document $it"))
        }
    }

    println("We indexed a lot of things asynchronously : ${thingDao.countAsync()}")
}
```

Output:

```
We indexed a lot of things asynchronously : 50

```

## Development status

Co-routine support is a work in progress in this library and there may be more changes
related to this in future versions. E.g. `Flow` seems like it could be useful when dealing
with scrolling searches.


---

[previous](query-dsl.md) | [parent](index.md)

This Markdown is Generated from Kotlin code. Please don't edit this file and instead edit the [source file](https://github.com/jillesvangurp/es-kotlin-wrapper-client/tree/master/src/test/kotlin/io/inbot/eskotlinwrapper/manual/CoRoutinesManualTest.kt) from which this page is generated.