package com.example.userservice.security

import com.example.userservice.entity.JlptLevel
import com.example.userservice.entity.UserEntity
import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.security.Keys
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.UUID
import java.util.function.Function
import javax.crypto.SecretKey

@Service
class JwtTokenUtil {

    @Value("\${jwt.secret}")
    private lateinit var secret: String

    @Value("\${jwt.secret-previous:}")
    private var previousSecret: String = ""

    @Value("\${jwt.expiration}")
    private val jwtExpiration: Long = 2592000000 // 24 hours by default

    private val secretKey: SecretKey by lazy {
        Keys.hmacShaKeyFor(secret.toByteArray())
    }

    private val previousSecretKey: SecretKey? by lazy {
        if (previousSecret.isNotBlank()) Keys.hmacShaKeyFor(previousSecret.toByteArray()) else null
    }

    private val logger = LoggerFactory.getLogger(JwtTokenUtil::class.java)

    private val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

    fun generateToken(user: UserEntity): String {
        val claims: Map<String, Any> = mapOf(
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

    private fun createToken(claims: Map<String, Any>, subject: String): String {
        val now = Date()
        val expirationDate = Date(now.time + jwtExpiration)

        return Jwts.builder()
            .setClaims(claims)
            .setSubject(subject)
            .setIssuedAt(now)
            .setExpiration(expirationDate)
            .signWith(secretKey, SignatureAlgorithm.HS512)
            .compact()
    }

    fun validateToken(token: String, userDetails: UserDetails): Boolean {
        val email = extractEmail(token)
        return email == userDetails.username && !isTokenExpired(token)
    }

    fun extractEmail(token: String): String {
        return extractClaim(token) { claims -> claims["email"] as String }
    }

    fun extractRoleId(jwt: String): Int {
        return extractClaim(jwt) { claims -> claims["role"] as Int }
    }

    fun extractExpiration(token: String): Date {
        return extractClaim(token, Claims::getExpiration)
    }

    fun <T> extractClaim(token: String, claimsResolver: Function<Claims, T>): T {
        val claims = extractAllClaims(token)
        return claimsResolver.apply(claims)
    }

    private fun extractAllClaims(token: String): Claims {
        return try {
            Jwts.parserBuilder().setSigningKey(secretKey).build().parseClaimsJws(token).body
        } catch (e: Exception) {
            val prevKey = previousSecretKey ?: throw e
            Jwts.parserBuilder().setSigningKey(prevKey).build().parseClaimsJws(token).body
        }
    }

    private fun isTokenExpired(token: String): Boolean {
        return extractExpiration(token).before(Date())
    }

    fun extractUserId(token: String): UUID? {
        return try {
            extractClaim(token) { claims -> claims["userId"] as UUID }
        } catch (e: Exception) {
            logger.error("Failed to extract userId from token: ${e.message}", e)
            null
        }
    }

    fun extractFullName(token: String): String? {
        return try {
            extractClaim(token) { claims -> claims["fullName"] as? String }
        } catch (e: Exception) {
            logger.error("Failed to extract fullName from token: ${e.message}", e)
            null
        }
    }

    fun extractProfilePicture(token: String): String? {
        return try {
            extractClaim(token) { claims -> claims["profilePicture"] as? String }
        } catch (e: Exception) {
            logger.error("Failed to extract profilePicture from token: ${e.message}", e)
            null
        }
    }

    fun extractCurrentLevel(token: String): JlptLevel? {
        return try {
            extractClaim(token) { claims ->
                (claims["currentLevel"] as? String)?.let { JlptLevel.valueOf(it) }
            }
        } catch (e: Exception) {
            logger.error("Failed to extract currentLevel from token: ${e.message}", e)
            null
        }
    }

    fun extractJlptGoal(token: String): JlptLevel? {
        return try {
            extractClaim(token) { claims ->
                (claims["jlptGoal"] as? String)?.let { JlptLevel.valueOf(it) }
            }
        } catch (e: Exception) {
            logger.error("Failed to extract jlptGoal from token: ${e.message}", e)
            null
        }
    }

    fun extractLastLogin(token: String): LocalDateTime? {
        return try {
            extractClaim(token) { claims ->
                (claims["lastLogin"] as? String)?.let { LocalDateTime.parse(it, dateTimeFormatter) }
            }
        } catch (e: Exception) {
            logger.error("Failed to extract lastLogin from token: ${e.message}", e)
            null
        }
    }
}
