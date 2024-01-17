package `in`.specmatic.core

import `in`.specmatic.core.pattern.*
import `in`.specmatic.core.pattern.withoutOptionality
import `in`.specmatic.core.utilities.URIUtils
import `in`.specmatic.core.value.JSONArrayValue
import `in`.specmatic.core.value.StringValue
import java.net.URI

data class HttpQueryParamPattern(val queryPatterns: Map<String, Pattern>) {
    fun generate(resolver: Resolver): List<Pair<String, String>> {
        return attempt(breadCrumb = "QUERY-PARAMS") {
            queryPatterns.map { it.key.removeSuffix("?") to it.value }.flatMap { (parameterName, pattern) ->
                attempt(breadCrumb = parameterName) {
                    val generatedValue =  resolver.withCyclePrevention(pattern) { it.generate(parameterName, pattern) }
                    if(generatedValue is JSONArrayValue) {
                        generatedValue.list.map { parameterName to it.toString() }
                    }
                    else {
                        listOf(parameterName to generatedValue.toString())
                    }
                }
            }
        }
    }

    fun newBasedOn(
        row: Row,
        resolver: Resolver
    ): List<Map<String, Pattern>> {
        val newQueryParamsList = attempt(breadCrumb = QUERY_PARAMS_BREADCRUMB) {
            val optionalQueryParams = queryPatterns

            forEachKeyCombinationIn(row.withoutOmittedKeys(optionalQueryParams), row) { entry ->
                newBasedOn(entry.mapKeys { withoutOptionality(it.key) }, row, resolver)
            }
        }
        return newQueryParamsList
    }

    fun matches(httpRequest: HttpRequest, resolver: Resolver): Result {
        val keyErrors =
            resolver.findKeyErrorList(queryPatterns, httpRequest.queryParams.asMap().mapValues { StringValue(it.value) })
        val keyErrorList: List<Result.Failure> = keyErrors.map {
            it.missingKeyToResult("query param", resolver.mismatchMessages).breadCrumb(it.name)
        }

        val results: List<Result?> = queryPatterns.keys.flatMap { key ->
            val keyName = key.removeSuffix("?")

            if (!httpRequest.queryParams.containsKey(keyName)) {
                listOf<Result?>(null)
            } else {
                try {
                    val patternValue: Pattern = queryPatterns.getValue(key)
                    val sampleValues: List<String> = httpRequest.queryParams.getValues(keyName)
                    sampleValues.map { sampleValue ->
                        val parsedValue = try {
                             patternValue.parse(sampleValue, resolver)
                        } catch (e: Exception) {
                            StringValue(sampleValue)
                        }
                        resolver.matchesPattern(keyName, patternValue, parsedValue).breadCrumb(keyName)
                    }
                } catch (e: ContractException) {
                    listOf(e.failure().breadCrumb(keyName)) // wrap single item in a list
                } catch (e: Throwable) {
                    listOf(Result.Failure(e.localizedMessage).breadCrumb(keyName)) // wrap single item in a list
                }
            }
        }

        val failures = keyErrorList.plus(results).filterIsInstance<Result.Failure>()
        return if (failures.isNotEmpty())
            Result.Failure.fromFailures(failures).breadCrumb(QUERY_PARAMS_BREADCRUMB)
        else
            Result.Success()
    }

    fun newBasedOn(resolver: Resolver): List<Map<String, Pattern>> {
        return attempt(breadCrumb = QUERY_PARAMS_BREADCRUMB) {
            val optionalQueryParams = queryPatterns

            allOrNothingCombinationIn(optionalQueryParams) { entry ->
                newBasedOn(entry.mapKeys { withoutOptionality(it.key) }, resolver)
            }
        }
    }

    override fun toString(): String {
        return if (queryPatterns.isNotEmpty()) {
            "?" + queryPatterns.mapKeys { it.key.removeSuffix("?") }.map { (key, value) ->
                "$key=$value"
            }.toList().joinToString(separator = "&")
        } else ""
    }

    fun negativeBasedOn(row: Row, resolver: Resolver): List<Map<String, Pattern>> {
        return attempt(breadCrumb = QUERY_PARAMS_BREADCRUMB) {
            val optionalQueryParams = queryPatterns

            forEachKeyCombinationIn(row.withoutOmittedKeys(optionalQueryParams), row) { entry ->
                negativeBasedOn(entry.mapKeys { withoutOptionality(it.key) }, row, resolver, true)
            }
        }
    }

    fun matches(uri: URI, queryParams: Map<String, String>, resolver: Resolver = Resolver()): Result {
        return matches(HttpRequest(path = uri.path, queryParametersMap =  queryParams), resolver)
    }
}

internal fun buildQueryPattern(
    urlPattern: URI,
    apiKeyQueryParams: Set<String> = emptySet()
): HttpQueryParamPattern {
    val queryPattern = URIUtils.parseQuery(urlPattern.query).mapKeys {
        "${it.key}?"
    }.mapValues {
        if (isPatternToken(it.value))
            DeferredPattern(it.value, it.key)
        else
            ExactValuePattern(StringValue(it.value))
    }.let { queryParams ->
        apiKeyQueryParams.associate { apiKeyQueryParam ->
            Pair("${apiKeyQueryParam}?", StringPattern())
        }.plus(queryParams)
    }
    return HttpQueryParamPattern(queryPattern)
}

const val QUERY_PARAMS_BREADCRUMB = "QUERY-PARAMS"