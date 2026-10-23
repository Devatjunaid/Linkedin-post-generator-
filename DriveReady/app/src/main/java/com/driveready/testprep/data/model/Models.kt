package com.driveready.testprep.data.model

/** Which family of road-sign design a country uses. Drives how signs are drawn. */
enum class SignStyle {
    /** Vienna Convention: red-bordered triangles warn, circles order (UK, EU, Asia, Middle East, Africa). */
    VIENNA,

    /** MUTCD-style: yellow diamonds warn, white rectangles regulate (USA, Canada, Australia, NZ). */
    DIAMOND
}

enum class DrivingSide { LEFT, RIGHT }

enum class SpeedUnit(val label: String) { MPH("mph"), KMH("km/h") }

/** Region-level label shown when a country delegates licensing to states/provinces. */
enum class RegionKind(val singular: String, val plural: String) {
    STATE("state", "states"),
    PROVINCE("province", "provinces"),
    EMIRATE("emirate", "emirates"),
    TERRITORY("state or territory", "states and territories")
}

data class TheorySpec(
    val name: String,
    val questions: Int,
    val passMark: Int,
    val minutes: Int?,
    val notes: List<String> = emptyList()
) {
    val passPercent: Int get() = if (questions == 0) 0 else (passMark * 100 + questions - 1) / questions
}

data class HazardSpec(
    val isOfficialPart: Boolean,
    val name: String,
    val description: String
)

data class OralQuestion(
    val question: String,
    val answer: List<String>,
    val tag: String
)

data class OralSpec(
    val name: String,
    val description: String,
    val techniques: List<String>,
    val questions: List<OralQuestion>
)

data class PracticalStep(val title: String, val detail: String)

data class PracticalSpec(
    val name: String,
    val durationMinutes: Int?,
    val passRule: String,
    val steps: List<PracticalStep>,
    val maneuverIds: List<String>,
    val commonFaults: List<String>
)

data class SyllabusModule(
    val title: String,
    val topics: List<String>
)

data class Region(
    val id: String,
    val name: String,
    val agency: String,
    val questions: Int,
    val passMark: Int,
    val permitAge: String,
    val notes: String = ""
) {
    val passPercent: Int get() = (passMark * 100 + questions - 1) / questions
}

data class Country(
    val id: String,
    val name: String,
    val code: String,
    val flagColors: List<Long>,
    val authority: String,
    val officialSite: String,
    val drivingSide: DrivingSide,
    val speedUnit: SpeedUnit,
    val signStyle: SignStyle,
    val minAge: String,
    val licenceStages: List<String>,
    val theory: TheorySpec,
    val hazard: HazardSpec?,
    val oral: OralSpec,
    val practical: PracticalSpec,
    val documents: List<String>,
    val syllabus: List<SyllabusModule>,
    val tips: List<String>,
    val regionKind: RegionKind? = null,
    val regions: List<Region> = emptyList()
) {
    val hasRegions: Boolean get() = regions.isNotEmpty()
}

enum class QuestionCategory(val label: String) {
    SIGNS("Road signs"),
    RULES("Rules of the road"),
    SAFETY("Safety & vehicle"),
    HAZARD("Hazard awareness"),
    VULNERABLE("Vulnerable road users"),
    MOTORWAY("Highways & motorways"),
    ALCOHOL("Alcohol, drugs & fatigue"),
    EMERGENCY("Incidents & emergencies"),
    LEGAL("Documents & law")
}

/** Sign glyphs the app can draw natively (no image files needed). */
enum class SignType {
    STOP, GIVE_WAY, NO_ENTRY, SPEED_LIMIT, NO_U_TURN, NO_OVERTAKING, NO_PARKING,
    ROUNDABOUT_AHEAD, PEDESTRIAN_CROSSING, CHILDREN, SLIPPERY_ROAD, TRAFFIC_SIGNALS_AHEAD,
    RAILWAY_CROSSING, TWO_WAY_TRAFFIC, KEEP_LEFT, KEEP_RIGHT, MINI_ROUNDABOUT, ROAD_WORKS,
    NATIONAL_SPEED_LIMIT, NO_LEFT_TURN, SHARP_BEND, ONE_WAY, HOSPITAL, PARKING
}

data class Question(
    val id: String,
    val category: QuestionCategory,
    val text: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String,
    val sign: SignType? = null,
    val signValue: String? = null,
    /** Empty = applies everywhere. Otherwise only to these country ids. */
    val countries: Set<String> = emptySet(),
    val drivingSide: DrivingSide? = null,
    val speedUnit: SpeedUnit? = null
)

data class SignInfo(
    val type: SignType,
    val name: String,
    val meaning: String,
    val group: SignGroup,
    val value: String? = null
)

enum class SignGroup(val label: String) { ORDERS("Orders"), WARNING("Warning"), INFO("Information") }
