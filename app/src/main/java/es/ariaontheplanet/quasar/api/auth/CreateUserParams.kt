package es.ariaontheplanet.quasar.api.auth

class CreateUserParams(
    val username: String,
    val email: String,
    val password: String,
    val agreement: Boolean,
    val reason: String?,
)
