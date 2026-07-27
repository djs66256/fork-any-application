package com.djs66256.short_drama.core.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

class ApiServiceTest {

    @Test
    fun `T-06 getDramas uses canonical page and pageSize query names`() {
        val method = ApiService::class.java.declaredMethods.single { it.name == "getDramas" }

        val pageQuery = method.parameterAnnotations[0]
            .filterIsInstance<Query>()
            .single()
        val pageSizeQuery = method.parameterAnnotations[1]
            .filterIsInstance<Query>()
            .single()

        assertEquals("page", pageQuery.value)
        assertEquals("pageSize", pageSizeQuery.value)
    }

    @Test
    fun `T-06 searchDramas uses canonical path and query names`() {
        val method = ApiService::class.java.declaredMethods.single { it.name == "searchDramas" }
        val getAnnotation = method.getAnnotation(GET::class.java)

        assertEquals("dramas/search", getAnnotation.value)
        assertEquals("q", method.parameterAnnotations[0].filterIsInstance<Query>().single().value)
        assertEquals("page", method.parameterAnnotations[1].filterIsInstance<Query>().single().value)
        assertEquals("pageSize", method.parameterAnnotations[2].filterIsInstance<Query>().single().value)
    }

    @Test
    fun `T-06 getHotSearches uses canonical path`() {
        val method = ApiService::class.java.declaredMethods.single { it.name == "getHotSearches" }
        val getAnnotation = method.getAnnotation(GET::class.java)

        assertEquals("dramas/hot-search", getAnnotation.value)
    }

    @Test
    fun `T-09 getDramaTags uses canonical path and gender query`() {
        val method = ApiService::class.java.declaredMethods.single { it.name == "getDramaTags" }
        val getAnnotation = method.getAnnotation(GET::class.java)

        assertEquals("dramas/tags", getAnnotation.value)
        assertEquals("gender", method.parameterAnnotations[0].filterIsInstance<Query>().single().value)
    }

    @Test
    fun `T-10 getDramaRankings uses canonical path and query names`() {
        val method = ApiService::class.java.declaredMethods.single { it.name == "getDramaRankings" }
        val getAnnotation = method.getAnnotation(GET::class.java)

        assertEquals("dramas/rankings", getAnnotation.value)
        assertEquals("type", method.parameterAnnotations[0].filterIsInstance<Query>().single().value)
        assertEquals("contentType", method.parameterAnnotations[1].filterIsInstance<Query>().single().value)
        assertEquals("page", method.parameterAnnotations[2].filterIsInstance<Query>().single().value)
        assertEquals("pageSize", method.parameterAnnotations[3].filterIsInstance<Query>().single().value)
    }

    @Test
    fun `T-10 bookDrama uses canonical path parameter`() {
        val method = ApiService::class.java.declaredMethods.single { it.name == "bookDrama" }
        val postAnnotation = method.getAnnotation(POST::class.java)

        assertEquals("dramas/{id}/book", postAnnotation.value)
        assertEquals("id", method.parameterAnnotations[0].filterIsInstance<Path>().single().value)
    }

    @Test
    fun `T-03 getRecentlyViewed uses canonical path and playback session header`() {
        val method = ApiService::class.java.declaredMethods.single { it.name == "getRecentlyViewed" }
        val getAnnotation = method.getAnnotation(GET::class.java)
        val headerAnnotation = method.parameterAnnotations[0].filterIsInstance<Header>().single()

        assertNotNull(getAnnotation)
        assertEquals("player/recently-viewed", getAnnotation!!.value)
        assertEquals("X-Playback-Session-Id", headerAnnotation.value)
    }

    @Test
    fun `T-06 api base url is normalized to canonical api prefix`() {
        assertEquals("http://10.0.2.2:3000/api/", ApiClient.normalizeApiBaseUrl("http://10.0.2.2:3000/api/"))
        assertEquals("http://10.0.2.2:3000/api/", ApiClient.normalizeApiBaseUrl("http://10.0.2.2:3000/api/v1"))
        assertEquals("http://10.0.2.2:3000/api/", ApiClient.normalizeApiBaseUrl("http://10.0.2.2:3000"))
    }
}
