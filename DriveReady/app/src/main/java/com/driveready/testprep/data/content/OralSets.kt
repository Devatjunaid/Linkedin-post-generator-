package com.driveready.testprep.data.content

import com.driveready.testprep.data.model.OralQuestion

private fun o(tag: String, question: String, vararg answer: String) = OralQuestion(question, answer.toList(), tag)

/** Speaking technique that works with any examiner, in any country. */
val UniversalOralTechniques = listOf(
    "Listen to the whole question, then pause for a second before you answer.",
    "Answer in steps: what you check, how you check it, and what 'good' looks like.",
    "If you're asked to SHOW something, narrate quietly while you do it.",
    "If you don't understand, politely ask the examiner to repeat — that is never a fault.",
    "Keep it short. Examiners want the safety check, not a lecture.",
    "Look at the control you're talking about; it shows you know where it is."
)

/** UK DVSA "show me, tell me" questions (car test). */
val UkOral = listOf(
    o("Tell me", "Tell me how you'd check that the brakes are working before starting a journey.",
        "Brakes should not feel spongy or slack.", "Test them as you set off.", "The vehicle should not pull to one side."),
    o("Tell me", "Tell me where you'd find the information for the recommended tyre pressures and how they should be checked.",
        "In the manufacturer's guide (often also on the door pillar).", "Use a reliable pressure gauge.", "Check and adjust when tyres are cold.", "Don't forget the spare and refit the valve caps."),
    o("Tell me", "Tell me how you make sure your head restraint is correctly adjusted.",
        "The rigid part should be at least as high as the eyes or top of the ears.", "As close to the back of the head as is comfortable."),
    o("Tell me", "Tell me how you'd check the tyres to ensure they have sufficient tread depth and are safe to use.",
        "No cuts or bulges.", "At least 1.6 mm of tread across the central three-quarters of the tyre, all the way around."),
    o("Tell me", "Tell me how you'd check that the headlights and tail lights are working.",
        "Switch them on (turn on the ignition if needed).", "Walk around the vehicle to check."),
    o("Tell me", "Tell me how you'd know if there was a problem with your anti-lock braking system.",
        "The ABS warning light on the dashboard would stay on."),
    o("Tell me", "Tell me how you'd check the direction indicators are working.",
        "Switch on the hazard warning lights (ignition on if needed).", "Walk around the vehicle to check every indicator."),
    o("Tell me", "Tell me how you'd check the brake lights are working on this car.",
        "Press the brake pedal and use reflections in windows, garage doors or other cars.", "Or ask someone to help."),
    o("Tell me", "Tell me how you'd check the power-assisted steering is working before starting a journey.",
        "Apply gentle pressure on the wheel while starting the engine — it should move slightly as the system starts.", "Or turn the wheel just after moving off: it should feel light."),
    o("Tell me", "Tell me how you'd switch on the rear fog light(s) and explain when you'd use it/them.",
        "Switch on dipped headlights, then the fog light switch; check the warning light.", "Use only when visibility drops below 100 metres."),
    o("Tell me", "Tell me how you switch your headlight from dipped to main beam and explain how you'd know it's on.",
        "Operate the stalk/switch with the ignition or engine on.", "A blue main-beam warning light shows on the dashboard."),
    o("Tell me", "Open the bonnet and tell me how you'd check that the engine has sufficient oil.",
        "Find the dipstick or electronic oil indicator.", "Check the level is between the minimum and maximum marks."),
    o("Tell me", "Open the bonnet and tell me how you'd check that the engine has sufficient engine coolant.",
        "Find the header tank's high and low marks.", "Top up to the correct level only when the engine is cold."),
    o("Tell me", "Open the bonnet and tell me how you'd check that you have a safe level of hydraulic brake fluid.",
        "Find the brake-fluid reservoir.", "Check the level against the high and low marks."),
    o("Show me", "When it's safe to do so, can you show me how you wash and clean the rear windscreen?",
        "Operate the rear washer control while keeping control of the car.", "Keep your eyes on the road."),
    o("Show me", "When it's safe to do so, can you show me how you wash and clean the front windscreen?",
        "Operate the front washer and wipers with one hand on the wheel."),
    o("Show me", "When it's safe to do so, can you show me how you'd switch on your dipped headlights?",
        "Operate the light switch without looking away from the road for long."),
    o("Show me", "When it's safe to do so, can you show me how you'd set the rear demister?",
        "Press the rear demister button; the indicator light comes on."),
    o("Show me", "When it's safe to do so, can you show me how you'd operate the horn?",
        "Press the horn briefly — only when it's safe and won't alarm others."),
    o("Show me", "When it's safe to do so, can you show me how you'd demist the front windscreen?",
        "Set the fan and air direction to the windscreen, and use the air-con if fitted."),
    o("Show me", "When it's safe to do so, can you show me how you'd open and close the side window?",
        "Use the window switch briefly while keeping full control.")
)

