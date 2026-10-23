package com.driveready.testprep.data.content

import com.driveready.testprep.data.model.PracticalStep
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/**
 * Top-down maneuver diagrams. Every scene is authored for RIGHT-hand traffic in a
 * 100 x 62 unit field with the vehicle facing east; the renderer mirrors it
 * horizontally for left-hand-traffic countries so the kerb is always on the
 * correct side.
 */
data class Pose(val x: Float, val y: Float, val angle: Float)

sealed interface SceneItem {
    data class Kerb(val x1: Float, val y1: Float, val x2: Float, val y2: Float) : SceneItem
    data class LaneLine(val x1: Float, val y1: Float, val x2: Float, val y2: Float) : SceneItem
    data class BayLine(val x1: Float, val y1: Float, val x2: Float, val y2: Float) : SceneItem
    data class ParkedCar(val x: Float, val y: Float, val angle: Float) : SceneItem
    data class Cone(val x: Float, val y: Float) : SceneItem
    data class Island(val x: Float, val y: Float, val r: Float) : SceneItem
    data class Pavement(val left: Float, val top: Float, val right: Float, val bottom: Float) : SceneItem
}

data class Maneuver(
    val id: String,
    val name: String,
    val summary: String,
    val steps: List<PracticalStep>,
    val faults: List<String>,
    val scene: List<SceneItem>,
    /** One pose per step, plus the start pose at index 0. */
    val poses: List<Pose>,
    val isTwoWheeler: Boolean = false
)

private fun step(title: String, detail: String) = PracticalStep(title, detail)

/** Builds forward-driving poses from a list of points, heading taken from the path direction. */
private fun pathPoses(points: List<Pair<Float, Float>>): List<Pose> = points.mapIndexed { i, p ->
    val next = points.getOrNull(i + 1) ?: p
    val prev = points.getOrNull(i - 1) ?: p
    val dx = next.first - prev.first
    val dy = next.second - prev.second
    val angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
    Pose(p.first, p.second, angle)
}

private val straightRoad = listOf(
    SceneItem.Pavement(0f, 52f, 100f, 62f),
    SceneItem.Pavement(0f, 0f, 100f, 14f),
    SceneItem.Kerb(0f, 52f, 100f, 52f),
    SceneItem.Kerb(0f, 14f, 100f, 14f),
    SceneItem.LaneLine(0f, 33f, 100f, 33f)
)

