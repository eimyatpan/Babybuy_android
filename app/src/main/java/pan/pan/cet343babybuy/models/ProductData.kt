package pan.pan.cet343babybuy.models

data class ProductData(
    var productId: String? = null,
    var name: String? = null,
    var description: String? = null,
    var category: String? = null,
    var price: Int? = null,
    var quantity: Int? = null,
    var image: String? = null,
    var storeLocationLat: Double? = null,
    var storeLocationLng: Double? = null,
    val userId: String? = null,
    var markAsPurchased: Boolean = false
)