/** US road-test pre-drive vehicle check and common examiner questions. */
val UsOral = listOf(
    o("Pre-drive check", "Show me the hand signal for a left turn.", "Extend your left arm straight out of the window, horizontally."),
    o("Pre-drive check", "Show me the hand signal for a right turn.", "Extend your left arm out and bend it upward at the elbow (some states also accept the right arm out of a passenger window)."),
    o("Pre-drive check", "Show me the hand signal for slowing down or stopping.", "Extend your left arm out and bend it downward at the elbow."),
    o("Pre-drive check", "Where is the parking (emergency) brake and how do you set and release it?", "Point to it, apply it, then release it."),
    o("Pre-drive check", "Turn on your windshield wipers and defroster.", "Operate both controls correctly."),
    o("Pre-drive check", "Show me your emergency flashers (hazard lights).", "Press the hazard button; all four indicators flash."),
    o("Pre-drive check", "Turn on your headlights and switch to high beam.", "Operate the light switch and stalk; point out the high-beam indicator."),
    o("Pre-drive check", "Sound the horn.", "A brief tap is enough."),
    o("Examiner question", "When must you stop for a school bus?", "When its red lights flash and stop arm is out — in both directions on an undivided road."),
    o("Examiner question", "What do you do at a stop sign with no limit line?", "Stop before the crosswalk; if there is none, stop before entering the intersection.")
)

/** India RTO viva / oral questions typically asked before or after the driving test. */
val IndiaOral = listOf(
    o("RTO viva", "What documents must you carry while driving?", "Driving licence, registration certificate (RC), insurance and PUC certificate — physical or in DigiLocker/mParivahan."),
    o("RTO viva", "What is the meaning of a triangular sign with a red border?", "It is a cautionary (warning) sign."),
    o("RTO viva", "What do circular signs with a red border indicate?", "Mandatory/prohibitory signs — you must obey them."),
    o("RTO viva", "When is overtaking prohibited?", "On bridges, near bends, at junctions, at pedestrian crossings and where there is a no-overtaking sign or solid line."),
    o("RTO viva", "What should you do when you hear an ambulance siren?", "Move to the left and give way."),
    o("RTO viva", "What is the use of the hazard warning light?", "To warn others when your vehicle has broken down or is a temporary obstruction."),
    o("RTO viva", "What is the legal age to drive a geared two-wheeler or car?", "18 years. Gearless up to 50 cc is allowed at 16 with parental consent."),
    o("RTO viva", "What is the validity of a learner's licence?", "Six months.")
)

/** Pakistan sign-test and examiner questions. */
val PakistanOral = listOf(
    o("Sign test", "What does a red circle with a horizontal white bar mean?", "No entry."),
    o("Sign test", "What does a triangle with a red border mean?", "Warning of a hazard ahead."),
    o("Sign test", "What does a blue circle with a white arrow mean?", "A mandatory direction you must follow."),
    o("Examiner question", "Who has priority at a roundabout?", "Vehicles already on the roundabout coming from the right."),
    o("Examiner question", "When should you use dipped headlights?", "At night in towns, in rain or fog, and when facing oncoming traffic."),
    o("Examiner question", "What should you check before starting the engine?", "Seat, mirrors, seatbelt, handbrake on, gear in neutral, doors closed.")
)

/** UAE RTA / Saudi / Qatar yard and road-test assessment questions. */
val GulfOral = listOf(
    o("Assessment", "What checks do you make before moving off?", "Seat, mirrors, seatbelt, doors, handbrake, gear, indicators and a blind-spot check."),
    o("Assessment", "How do you enter a roundabout?", "Choose the lane for your exit, give way to traffic already on the roundabout, signal on exit."),
    o("Assessment", "What must you do if a vehicle with flashing blue and red lights approaches?", "Move aside safely and give way."),
    o("Assessment", "When must you use the hazard lights?", "When stopped due to a breakdown or emergency — not for parking in prohibited areas."),
    o("Assessment", "What is the rule for children in the front seat?", "Children under 10 (or shorter than 145 cm in the UAE) must not sit in the front."),
    o("Assessment", "What do you do in a sandstorm with low visibility?", "Slow down, increase following distance, use dipped headlights — not hazard lights while moving.")
)

