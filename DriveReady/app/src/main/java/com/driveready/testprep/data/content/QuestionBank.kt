package com.driveready.testprep.data.content

import com.driveready.testprep.data.model.DrivingSide
import com.driveready.testprep.data.model.Question
import com.driveready.testprep.data.model.QuestionCategory
import com.driveready.testprep.data.model.QuestionCategory.ALCOHOL
import com.driveready.testprep.data.model.QuestionCategory.EMERGENCY
import com.driveready.testprep.data.model.QuestionCategory.HAZARD
import com.driveready.testprep.data.model.QuestionCategory.LEGAL
import com.driveready.testprep.data.model.QuestionCategory.MOTORWAY
import com.driveready.testprep.data.model.QuestionCategory.RULES
import com.driveready.testprep.data.model.QuestionCategory.SAFETY
import com.driveready.testprep.data.model.QuestionCategory.SIGNS
import com.driveready.testprep.data.model.QuestionCategory.VULNERABLE
import com.driveready.testprep.data.model.SignType
import com.driveready.testprep.data.model.SpeedUnit

private var counter = 0

private fun q(
    category: QuestionCategory,
    text: String,
    options: List<String>,
    correct: Int,
    explanation: String,
    sign: SignType? = null,
    signValue: String? = null,
    countries: Set<String> = emptySet(),
    side: DrivingSide? = null,
    unit: SpeedUnit? = null
) = Question(
    id = "q${counter++}",
    category = category,
    text = text,
    options = options,
    correctIndex = correct,
    explanation = explanation,
    sign = sign,
    signValue = signValue,
    countries = countries,
    drivingSide = side,
    speedUnit = unit
)

