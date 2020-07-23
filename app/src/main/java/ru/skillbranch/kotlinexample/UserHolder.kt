package ru.skillbranch.kotlinexample

import androidx.annotation.VisibleForTesting
import java.lang.IllegalArgumentException

object UserHolder {

    private val map = mutableMapOf<String, User>()

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    fun clearHolder() {
        map.clear()
    }

    fun registerUser(fullName: String, email: String, password: String): User {
        val user = User.makeUser(fullName, email, password)

        if (map[user.login] != null) {
            throw IllegalArgumentException("A user with this email already exists")
        }

        map[user.login] = user
        return user
    }

    fun registerUserByPhone(fullName: String, rawPhone: String): User {
        val user = User.makeUser(fullName, phone = rawPhone)

        if (user.login.first() != '+' || user.login.length != 12) {
            throw IllegalArgumentException("Error phone")
        }

        if (map[rawPhone] != null) {
            throw IllegalArgumentException("A user with this phone already exists")
        }

        map[rawPhone] = user
        return user
    }

    fun loginUser(login: String, password: String): String? {
        return map[login]?.let {
            if (it.checkPassword(password)) {
                it.userInfo
            } else {
                null
            }
        }
    }

    fun requestAccessCode(login: String): Unit {
        val user = map[login]
        val generateAccessCode = user?.generateAccessCode()
        user?.changePassword(user.accessCode!!, generateAccessCode!!)
    }

    fun importUsers(csvRows: List<String>): List<User> {
        val users = mutableListOf<User>();

        for (row in csvRows) {
            val userAttrs = row.split(";")

            val fullName = if (userAttrs[0].isEmpty()) null else userAttrs[0]
            val email = if (userAttrs[0].isEmpty()) null else userAttrs[1]
            val saltAndHash = if (userAttrs[0].isEmpty()) null else userAttrs[2]
            val phone = if (userAttrs[0].isEmpty()) null else userAttrs[3]

            var salt: String? = null
            var hash: String? = null
            if (saltAndHash != null) {
                val split = saltAndHash.split(":");
                salt = split[0]
                hash = split[1]
            }

            val user = User.makeUser(fullName!!, email, null, phone, salt, hash)

            map[user.login] = user
            users.add(user)
        }

        return users;
    }

}