/** Germany: the "Sicherheitskontrolle" — safety-check questions in the practical test. */
val GermanyOral = listOf(
    o("Sicherheitskontrolle", "Show me how you check the tyre tread depth.", "At least 1.6 mm everywhere; check the tread-wear indicators."),
    o("Sicherheitskontrolle", "Show me how to check the engine oil level.", "Engine off, car level: pull the dipstick, wipe, re-insert, check between MIN and MAX."),
    o("Sicherheitskontrolle", "How do you check the brake-fluid level?", "Look at the reservoir: level between MIN and MAX."),
    o("Sicherheitskontrolle", "Switch on the hazard lights and check that they work.", "Press the button and walk round the car."),
    o("Sicherheitskontrolle", "Where is the warning triangle and first-aid kit, and how do you use the triangle?", "Show their location; place the triangle about 100 m behind the car on rural roads, 150–400 m on the Autobahn."),
    o("Sicherheitskontrolle", "How do you check that the brake lights work?", "Press the pedal and check reflections, or ask a helper.")
)

/** France: "vérifications" — one vehicle-check question plus a safety / first-aid question. */
val FranceOral = listOf(
    o("Vérification", "Show me the control for the rear fog light.", "Point to and operate the switch; the dashboard light comes on."),
    o("Vérification", "Check the windscreen washer fluid level.", "Open the bonnet, locate the reservoir and check the level."),
    o("Premiers secours", "How do you alert the emergency services?", "Call 112 (or 15/17/18) and give the location, the number of victims and their state."),
    o("Premiers secours", "How do you protect the scene of an accident?", "Hazard lights on, wear the high-visibility vest, place the warning triangle, keep people off the road."),
    o("Sécurité routière", "What is the purpose of the head restraint?", "To protect the neck in case of a rear impact (whiplash).")
)

/** Ireland: technical checks and hand signals asked on the RSA driving test. */
val IrelandOral = listOf(
    o("Technical check", "Show me how you'd check the tyres.", "Look for cuts and bulges; tread at least 1.6 mm; correct pressure from the handbook."),
    o("Technical check", "Show me how you'd check the oil level.", "Use the dipstick; level between the marks."),
    o("Technical check", "Show me how you'd check the lights and indicators.", "Switch on and walk around, or use reflections."),
    o("Hand signals", "Show me the hand signal for turning right.", "Right arm extended straight out of the window, palm forward."),
    o("Hand signals", "Show me the hand signal for slowing down.", "Right arm extended, palm down, moved slowly up and down."),
    o("Hand signals", "Show me the hand signal for turning left.", "Right arm extended and rotated anti-clockwise in a circle.")
)

/** South Africa K53: pre-trip inspection (exterior and interior). */
val SouthAfricaOral = listOf(
    o("K53 pre-trip", "Show me the exterior checks before you drive.", "Walk around: tyres, lights, number plates, windows, mirrors, leaks under the vehicle."),
    o("K53 pre-trip", "Show me the interior checks before you drive.", "Seat, mirrors, seatbelt, handbrake, neutral, doors, gauges and warning lights."),
    o("K53 observation", "What does K53 require before moving off?", "Check mirrors, signal, check the blind spot and only then move off."),
    o("K53 observation", "How often should you check your mirrors while driving?", "Every 5–8 seconds, and before changing speed or direction.")
)

/** Generic pre-drive examiner questions used where no formal oral exists. */
val GenericOral = listOf(
    o("Pre-drive check", "What do you check before starting the engine?", "Doors closed, seat and head restraint, mirrors, seatbelt, handbrake on, neutral/park."),
    o("Pre-drive check", "How do you check the tyres?", "Correct pressure when cold, no cuts or bulges, tread above the legal minimum."),
    o("Pre-drive check", "Show me the hazard warning lights.", "Press the hazard button; all indicators flash together."),
    o("Pre-drive check", "How would you demist the windscreen?", "Fan to the windscreen, temperature up, air-con on if fitted."),
    o("Examiner question", "What does an amber traffic light mean?", "Stop unless you've already crossed the line or are so close stopping could cause a crash."),
    o("Examiner question", "When would you use dipped headlights during the day?", "When visibility is seriously reduced, e.g. heavy rain or fog.")
)