/** Universal questions: road-safety principles shared by every licensing system. */
private val universal = listOf(
    q(SIGNS, "What does this sign mean?", listOf("Stop completely and give way", "Slow down and proceed", "Stop only if traffic is coming", "No stopping"), 0,
        "An octagonal STOP sign means you must come to a complete stop at the line, then give way to all traffic before moving.", SignType.STOP),
    q(SIGNS, "What must you do at this sign?", listOf("Give way to traffic on the major road", "Stop in all cases", "Speed up to merge", "Sound your horn"), 0,
        "A downward-pointing triangle means give way (yield). Slow down and stop only if necessary to let traffic on the main road pass.", SignType.GIVE_WAY),
    q(SIGNS, "What does this sign mean?", listOf("No entry for vehicular traffic", "One-way street ahead", "Road closed to pedestrians", "Parking only"), 0,
        "A red circle with a white horizontal bar means no entry — usually the exit of a one-way street.", SignType.NO_ENTRY),
    q(SIGNS, "What does this sign mean?", listOf("No U-turns", "U-turn lane ahead", "Turn around permitted", "Dead end"), 0,
        "A U-turn arrow crossed out means U-turns are prohibited.", SignType.NO_U_TURN),
    q(SIGNS, "What does this sign mean?", listOf("No overtaking", "Two-way traffic", "Overtaking lane ahead", "Lane closed"), 0,
        "This sign prohibits overtaking until the restriction ends.", SignType.NO_OVERTAKING),
    q(SIGNS, "What does this sign mean?", listOf("No parking", "Parking area", "No entry", "Clearway ends"), 0,
        "A 'P' crossed out means no parking. Stopping briefly may still be allowed depending on local rules.", SignType.NO_PARKING),
    q(SIGNS, "What does this sign warn you about?", listOf("Pedestrian crossing ahead", "Pedestrian zone", "No pedestrians", "School bus stop"), 0,
        "Slow down and be ready to stop for people crossing.", SignType.PEDESTRIAN_CROSSING),
    q(SIGNS, "What does this sign warn you about?", listOf("Children likely ahead — school or playground", "Pedestrian crossing only", "No children allowed", "Bus stop"), 0,
        "Expect children near schools and playgrounds. Reduce speed and watch for sudden movement.", SignType.CHILDREN),
    q(SIGNS, "What does this sign warn you about?", listOf("Slippery road", "Winding road", "Loose chippings only", "Uneven surface"), 0,
        "The skidding car shows the road may be slippery, especially when wet or icy. Reduce speed and avoid harsh braking.", SignType.SLIPPERY_ROAD),
    q(SIGNS, "What does this sign warn you about?", listOf("Traffic signals ahead", "Level crossing", "Police control", "Signals not in use"), 0,
        "Traffic lights are ahead — be ready to stop.", SignType.TRAFFIC_SIGNALS_AHEAD),
    q(SIGNS, "What does this sign warn you about?", listOf("Railway level crossing ahead", "Tram stop", "Bridge ahead", "Toll booth"), 0,
        "A level crossing is ahead. Never stop on the tracks and obey any barriers or flashing lights.", SignType.RAILWAY_CROSSING),
    q(SIGNS, "What does this sign warn you about?", listOf("Two-way traffic ahead", "One-way street", "Dual carriageway begins", "Overtaking allowed"), 0,
        "You are leaving a one-way section or dual carriageway; expect oncoming traffic.", SignType.TWO_WAY_TRAFFIC),
    q(SIGNS, "What does this sign warn you about?", listOf("Road works ahead", "Men at work in a factory", "Road closed", "Diversion"), 0,
        "Road works ahead — slow down, obey temporary limits and watch for workers.", SignType.ROAD_WORKS),
    q(SIGNS, "What does this sign warn you about?", listOf("Sharp bend ahead", "Turn now", "One-way traffic", "Lane ends"), 0,
        "Reduce speed before the bend, not in it.", SignType.SHARP_BEND),
    q(SIGNS, "What does this sign warn you about?", listOf("Roundabout ahead", "Mini roundabout", "U-turn area", "Circular route"), 0,
        "Prepare early: choose your lane and be ready to give way.", SignType.ROUNDABOUT_AHEAD),
    q(SIGNS, "What does this sign mean?", listOf("No left turn", "Left turn only", "Keep left", "Left lane closed"), 0,
        "A crossed-out arrow prohibits that turn.", SignType.NO_LEFT_TURN),
    q(SIGNS, "What does this sign show?", listOf("Hospital ahead", "First-aid post only", "Ambulance station", "Red Cross office"), 0,
        "Hospital ahead — keep noise down and watch for emergency vehicles.", SignType.HOSPITAL),

    q(RULES, "When are you allowed to proceed on an amber (yellow) light after it was green?", listOf("Only if you are so close to the line that stopping might cause a crash", "Whenever no police are around", "Always — amber means go faster", "Only when turning"), 0,
        "Amber means stop, unless you have already crossed the line or are so close that stopping could cause a collision."),
    q(RULES, "Two vehicles arrive at an all-way stop at the same time. Who goes first?", listOf("Follow the local priority rule (usually the vehicle on the right in right-hand traffic)", "The larger vehicle", "The faster vehicle", "Whoever sounds the horn"), 0,
        "When arrival is simultaneous, a fixed priority rule applies. Make eye contact and never assume."),
    q(RULES, "What is the safest minimum following distance in good conditions?", listOf("At least a 2–3 second gap", "One car length", "Half a second", "Close enough to read their number plate"), 0,
        "Use a fixed point and count seconds. Double the gap (or more) in rain, ice or poor visibility."),
    q(RULES, "In wet weather your stopping distance can be…", listOf("At least double", "The same", "Halved", "Slightly shorter because tyres grip better"), 0,
        "Wet roads reduce grip — allow at least twice the dry gap. On ice it can be ten times longer."),
    q(RULES, "When should you use your horn?", listOf("To warn others of your presence when there is danger", "To greet a friend", "To tell others you have priority", "When annoyed at slow drivers"), 0,
        "The horn is a warning device only."),
    q(RULES, "An emergency vehicle with sirens approaches from behind. What should you do?", listOf("Pull over safely and let it pass without breaking the law", "Speed up to stay ahead", "Stop immediately wherever you are", "Ignore it"), 0,
        "Move aside safely and predictably; don't run red lights or mount pavements dangerously."),
    q(RULES, "Where is overtaking most dangerous?", listOf("Approaching a bend, hill crest or junction", "On a long straight with clear visibility", "On a dual carriageway", "When the other car signals to let you"), 0,
        "You cannot see oncoming traffic near bends, crests and junctions."),
    q(RULES, "What does a flashing red traffic signal usually mean?", listOf("Stop, then proceed when it is safe (treat as a STOP sign)", "Go faster", "Signal is broken — ignore it", "Pedestrians only"), 0,
        "A flashing red light is treated like a STOP sign in most countries; at level crossings it means you must stop and wait."),
    q(RULES, "When is it acceptable to cross a solid centre line?", listOf("Only where the law allows, e.g. to pass a stationary obstruction when safe", "Whenever you want to overtake", "At night only", "Never under any circumstances"), 0,
        "Solid lines mean do not cross except in the limited situations your local code permits."),

    q(SAFETY, "What is the legal minimum tyre tread depth in most countries for cars?", listOf("1.6 mm", "0.5 mm", "3 mm", "There is no minimum"), 0,
        "1.6 mm is the minimum in the UK, EU, Australia and many others; some US states allow 2/32\" (≈1.6 mm)."),
    q(SAFETY, "How should a head restraint be adjusted?", listOf("Rigid part at least level with your eyes or top of ears, close to the head", "As low as possible", "Removed for comfort", "Tilted forwards onto the head"), 0,
        "A correctly set head restraint reduces whiplash injuries."),
    q(SAFETY, "When should tyre pressures be checked?", listOf("When the tyres are cold, using a reliable gauge", "Straight after a long motorway drive", "Only at the annual inspection", "When the car pulls to one side"), 0,
        "Warm tyres give a falsely high reading."),
    q(SAFETY, "Your ABS warning light stays on while driving. What does it mean?", listOf("There is a fault with the anti-lock braking system", "The handbrake is on", "Fuel is low", "The lights are on"), 0,
        "Normal brakes usually still work, but have the ABS checked as soon as possible."),
    q(SAFETY, "Why should you keep your seatbelt fastened even on short trips?", listOf("Most crashes happen close to home and belts prevent ejection and severe injury", "It is only needed on highways", "It keeps your clothes neat", "Airbags make belts unnecessary"), 0,
        "Airbags are designed to work with seatbelts, not instead of them."),
    q(SAFETY, "Where should a rear-facing child seat never be fitted?", listOf("In front of an active passenger airbag", "In the rear seat", "Behind the driver", "In a car with ISOFIX"), 0,
        "An inflating airbag can seriously injure a baby in a rear-facing seat."),
    q(SAFETY, "What causes most skids?", listOf("Harsh braking, steering or acceleration for the conditions", "Old wiper blades", "Low fuel", "Driving with lights on"), 0,
        "Smooth inputs and suitable speed prevent skids."),

    q(HAZARD, "A ball bounces into the road ahead from between parked cars. What should you expect?", listOf("A child may run out after it", "Nothing — just avoid the ball", "A dog show nearby", "The road is closed"), 0,
        "This is a classic developing hazard. Slow down and be ready to stop."),
    q(HAZARD, "You see brake lights several cars ahead. What should you do?", listOf("Ease off and be ready to slow down", "Overtake quickly", "Sound your horn", "Keep your speed until the car in front brakes"), 0,
        "Looking well ahead gives you more time to react smoothly."),
    q(HAZARD, "A car waiting at a side road is creeping forward. What is the hazard?", listOf("It may pull out in front of you", "It is parking", "It has broken down", "There is none"), 0,
        "Cover your brake and watch the driver and wheels for movement."),
    q(HAZARD, "An ice-cream van is stopped on your side of the road. What should you do?", listOf("Slow down; children may cross without looking", "Speed past", "Flash your lights", "Overtake on the pavement side"), 0,
        "Children's attention is on the van, not traffic."),
    q(HAZARD, "Why are motorcyclists hard to see at junctions?", listOf("Their narrow profile can be hidden by pillars and other vehicles", "They always ride at night", "They have no lights", "They are always speeding"), 0,
        "Look twice for bikes — check carefully around windscreen pillars."),

    q(VULNERABLE, "How much space should you leave when passing a cyclist?", listOf("As much as a car — at least 1–1.5 m, more at higher speed", "30 cm", "Just enough to avoid touching", "No space is needed"), 0,
        "Many countries now set a legal minimum of 1 m in towns and 1.5 m at higher speeds."),
    q(VULNERABLE, "A pedestrian with a white cane is waiting to cross. What does it mean?", listOf("They may be blind or partially sighted — give them extra time", "They are a road-works official", "They want a lift", "Nothing special"), 0,
        "A white cane (red-and-white for deaf-blind) signals visual impairment."),
    q(VULNERABLE, "You are turning into a side road and pedestrians are already crossing it. What should you do?", listOf("Give way to them", "Sound your horn so they hurry", "Drive around them", "They must wait for you"), 0,
        "Pedestrians already crossing have priority in almost every road code."),
    q(VULNERABLE, "Horse riders are ahead on a narrow road. How should you pass?", listOf("Slowly and wide, without revving or sounding the horn", "Quickly to minimise time near them", "Close, then brake", "Flash lights to warn them"), 0,
        "Horses can be startled; pass at walking pace, giving plenty of room."),
    q(VULNERABLE, "Older drivers and learner drivers ahead may…", listOf("React more slowly — be patient and leave extra room", "Always drive too fast", "Have priority everywhere", "Never use mirrors"), 0,
        "Anticipate hesitation and avoid pressuring them."),

    q(MOTORWAY, "What should you do when joining a motorway/highway from a slip road?", listOf("Match the traffic speed and merge into a safe gap", "Stop at the end of the slip road", "Force your way in", "Drive on the hard shoulder first"), 0,
        "Use the acceleration lane to build speed and merge smoothly."),
    q(MOTORWAY, "You miss your exit on a motorway. What should you do?", listOf("Continue to the next exit", "Reverse along the hard shoulder", "Make a U-turn through the central reservation", "Stop and wait"), 0,
        "Reversing or turning on a motorway is extremely dangerous and illegal."),
    q(MOTORWAY, "Which lane should you normally use on a multi-lane motorway?", listOf("The lane nearest the kerb side, using others only to overtake", "The fastest lane", "Any lane you like", "The middle lane always"), 0,
        "Keep to the kerb-side lane unless overtaking; then move back when safe."),

    q(ALCOHOL, "What is the only way to remove alcohol from your body?", listOf("Time", "Black coffee", "A cold shower", "Eating a big meal"), 0,
        "The liver processes alcohol at a fixed rate; nothing speeds it up."),
    q(ALCOHOL, "You feel drowsy on a long trip. What is the best action?", listOf("Stop somewhere safe, take a short nap and/or a caffeinated drink", "Open the window and continue", "Turn up the music", "Drive faster to get there sooner"), 0,
        "Tiredness causes many fatal crashes. Plan a break every 2 hours."),
    q(ALCOHOL, "Some cold and allergy medicines can…", listOf("Cause drowsiness and impair driving", "Improve concentration", "Have no effect on driving", "Make alcohol safer"), 0,
        "Always read the label; driving while impaired by medicine can be an offence."),

    q(EMERGENCY, "You arrive first at a crash. What should you do first?", listOf("Make the scene safe — hazard lights, warn traffic, call emergency services", "Move all injured people immediately", "Give injured people something to drink", "Drive on"), 0,
        "Protect the scene, call for help, then give first aid if trained. Don't move casualties unless there is immediate danger."),
    q(EMERGENCY, "Your car breaks down on a busy road. What should you do?", listOf("Pull as far off the road as possible, use hazard lights and get passengers to a safe place", "Stay in the car with seatbelts off", "Try to fix it in the live lane", "Walk along the carriageway for help"), 0,
        "Your safety comes first; use a warning triangle only where it is safe to place one."),
    q(EMERGENCY, "Your engine overheats. What should you do first?", listOf("Pull over safely and switch off; let it cool before opening the coolant cap", "Open the radiator cap immediately", "Pour cold water on the engine", "Keep driving to the garage"), 0,
        "Opening a hot pressurised system can cause severe burns."),
    q(EMERGENCY, "A tyre bursts while you are driving. What should you do?", listOf("Hold the steering firmly, ease off the gas and slow down gradually", "Brake hard immediately", "Steer sharply to the side", "Accelerate"), 0,
        "Harsh braking or steering can cause loss of control."),

    q(LEGAL, "Which documents should you normally be able to show when driving?", listOf("Driving licence, vehicle registration and valid insurance", "Passport only", "Your birth certificate", "Nothing is required"), 0,
        "Exact requirements vary, but licence, registration and insurance are needed almost everywhere."),
    q(LEGAL, "Using a hand-held mobile phone while driving is…", listOf("Illegal in most countries and seriously distracting", "Allowed at traffic lights everywhere", "Safe if you use speaker mode while holding it", "Only illegal at night"), 0,
        "Even hands-free calls reduce attention. Park safely before using a phone.")
)

