package com.player.streameraudio.data.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProxyEndpointValidatorTest {
    @Test
    fun `normalizes https host path and trailing slash`() {
        val result = ProxyEndpointValidator.validate("Home", " HTTPS://Example.COM/proxy/ ")

        assertEquals(
            ProxyValidationResult.Valid("https://example.com/proxy"),
            result
        )
    }

    @Test
    fun `accepts a non-default port`() {
        val result = ProxyEndpointValidator.validate("Local", "https://proxy.example:8443")

        assertEquals(ProxyValidationResult.Valid("https://proxy.example:8443"), result)
    }

    @Test
    fun `rejects blank names`() {
        assertInvalid(ProxyEndpointValidator.validate("  ", "https://proxy.example"))
    }

    @Test
    fun `rejects cleartext urls`() {
        assertInvalid(ProxyEndpointValidator.validate("Unsafe", "http://proxy.example"))
    }

    @Test
    fun `rejects urls without a host`() {
        assertInvalid(ProxyEndpointValidator.validate("Invalid", "https:///proxy"))
    }

    @Test
    fun `rejects embedded credentials`() {
        assertInvalid(ProxyEndpointValidator.validate("Invalid", "https://user:pass@proxy.example"))
    }

    @Test
    fun `rejects query parameters`() {
        assertInvalid(ProxyEndpointValidator.validate("Invalid", "https://proxy.example?token=secret"))
    }

    @Test
    fun `rejects fragments`() {
        assertInvalid(ProxyEndpointValidator.validate("Invalid", "https://proxy.example/#fragment"))
    }

    private fun assertInvalid(result: ProxyValidationResult) {
        assertTrue(result is ProxyValidationResult.Invalid)
    }
}
