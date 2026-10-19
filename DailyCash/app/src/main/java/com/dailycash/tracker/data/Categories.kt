package com.dailycash.tracker.data

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.BakeryDining
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.material.icons.rounded.CardTravel
import androidx.compose.material.icons.rounded.Checkroom
import androidx.compose.material.icons.rounded.Chair
import androidx.compose.material.icons.rounded.ChildCare
import androidx.compose.material.icons.rounded.ConfirmationNumber
import androidx.compose.material.icons.rounded.ContentCut
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.Devices
import androidx.compose.material.icons.rounded.DirectionsBoat
import androidx.compose.material.icons.rounded.DirectionsBus
import androidx.compose.material.icons.rounded.Eco
import androidx.compose.material.icons.rounded.EmojiFoodBeverage
import androidx.compose.material.icons.rounded.Face
import androidx.compose.material.icons.rounded.Fastfood
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material.icons.rounded.Flight
import androidx.compose.material.icons.rounded.Handyman
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Hotel
import androidx.compose.material.icons.rounded.Icecream
import androidx.compose.material.icons.automirrored.rounded.Label
import androidx.compose.material.icons.rounded.LocalAtm
import androidx.compose.material.icons.rounded.LocalBar
import androidx.compose.material.icons.rounded.LocalCafe
import androidx.compose.material.icons.rounded.LocalDrink
import androidx.compose.material.icons.rounded.LocalGasStation
import androidx.compose.material.icons.rounded.LocalGroceryStore
import androidx.compose.material.icons.rounded.LocalHospital
import androidx.compose.material.icons.rounded.LocalLaundryService
import androidx.compose.material.icons.rounded.LocalParking
import androidx.compose.material.icons.rounded.LocalPharmacy
import androidx.compose.material.icons.rounded.LocalTaxi
import androidx.compose.material.icons.rounded.MedicalServices
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Museum
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Opacity
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Pets
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.RamenDining
import androidx.compose.material.icons.rounded.Redeem
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.SetMeal
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.ShoppingBasket
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.SportsSoccer
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material.icons.rounded.Subway
import androidx.compose.material.icons.rounded.Tapas
import androidx.compose.material.icons.rounded.Toll
import androidx.compose.material.icons.rounded.Train
import androidx.compose.material.icons.rounded.TwoWheeler
import androidx.compose.material.icons.rounded.VolunteerActivism
import androidx.compose.material.icons.rounded.Watch
import androidx.compose.material.icons.rounded.Whatshot
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material.icons.rounded.Work
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

enum class CategoryGroup(val title: String, val tint: Color, val tile: Color) {
    FOOD("Food & drinks", Color(0xFFFFB547), Color(0xFF33240F)),
    GROCERIES("Groceries", Color(0xFF6EF0B9), Color(0xFF11302A)),
    TRANSPORT("Transport", Color(0xFF9DBDF0), Color(0xFF152338)),
    HOME("Home & bills", Color(0xFFC6A8FF), Color(0xFF261D3A)),
    HEALTH("Health & care", Color(0xFFFF8F73), Color(0xFF3A1A16)),
    SHOPPING("Shopping", Color(0xFFFF9BC8), Color(0xFF3A1729)),
    FUN("Fun & going out", Color(0xFFF5E27A), Color(0xFF333014)),
    LIFE("Family & life", Color(0xFF7FE0E8), Color(0xFF0F3033)),
    CUSTOM("Your own", Color(0xFFEEF3EF), Color(0xFF222B28)),
    INCOME("Cash in", Color(0xFF6EF0B9), Color(0xFF11302A)),
}

data class Category(val id: String, val name: String, val icon: ImageVector, val group: CategoryGroup)

object Categories {
    const val CUSTOM_PREFIX = "custom:"

