package com.djs66256.short_drama.core.network

import org.junit.Assert.assertEquals
import org.junit.Test
import retrofit2.http.GET
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
    fun `T-06 api base url is normalized to canonical api prefix`() {
        assertEquals("http://10.0.2.2:3000/api/", ApiClient.normalizeApiBaseUrl("http://10.0.2.2:3000/api/"))
        assertEquals("http://10.0.2.2:3000/api/", ApiClient.normalizeApiBaseUrl("http://10.0.2.2:3000/api/v1"))
        assertEquals("http://10.0.2.2:3000/api/", ApiClient.normalizeApiBaseUrl("http://10.0.2.2:3000"))
    }
}
