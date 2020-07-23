package ru.skillbranch.kotlinexample

import androidx.annotation.VisibleForTesting
import java.lang.IllegalArgumentException
import java.lang.StringBuilder
import java.math.BigInteger
import java.security.MessageDigest
import java.security.SecureRandom

class User private constructor(
    val firstName: String,
    val lastName: String?,
    email: String? = null,
    rawPhone: String? = null,
    meta: Map<String, Any>? = null
) {

    val userInfo: String
    private val fullName: String
        get() = listOfNotNull(firstName, lastName)
            .joinToString(" ")
            .capitalize()

    private val initials: String
        get() = listOfNotNull(firstName, lastName)
            .map { it.first().toUpperCase() }
            .joinToString(" ")

    private var phone: String? = null
        set(value) {
            field = value?.replace("""[^+\d]""".toRegex(), "")
        }


    private var _login: String? = null
    var login: String
        set(value) {
            _login = value.toLowerCase()
        }
        get() = _login!!

    private var salt: String? = null

    private lateinit var passwordHash: String

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    var accessCode: String? = null

    constructor(
        firstName: String,
        lastName: String?,
        rawPhone: String
    ) : this(firstName, lastName, rawPhone = rawPhone, meta = mapOf("auth" to "sms")) {

        val code = generateAccessCode()
        accessCode = code
        passwordHash = encrypt(code)
        sendAccessCodeToUser(rawPhone, code)
    }

    constructor(
        firstName: String,
        lastName: String?,
        email: String?,
        password: String
    ) : this(firstName, lastName, email = email, meta = mapOf("auth" to "password")) {
        passwordHash = encrypt((password))
    }

    constructor(
        firstName: String,
        lastName: String?,
        email: String?,
        phone: String,
        salt: String,
        hash: String
    ) : this(firstName, lastName, email = email, meta = mapOf("src" to "csv")) {
        this.salt = salt
        passwordHash = hash
        accessCode = generateAccessCode()
    }

    init {
        phone = rawPhone
        login = email ?: phone!!

        userInfo = """
              firstName: $firstName
              lastName: $lastName
              login: $login
              fullName: $fullName
              initials: $initials
              email: $email
              phone: $phone
              meta: $meta
            """.trimIndent()
    }

    fun checkPassword(password: String) = encrypt(password) == passwordHash.also {
        println("Checking password")
    }

    fun changePassword(oldPassword: String, newPassword: String) {
        if (checkPassword(oldPassword)) {
            passwordHash = encrypt(newPassword)

            if (!accessCode.isNullOrEmpty()) {
                accessCode = newPassword
            } else {
                throw IllegalArgumentException("Error");
            }
        }
    }

    fun generateAccessCode(): String {
        val possible = "ABCabc0123"

        return StringBuilder().apply {
            repeat(6) {
                (possible.indices).random().also { index ->
                    append(possible[index])
                }
            }
        }.toString()
    }

    private fun encrypt(passwod: String): String {
        if (salt.isNullOrEmpty()) {
            salt = ByteArray(16).also { SecureRandom().nextBytes(it) }.toString()
        }

        return salt.plus(passwod).md5()
    }

    private fun sendAccessCodeToUser(phone: String, code: String) {
        // sended
    }

    private fun String.md5(): String {
        val md5 = MessageDigest.getInstance("MD5")
        val digest = md5.digest(toByteArray())
        val hex = BigInteger(1, digest).toString(16)
        return hex.padStart(32, '0')
    }

    companion object Factory {
        fun makeUser(
            fullName: String,
            email: String? = null,
            password: String? = null,
            phone: String? = null,
            salt: String? = null,
            hash: String? = null
        ): User {
            val (firstName, lastName) = fullName.fullNameToPair()

            return when {
                !salt.isNullOrBlank() && !hash.isNullOrBlank() -> User(firstName, lastName, email, phone!!, salt, hash)
                !phone.isNullOrBlank() -> User(firstName, lastName, phone)
                !email.isNullOrBlank() && !password.isNullOrBlank() -> User(
                    firstName,
                    lastName,
                    email = email,
                    password = password
                )
                else -> throw IllegalArgumentException("Email or phone error")
            }
        }


        private fun String.fullNameToPair(): Pair<String, String?> {
            return this.split(" ")
                .filter { it.isNotBlank() }
                .run {
                    when (size) {
                        1 -> first() to null
                        2 -> first() to last()
                        else -> throw IllegalArgumentException("Fullname error")
                    }
                }
        }
    }

}