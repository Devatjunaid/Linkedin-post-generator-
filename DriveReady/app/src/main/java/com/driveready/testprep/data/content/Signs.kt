package com.driveready.testprep.data.content

import com.driveready.testprep.data.model.Country
import com.driveready.testprep.data.model.DrivingSide
import com.driveready.testprep.data.model.SignGroup
import com.driveready.testprep.data.model.SignInfo
import com.driveready.testprep.data.model.SignType
import com.driveready.testprep.data.model.SpeedUnit

/** The sign library, adapted to the selected country's side of the road and speed unit. */
fun signsFor(country: Country): List<SignInfo> {
    val limit = if (country.speedUnit == SpeedUnit.MPH) "30" else "50"
    val unit = country.speedUnit.label
    val keep = if (country.drivingSide == DrivingSide.LEFT) SignType.KEEP_LEFT else SignType.KEEP_RIGHT
    val keepName = if (country.drivingSide == DrivingSide.LEFT) "Keep left" else "Keep right"
    val base = listOf(
        SignInfo(SignType.STOP, "Stop", "Come to a complete stop at the line and give way to all traffic.", SignGroup.ORDERS),
        SignInfo(SignType.GIVE_WAY, if (country.signStyle.name == "DIAMOND" && country.id != "ie") "Yield" else "Give way", "Slow down and give way to traffic on the major road; stop if necessary.", SignGroup.ORDERS),
        SignInfo(SignType.NO_ENTRY, "No entry", "No vehicles may enter — usually the exit of a one-way street.", SignGroup.ORDERS),
        SignInfo(SignType.SPEED_LIMIT, "Maximum $limit $unit", "Do not exceed the number shown.", SignGroup.ORDERS, limit),
        SignInfo(SignType.NO_U_TURN, "No U-turns", "U-turns are prohibited.", SignGroup.ORDERS),
        SignInfo(SignType.NO_OVERTAKING, "No overtaking", "Do not overtake until the restriction ends.", SignGroup.ORDERS),
        SignInfo(SignType.NO_PARKING, "No parking", "Parking is not allowed. Check plates for times.", SignGroup.ORDERS),
        SignInfo(SignType.NO_LEFT_TURN, "No left turn", "Left turns are prohibited.", SignGroup.ORDERS),
        SignInfo(keep, keepName, "Pass the obstruction on the side shown by the arrow.", SignGroup.ORDERS),
        SignInfo(SignType.MINI_ROUNDABOUT, "Mini roundabout / roundabout", "Give way as at a roundabout; circulate in the direction of the arrows.", SignGroup.ORDERS),
        SignInfo(SignType.ONE_WAY, "One-way traffic", "Traffic flows only in the direction of the arrow.", SignGroup.INFO),
        SignInfo(SignType.ROUNDABOUT_AHEAD, "Roundabout ahead", "Prepare to give way and choose your lane early.", SignGroup.WARNING),
        SignInfo(SignType.PEDESTRIAN_CROSSING, "Pedestrian crossing ahead", "Slow down and be ready to stop for pedestrians.", SignGroup.WARNING),
        SignInfo(SignType.CHILDREN, "Children / school", "Children may be crossing — reduce speed.", SignGroup.WARNING),
        SignInfo(SignType.SLIPPERY_ROAD, "Slippery road", "Road may be slippery when wet or icy.", SignGroup.WARNING),
        SignInfo(SignType.TRAFFIC_SIGNALS_AHEAD, "Traffic signals ahead", "Be ready to stop at the lights.", SignGroup.WARNING),
        SignInfo(SignType.RAILWAY_CROSSING, "Level crossing", "Railway crossing ahead; obey barriers and lights.", SignGroup.WARNING),
        SignInfo(SignType.TWO_WAY_TRAFFIC, "Two-way traffic", "Oncoming traffic ahead.", SignGroup.WARNING),
        SignInfo(SignType.ROAD_WORKS, "Road works", "Workers and equipment ahead; obey temporary limits.", SignGroup.WARNING),
        SignInfo(SignType.SHARP_BEND, "Sharp bend", "Slow down before the bend.", SignGroup.WARNING),
        SignInfo(SignType.HOSPITAL, "Hospital", "Hospital ahead — keep noise down.", SignGroup.INFO),
        SignInfo(SignType.PARKING, "Parking", "Parking place or car park.", SignGroup.INFO)
    )
    val extra = if (country.id == "uk") listOf(
        SignInfo(SignType.NATIONAL_SPEED_LIMIT, "National speed limit", "60 mph single carriageway, 70 mph dual carriageway/motorway for cars.", SignGroup.ORDERS)
    ) else emptyList()
    return base + extra
}