private val leftSideRules = listOf(
    q(RULES, "At a roundabout, you should normally give way to traffic coming from…", listOf("Your right", "Your left", "Behind you", "Straight ahead"), 0,
        "In left-hand traffic countries traffic circulates clockwise, so give way to traffic from the right already on the roundabout.", side = DrivingSide.LEFT),
    q(RULES, "On a road with two lanes in your direction, you should normally drive in…", listOf("The left lane, using the right lane to overtake", "The right lane", "Straddling both lanes", "Whichever lane is emptier"), 0,
        "Keep left unless overtaking.", side = DrivingSide.LEFT),
    q(SIGNS, "What does this sign mean?", listOf("Keep left of the obstruction", "Turn left ahead", "Left lane ends", "Left turn prohibited"), 0,
        "Pass on the side shown by the arrow, usually around a traffic island.", SignType.KEEP_LEFT, side = DrivingSide.LEFT)
)

private val rightSideRules = listOf(
    q(RULES, "At a roundabout, you should normally give way to traffic…", listOf("Already on the roundabout, coming from your left", "Coming from your right", "Behind you", "Waiting to enter"), 0,
        "In right-hand traffic countries traffic circulates anticlockwise; yield to vehicles already on the roundabout.", side = DrivingSide.RIGHT),
    q(RULES, "On a multi-lane road, slower traffic should keep…", listOf("To the right, overtaking on the left", "To the left", "In the centre", "Anywhere"), 0,
        "Keep right except to pass.", side = DrivingSide.RIGHT),
    q(SIGNS, "What does this sign mean?", listOf("Keep right of the obstruction", "Turn right ahead", "Right lane ends", "No right turn"), 0,
        "Pass on the side shown by the arrow.", SignType.KEEP_RIGHT, side = DrivingSide.RIGHT)
)

