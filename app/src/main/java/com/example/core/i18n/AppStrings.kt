package com.example.core.i18n

enum class AppLanguage(val code: String, val label: String) {
    SINHALA("si", "සිංහල"),
    ENGLISH("en", "English");

    companion object {
        fun fromCode(code: String?): AppLanguage =
            entries.firstOrNull { it.code == code } ?: SINHALA
    }
}

data class BilingualText(
    val si: String,
    val en: String
) {
    fun get(lang: AppLanguage): String = if (lang == AppLanguage.SINHALA) si else en
}

object MathraStrings {
    val appName = BilingualText("මත්‍රා ටැක්සි", "MATHRA TAXI")
    val poweredBy = BilingualText(
        "MATHRA DEVELOPMENT TEAM විසින් බලගැන්වේ",
        "POWERED BY MATHRA DEVELOPMENT TEAM"
    )
    val greeting = BilingualText("ආයුබෝවන්!", "Ayubowan!")
    val subtitle = BilingualText(
        "ශ්‍රී ලංකාවේ ප්‍රමුඛතම සුඛෝපභෝගී ටැක්සි සේවාව",
        "Sri Lanka's Next-Gen Ride & Digital Meter Platform"
    )
    val signInGoogle = BilingualText("Google සමඟ පිවිසෙන්න", "Sign in with Google")
    val currentLocation = BilingualText("වත්මන් ස්ථානය", "Current Location")
    val pickupPoint = BilingualText("ගමන් ආරම්භක ස්ථානය", "Pickup Location")
    val whereTo = BilingualText("ඔබ යන්නේ කොහෙද?", "Where are you going?")
    val popularDestinations = BilingualText("ජනප්‍රිය ගමනාන්ත", "Popular Sri Lanka Destinations")
    val selectVehicle = BilingualText("වාහන වර්ගය තෝරන්න", "Select Vehicle Category")
    val fareEstimate = BilingualText("ඇස්තමේන්තු ගාස්තුව", "Estimated Fare")
    val bookRide = BilingualText("දැන් ගමන වෙන්කරන්න", "Book Mathra Ride")
    val confirmRide = BilingualText("ගමන තහවුරු කරන්න", "Confirm Ride")
    val cancelRide = BilingualText("ගමන අවලංගු කරන්න", "Cancel Ride")
    val rideHistory = BilingualText("ගමන් ඉතිහාසය", "Ride History")
    val notifications = BilingualText("දැනුම්දීම්", "Notifications")
    val profile = BilingualText("මගේ ගිණුම", "Profile")
    val settings = BilingualText("සැකසුම්", "Settings")
    val support = BilingualText("පාරිභෝගික සහාය", "Customer Support")
    val emergencySos = BilingualText("හදිසි SOS", "Emergency SOS")
    val about = BilingualText("මත්‍රා ටැක්සි ගැන", "About Mathra Taxi")
    val digitalMeter = BilingualText("ඩිජිටල් ටැක්සි මීටරය", "DIGITAL TAXI METER")
    val passengerMode = BilingualText("මගී ප්‍රකාරය", "Passenger")
    val driverMode = BilingualText("රියදුරු ප්‍රකාරය", "Driver")
    val adminMode = BilingualText("පරිපාලක පුවරුව", "Admin")
    val goOnline = BilingualText("සේවයට එක්වන්න", "Go Online")
    val goOffline = BilingualText("සේවයෙන් ඉවත්වන්න", "Go Offline")
    val driverArrived = BilingualText("මම පැමිණියා", "I Have Arrived")
    val startTrip = BilingualText("ගමන අරඹන්න", "Start Trip")
    val completeTrip = BilingualText("ගමන අවසන් කරන්න", "Complete Trip")
    val earnings = BilingualText("ආදායම් වාර්තාව", "Earnings")
    val verificationPending = BilingualText(
        "පරිපාලක අනුමැතිය බලාපොරොත්තුවෙන්",
        "Pending Admin Verification"
    )
    val digitalReceipt = BilingualText("ඩිජිටල් රිසිට්පත", "Digital Receipt")
    val paymentMethod = BilingualText("ගෙවීම් ක්‍රමය", "Payment Method")
    val rateTrip = BilingualText("ගමන අගයන්න", "Rate & Review Trip")
}