val Maneuvers: Map<String, Maneuver> = listOf(
    Maneuver(
        id = "parallel_park",
        name = "Parallel parking",
        summary = "Reverse into a space behind a parked vehicle, finishing close and parallel to the kerb.",
        steps = listOf(
            step("Position alongside", "Stop parallel to the target car, about 1 metre (a door's width) away, rear bumpers roughly level. Select reverse."),
            step("Observe and creep back", "Check all mirrors and both blind spots. Reverse slowly using clutch control until your rear wheels are level with the target car's rear bumper."),
            step("Steer towards the kerb", "Steer briskly about one full turn towards the kerb. Keep looking all around, especially for pedestrians and cyclists."),
            step("Straighten at 45°", "When the car is roughly 45° to the kerb, straighten the wheels and keep reversing slowly."),
            step("Steer away", "As your front clears the target car's rear corner, steer away from the kerb to swing the front in."),
            step("Finish parallel", "Straighten up as the car becomes parallel. Finish within about 30 cm of the kerb, without touching it.")
        ),
        faults = listOf("Mounting or hitting the kerb", "Poor observation before and during the reverse", "Finishing too far from the kerb", "Not reacting to other road users"),
        scene = listOf(
            SceneItem.Pavement(0f, 52f, 100f, 62f),
            SceneItem.Kerb(0f, 52f, 100f, 52f),
            SceneItem.LaneLine(0f, 22f, 100f, 22f),
            SceneItem.ParkedCar(66f, 46f, 0f),
            SceneItem.ParkedCar(14f, 46f, 0f)
        ),
        poses = listOf(
            Pose(66f, 36f, 0f), Pose(56f, 36f, 0f), Pose(49f, 39f, -22f),
            Pose(44f, 42.5f, -42f), Pose(40f, 45.5f, -15f), Pose(40f, 46f, 0f), Pose(40f, 46f, 0f)
        )
    ),
    Maneuver(
        id = "bay_park",
        name = "Bay parking (reverse in)",
        summary = "Reverse into a marked parking bay, finishing straight and between the lines.",
        steps = listOf(
            step("Choose your bay", "Drive slowly along the aisle. Count the bays and position about 1.5 m from the bay ends."),
            step("Pass the bay", "Stop when the bay line you will reverse past is level with your shoulder (or door mirror on smaller cars)."),
            step("Observe, then steer hard", "Full observation. Reverse slowly and apply full lock towards the bay."),
            step("Watch both lines", "Use both door mirrors: as the bay lines appear parallel to the car, start to straighten."),
            step("Straighten and centre", "Wheels straight; make small corrections so you are central between the lines."),
            step("Stop inside the bay", "Stop when the front of the car is inside the bay. Handbrake, neutral.")
        ),
        faults = listOf("Finishing over a line", "Not looking around while moving", "Hitting a neighbouring vehicle"),
        scene = listOf(
            SceneItem.BayLine(34f, 30f, 34f, 56f), SceneItem.BayLine(46f, 30f, 46f, 56f),
            SceneItem.BayLine(58f, 30f, 58f, 56f), SceneItem.BayLine(70f, 30f, 70f, 56f),
            SceneItem.BayLine(34f, 56f, 70f, 56f),
            SceneItem.ParkedCar(40f, 44f, -90f), SceneItem.ParkedCar(64f, 44f, -90f)
        ),
        poses = listOf(
            Pose(36f, 18f, 0f), Pose(64f, 18f, 0f), Pose(60f, 24f, -40f),
            Pose(55f, 32f, -75f), Pose(52f, 40f, -90f), Pose(52f, 45f, -90f), Pose(52f, 46f, -90f)
        )
    ),
    Maneuver(
        id = "three_point_turn",
        name = "Turn in the road (3-point turn)",
        summary = "Turn the car to face the opposite way using forward and reverse gears.",
        steps = listOf(
            step("Pull up safely", "Stop on the correct side of the road. Check mirrors and blind spot before moving off."),
            step("Forward to the far kerb", "Move slowly forward, steering briskly to full lock. Just before the far kerb, steer the other way."),
            step("Observe, then reverse", "Look all around. Reverse slowly with full lock the other way, watching out of the rear window."),
            step("Stop short of the kerb", "Steer back the other way just before stopping so the wheels are ready."),
            step("Forward and away", "Final full observation, then drive forward and straighten in the correct lane.")
        ),
        faults = listOf("Touching the kerb", "Not looking around before each move", "Taking too many moves"),
        scene = straightRoad,
        poses = listOf(
            Pose(34f, 43f, 0f), Pose(48f, 36f, -40f), Pose(56f, 24f, -85f),
            Pose(52f, 36f, -135f), Pose(42f, 26f, -170f), Pose(30f, 24f, -180f)
        )
    ),
    Maneuver(
        id = "reverse_corner",
        name = "Reverse around a corner",
        summary = "Reverse from a main road into a side road on your kerb side, keeping close to the kerb.",
        steps = listOf(
            step("Stop past the junction", "Stop close to the kerb about 2–3 car lengths past the side road. Check it is safe and legal."),
            step("Reverse in a straight line", "Full observation, then reverse slowly keeping parallel to the kerb."),
            step("Start the turn at the corner", "When the kerb starts to curve (look in the kerb-side mirror), steer to follow it."),
            step("Follow the curve", "Look over your shoulder frequently. Keep about 30–50 cm from the kerb."),
            step("Straighten up", "Straighten as the kerb straightens and continue back for about 2–3 car lengths.")
        ),
        faults = listOf("Swinging wide into the opposite lane", "Mounting the kerb", "Missing a pedestrian or vehicle behind"),
        scene = listOf(
            SceneItem.Pavement(0f, 42f, 42f, 62f), SceneItem.Pavement(62f, 42f, 100f, 62f),
            SceneItem.Kerb(0f, 42f, 42f, 42f), SceneItem.Kerb(62f, 42f, 100f, 42f),
            SceneItem.Kerb(42f, 42f, 42f, 62f), SceneItem.Kerb(62f, 42f, 62f, 62f),
            SceneItem.Kerb(0f, 8f, 100f, 8f), SceneItem.LaneLine(0f, 25f, 100f, 25f)
        ),
        poses = listOf(
            Pose(80f, 36f, 0f), Pose(64f, 36f, 0f), Pose(52f, 40f, -35f),
            Pose(49f, 48f, -75f), Pose(51f, 56f, -90f)
        )
    ),
    Maneuver(
        id = "pull_up_right",
        name = "Pull up on the opposite side & reverse",
        summary = "Pull up on the opposite side of the road, reverse about two car lengths, then rejoin traffic.",
        steps = listOf(
            step("Cross safely", "Mirrors, signal and look ahead. Cross only when there is no oncoming traffic."),
            step("Stop close to the kerb", "Stop parallel and reasonably close to the kerb on the opposite side."),
            step("Reverse two lengths", "Full observation, then reverse in a straight line for about 2 car lengths."),
            step("Rejoin", "Check all around, including blind spots, and rejoin the correct side when safe.")
        ),
        faults = listOf("Crossing in front of oncoming traffic", "Not checking blind spots when rejoining"),
        scene = straightRoad,
        poses = listOf(Pose(14f, 43f, 0f), Pose(34f, 34f, -25f), Pose(52f, 23f, 0f), Pose(36f, 23f, 0f), Pose(56f, 31f, 20f), Pose(80f, 43f, 0f))
    ),
    Maneuver(
        id = "hill_start",
        name = "Hill start",
        summary = "Move off on an uphill gradient without rolling back.",
        steps = listOf(
            step("Prepare", "Clutch down, select 1st gear, set slightly more gas than on the flat."),
            step("Find the bite", "Bring the clutch up slowly until the engine note drops and the bonnet lifts slightly."),
            step("Observe", "Check mirrors and blind spot. Signal only if it would help others."),
            step("Release and go", "Release the parking brake smoothly, adding gas as you lift the clutch. No roll-back.")
        ),
        faults = listOf("Rolling back", "Stalling", "Moving off without a blind-spot check"),
        scene = straightRoad,
        poses = listOf(Pose(18f, 43f, 0f), Pose(18f, 43f, 0f), Pose(18f, 43f, 0f), Pose(40f, 43f, 0f), Pose(80f, 43f, 0f))
    ),
    Maneuver(
        id = "emergency_stop",
        name = "Emergency stop",
        summary = "Stop quickly and under control when the examiner signals.",
        steps = listOf(
            step("React", "When the examiner signals, move your foot from the gas to the brake immediately."),
            step("Brake firmly", "Press the brake progressively but firmly. With ABS, keep pressing hard."),
            step("Clutch late", "Push the clutch down just before the car stops to avoid stalling."),
            step("Secure and observe", "Parking brake on, neutral. Check both blind spots before moving off again.")
        ),
        faults = listOf("Slow reaction", "Skidding / locking wheels without ABS", "Not checking blind spots afterwards"),
        scene = straightRoad,
        poses = listOf(Pose(8f, 43f, 0f), Pose(40f, 43f, 0f), Pose(54f, 43f, 0f), Pose(58f, 43f, 0f), Pose(58f, 43f, 0f))
    ),
    Maneuver(
        id = "angle_park",
        name = "Angle parking (45°)",
        summary = "Drive forward into an angled bay, used in many yard tests (UAE, Singapore, US).",
        steps = listOf(
            step("Approach slowly", "Keep about 1.5 m from the parked vehicles and signal your intention."),
            step("Turn in", "When your mirror passes the bay's first line, steer firmly into the bay."),
            step("Straighten", "Straighten as the car aligns with the bay lines."),
            step("Stop", "Stop before the end line. Secure the car.")
        ),
        faults = listOf("Crossing a bay line", "Turning in too early and clipping the neighbour"),
        scene = listOf(
            SceneItem.BayLine(40f, 34f, 54f, 56f), SceneItem.BayLine(54f, 34f, 68f, 56f),
            SceneItem.BayLine(68f, 34f, 82f, 56f), SceneItem.ParkedCar(48f, 45f, 57f)
        ),
        poses = listOf(Pose(20f, 20f, 0f), Pose(44f, 20f, 0f), Pose(56f, 30f, 45f), Pose(63f, 44f, 57f), Pose(65f, 47f, 57f))
    ),
    Maneuver(
        id = "garage_park",
        name = "Garage / box parking",
        summary = "Reverse into a three-sided box or garage marked with poles or walls (UAE, Saudi, India, Pakistan yards).",
        steps = listOf(
            step("Line up", "Drive past the garage opening, keeping about 1.5 m clearance."),
            step("Reference point", "Stop when the far pole of the opening lines up with your rear side window."),
            step("Reverse with full lock", "Reverse slowly at full lock while watching both mirrors."),
            step("Straighten inside", "Straighten when the car is parallel to the side walls; centre the car."),
            step("Stop without touching", "Stop before the back wall or line — touching a pole is usually an instant fail.")
        ),
        faults = listOf("Touching a pole or wall", "Stopping outside the box", "Excessive corrections"),
        scene = listOf(
            SceneItem.Cone(44f, 32f), SceneItem.Cone(44f, 44f), SceneItem.Cone(44f, 56f),
            SceneItem.Cone(60f, 32f), SceneItem.Cone(60f, 44f), SceneItem.Cone(60f, 56f),
            SceneItem.Cone(48f, 57f), SceneItem.Cone(52f, 57f), SceneItem.Cone(56f, 57f)
        ),
        poses = listOf(Pose(30f, 18f, 0f), Pose(64f, 18f, 0f), Pose(59f, 25f, -45f), Pose(53f, 36f, -85f), Pose(52f, 47f, -90f))
    ),
    Maneuver(
        id = "alley_dock",
        name = "Alley docking",
        summary = "Reverse into a narrow alley marked by poles — part of the South African K53 yard test.",
        steps = listOf(
            step("Pre-observation", "K53 requires full observation (mirrors and blind spots) before each movement."),
            step("Drive past", "Pass the alley entrance and stop with the car parallel to the line."),
            step("Reverse in", "Reverse slowly with full lock; keep checking both sides."),
            step("Align and stop", "Straighten in the alley and stop inside the marked area without touching a pole.")
        ),
        faults = listOf("Touching a pole", "Missing K53 observation steps", "More than the allowed number of shunts"),
        scene = listOf(
            SceneItem.Cone(46f, 30f), SceneItem.Cone(46f, 42f), SceneItem.Cone(46f, 54f),
            SceneItem.Cone(58f, 30f), SceneItem.Cone(58f, 42f), SceneItem.Cone(58f, 54f)
        ),
        poses = listOf(Pose(30f, 16f, 0f), Pose(64f, 16f, 0f), Pose(58f, 24f, -50f), Pose(53f, 36f, -88f), Pose(52f, 46f, -90f))
    ),
    Maneuver(
        id = "roundabout",
        name = "Roundabouts",
        summary = "Approach, give way correctly, choose the right lane and signal your exit.",
        steps = listOf(
            step("Read the signs early", "Pick your exit and lane from the advance sign."),
            step("MSPSL on approach", "Mirrors, Signal, Position, Speed, Look."),
            step("Give way", "Give way to traffic already on the roundabout (coming from the driver's side)."),
            step("Signal off", "Signal to exit after passing the exit before yours."),
            step("Exit in lane", "Check the near-side mirror for cyclists and leave in the correct lane.")
        ),
        faults = listOf("Not giving way", "Wrong lane", "No or misleading signals"),
        scene = listOf(SceneItem.Island(50f, 31f, 10f), SceneItem.Kerb(0f, 52f, 34f, 52f), SceneItem.Kerb(66f, 52f, 100f, 52f), SceneItem.Kerb(0f, 10f, 34f, 10f), SceneItem.Kerb(66f, 10f, 100f, 10f)),
        poses = pathPoses(
            listOf(Pair(4f, 40f), Pair(20f, 40f)) +
                listOf(150.0, 120.0, 90.0, 60.0, 30.0).map { deg ->
                    val t = Math.toRadians(deg)
                    Pair((50 + 17 * cos(t)).toFloat(), (31 + 17 * sin(t)).toFloat())
                } +
                listOf(Pair(80f, 40f), Pair(96f, 40f))
        )
    ),
    Maneuver(
        id = "lane_change",
        name = "Lane change & MSPSL",
        summary = "The core routine every examiner looks for: Mirrors, Signal, Position, Speed, Look.",
        steps = listOf(
            step("Mirrors", "Interior mirror, then the door mirror on the side you are moving to."),
            step("Signal", "Signal early enough to tell others what you plan."),
            step("Blind spot", "Quick shoulder check into the blind spot."),
            step("Move smoothly", "Change lane gradually, keeping your speed."),
            step("Cancel", "Cancel the signal once you are settled in the new lane.")
        ),
        faults = listOf("No blind-spot check", "Signal not cancelled", "Cutting in on another vehicle"),
        scene = listOf(SceneItem.Kerb(0f, 52f, 100f, 52f), SceneItem.Kerb(0f, 12f, 100f, 12f), SceneItem.LaneLine(0f, 32f, 100f, 32f), SceneItem.ParkedCar(84f, 42f, 0f)),
        poses = pathPoses(listOf(Pair(6f, 42f), Pair(26f, 42f), Pair(44f, 40f), Pair(58f, 34f), Pair(72f, 26f), Pair(94f, 22f))).let { p ->
            p.mapIndexed { i, pose -> if (i == p.lastIndex) pose.copy(angle = 0f) else pose }
        }
    ),
    Maneuver(
        id = "figure_eight",
        name = "Figure-8 track (two-wheeler)",
        summary = "Ride an '8' within the marked track without putting a foot down — standard in India and Pakistan two-wheeler tests.",
        steps = listOf(
            step("Low gear, steady throttle", "Use 1st/2nd gear with a little throttle; control speed with the rear brake."),
            step("Look ahead", "Look where you want to go — through the turn, not at the ground."),
            step("Lean, don't steer", "Lean gently into each loop; keep your knees against the tank."),
            step("Smooth crossover", "Straighten briefly at the crossover, then lean the other way."),
            step("Finish", "Complete the 8 without touching the boundary or putting a foot down.")
        ),
        faults = listOf("Putting a foot down", "Touching or crossing the boundary", "Stalling"),
        scene = listOf(SceneItem.Island(32f, 31f, 7f), SceneItem.Island(68f, 31f, 7f)),
        poses = pathPoses((0..16).map { i ->
            val t = i / 16.0 * 2 * Math.PI
            Pair((50 + 30 * sin(t)).toFloat(), (31 + 14 * sin(2 * t)).toFloat())
        }),
        isTwoWheeler = true
    ),
    Maneuver(
        id = "s_course",
        name = "S-course & crank course",
        summary = "Drive through a narrow S-bend and right-angle crank without touching the kerbs (Singapore, Malaysia, Japan-style circuits).",
        steps = listOf(
            step("Slow entry", "Enter at walking pace using clutch control or light brake."),
            step("Keep to the outside", "Position on the outside before each bend to give the rear wheels room."),
            step("Steer late, steer fully", "Turn when your shoulder passes the bend point."),
            step("Mirror checks", "Use door mirrors to watch the rear wheels against the kerb.")
        ),
        faults = listOf("Mounting the kerb", "Stopping repeatedly", "Excessive speed"),
        scene = listOf(SceneItem.Kerb(0f, 18f, 30f, 18f), SceneItem.Kerb(0f, 32f, 22f, 32f), SceneItem.Kerb(30f, 18f, 30f, 44f), SceneItem.Kerb(22f, 32f, 22f, 56f), SceneItem.Kerb(30f, 44f, 100f, 44f), SceneItem.Kerb(22f, 56f, 100f, 56f)),
        poses = pathPoses(listOf(Pair(4f, 25f), Pair(18f, 25f), Pair(26f, 32f), Pair(26f, 44f), Pair(34f, 50f), Pair(60f, 50f), Pair(92f, 50f)))
    ),
    Maneuver(
        id = "cone_slalom",
        name = "Cone slalom / zig-zag",
        summary = "Weave between cones in a smooth rhythm — used in several yard and two-wheeler tests.",
        steps = listOf(
            step("Set a slow, even speed", "Pick a speed you can hold through the whole course."),
            step("Look two cones ahead", "Plan the line early."),
            step("Smooth inputs", "Small steering inputs, no sudden braking."),
            step("Exit straight", "Straighten and stop at the end line.")
        ),
        faults = listOf("Hitting or missing a cone", "Stopping in the course"),
        scene = listOf(SceneItem.Cone(25f, 31f), SceneItem.Cone(45f, 31f), SceneItem.Cone(65f, 31f), SceneItem.Cone(85f, 31f)),
        poses = pathPoses(listOf(Pair(6f, 31f), Pair(15f, 22f), Pair(25f, 22f), Pair(35f, 40f), Pair(45f, 40f), Pair(55f, 22f), Pair(65f, 22f), Pair(75f, 40f), Pair(85f, 40f), Pair(96f, 31f)))
    )
).associateBy { it.id }