private val speedSigns = listOf(
    q(SIGNS, "What does this sign mean?", listOf("Maximum speed 30 mph", "Minimum speed 30 mph", "Recommended speed 30 mph", "End of 30 mph zone"), 0,
        "A number in a red ring is a maximum limit. A blue circle with a number is a minimum.", SignType.SPEED_LIMIT, "30", unit = SpeedUnit.MPH),
    q(SIGNS, "What does this sign mean?", listOf("Maximum speed 50 km/h", "Minimum speed 50 km/h", "Road number 50", "Distance 50 km"), 0,
        "A number inside a red ring is the maximum speed limit in km/h.", SignType.SPEED_LIMIT, "50", unit = SpeedUnit.KMH)
)

private fun c(vararg ids: String) = ids.toSet()

/** Country-specific questions, written from each country's current code. */
private val countrySpecific = listOf(
    // United Kingdom
    q(SIGNS, "What does this sign mean?", listOf("National speed limit applies", "No vehicles", "End of all restrictions", "Clearway"), 0,
        "White circle with a black diagonal band: national speed limit (60 mph single carriageway, 70 mph dual carriageway/motorway for cars).", SignType.NATIONAL_SPEED_LIMIT, countries = c("uk")),
    q(RULES, "What is the speed limit for a car on a single carriageway with the national speed limit sign?", listOf("60 mph", "50 mph", "70 mph", "40 mph"), 0,
        "Cars: 30 mph built-up, 60 mph single carriageway, 70 mph dual carriageway and motorway.", countries = c("uk")),
    q(RULES, "What is the typical stopping distance at 70 mph in good conditions (Highway Code)?", listOf("96 metres (315 feet)", "53 metres", "36 metres", "150 metres"), 0,
        "21 m thinking + 75 m braking = 96 m, about 24 car lengths.", countries = c("uk")),
    q(MOTORWAY, "A red X is shown above your lane on a smart motorway. What does it mean?", listOf("Do not drive in that lane", "Lane for emergency vehicles only, you may use it slowly", "Speed camera ahead", "Road works end"), 0,
        "Driving under a red X is illegal and dangerous — there may be a stopped vehicle ahead.", countries = c("uk")),
    q(LEGAL, "How many penalty points will cancel a new driver's licence within 2 years of passing?", listOf("6", "12", "3", "9"), 0,
        "Under the New Drivers Act, 6 or more points within 2 years of passing revokes your licence.", countries = c("uk")),
    q(VULNERABLE, "Under the Highway Code hierarchy of road users, who has the greatest responsibility to reduce danger?", listOf("Those driving vehicles that can cause the greatest harm", "Pedestrians", "Cyclists", "Everyone equally"), 0,
        "Rule H1: drivers of larger vehicles carry the greatest responsibility.", countries = c("uk")),

    // United States
    q(RULES, "A school bus ahead has stopped with flashing red lights and its stop arm out. What must you do on an undivided road?", listOf("Stop, whichever direction you are travelling, until the lights stop flashing", "Pass slowly", "Stop only if you are behind it", "Sound your horn and pass"), 0,
        "All US states require traffic in both directions to stop on undivided roads.", countries = c("us")),
    q(ALCOHOL, "What is the legal blood alcohol limit for drivers 21 and over in most US states?", listOf("0.08%", "0.05%", "0.10%", "0.02%"), 0,
        "0.08% in every state except Utah (0.05%). Drivers under 21 face zero-tolerance limits (0.00–0.02%).", countries = c("us")),
    q(RULES, "Unless a sign prohibits it, can you turn right on a red light?", listOf("Yes, after a complete stop and yielding to pedestrians and traffic", "Never", "Yes, without stopping", "Only at night"), 0,
        "Right on red is permitted in all states unless posted otherwise (NYC is a notable exception).", countries = c("us")),
    q(RULES, "How far before a turn should you start signalling in most states?", listOf("At least 100 feet", "10 feet", "Only as you turn", "1 mile"), 0,
        "Most handbooks require at least 100 ft in town (some require more on highways).", countries = c("us")),
    q(RULES, "What does a 'Move Over' law require when you approach a stopped emergency vehicle with lights flashing?", listOf("Move over a lane if safe, or slow down significantly", "Stop in your lane", "Speed up to pass quickly", "Flash your headlights"), 0,
        "All 50 states have move-over laws protecting responders on the roadside.", countries = c("us")),
    q(SIGNS, "What does a diamond-shaped yellow sign indicate?", listOf("A warning of a possible hazard ahead", "A regulation you must obey", "A service area", "A school zone only"), 0,
        "In the US, yellow diamonds warn; white rectangles regulate; green guides.", SignType.SHARP_BEND, countries = c("us", "ca", "au", "nz")),

    // Canada
    q(ALCOHOL, "In Ontario, what blood alcohol level is allowed for G1 and G2 drivers?", listOf("Zero", "0.05%", "0.08%", "0.02%"), 0,
        "Novice (G1/G2) drivers and all drivers 21 and under must have a BAC of zero.", countries = c("ca")),
    q(LEGAL, "Who may accompany an Ontario G1 driver?", listOf("A fully licensed (G) driver with at least 4 years of experience", "Any adult", "Any G2 driver", "No one — G1 drivers drive alone"), 0,
        "The accompanying driver must also have a BAC below 0.05% and sit in the front passenger seat.", countries = c("ca")),
    q(VULNERABLE, "In Ontario, what is the minimum distance when passing a cyclist where practical?", listOf("1 metre", "0.5 metre", "2 metres", "No minimum"), 0,
        "The Highway Traffic Act requires at least 1 m when passing cyclists, where practical.", countries = c("ca")),

    // Australia
    q(ALCOHOL, "What alcohol limit applies to learner and provisional drivers in NSW?", listOf("Zero", "0.05", "0.02", "0.08"), 0,
        "Learner and P-plate drivers must have zero blood alcohol.", countries = c("au")),
    q(RULES, "What is the default speed limit in a built-up area in Australia unless signed otherwise?", listOf("50 km/h", "60 km/h", "40 km/h", "70 km/h"), 0,
        "The default urban limit is 50 km/h. School zones are usually 40 km/h during set times.", countries = c("au")),
    q(RULES, "What is the maximum speed for a learner driver in NSW?", listOf("90 km/h", "80 km/h", "100 km/h", "110 km/h"), 0,
        "NSW learners must not exceed 90 km/h, even where the limit is higher.", countries = c("au")),
    q(RULES, "At a T-intersection without signs, who gives way?", listOf("The vehicle on the road that ends (the terminating road)", "The vehicle on the right", "The faster vehicle", "Nobody"), 0,
        "Vehicles on the terminating road give way to all vehicles on the continuing road.", countries = c("au")),

    // India
    q(LEGAL, "Which documents must you carry while driving in India?", listOf("Driving licence, RC, insurance certificate and PUC certificate", "Only Aadhaar", "Only the RC", "Passport"), 0,
        "Digital copies in DigiLocker or mParivahan are accepted.", countries = c("in")),
    q(ALCOHOL, "What is the legal blood alcohol limit in India?", listOf("30 mg per 100 ml of blood", "80 mg per 100 ml", "50 mg per 100 ml", "Zero"), 0,
        "The Motor Vehicles Act sets 30 mg/100 ml; penalties increased sharply under the 2019 amendment.", countries = c("in")),
    q(LEGAL, "After getting a learner's licence, how soon can you apply for a permanent driving licence?", listOf("After 30 days", "Immediately", "After 1 year", "After 6 months only"), 0,
        "You can apply after 30 days and within the learner licence's 6-month validity.", countries = c("in")),
    q(SAFETY, "On a two-wheeler in India, who must wear a helmet?", listOf("Both the rider and the pillion rider", "Only the rider", "Only on highways", "Only children"), 0,
        "Helmets are compulsory for rider and pillion (Sikh men wearing turbans are exempt in many states).", countries = c("in")),
    q(RULES, "When the traffic light shows green but a traffic police officer signals you to stop, you should…", listOf("Obey the police officer", "Follow the light", "Sound the horn", "Go slowly"), 0,
        "Signals from an authorised officer override traffic lights and signs.", countries = c("in", "pk")),

    // Pakistan
    q(LEGAL, "In Punjab, what must you do after passing the e-sign test?", listOf("Take the road test within the allowed window, or retake the sign test", "Wait one year", "Nothing — you get a licence", "Pay a fine"), 0,
        "Since September 2026, Punjab requires the road test within 24 hours of passing the e-sign test.", countries = c("pk")),
    q(RULES, "Near hospitals and schools you should…", listOf("Avoid using the horn and drive slowly", "Use the horn to warn pedestrians", "Speed up to clear the area", "Park on the footpath"), 0,
        "Horn-free zones protect patients and students.", countries = c("pk", "in")),
    q(SAFETY, "Who must wear a seatbelt on motorways in Pakistan?", listOf("The driver and all passengers", "Only the driver", "Only front passengers", "Nobody"), 0,
        "Seatbelts are strictly enforced by the National Highways & Motorway Police.", countries = c("pk")),

    // UAE
    q(SAFETY, "In the UAE, children under what age or height must not sit in the front seat?", listOf("Under 10 years or shorter than 145 cm", "Under 5 years", "Under 16 years", "There is no rule"), 0,
        "Federal traffic law also requires child seats for children up to 4 years old.", countries = c("ae")),
    q(ALCOHOL, "What is the alcohol limit for drivers in the UAE?", listOf("Zero tolerance", "0.05%", "0.08%", "0.02%"), 0,
        "Any detectable alcohol while driving is an offence.", countries = c("ae", "sa", "qa")),
    q(RULES, "In Dubai, what must you do when entering a roundabout?", listOf("Give way to vehicles already on the roundabout", "Accelerate into the first gap", "Use the hazard lights", "Stop in the middle"), 0,
        "Enter only when the way is clear and use the correct lane for your exit.", countries = c("ae")),

    // Saudi Arabia
    q(LEGAL, "Which platform is used to book driving-licence services in Saudi Arabia?", listOf("Absher", "Tawakkalna only", "Nafath only", "Etimad"), 0,
        "Applicants book through Absher and train at an authorised school such as Dallah.", countries = c("sa")),
    q(LEGAL, "What is the Saher system?", listOf("An automated traffic-violation monitoring network", "A driving school", "A parking app", "A toll system"), 0,
        "Saher cameras record speeding and red-light violations automatically.", countries = c("sa")),

    // Germany
    q(RULES, "At an unmarked junction in Germany, who has priority?", listOf("Traffic coming from the right (rechts vor links)", "Traffic coming from the left", "The larger vehicle", "The faster vehicle"), 0,
        "Unless signs or signals say otherwise, give way to vehicles from the right.", countries = c("de")),
    q(MOTORWAY, "On a German Autobahn with no speed limit sign, what is the advisory speed?", listOf("130 km/h (Richtgeschwindigkeit)", "100 km/h", "No guidance exists", "160 km/h"), 0,
        "130 km/h is recommended; exceeding it can affect liability in a crash.", countries = c("de")),
    q(MOTORWAY, "Traffic on the Autobahn is backing up. How do you form an emergency corridor (Rettungsgasse)?", listOf("Leftmost lane moves left; all other lanes move right", "Everyone moves right", "Everyone moves left", "Stop in the middle"), 0,
        "Form the corridor as soon as traffic slows, not when sirens are heard.", countries = c("de")),
    q(ALCOHOL, "What alcohol limit applies during the probationary period (Probezeit) in Germany?", listOf("0.0 ‰", "0.5 ‰", "0.3 ‰", "0.8 ‰"), 0,
        "Novice drivers and everyone under 21 must not drink and drive at all.", countries = c("de")),

    // France
    q(RULES, "What does 'priorité à droite' mean?", listOf("At junctions without signs, give way to traffic from the right", "Overtake on the right", "Keep right on motorways", "Right turn on red allowed"), 0,
        "It applies unless signs give you priority.", countries = c("fr")),
    q(MOTORWAY, "What is the motorway speed limit in France in rain?", listOf("110 km/h", "130 km/h", "90 km/h", "100 km/h"), 0,
        "130 km/h in dry weather, reduced to 110 km/h when it rains.", countries = c("fr")),
    q(ALCOHOL, "What is the blood alcohol limit for probationary licence holders in France?", listOf("0.2 g/l", "0.5 g/l", "0.8 g/l", "Zero is not enforced"), 0,
        "0.5 g/l for experienced drivers, 0.2 g/l during the probationary period.", countries = c("fr")),

    // Ireland
    q(LEGAL, "What must a learner permit holder in Ireland do when driving?", listOf("Display L-plates and be accompanied by someone who has held a full licence for at least 2 years", "Drive only at night", "Drive alone after 6 months", "Display N-plates"), 0,
        "N-plates are for the first 2 years after passing the test.", countries = c("ie")),
    q(RULES, "What is the default speed limit on Irish motorways?", listOf("120 km/h", "100 km/h", "130 km/h", "110 km/h"), 0,
        "Motorways 120, national roads 100, regional/local 80 (lower where signed), built-up 50 km/h.", countries = c("ie")),

    // New Zealand
    q(LEGAL, "Who can supervise a learner driver in New Zealand?", listOf("Someone who has held a full licence for at least 2 years", "Any adult", "A restricted licence holder", "No one"), 0,
        "The supervisor must sit in the front passenger seat.", countries = c("nz")),
    q(ALCOHOL, "What alcohol limit applies to drivers under 20 in New Zealand?", listOf("Zero", "50 mg/100 ml", "80 mg/100 ml", "20 mg/100 ml"), 0,
        "There is a zero alcohol limit for drivers under 20.", countries = c("nz")),

    // South Africa
    q(LEGAL, "How long is a South African learner's licence valid?", listOf("24 months", "6 months", "12 months", "5 years"), 0,
        "You must pass the K53 driving test before it expires.", countries = c("za")),
    q(RULES, "What is the general speed limit on a South African freeway?", listOf("120 km/h", "100 km/h", "80 km/h", "140 km/h"), 0,
        "Urban 60 km/h, rural roads 100 km/h, freeways 120 km/h unless signed.", countries = c("za")),

    // Singapore
    q(LEGAL, "In Singapore, which theory test must you pass before the Final Theory Test?", listOf("Basic Theory Test (BTT)", "Riding Theory Test", "Hazard Perception only", "None"), 0,
        "Both the BTT and FTT need 45/50 to pass.", countries = c("sg")),
    q(LEGAL, "What happens if a probationary driver in Singapore accumulates 13 or more demerit points?", listOf("The licence is revoked", "A warning letter", "Nothing", "A fine only"), 0,
        "New drivers are on probation for 1 year and must display a P-plate.", countries = c("sg")),

    // Malaysia
    q(LEGAL, "How many correct answers out of 50 are needed to pass Malaysia's KPP01 computer test?", listOf("42", "35", "45", "40"), 0,
        "The KPP01 test has 50 questions in 45 minutes.", countries = c("my")),
    q(LEGAL, "What licence do new drivers in Malaysia receive after passing?", listOf("A probationary (P) licence, valid for 2 years", "A full licence immediately", "A learner licence", "An international licence"), 0,
        "P licence holders must display a 'P' plate.", countries = c("my")),

    // Philippines
    q(LEGAL, "How many hours is the mandatory Theoretical Driving Course (TDC) in the Philippines?", listOf("15 hours", "8 hours", "4 hours", "40 hours"), 0,
        "A 15-hour TDC is required for the student permit; an 8-hour Practical Driving Course is required before the licence.", countries = c("ph")),
    q(SAFETY, "Under the Philippine Child Safety in Motor Vehicles Act, children of what age or height may not sit in the front seat?", listOf("12 and below, or shorter than 150 cm", "5 and below", "16 and below", "There is no rule"), 0,
        "Smaller children must use an appropriate child restraint in the rear.", countries = c("ph")),

    // Qatar
    q(LEGAL, "In Qatar, which authority runs the driving licence process?", listOf("The Traffic Department (Ministry of Interior)", "Ashghal", "Kahramaa", "Hamad Port"), 0,
        "Training happens at a licensed school such as Karwa or Al Khebra before the official tests.", countries = c("qa"))
)

val QuestionBank: List<Question> = universal + leftSideRules + rightSideRules + speedSigns + countrySpecific

fun questionsFor(countryId: String, side: DrivingSide, unit: SpeedUnit): List<Question> =
    QuestionBank.filter { q ->
        (q.countries.isEmpty() || countryId in q.countries) &&
            (q.drivingSide == null || q.drivingSide == side) &&
            (q.speedUnit == null || q.speedUnit == unit)
    }
