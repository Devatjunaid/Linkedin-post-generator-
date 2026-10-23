package com.driveready.testprep.data.content

enum class HazardScene { RESIDENTIAL, URBAN, RURAL }

enum class HazardKind { CHILD_RUNS_OUT, CAR_EMERGES, LEAD_CAR_BRAKES, CYCLIST_SWERVES, PEDESTRIAN_STEPS_OUT, ANIMAL_CROSSES }

data class HazardClip(
    val id: String,
    val title: String,
    val scene: HazardScene,
    val kind: HazardKind,
    val durationSec: Float,
    /** Moment the hazard starts to develop: the scoring window opens here. */
    val developAt: Float,
    val lesson: String
)

/** Length of the scoring window after a hazard starts developing (5 bands of 1 s → 5..1 points). */
const val HAZARD_WINDOW_SEC = 5f

/** More than this many taps in a single clip is treated as pattern-clicking and scores zero. */
const val HAZARD_MAX_TAPS = 8

val HazardClips = listOf(
    HazardClip("h1", "Residential street", HazardScene.RESIDENTIAL, HazardKind.CHILD_RUNS_OUT, 14f, 6.5f,
        "A ball rolled out first — the child following it was the developing hazard. Parked cars hide pedestrians, so slow down early."),
    HazardClip("h2", "Side-road junction", HazardScene.URBAN, HazardKind.CAR_EMERGES, 13f, 5.5f,
        "The car at the side road started creeping forward. Watch the wheels and the driver's head, not just the car."),
    HazardClip("h3", "Busy main road", HazardScene.URBAN, HazardKind.LEAD_CAR_BRAKES, 12f, 5f,
        "Brake lights ahead mean the gap is closing. Click as soon as the gap starts to shrink, not when you are already close."),
    HazardClip("h4", "Cyclist and parked van", HazardScene.URBAN, HazardKind.CYCLIST_SWERVES, 14f, 7f,
        "The cyclist had to move out around the parked van. Expect cyclists to pull out around obstructions."),
    HazardClip("h5", "Zebra crossing", HazardScene.RESIDENTIAL, HazardKind.PEDESTRIAN_STEPS_OUT, 13f, 6f,
        "A pedestrian waiting at a crossing may step out at any moment. Be ready to stop."),
    HazardClip("h6", "Country lane at dusk", HazardScene.RURAL, HazardKind.ANIMAL_CROSSES, 15f, 8.5f,
        "Warning signs and hedges near fields mean animals may cross. Reduce speed in rural areas, especially at dawn and dusk.")
)
