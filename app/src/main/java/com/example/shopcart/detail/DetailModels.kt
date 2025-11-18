package com.example.shopcart.detail

import java.io.Serializable

data class DetailPromo(
    val id: Int?,
    val description: String?,
    val discount: Double?,
    val startDate: String?,
    val endDate: String?,
    val state: Boolean?,
    val imagePromo: String?
) : Serializable

data class DetailCategory(
    val id: Int?,
    val description: String?
) : Serializable

data class DetailConsole(
    val id: Int?,
    val description: String?
) : Serializable

data class GameDetail(
    val id: Int,
    val title: String,
    val description: String?,
    val console: DetailConsole?,
    val hasDiscount: Boolean,
    val price: Double,
    val image: String?,
    val image2: String?,
    val image3: String?,
    val mini: String?,
    val detailsPromo: List<DetailPromo>?,
    val detailsCategories: List<DetailCategory>?
) : Serializable
