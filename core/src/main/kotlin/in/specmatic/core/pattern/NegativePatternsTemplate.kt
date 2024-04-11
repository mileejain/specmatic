package `in`.specmatic.core.pattern

import `in`.specmatic.core.Resolver

abstract class NegativePatternsTemplate {
    fun negativeBasedOn(patternMap: Map<String, Pattern>, row: Row, resolver: Resolver): Sequence<Map<String, Pattern>> {
        val eachKeyMappedToPatternMap = patternMap.mapValues { patternMap }
        val negativePatternsMap = getNegativePatterns(patternMap, resolver, row)

        val modifiedPatternMap: Map<String, Sequence<Map<String, Sequence<Pattern>>>> = eachKeyMappedToPatternMap.mapValues { (keyToNegate, patterns) ->
            val negativePatterns = negativePatternsMap[keyToNegate]
            negativePatterns!!.map { negativePattern ->
                patterns.mapValues { (key, pattern) ->
                    attempt(breadCrumb = key) {
                        when (key == keyToNegate) {
                            true ->
                                attempt(breadCrumb = "Setting $key to $negativePattern for negative test scenario") {
                                    negativePatternsForKey(key, negativePattern, resolver)
                                }

                            else -> newBasedOn(row, key, pattern, resolver)
                        }
                    }
                }
            }
        }
        if (modifiedPatternMap.values.isEmpty())
            return sequenceOf(emptyMap())
        return modifiedPatternMap.values.asSequence().flatMap { list: Sequence<Map<String, Sequence<Pattern>>> ->
            list.flatMap { patternList(it) }
        }
    }

    fun negativeBasedOnR(
        patternMap: Map<String, Pattern>,
        row: Row,
        resolver: Resolver
    ): Sequence<ReturnValue<Map<String, Pattern>>> {
        val eachKeyMappedToPatternMap = patternMap.mapValues { patternMap }
        val negativePatternsMap = getNegativePatterns(patternMap, resolver, row)

        val modifiedPatternMap: Map<String, Sequence<Map<String, Sequence<ReturnValue<Pattern>>>>> =
            eachKeyMappedToPatternMap.mapValues { (keyToNegate, patterns) ->
                val negativePatterns = negativePatternsMap[keyToNegate]
                negativePatterns!!.map { negativePattern ->
                    patterns.mapValues { (key, pattern) ->
                        attempt(breadCrumb = key) {
                            when (key) {
                                keyToNegate ->
                                    attempt(breadCrumb = "Setting $key to $negativePattern for negative test scenario") {
                                        negativePatternsForKey(key, negativePattern, resolver).map {
                                            if (negativePattern is ScalarType) {
                                                HasValue(
                                                    it,
                                                    "${pattern.typeName} in the spec, attempting with ${negativePattern.typeName}",
                                                    key
                                                )
                                            } else
                                                HasValue(it)
                                        }
                                    }

                                else -> newBasedOn(row, key, pattern, resolver).map { HasValue(it) }
                            }
                        }
                    }
                }
            }

        if (modifiedPatternMap.values.isEmpty())
            return sequenceOf(HasValue(emptyMap()))

        return modifiedPatternMap.values.asSequence().flatMap { list ->
            list.flatMap { patternListR(it) }
        }
    }

    abstract fun getNegativePatterns(
        patternMap: Map<String, Pattern>,
        resolver: Resolver,
        row: Row
    ): Map<String, Sequence<Pattern>>

    abstract fun negativePatternsForKey(
        key: String,
        negativePattern: Pattern,
        resolver: Resolver,
    ): Sequence<Pattern>
}
