package `in`.specmatic.core

import kotlin.collections.Map

data class QueryParameters(val map: Map<String, String> = kotlin.collections.HashMap(), val paramPairs: List<Pair<String, String>> = map.toList()) : Map<String, String> by map {

    fun plus(map: Map<String, String>): QueryParameters {
        return QueryParameters(this.map + map, paramPairs + map.toList())
    }

    fun plus(pair: Pair<String, String>): QueryParameters {
        return QueryParameters(this.map + pair, paramPairs + pair)
    }

    fun minus(name: String): QueryParameters {
        return QueryParameters(this.map - name, paramPairs.filterNot { it.first == name })
    }

    fun asMap():Map<String, String> {
        return paramPairs.toMap()
    }

    fun getValues(key: String): List<String> {
        return paramPairs.filter { it.first == key }.map { it.second }
    }

    override fun containsKey(key: String): Boolean {
        return paramPairs.any { it.first == key }
    }
}