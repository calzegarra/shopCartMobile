package com.example.shopcart.cart

import com.example.shopcart.detail.GameDetail

data class CartItem(
    val game: GameDetail,
    val finalPrice: Double
)

object CartStore {
    private val items = mutableListOf<CartItem>()

    fun add(item: CartItem) {
        items.add(item)
    }

    fun count(): Int = items.size

    fun all(): List<CartItem> = items.toList()

    fun total(): Double = items.sumOf { it.finalPrice }

    fun clear() = items.clear()
}
