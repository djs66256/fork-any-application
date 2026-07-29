package com.djs66256.short_drama.core.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.http.Body
import retrofit2.http.DELETE
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
    fun `T-12 getDramaChannel uses canonical path and query names`() {
        val method = ApiService::class.java.declaredMethods.single { it.name == "getDramaChannel" }
        val getAnnotation = requireNotNull(method.getAnnotation(GET::class.java))

        assertEquals("dramas/channel", getAnnotation.value)
        assertEquals("channel", method.parameterAnnotations[0].filterIsInstance<Query>().single().value)
        assertEquals("page", method.parameterAnnotations[1].filterIsInstance<Query>().single().value)
        assertEquals("pageSize", method.parameterAnnotations[2].filterIsInstance<Query>().single().value)
    }

    @Test
    fun `T-06 searchDramas uses canonical path and query names`() {
        val method = ApiService::class.java.declaredMethods.single { it.name == "searchDramas" }
        val getAnnotation = requireNotNull(method.getAnnotation(GET::class.java))

        assertEquals("dramas/search", getAnnotation.value)
        assertEquals("q", method.parameterAnnotations[0].filterIsInstance<Query>().single().value)
        assertEquals("page", method.parameterAnnotations[1].filterIsInstance<Query>().single().value)
        assertEquals("pageSize", method.parameterAnnotations[2].filterIsInstance<Query>().single().value)
    }

    @Test
    fun `T-06 getHotSearches uses canonical path`() {
        val method = ApiService::class.java.declaredMethods.single { it.name == "getHotSearches" }
        val getAnnotation = requireNotNull(method.getAnnotation(GET::class.java))

        assertEquals("dramas/hot-search", getAnnotation.value)
    }

    @Test
    fun `T-09 getDramaTags uses canonical path and gender query`() {
        val method = ApiService::class.java.declaredMethods.single { it.name == "getDramaTags" }
        val getAnnotation = requireNotNull(method.getAnnotation(GET::class.java))

        assertEquals("dramas/tags", getAnnotation.value)
        assertEquals("gender", method.parameterAnnotations[0].filterIsInstance<Query>().single().value)
    }

    @Test
    fun `T-10 getDramaRankings uses canonical path and query names`() {
        val method = ApiService::class.java.declaredMethods.single { it.name == "getDramaRankings" }
        val getAnnotation = requireNotNull(method.getAnnotation(GET::class.java))

        assertEquals("dramas/rankings", getAnnotation.value)
        assertEquals("type", method.parameterAnnotations[0].filterIsInstance<Query>().single().value)
        assertEquals("contentType", method.parameterAnnotations[1].filterIsInstance<Query>().single().value)
        assertEquals("page", method.parameterAnnotations[2].filterIsInstance<Query>().single().value)
        assertEquals("pageSize", method.parameterAnnotations[3].filterIsInstance<Query>().single().value)
    }

    @Test
    fun `T-10 bookDrama uses canonical path parameter`() {
        val method = ApiService::class.java.declaredMethods.single { it.name == "bookDrama" }
        val postAnnotation = requireNotNull(method.getAnnotation(POST::class.java))

        assertEquals("dramas/{id}/book", postAnnotation.value)
        assertEquals("id", method.parameterAnnotations[0].filterIsInstance<Path>().single().value)
    }

    @Test
    fun `T-09 getDramaComments uses canonical path and query names`() {
        val method = ApiService::class.java.declaredMethods.single { it.name == "getDramaComments" }
        val getAnnotation = requireNotNull(method.getAnnotation(GET::class.java))

        assertEquals("dramas/{id}/comments", getAnnotation.value)
        assertEquals("id", method.parameterAnnotations[0].filterIsInstance<Path>().single().value)
        assertEquals("page", method.parameterAnnotations[1].filterIsInstance<Query>().single().value)
        assertEquals("pageSize", method.parameterAnnotations[2].filterIsInstance<Query>().single().value)
        assertEquals("sort", method.parameterAnnotations[3].filterIsInstance<Query>().single().value)
    }

    @Test
    fun `T-09 createDramaComment uses canonical path and request body`() {
        val method = ApiService::class.java.declaredMethods.single { it.name == "createDramaComment" }
        val postAnnotation = requireNotNull(method.getAnnotation(POST::class.java))

        assertEquals("dramas/{id}/comments", postAnnotation.value)
        assertEquals("id", method.parameterAnnotations[0].filterIsInstance<Path>().single().value)
        assertEquals(1, method.parameterAnnotations[1].filterIsInstance<Body>().size)
    }

    @Test
    fun `T-09 toggleDramaCommentLike uses canonical nested path params`() {
        val method = ApiService::class.java.declaredMethods.single { it.name == "toggleDramaCommentLike" }
        val postAnnotation = requireNotNull(method.getAnnotation(POST::class.java))

        assertEquals("dramas/{id}/comments/{commentId}/like", postAnnotation.value)
        assertEquals("id", method.parameterAnnotations[0].filterIsInstance<Path>().single().value)
        assertEquals("commentId", method.parameterAnnotations[1].filterIsInstance<Path>().single().value)
    }

    @Test
    fun `T-03 getRecentlyViewed uses canonical path and playback session header`() {
        val method = ApiService::class.java.declaredMethods.single { it.name == "getRecentlyViewed" }
        val getAnnotation = requireNotNull(method.getAnnotation(GET::class.java))
        val headerAnnotation = method.parameterAnnotations[0].filterIsInstance<Header>().single()

        assertNotNull(getAnnotation)
        assertEquals("player/recently-viewed", getAnnotation.value)
        assertEquals("X-Playback-Session-Id", headerAnnotation.value)
    }

    @Test
    fun `T-01 check in api routes use canonical contracts`() {
        val statusMethod = ApiService::class.java.declaredMethods.single { it.name == "getCheckInStatus" }
        val statusAnnotation = requireNotNull(statusMethod.getAnnotation(GET::class.java))
        assertEquals("check-ins/status", statusAnnotation.value)
        assertEquals(
            "X-Installation-Id",
            statusMethod.parameterAnnotations[0].filterIsInstance<Header>().single().value,
        )

        val submitMethod = ApiService::class.java.declaredMethods.single { it.name == "submitCheckIn" }
        val submitAnnotation = requireNotNull(submitMethod.getAnnotation(POST::class.java))
        assertEquals("check-ins", submitAnnotation.value)
        assertEquals(
            "X-Installation-Id",
            submitMethod.parameterAnnotations[0].filterIsInstance<Header>().single().value,
        )
    }

    @Test
    fun `T-01 message api routes use canonical contracts`() {
        val previewMethod = ApiService::class.java.declaredMethods.single { it.name == "getMessagePreview" }
        val previewAnnotation = requireNotNull(previewMethod.getAnnotation(GET::class.java))
        assertEquals("messages/preview", previewAnnotation.value)
        assertEquals(Any::class.java, previewMethod.returnType)
        assertTrue(
            previewMethod.genericParameterTypes.last().typeName.contains(
                "Response<com.djs66256.short_drama.data.dto.MessagePreviewDto>",
            ),
        )

        val systemMethod = ApiService::class.java.declaredMethods.single { it.name == "getSystemMessages" }
        val systemAnnotation = requireNotNull(systemMethod.getAnnotation(GET::class.java))
        assertEquals("messages/system", systemAnnotation.value)
        assertEquals("page", systemMethod.parameterAnnotations[0].filterIsInstance<Query>().single().value)
        assertEquals("pageSize", systemMethod.parameterAnnotations[1].filterIsInstance<Query>().single().value)

        val interactionMethod = ApiService::class.java.declaredMethods.single { it.name == "getInteractionMessages" }
        val interactionAnnotation = requireNotNull(interactionMethod.getAnnotation(GET::class.java))
        assertEquals("messages/interactions", interactionAnnotation.value)
        assertEquals("page", interactionMethod.parameterAnnotations[0].filterIsInstance<Query>().single().value)
        assertEquals("pageSize", interactionMethod.parameterAnnotations[1].filterIsInstance<Query>().single().value)
    }

    @Test
    fun `T-11 auth api routes use canonical contracts`() {
        val sendOtpMethod = ApiService::class.java.declaredMethods.single { it.name == "sendOtpRequest" }
        val sendOtpAnnotation = sendOtpMethod.getAnnotation(POST::class.java)
        assertEquals("auth/otp-requests", sendOtpAnnotation.value)
        assertEquals(1, sendOtpMethod.parameterAnnotations[0].filterIsInstance<Body>().size)

        val createSessionMethod = ApiService::class.java.declaredMethods.single { it.name == "createAuthSession" }
        val createSessionAnnotation = createSessionMethod.getAnnotation(POST::class.java)
        assertEquals("auth/sessions", createSessionAnnotation.value)
        assertEquals(1, createSessionMethod.parameterAnnotations[0].filterIsInstance<Body>().size)

        val refreshMethod = ApiService::class.java.declaredMethods.single { it.name == "refreshAuthSession" }
        val refreshAnnotation = refreshMethod.getAnnotation(POST::class.java)
        assertEquals("auth/session-refreshes", refreshAnnotation.value)
        assertEquals(1, refreshMethod.parameterAnnotations[0].filterIsInstance<Body>().size)

        val meMethod = ApiService::class.java.declaredMethods.single { it.name == "getCurrentUser" }
        val meAnnotation = meMethod.getAnnotation(GET::class.java)
        assertEquals("users/me", meAnnotation.value)

        val logoutMethod = ApiService::class.java.declaredMethods.single { it.name == "logout" }
        val logoutAnnotation = logoutMethod.getAnnotation(DELETE::class.java)
        assertEquals("auth/session", logoutAnnotation.value)
    }

    @Test
    fun `T-06 api base url is normalized to canonical api prefix`() {
        assertEquals("http://10.0.2.2:3000/api/", ApiClient.normalizeApiBaseUrl("http://10.0.2.2:3000/api/"))
        assertEquals("http://10.0.2.2:3000/api/", ApiClient.normalizeApiBaseUrl("http://10.0.2.2:3000/api/v1"))
        assertEquals("http://10.0.2.2:3000/api/", ApiClient.normalizeApiBaseUrl("http://10.0.2.2:3000"))
    }
}
