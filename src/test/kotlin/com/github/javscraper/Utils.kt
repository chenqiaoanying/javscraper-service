package com.github.javscraper

import org.json.JSONArray
import org.json.JSONObject
import org.skyscreamer.jsonassert.JSONAssert
import org.skyscreamer.jsonassert.JSONCompareMode
import java.util.*

fun compareJson(expected: String, actual: String, compareMode: JSONCompareMode, vararg ignoreFields: String) {
    JSONAssert.assertEquals(
        JSONObject(expected).removeRecursive(*ignoreFields),
        JSONObject(actual).removeRecursive(*ignoreFields),
        compareMode
    )
}

fun JSONObject.removeRecursive(vararg keys: String): JSONObject {
    val stack = LinkedList<Any>()
    stack.push(this)
    while (stack.isNotEmpty()) {
        val node = stack.pop()
        if (node is JSONObject) {
            keys.forEach { node.remove(it) }
            node.keys().forEach {
                val childNode = node.get(it as String)
                if (childNode is JSONObject || childNode is JSONArray) {
                    stack.push(childNode)
                }
            }
        } else if (node is JSONArray) {
            var count = 0
            while (!node.isNull(count)) {
                val childNode = node[count]
                if (childNode is JSONObject || childNode is JSONArray) {
                    stack.push(childNode)
                }
                count++
            }
        }
    }
    return this
}