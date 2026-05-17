package com.example.learningservice.security

import com.example.learningservice.entity.UserEntity
import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.function.Function
import javax.crypto.SecretKey

@Service
class JwtTokenUtil {
    @Value("\${jwt.secret}")
    private lateinit var secret: String

    @Value("\${jwt.expiration}")
    private val jwtExpiration: Long = 2592000000 // 24 hours by default

    private val secretKey: SecretKey by lazy {
        Keys.hmacShaKeyFor(secret.toByteArray())
    }

    private val logger = LoggerFactory.getLogger(JwtTokenUtil::class.java)

    private val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

    fun generateToken(user: UserEntity): String {
        val claims: Map<String, Any> =
            mapOf(
                "userId" to user.userId?.toString().orEmpty(),
                "role" to user.role.roleId,
                "email" to user.email,
                "fullName" to user.fullName,
                "profilePicture" to user.profilePicture.orEmpty(),
                "currentLevel" to user.currentLevel?.name.orEmpty(),
                "jlptGoal" to user.jlptGoal?.name.orEmpty(),
                "lastLogin" to (user.lastLogin?.format(dateTimeFormatter) ?: LocalDateTime.now().format(dateTimeFormatter)),
            )
        return createToken(claims, user.email)
    }

    private fun createToken(
        claims: Map<String, Any>,
        subject: String,
    ): String {
        val now = Date()
        val expirationDate = Date(now.time + jwtExpiration)

        return Jwts
            .builder()
            .claims(claims)
            .subject(subject)
            .issuedAt(now)
            .expiration(expirationDate)
            .signWith(secretKey)
            .compact()
    }

    fun validateToken(
        token: String,
        userDetails: UserDetails,
    ): Boolean {
        val email = extractEmail(token)
        return email == userDetails.username && !isTokenExpired(token)
    }

    fun extractEmail(token: String): String = extractClaim(token) { claims -> claims["email"] as String }

    fun extractRoleId(jwt: String): Int = extractClaim(jwt) { claims -> claims["role"] as Int }

    fun extractExpiration(token: String): Date = extractClaim(token, Claims::getExpiration)

    fun <T> extractClaim(
        token: String,
        claimsResolver: Function<Claims, T>,
    ): T {
        val claims = extractAllClaims(token)
        return claimsResolver.apply(claims)
    }

    private fun extractAllClaims(token: String): Claims =
        Jwts
            .parser()
            .verifyWith(secretKey)
            .build()
            .parseSignedClaims(token)
            .payload

    private fun isTokenExpired(token: String): Boolean = extractExpiration(token).before(Date())
}
