package mitsoschedule.core.storage

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Шифрование небольших секретов (пароль кабинета) перед записью в DataStore.
 * Оба метода возвращают null при любой ошибке, а не бросают исключение: секрет в таком случае
 * считается потерянным (пользователь войдёт заново), но никогда не пишется открытым текстом.
 */
interface SecretCipher {
    fun encrypt(plain: String): String?
    fun decrypt(token: String): String?

    companion object {
        /** Префикс зашифрованных значений; всё без него считается устаревшим открытым текстом. */
        const val PREFIX = "enc1:"
    }
}

/** AES-256-GCM; ключ поставляет [keyProvider] (в приложении это ключ из Android Keystore). */
class AesGcmSecretCipher(private val keyProvider: () -> SecretKey) : SecretCipher {

    override fun encrypt(plain: String): String? = try {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, keyProvider())
        val iv = cipher.iv
        val body = cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
        SecretCipher.PREFIX + Base64.getEncoder().encodeToString(iv + body)
    } catch (_: Exception) {
        null
    }

    override fun decrypt(token: String): String? = try {
        require(token.startsWith(SecretCipher.PREFIX))
        val bytes = Base64.getDecoder().decode(token.removePrefix(SecretCipher.PREFIX))
        val iv = bytes.copyOfRange(0, IV_SIZE)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, keyProvider(), GCMParameterSpec(TAG_BITS, iv))
        String(cipher.doFinal(bytes, IV_SIZE, bytes.size - IV_SIZE), Charsets.UTF_8)
    } catch (_: Exception) {
        null
    }

    private companion object {
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val IV_SIZE = 12
        const val TAG_BITS = 128
    }
}

/** Шифр на ключе из Android Keystore: ключ не покидает устройство и не попадает в бэкап. */
fun keystoreSecretCipher(alias: String): SecretCipher = AesGcmSecretCipher { keystoreKey(alias) }

private const val ANDROID_KEYSTORE = "AndroidKeyStore"

private fun keystoreKey(alias: String): SecretKey {
    val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
    (keyStore.getKey(alias, null) as? SecretKey)?.let { return it }

    val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
    generator.init(
        KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()
    )
    return generator.generateKey()
}

