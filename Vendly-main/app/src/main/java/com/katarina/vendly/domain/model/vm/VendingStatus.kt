package com.katarina.vendly.domain.model.vm

enum class VendingStatus(val code: String, val label: String) {
    FULL("full", "Full"),
    LOW("low", "Low"),
    EMPTY("empty", "Empty"),
    OUT_OF_ORDER("out_of_order", "Out of order");

    companion object {
        fun fromCode(code: String?): VendingStatus =
            values().firstOrNull { it.code.equals(code, ignoreCase = true) } ?: FULL

        /** Accepts either a code ("full") or a label ("Full"); returns the canonical code. */
        fun normalize(input: String?): String {
            if (input.isNullOrBlank()) return FULL.code
            val t = input.trim()
            values().firstOrNull {
                it.code.equals(t, true) || it.label.equals(t, true)
            }?.let { return it.code }
            return FULL.code
        }
    }
}