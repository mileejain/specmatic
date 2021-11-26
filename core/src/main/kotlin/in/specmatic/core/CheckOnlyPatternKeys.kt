package `in`.specmatic.core

import `in`.specmatic.core.pattern.isMissingKey

internal object CheckOnlyPatternKeys: KeyErrorCheck {
    override fun validate(pattern: Map<String, Any>, actual: Map<String, Any>): KeyError? {
        return pattern.minus("...").keys.find { key ->
            isMissingKey(actual, key)
        }?.toMissingKeyError()
    }
}