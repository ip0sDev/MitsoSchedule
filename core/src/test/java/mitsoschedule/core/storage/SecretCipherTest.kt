package mitsoschedule.core.storage

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import javax.crypto.KeyGenerator

class SecretCipherTest {

    private fun newCipher(): AesGcmSecretCipher {
        val key = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()
        return AesGcmSecretCipher { key }
    }

    @Test
    fun roundTripRestoresOriginal() {
        val cipher = newCipher()
        val token = cipher.encrypt("""{"login":"419445","password":"секрет"}""")!!
        assertTrue(token.startsWith(SecretCipher.PREFIX))
        assertFalse(token.contains("419445"))
        assertEquals("""{"login":"419445","password":"секрет"}""", cipher.decrypt(token))
    }

    @Test
    fun sameInputProducesDifferentTokens() {
        val cipher = newCipher()
        assertNotEquals(cipher.encrypt("pass"), cipher.encrypt("pass"))
    }

    @Test
    fun otherKeyOrTamperedTokenIsRejected() {
        val token = newCipher().encrypt("pass")!!
        assertNull(newCipher().decrypt(token))

        val cipher = newCipher()
        val own = cipher.encrypt("pass")!!
        val tampered = own.dropLast(4) + (if (own.takeLast(4) == "AAAA") "BBBB" else "AAAA")
        assertNull(cipher.decrypt(tampered))
        assertNull(cipher.decrypt("not-a-token"))
    }
}
