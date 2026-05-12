package com.example.userservice.validation

import com.example.userservice.dto.UpdateProfileRequestDto
import jakarta.validation.Constraint
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import jakarta.validation.Payload
import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [JlptProgressionValidator::class])
annotation class ValidJlptProgression(
    val message: String = "Current level cannot be higher than goal level",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = [],
)

class JlptProgressionValidator : ConstraintValidator<ValidJlptProgression, UpdateProfileRequestDto> {
    override fun isValid(value: UpdateProfileRequestDto?, context: ConstraintValidatorContext?): Boolean {
        if (value == null) {
            return true // Let @NotNull handle null value validation
        }

        // For JLPT levels, N5 is lowest, N1 is highest
        // Higher ordinal value means lower level (typically N5=4, N4=3, N3=2, N2=1, N1=0)
        // Current level ordinal should be <= goal level ordinal
        // Example: Current N5 (ordinal 4), Goal N3 (ordinal 2): 4 > 2, which is invalid
        // Example: Current N3 (ordinal 2), Goal N1 (ordinal 0): 2 > 0, which is invalid
        // Example: Current N3 (ordinal 2), Goal N3 (ordinal 2): 2 == 2, which is valid
        return value.currentLevel.ordinal <= value.jlptGoal.ordinal
    }
}