    /** 60 everyday things people pay cash for. */
    val spend: List<Category> = listOf(
        Category("restaurant", "Restaurant", Icons.Rounded.Restaurant, CategoryGroup.FOOD),
        Category("fast_food", "Fast food", Icons.Rounded.Fastfood, CategoryGroup.FOOD),
        Category("coffee", "Coffee", Icons.Rounded.LocalCafe, CategoryGroup.FOOD),
        Category("tea_snacks", "Tea & snacks", Icons.Rounded.EmojiFoodBeverage, CategoryGroup.FOOD),
        Category("bakery", "Bakery", Icons.Rounded.BakeryDining, CategoryGroup.FOOD),
        Category("street_food", "Street food", Icons.Rounded.Tapas, CategoryGroup.FOOD),
        Category("noodles", "Noodles & takeaway", Icons.Rounded.RamenDining, CategoryGroup.FOOD),
        Category("dessert", "Dessert", Icons.Rounded.Icecream, CategoryGroup.FOOD),
        Category("drinks", "Drinks & bar", Icons.Rounded.LocalBar, CategoryGroup.FOOD),

        Category("groceries", "Groceries", Icons.Rounded.LocalGroceryStore, CategoryGroup.GROCERIES),
        Category("fruit_veg", "Fruit & veg", Icons.Rounded.Eco, CategoryGroup.GROCERIES),
        Category("meat_fish", "Meat & fish", Icons.Rounded.SetMeal, CategoryGroup.GROCERIES),
        Category("water", "Drinking water", Icons.Rounded.LocalDrink, CategoryGroup.GROCERIES),
        Category("market", "Local market", Icons.Rounded.Storefront, CategoryGroup.GROCERIES),
        Category("household", "Household items", Icons.Rounded.ShoppingBasket, CategoryGroup.GROCERIES),

        Category("taxi", "Taxi & ride", Icons.Rounded.LocalTaxi, CategoryGroup.TRANSPORT),
        Category("bus", "Bus", Icons.Rounded.DirectionsBus, CategoryGroup.TRANSPORT),
        Category("train", "Train", Icons.Rounded.Train, CategoryGroup.TRANSPORT),
        Category("metro", "Metro", Icons.Rounded.Subway, CategoryGroup.TRANSPORT),
        Category("fuel", "Fuel", Icons.Rounded.LocalGasStation, CategoryGroup.TRANSPORT),
        Category("parking", "Parking", Icons.Rounded.LocalParking, CategoryGroup.TRANSPORT),
        Category("bike", "Bike & scooter", Icons.Rounded.TwoWheeler, CategoryGroup.TRANSPORT),
        Category("flight", "Flight", Icons.Rounded.Flight, CategoryGroup.TRANSPORT),
        Category("ferry", "Ferry & boat", Icons.Rounded.DirectionsBoat, CategoryGroup.TRANSPORT),
        Category("tolls", "Tolls", Icons.Rounded.Toll, CategoryGroup.TRANSPORT),

        Category("rent", "Rent", Icons.Rounded.Home, CategoryGroup.HOME),
        Category("electricity", "Electricity", Icons.Rounded.FlashOn, CategoryGroup.HOME),
        Category("water_bill", "Water bill", Icons.Rounded.Opacity, CategoryGroup.HOME),
        Category("internet", "Internet", Icons.Rounded.Wifi, CategoryGroup.HOME),
        Category("mobile", "Mobile recharge", Icons.Rounded.PhoneAndroid, CategoryGroup.HOME),
        Category("laundry", "Laundry", Icons.Rounded.LocalLaundryService, CategoryGroup.HOME),
        Category("repairs", "Repairs", Icons.Rounded.Handyman, CategoryGroup.HOME),
        Category("cooking_gas", "Cooking gas", Icons.Rounded.Whatshot, CategoryGroup.HOME),

        Category("doctor", "Doctor", Icons.Rounded.LocalHospital, CategoryGroup.HEALTH),
        Category("medical_tests", "Medical tests", Icons.Rounded.MedicalServices, CategoryGroup.HEALTH),
        Category("pharmacy", "Pharmacy", Icons.Rounded.LocalPharmacy, CategoryGroup.HEALTH),
        Category("gym", "Gym", Icons.Rounded.FitnessCenter, CategoryGroup.HEALTH),
        Category("spa", "Spa & massage", Icons.Rounded.Spa, CategoryGroup.HEALTH),
        Category("haircut", "Haircut", Icons.Rounded.ContentCut, CategoryGroup.HEALTH),
        Category("beauty", "Beauty & care", Icons.Rounded.Face, CategoryGroup.HEALTH),

        Category("clothes", "Clothes", Icons.Rounded.Checkroom, CategoryGroup.SHOPPING),
        Category("shopping", "Shopping", Icons.Rounded.ShoppingBag, CategoryGroup.SHOPPING),
        Category("electronics", "Electronics", Icons.Rounded.Devices, CategoryGroup.SHOPPING),
        Category("furniture", "Furniture", Icons.Rounded.Chair, CategoryGroup.SHOPPING),
        Category("books", "Books & stationery", Icons.AutoMirrored.Rounded.MenuBook, CategoryGroup.SHOPPING),
        Category("accessories", "Accessories", Icons.Rounded.Watch, CategoryGroup.SHOPPING),

        Category("movies", "Movies", Icons.Rounded.Movie, CategoryGroup.FUN),
        Category("games", "Games", Icons.Rounded.SportsEsports, CategoryGroup.FUN),
        Category("music", "Music & concerts", Icons.Rounded.MusicNote, CategoryGroup.FUN),
        Category("tickets", "Tickets & events", Icons.Rounded.ConfirmationNumber, CategoryGroup.FUN),
        Category("sports", "Sports", Icons.Rounded.SportsSoccer, CategoryGroup.FUN),
        Category("sightseeing", "Sightseeing", Icons.Rounded.Museum, CategoryGroup.FUN),

        Category("childcare", "Kids & childcare", Icons.Rounded.ChildCare, CategoryGroup.LIFE),
        Category("education", "Education & fees", Icons.Rounded.School, CategoryGroup.LIFE),
        Category("pets", "Pets", Icons.Rounded.Pets, CategoryGroup.LIFE),
        Category("gifts", "Gifts", Icons.Rounded.CardGiftcard, CategoryGroup.LIFE),
        Category("charity", "Charity & donations", Icons.Rounded.VolunteerActivism, CategoryGroup.LIFE),
        Category("hotel", "Hotel & stay", Icons.Rounded.Hotel, CategoryGroup.LIFE),
        Category("atm_fees", "ATM & bank fees", Icons.Rounded.LocalAtm, CategoryGroup.LIFE),
        Category("tips", "Tips", Icons.Rounded.Payments, CategoryGroup.LIFE),
    )

    /** Where cash comes from when topping up a wallet. */
    val income: List<Category> = listOf(
        Category("in_atm", "ATM withdrawal", Icons.Rounded.LocalAtm, CategoryGroup.INCOME),
        Category("in_exchange", "Currency exchange", Icons.Rounded.SwapHoriz, CategoryGroup.INCOME),
        Category("in_salary", "Salary / wages", Icons.Rounded.Work, CategoryGroup.INCOME),
        Category("in_received", "Received / gift", Icons.Rounded.Redeem, CategoryGroup.INCOME),
        Category("in_bank", "From bank", Icons.Rounded.AccountBalance, CategoryGroup.INCOME),
        Category("in_travel", "Travel budget", Icons.Rounded.CardTravel, CategoryGroup.INCOME),
    )

    private val byId = (spend + income).associateBy { it.id }

    fun custom(name: String) = Category(CUSTOM_PREFIX + name, name, Icons.AutoMirrored.Rounded.Label, CategoryGroup.CUSTOM)

    fun resolve(id: String, label: String): Category =
        byId[id] ?: if (id.startsWith(CUSTOM_PREFIX)) custom(label) else custom(label.ifBlank { "Other" })
}
