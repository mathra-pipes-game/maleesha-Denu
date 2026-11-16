package com.example.domain.model

data class SriLankaPlace(
    val id: String,
    val nameEn: String,
    val nameSi: String,
    val district: String,
    val latitude: Double,
    val longitude: Double,
    val isPopular: Boolean = false
)

object SriLankaPlaces {
    val all: List<SriLankaPlace> = listOf(
        SriLankaPlace("colombo_fort", "Colombo Fort Railway Station", "කොළඹ කොටුව දුම්රිය ස්ථානය", "Colombo", 6.9337, 79.8500, true),
        SriLankaPlace("galle_face", "Galle Face Green", "ගාලු මුවදොර පිටිය", "Colombo", 6.9271, 79.8442, true),
        SriLankaPlace("bia_airport", "Bandaranaike Intl Airport (BIA)", "කටුනායක ජාත්‍යන්තර ගුවන්තොටුපළ", "Gampaha", 7.1808, 79.8841, true),
        SriLankaPlace("lotus_tower", "Colombo Lotus Tower", "නෙළුම් කුළුණ, කොළඹ", "Colombo", 6.9270, 79.8586, true),
        SriLankaPlace("pettah", "Pettah Floating Market", "පිටකොටුව පාවෙන වෙළඳපොළ", "Colombo", 6.9349, 79.8535, false),
        SriLankaPlace("bambalapitiya", "Bambalapitiya Junction", "බම්බලපිටිය හන්දිය", "Colombo", 6.8920, 79.8554, false),
        SriLankaPlace("nugegoda", "Nugegoda Super Market", "නුගේගොඩ නගර මධ්‍යය", "Colombo", 6.8649, 79.8997, true),
        SriLankaPlace("mount_lavinia", "Mount Lavinia Beach", "ගල්කිස්ස වෙරළ තීරය", "Colombo", 6.8301, 79.8636, false),
        SriLankaPlace("battaramulla", "Battaramulla Sethsiripaya", "බත්තරමුල්ල සෙත්සිරිපාය", "Colombo", 6.9016, 79.9182, false),
        SriLankaPlace("maharagama", "Maharagama Clock Tower", "මහරගම නගරය", "Colombo", 6.8480, 79.9265, false),
        SriLankaPlace("dehiwala", "Dehiwala Zoological Gardens", "දෙහිවල සත්වෝද්‍යානය", "Colombo", 6.8567, 79.8744, false),
        SriLankaPlace("kandy_temple", "Kandy Temple of the Tooth", "මහනුවර ශ්‍රී දළදා මාලිගාව", "Kandy", 7.2936, 80.6413, true),
        SriLankaPlace("galle_fort", "Galle Dutch Fort", "ගාල්ල කොටුව", "Galle", 6.0267, 80.2170, true),
        SriLankaPlace("matara_town", "Matara Central Bus Stand", "මාතර නගරය", "Matara", 5.9485, 80.5353, false),
        SriLankaPlace("negombo_beach", "Negombo Beach Park", "මීගමුව වෙරළ උද්‍යානය", "Gampaha", 7.2323, 79.8415, false),
        SriLankaPlace("kadawatha", "Kadawatha Expressway Interchange", "කඩවත අධිවේගී පිවිසුම", "Gampaha", 7.0018, 79.9522, false)
    )

    val popular: List<SriLankaPlace> get() = all.filter { it.isPopular }

    fun search(query: String): List<SriLankaPlace> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return all
        return all.filter {
            it.nameEn.lowercase().contains(q) ||
                it.nameSi.contains(q) ||
                it.district.lowercase().contains(q)
        }
    }
}
