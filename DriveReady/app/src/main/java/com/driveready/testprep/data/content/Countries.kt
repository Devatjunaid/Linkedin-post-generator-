package com.driveready.testprep.data.content

import com.driveready.testprep.data.model.Country
import com.driveready.testprep.data.model.DrivingSide
import com.driveready.testprep.data.model.HazardSpec
import com.driveready.testprep.data.model.OralQuestion
import com.driveready.testprep.data.model.OralSpec
import com.driveready.testprep.data.model.PracticalSpec
import com.driveready.testprep.data.model.PracticalStep
import com.driveready.testprep.data.model.RegionKind
import com.driveready.testprep.data.model.SignStyle
import com.driveready.testprep.data.model.SpeedUnit
import com.driveready.testprep.data.model.SyllabusModule
import com.driveready.testprep.data.model.TheorySpec

private fun ps(title: String, detail: String) = PracticalStep(title, detail)
private fun mod(title: String, vararg topics: String) = SyllabusModule(title, topics.toList())

private val practiceHazard = HazardSpec(
    isOfficialPart = false,
    name = "Hazard perception practice",
    description = "Not a separate exam here, but examiners mark how early you spot and react to hazards on the road test. Train the same skill with animated clips."
)

private fun oral(name: String, description: String, questions: List<OralQuestion>, extraTechniques: List<String> = emptyList()) =
    OralSpec(name, description, extraTechniques + UniversalOralTechniques, questions)

/** Core modules every learner needs, plus country-specific modules appended per country. */
private fun coreSyllabus(sideLabel: String, extra: List<SyllabusModule>): List<SyllabusModule> = listOf(
    mod("1. Getting started", "Licence stages & eligibility", "Documents you need", "How the tests are booked and scored"),
    mod("2. Road signs & markings", "Regulatory (order) signs", "Warning signs", "Information & direction signs", "Road markings & lane lines", "Traffic lights & police signals"),
    mod("3. Rules of the road", "Speed limits", "Priority & right-of-way", "Junctions & intersections", "Roundabouts ($sideLabel)", "Overtaking", "Parking & stopping rules"),
    mod("4. Safe driving skills", "Mirrors-Signal-Position-Speed-Look routine", "Following distance & stopping distance", "Lane discipline", "Night, rain & fog driving"),
    mod("5. Hazard awareness", "Scanning & anticipation", "Developing hazards", "Vulnerable road users", "Hazard perception practice"),
    mod("6. Vehicle safety", "Pre-drive checks", "Tyres, lights, fluids", "Warning lights", "Seatbelts & child restraints"),
    mod("7. Alcohol, drugs & fatigue", "Legal limits", "Medicines", "Tiredness & distraction"),
    mod("8. Emergencies", "Breakdowns", "Collisions & first aid", "Tyre blow-outs & skids")
) + extra

private val LeftTraffic = "left-hand traffic"
private val RightTraffic = "right-hand traffic"

val Countries: List<Country> = listOf(
    Country(
        id = "us", name = "United States", code = "US", flagColors = listOf(0xFFB22234, 0xFFFFFFFF, 0xFF3C3B6E),
        authority = "State DMV / licensing agency", officialSite = "usa.gov/motor-vehicle-services",
        drivingSide = DrivingSide.RIGHT, speedUnit = SpeedUnit.MPH, signStyle = SignStyle.DIAMOND,
        minAge = "Learner permit from 14–16 depending on state",
        licenceStages = listOf("Learner permit (knowledge test + vision test)", "Supervised driving period (often 6–12 months for teens)", "Road test → provisional / intermediate licence", "Full licence"),
        theory = TheorySpec("Knowledge (permit) test", 0, 0, null, listOf("Question count and pass mark are set by each state — pick your state for exact numbers.", "Most states require 80%.", "A vision test is taken at the same visit.")),
        hazard = practiceHazard,
        oral = oral("Pre-drive check & examiner questions", "Before the road test, the examiner asks you to show hand signals and vehicle controls. Failing the vehicle check can end the test before you drive.", UsOral),
        practical = PracticalSpec(
            name = "Road (skills) test", durationMinutes = 20,
            passRule = "Scored on a state checklist. Critical errors (running a stop sign, hitting a curb hard, examiner intervention) mean an automatic fail.",
            steps = listOf(
                ps("Documents & vehicle check", "Permit, proof of insurance and registration. The car must have working lights, horn, brakes and seatbelts."),
                ps("Pre-drive check", "Hand signals, lights, wipers, defroster, horn, parking brake."),
                ps("Residential & business streets", "Stop signs, right turns, left turns, lane changes, school and pedestrian zones."),
                ps("Maneuvers", "Depending on the state: parallel parking, 3-point turn, backing in a straight line, angle parking."),
                ps("Highway segment (some states)", "Merging, lane changes and keeping a safe gap at speed."),
                ps("Debrief", "The examiner tells you your score and any critical errors.")
            ),
            maneuverIds = listOf("parallel_park", "three_point_turn", "angle_park", "lane_change", "hill_start"),
            commonFaults = listOf("Rolling stops", "Not checking blind spots", "Poor speed control", "Wide or short turns", "Not yielding to pedestrians")
        ),
        documents = listOf("Proof of identity (birth certificate / passport)", "Social Security number", "Proof of residency (2 documents)", "Parent consent form (under 18)", "Driver-ed certificate if required by your state"),
        syllabus = coreSyllabus(RightTraffic, listOf(
            mod("9. State-specific laws", "Graduated licensing (GDL) restrictions", "Cell-phone & texting laws", "Move-over law", "School bus law", "Implied consent & DUI penalties"),
            mod("10. Road test prep", "Pre-drive check", "Parallel parking & 3-point turn", "Critical errors that fail you")
        )),
        tips = listOf("Come to a full stop behind the line — count 'one-thousand-one'.", "Exaggerate head movements so the examiner sees every check.", "Keep both hands on the wheel at 9 and 3."),
        regionKind = RegionKind.STATE, regions = UsStates
    ),
    Country(
        id = "uk", name = "United Kingdom", code = "GB", flagColors = listOf(0xFF012169, 0xFFFFFFFF, 0xFFC8102E),
        authority = "DVSA (England, Scotland, Wales) · DVA (Northern Ireland)", officialSite = "gov.uk/driving-test",
        drivingSide = DrivingSide.LEFT, speedUnit = SpeedUnit.MPH, signStyle = SignStyle.VIENNA,
        minAge = "17 (provisional licence from 15y9m)",
        licenceStages = listOf("Provisional licence", "Theory test (multiple choice + hazard perception)", "Practical driving test", "Full licence — probation for 2 years (6 points = revoked)"),
        theory = TheorySpec("Theory test", 50, 43, 57, listOf("Multiple choice: 50 questions in 57 minutes, 43 to pass.", "Hazard perception: 14 video clips, 44 out of 75 to pass.", "You must pass both parts at the same sitting.", "The certificate is valid for 2 years.")),
        hazard = HazardSpec(true, "Hazard perception test", "14 clips; 13 contain one scoreable developing hazard and one contains two. Each hazard scores 0–5 depending on how early you click. Clicking in a pattern scores zero for that clip. Pass mark 44/75."),
        oral = oral("Show me, tell me", "One 'tell me' question is asked at the start of the test, before you drive. One 'show me' question is asked while you're driving. Getting one or both wrong is 1 driving fault.", UkOral),
        practical = PracticalSpec(
            name = "Practical driving test", durationMinutes = 40,
            passRule = "Pass with 15 or fewer driving faults and no serious or dangerous faults.",
            steps = listOf(
                ps("Eyesight check", "Read a number plate from 20 metres."),
                ps("'Tell me' question", "Vehicle safety question before you set off."),
                ps("General driving", "Different roads and traffic conditions; may include stopping at the side of the road and a hill start."),
                ps("Independent driving (~20 min)", "Follow a sat nav or traffic signs."),
                ps("Reversing manoeuvre", "One of: parallel park, park in a bay (forward or reverse), or pull up on the right and reverse 2 car lengths."),
                ps("'Show me' question", "Done while driving."),
                ps("Possible emergency stop", "Asked in about 1 in 3 tests.")
            ),
            maneuverIds = listOf("parallel_park", "bay_park", "pull_up_right", "emergency_stop", "hill_start", "roundabout", "lane_change", "three_point_turn", "reverse_corner"),
            commonFaults = listOf("Not making effective observation at junctions", "Not using mirrors correctly when changing direction", "Incorrect positioning when turning right", "Poor response to traffic lights", "Lack of control when moving off")
        ),
        documents = listOf("UK photocard provisional licence", "Theory test pass certificate number", "Glasses or contact lenses if needed", "A suitable car (if using your own): taxed, insured, MOT'd, L-plates, extra interior mirror"),
        syllabus = coreSyllabus(LeftTraffic, listOf(
            mod("9. DVSA theory topics", "Alertness", "Attitude", "Safety and your vehicle", "Safety margins", "Hazard awareness", "Vulnerable road users", "Other types of vehicle", "Vehicle handling", "Motorway rules", "Rules of the road", "Road and traffic signs", "Essential documents", "Incidents, accidents and emergencies", "Vehicle loading"),
            mod("10. Highway Code essentials", "Hierarchy of road users (Rules H1–H3)", "Smart motorways & red X", "Stopping distances table", "Penalty points & New Drivers Act")
        )),
        tips = listOf("Book the theory test as soon as you have your provisional licence.", "Practise hazard clips: click when a hazard starts to develop, and once more as it develops — never click in a rhythm.", "Ask your instructor for a mock test on real test routes."),
    ),
    Country(
        id = "ca", name = "Canada", code = "CA", flagColors = listOf(0xFFD52B1E, 0xFFFFFFFF, 0xFFD52B1E),
        authority = "Provincial licensing authority", officialSite = "canada.ca",
        drivingSide = DrivingSide.RIGHT, speedUnit = SpeedUnit.KMH, signStyle = SignStyle.DIAMOND,
        minAge = "14–16 depending on province",
        licenceStages = listOf("Learner stage (knowledge test)", "Novice / probationary stage (first road test)", "Full licence (second road test in most provinces)"),
        theory = TheorySpec("Knowledge test", 0, 0, null, listOf("Each province sets its own test — pick your province.", "Ontario's G1 has separate sign and rule sections.")),
        hazard = practiceHazard,
        oral = oral("Pre-drive & examiner questions", "There is no formal oral test, but examiners may ask you to identify controls before the road test.", GenericOral),
        practical = PracticalSpec(
            name = "Road tests (e.g. Ontario G2 & G)", durationMinutes = 20,
            passRule = "Marked on observation, signals, speed control and maneuvers; dangerous actions fail immediately.",
            steps = listOf(
                ps("Vehicle check", "Signals, brake lights, horn and documents."),
                ps("Residential & urban driving", "Intersections, turns, lane changes, school zones."),
                ps("Maneuvers", "Parallel park, 3-point turn, roadside stop and start."),
                ps("Highway (G test)", "Entering and exiting at speed, lane changes."),
                ps("Results", "Score sheet reviewed with you.")
            ),
            maneuverIds = listOf("parallel_park", "three_point_turn", "lane_change", "roundabout", "reverse_corner"),
            commonFaults = listOf("Rolling stops", "Missed shoulder checks", "Too fast in school zones", "Not stopping at the correct position")
        ),
        documents = listOf("Proof of identity and legal name", "Proof of residence in the province", "Licence fees", "Corrective lenses for the vision test"),
        syllabus = coreSyllabus(RightTraffic, listOf(
            mod("9. Graduated licensing", "Learner restrictions (zero BAC, accompanying driver)", "Novice restrictions", "Demerit points"),
            mod("10. Winter driving", "Winter tyres", "Black ice", "Snow ploughs & visibility")
        )),
        tips = listOf("Ontario G1: you must pass BOTH the 20 sign and 20 rule questions.", "Do a full stop and shoulder check at every intersection on the road test."),
        regionKind = RegionKind.PROVINCE, regions = CanadaProvinces
    ),
    Country(
        id = "au", name = "Australia", code = "AU", flagColors = listOf(0xFF012169, 0xFFFFFFFF, 0xFFE4002B),
        authority = "State or territory transport authority", officialSite = "infrastructure.gov.au",
        drivingSide = DrivingSide.LEFT, speedUnit = SpeedUnit.KMH, signStyle = SignStyle.DIAMOND,
        minAge = "16 for a learner licence",
        licenceStages = listOf("Learner (L) — knowledge test, supervised hours log", "Hazard Perception Test", "Provisional P1 (driving test) → P2", "Full licence"),
        theory = TheorySpec("Driver knowledge test", 0, 0, null, listOf("Each state sets its own test — pick your state.", "Learners must log supervised hours (e.g. 120 hours in NSW for under-25s).")),
        hazard = HazardSpec(true, "Hazard Perception Test (HPT)", "Computer-based test of reacting to traffic situations: when it's safe to turn, overtake or slow down. Required before the driving test in NSW, VIC and WA."),
        oral = oral("Examiner instructions", "There's no oral test, but you must follow the examiner's directions and may be asked to show controls.", GenericOral),
        practical = PracticalSpec(
            name = "Driving test (P1)", durationMinutes = 45,
            passRule = "Scored on a test sheet; critical errors (e.g. failing to give way, speeding, examiner intervention) are an immediate fail.",
            steps = listOf(
                ps("Pre-drive check", "Vehicle roadworthy, L plates, controls."),
                ps("Low-risk driving", "Gaps, speed, observation and positioning."),
                ps("Maneuvers", "Reverse parallel park, 3-point turn (or U-turn), kerb-side stop."),
                ps("Varied traffic", "Roundabouts, multi-lane roads, school zones."),
                ps("Result", "Examiner explains your errors.")
            ),
            maneuverIds = listOf("parallel_park", "three_point_turn", "hill_start", "roundabout", "lane_change", "reverse_corner"),
            commonFaults = listOf("Speeding in school zones", "Failing to give way", "Poor observation when changing lanes", "Stop-line overruns")
        ),
        documents = listOf("Proof of identity", "Completed learner logbook / app", "Hazard Perception Test pass"),
        syllabus = coreSyllabus(LeftTraffic, listOf(
            mod("9. Australian road rules", "Give way at T-intersections", "School zones (40 km/h)", "Roundabout signalling", "Mobile phone rules for L/P drivers"),
            mod("10. Graduated licensing", "Learner speed limits", "P-plate restrictions", "Zero BAC for L/P drivers")
        )),
        tips = listOf("Use the full 3-second gap rule.", "Practise the HPT until you can wait for a safe gap without hesitating."),
        regionKind = RegionKind.TERRITORY, regions = AustraliaStates
    ),
    Country(
        id = "in", name = "India", code = "IN", flagColors = listOf(0xFFFF9933, 0xFFFFFFFF, 0xFF138808),
        authority = "State transport department · Regional Transport Office (RTO)", officialSite = "parivahan.gov.in",
        drivingSide = DrivingSide.LEFT, speedUnit = SpeedUnit.KMH, signStyle = SignStyle.VIENNA,
        minAge = "18 (16 for gearless ≤50 cc with parental consent)",
        licenceStages = listOf("Learner's licence (LL) — computer test", "Wait at least 30 days (LL valid 6 months)", "Driving test at RTO / automated track", "Permanent driving licence"),
        theory = TheorySpec("Learner's licence test", 15, 9, 20, listOf("Usually 15 questions, 9 correct to pass (60%); some states use 20 questions.", "Covers traffic signs, rules and the Motor Vehicles Act.", "Apply online through the Sarathi portal (parivahan.gov.in).")),
        hazard = practiceHazard,
        oral = oral("RTO viva questions", "Many RTO inspectors ask a few spoken questions about signs, documents and rules before or after the driving test.", IndiaOral),
        practical = PracticalSpec(
            name = "Driving test (RTO / automated driving test track)", durationMinutes = 15,
            passRule = "Inspector or automated sensors mark each track segment; touching boundary lines or putting a foot down usually fails that segment.",
            steps = listOf(
                ps("Document verification", "Learner's licence, ID, address proof, vehicle with L-board."),
                ps("Two-wheeler: figure-8 track", "Ride the 8 without touching the lines or putting a foot down."),
                ps("Four-wheeler: track tests", "Forward and reverse 'H'/S track, parking, gradient (hill start) depending on the state."),
                ps("Road drive (some RTOs)", "Short drive on the road with the inspector."),
                ps("Result", "Licence issued and posted if you pass.")
            ),
            maneuverIds = listOf("figure_eight", "garage_park", "hill_start", "parallel_park", "cone_slalom", "three_point_turn"),
            commonFaults = listOf("Touching boundary lines", "Putting a foot down on the 8", "Rolling back on the gradient", "Not using indicators")
        ),
        documents = listOf("Valid learner's licence", "Age proof and address proof (Aadhaar etc.)", "Form 1 (self-declaration of fitness) / Form 1A medical certificate if over 40", "Passport-size photos", "Vehicle with valid RC, insurance, PUC and L-board"),
        syllabus = coreSyllabus(LeftTraffic, listOf(
            mod("9. Motor Vehicles Act essentials", "Documents to carry", "Penalties after the 2019 amendment", "Helmet & seatbelt rules", "Drunk driving limit (30 mg/100 ml)"),
            mod("10. RTO test track", "Figure-8 track", "H-track / reverse parking", "Gradient test", "Hand signals")
        )),
        tips = listOf("Practise the 8 at walking pace with steady throttle.", "Book your slot on Sarathi early — slots fill quickly.", "Keep digital documents in DigiLocker."),
    ),
    Country(
        id = "pk", name = "Pakistan", code = "PK", flagColors = listOf(0xFF01411C, 0xFFFFFFFF, 0xFF01411C),
        authority = "Provincial traffic police (e.g. Punjab DLIMS, Islamabad Traffic Police, Sindh)", officialSite = "dlims.punjab.gov.pk",
        drivingSide = DrivingSide.LEFT, speedUnit = SpeedUnit.KMH, signStyle = SignStyle.VIENNA,
        minAge = "18 for a car licence",
        licenceStages = listOf("Learner permit (with road-safety class in some cities)", "Waiting period (e.g. 42 days in Islamabad)", "Sign test (computerised e-sign test in Punjab)", "Road / practical test", "Driving licence"),
        theory = TheorySpec("Sign test (e-sign)", 0, 0, null, listOf("A computerised test of road signs and traffic rules.", "Punjab (from 17 Sep 2026): the road test must be taken within 24 hours of passing the e-sign test or the result expires.", "Some offices also run a short written or oral rules check.")),
        hazard = practiceHazard,
        oral = oral("Sign test & examiner questions", "Expect questions on sign meanings and basic rules, either on screen or asked by the examiner.", PakistanOral),
        practical = PracticalSpec(
            name = "Practical driving test", durationMinutes = 10,
            passRule = "Examiner-marked; touching cones or poles, rolling back or stalling repeatedly usually fails.",
            steps = listOf(
                ps("Document check & biometrics", "CNIC, learner permit, medical certificate where required."),
                ps("Track test", "Drive forward and reverse through a marked cone/pole track (parking test)."),
                ps("Road test", "Short supervised drive on the road (varies by city)."),
                ps("Result", "Licence printed or couriered.")
            ),
            maneuverIds = listOf("garage_park", "cone_slalom", "parallel_park", "hill_start", "figure_eight"),
            commonFaults = listOf("Touching a pole while reversing", "Not checking mirrors", "Rolling back", "No indicator before moving off")
        ),
        documents = listOf("Original CNIC + copies", "Learner permit", "Medical certificate (where required)", "Passport photos", "Fee challan"),
        syllabus = coreSyllabus(LeftTraffic, listOf(
            mod("9. Local rules", "Motorway rules (NH&MP)", "Helmet & seatbelt enforcement", "Horn-free zones", "One-way violations & fines"),
            mod("10. Test day", "e-Sign test practice", "Reverse parking track", "Road test etiquette")
        )),
        tips = listOf("Practise reversing through the cone track — it's where most candidates fail.", "In Punjab, plan to take the road test right after the sign test."),
    ),
    Country(
        id = "ae", name = "United Arab Emirates", code = "AE", flagColors = listOf(0xFF00732F, 0xFFFFFFFF, 0xFFFF0000),
        authority = "RTA (Dubai) · ITC / Abu Dhabi Police · Sharjah Police", officialSite = "rta.ae",
        drivingSide = DrivingSide.RIGHT, speedUnit = SpeedUnit.KMH, signStyle = SignStyle.VIENNA,
        minAge = "17 to start, licence at 17–18 depending on emirate",
        licenceStages = listOf("Open traffic file + eye test", "Theory lectures & knowledge test", "Practical lessons & internal assessments", "Parking (Smart Yard) test", "Road test → licence"),
        theory = TheorySpec("Theory (knowledge) test", 35, 28, 30, listOf("Format varies by emirate; guides report 35–40 questions in sections such as signs, risk perception and rules.", "Computer-based, available in several languages.", "Confirm the current format with your driving school.")),
        hazard = HazardSpec(false, "Risk perception", "Dubai's theory test includes risk-perception (hazard) questions using videos or images, and the road test assesses observation."),
        oral = oral("Assessment & examiner questions", "Internal assessments and the road test can include spoken questions on rules and safety.", GulfOral),
        practical = PracticalSpec(
            name = "Parking (Smart Yard) + road test", durationMinutes = 20,
            passRule = "Smart Yard uses cameras and sensors; touching a pole or crossing a line is a fault. The road test is examiner-marked.",
            steps = listOf(
                ps("Parking test", "Parallel parking, garage parking, angle parking, hill start (ramp) and emergency brake."),
                ps("Road test", "City and highway driving with the examiner."),
                ps("Roundabouts", "Correct lane selection and give-way."),
                ps("Result", "Pass and the licence is issued the same day in many centres.")
            ),
            maneuverIds = listOf("parallel_park", "garage_park", "angle_park", "hill_start", "emergency_stop", "roundabout"),
            commonFaults = listOf("Touching a pole in the yard", "Wrong lane at roundabouts", "Not checking blind spots", "Speeding on the road test")
        ),
        documents = listOf("Emirates ID", "Passport & visa copy", "Eye test result", "NOC from sponsor (where required)", "Photographs"),
        syllabus = coreSyllabus(RightTraffic, listOf(
            mod("9. UAE traffic law", "Black points system", "Children in front seats", "Zero alcohol", "Mobile phone fines", "Sandstorm & fog driving"),
            mod("10. Smart Yard test", "Parallel & garage parking", "Ramp start", "Emergency braking")
        )),
        tips = listOf("Practise each Smart Yard exercise until you can do it without corrections.", "On roundabouts, choose your lane before you enter."),
    ),
    Country(
        id = "sa", name = "Saudi Arabia", code = "SA", flagColors = listOf(0xFF006C35, 0xFFFFFFFF, 0xFF006C35),
        authority = "General Department of Traffic (Moroor) via Absher", officialSite = "absher.sa",
        drivingSide = DrivingSide.RIGHT, speedUnit = SpeedUnit.KMH, signStyle = SignStyle.VIENNA,
        minAge = "18 (learner permit at 17, Hijri)",
        licenceStages = listOf("Medical check linked to Absher", "Enrol at an authorised school (e.g. Dallah)", "Theory test", "Practical training", "Practical test → licence"),
        theory = TheorySpec("Theory test", 20, 17, 20, listOf("Commonly reported as 20 questions with about 85% to pass; official sources don't publish a fixed format.", "Taken at the authorised driving school.")),
        hazard = practiceHazard,
        oral = oral("Examiner questions", "Practical examiners may ask about pre-drive checks and rules.", GulfOral),
        practical = PracticalSpec(
            name = "Practical driving test", durationMinutes = 15,
            passRule = "Examiner-marked yard and road test.",
            steps = listOf(
                ps("Yard exercises", "Parking, reversing, hill start."),
                ps("Road drive", "Lane discipline, roundabouts, signals."),
                ps("Result", "Licence issued through Absher after passing.")
            ),
            maneuverIds = listOf("parallel_park", "garage_park", "hill_start", "roundabout", "lane_change"),
            commonFaults = listOf("Not using indicators", "Wrong lane at roundabouts", "Touching yard poles")
        ),
        documents = listOf("National ID or Iqama", "Medical report (Absher-linked)", "Photos", "Clear traffic fines"),
        syllabus = coreSyllabus(RightTraffic, listOf(
            mod("9. Saudi traffic system", "Saher cameras", "Absher services", "Zero alcohol", "Mobile phone ban")
        )),
        tips = listOf("Book everything through Absher and keep your medical report linked."),
    ),
    Country(
        id = "qa", name = "Qatar", code = "QA", flagColors = listOf(0xFF8A1538, 0xFFFFFFFF, 0xFF8A1538),
        authority = "Traffic Department, Ministry of Interior", officialSite = "portal.moi.gov.qa",
        drivingSide = DrivingSide.RIGHT, speedUnit = SpeedUnit.KMH, signStyle = SignStyle.VIENNA,
        minAge = "18",
        licenceStages = listOf("Eye test & file opening", "Lessons at a licensed school", "Theory (signal) test", "Parking / yard test", "Road test → licence"),
        theory = TheorySpec("Theory (signal) test", 0, 0, null, listOf("Format is set by the Traffic Department and your school; confirm the current pass mark.")),
        hazard = practiceHazard,
        oral = oral("Examiner questions", "Expect questions on signs and safety during training assessments.", GulfOral),
        practical = PracticalSpec(
            name = "Parking + road test", durationMinutes = 20,
            passRule = "Examiner-marked.",
            steps = listOf(ps("Yard test", "Parking, ramp start and S-course."), ps("Road test", "City and roundabout driving."), ps("Result", "Licence issued after passing.")),
            maneuverIds = listOf("parallel_park", "garage_park", "hill_start", "s_course", "roundabout"),
            commonFaults = listOf("Touching cones", "Lane discipline at roundabouts")
        ),
        documents = listOf("QID", "Eye test", "Photos", "Sponsor NOC if required"),
        syllabus = coreSyllabus(RightTraffic, listOf(mod("9. Qatar traffic law", "Zero alcohol", "Roundabout rules", "Seatbelt & child-seat law"))),
        tips = listOf("Practise the S-course and ramp start in your school's yard."),
    ),
    Country(
        id = "de", name = "Germany", code = "DE", flagColors = listOf(0xFF000000, 0xFFDD0000, 0xFFFFCE00),
        authority = "Fahrerlaubnisbehörde · tests by TÜV / DEKRA", officialSite = "bmv.de",
        drivingSide = DrivingSide.RIGHT, speedUnit = SpeedUnit.KMH, signStyle = SignStyle.VIENNA,
        minAge = "18 (17 with accompanied driving 'BF17')",
        licenceStages = listOf("Driving school (theory lessons + practical lessons incl. special drives)", "First aid course & eye test", "Theory exam (TÜV/DEKRA)", "Practical exam", "Probation (Probezeit) for 2 years"),
        theory = TheorySpec("Theorieprüfung (Class B)", 30, 0, null, listOf("30 multiple-choice questions (20 basic + 10 class-specific).", "Pass with at most 10 error points; questions are weighted 2–5 points.", "Two wrong 5-point questions = fail.")),
        hazard = practiceHazard,
        oral = oral("Sicherheitskontrolle", "At the start of the practical exam the examiner asks you to explain or show vehicle safety checks.", GermanyOral),
        practical = PracticalSpec(
            name = "Praktische Prüfung (Class B)", durationMinutes = 55,
            passRule = "Examiner-marked; a single serious error (e.g. ignoring priority, endangering others) fails the test.",
            steps = listOf(
                ps("Abfahrtkontrolle / safety check", "Explain or show vehicle checks."),
                ps("Grundfahraufgaben", "Two basic tasks, e.g. reverse parking (parallel or bay), turning around, emergency braking."),
                ps("City driving", "Rechts vor links, pedestrians, cyclists, trams."),
                ps("Country road & Autobahn", "Entering, overtaking, leaving the Autobahn."),
                ps("Result", "Licence handed over on the spot after passing.")
            ),
            maneuverIds = listOf("parallel_park", "bay_park", "three_point_turn", "emergency_stop", "lane_change"),
            commonFaults = listOf("Missing 'rechts vor links'", "Shoulder check missed when turning right (cyclists)", "Too slow on the Autobahn", "Speeding in 30 zones")
        ),
        documents = listOf("Ausbildungsbescheinigung from the driving school", "First-aid course certificate", "Eye test (Sehtest)", "Biometric photo", "ID card or passport"),
        syllabus = coreSyllabus(RightTraffic, listOf(
            mod("9. StVO essentials", "Rechts vor links", "Rettungsgasse", "Richtgeschwindigkeit 130 km/h", "Probezeit & 0.0‰ rule", "Winter tyre duty (situational)"),
            mod("10. Special drives", "Overland drives", "Autobahn drives", "Night drives")
        )),
        tips = listOf("Exaggerate the shoulder check on right turns — cyclists are a top fail reason.", "Practise the official question catalogue daily."),
    ),
    Country(
        id = "fr", name = "France", code = "FR", flagColors = listOf(0xFF0055A4, 0xFFFFFFFF, 0xFFEF4135),
        authority = "ANTS · examiners of the Ministry of the Interior", officialSite = "securite-routiere.gouv.fr",
        drivingSide = DrivingSide.RIGHT, speedUnit = SpeedUnit.KMH, signStyle = SignStyle.VIENNA,
        minAge = "17 (since 2024)",
        licenceStages = listOf("Register via ANTS (NEPH number)", "Code de la route (ETG)", "Driving lessons (min. 20 hours)", "Practical exam", "Probationary licence (3 years, 2 with accompanied driving)"),
        theory = TheorySpec("Code de la route (ETG)", 40, 35, 30, listOf("40 questions, 35 correct to pass.", "Taken at an approved test centre.")),
        hazard = practiceHazard,
        oral = oral("Vérifications & premiers secours", "During the practical exam you answer one vehicle-check question and one road-safety or first-aid question.", FranceOral),
        practical = PracticalSpec(
            name = "Examen pratique", durationMinutes = 32,
            passRule = "Scored out of 31 points; 20 needed with no eliminating fault.",
            steps = listOf(
                ps("Welcome & installation", "Adjust seat and mirrors."),
                ps("Vérifications", "One technical check and one safety/first-aid question."),
                ps("Driving with directions", "Including about 5 minutes of independent driving."),
                ps("Maneuvers", "Créneau (parallel), bataille (bay) or demi-tour; a precise stop."),
                ps("Result", "Published online 48 hours later.")
            ),
            maneuverIds = listOf("parallel_park", "bay_park", "three_point_turn", "roundabout", "lane_change"),
            commonFaults = listOf("Missing priorité à droite", "Speed not adapted", "Unsafe lane change")
        ),
        documents = listOf("ID", "ANTS registration", "ASSR2 or ASR (if born after 1988)", "Driving school file"),
        syllabus = coreSyllabus(RightTraffic, listOf(mod("9. Code essentials", "Priorité à droite", "80 km/h roads", "Motorway 130/110 km/h", "Alcohol limits 0.5 / 0.2 g/l", "'A' plate for novices"))),
        tips = listOf("Learn the 12 official vérification questions by heart.", "Look for priorité à droite in residential areas."),
    ),
    Country(
        id = "ie", name = "Ireland", code = "IE", flagColors = listOf(0xFF169B62, 0xFFFFFFFF, 0xFFFF883E),
        authority = "Road Safety Authority (RSA) · NDLS", officialSite = "rsa.ie",
        drivingSide = DrivingSide.LEFT, speedUnit = SpeedUnit.KMH, signStyle = SignStyle.DIAMOND,
        minAge = "17",
        licenceStages = listOf("Driver theory test", "Learner permit", "12 Essential Driver Training (EDT) lessons", "Wait 6 months on the permit", "Driving test → N-plates for 2 years"),
        theory = TheorySpec("Driver theory test", 40, 35, 45, listOf("40 questions in 45 minutes, 35 to pass (87.5%).", "The certificate is valid for 2 years.")),
        hazard = practiceHazard,
        oral = oral("Technical checks & hand signals", "The RSA test includes questions on vehicle checks and a demonstration of hand signals.", IrelandOral),
        practical = PracticalSpec(
            name = "RSA driving test", durationMinutes = 40,
            passRule = "Fail with any Grade 3 fault, 4+ Grade 2 faults in one aspect, or 9+ Grade 2 faults in total.",
            steps = listOf(
                ps("Paperwork & technical checks", "Questions on tyres, lights, fluids; show hand signals."),
                ps("Road drive", "Urban and rural roads."),
                ps("Maneuvers", "Reverse around a corner, turnabout, hill start."),
                ps("Result", "Explained at the end.")
            ),
            maneuverIds = listOf("reverse_corner", "three_point_turn", "hill_start", "roundabout", "lane_change"),
            commonFaults = listOf("Observation at junctions", "Positioning", "Progress (too slow)")
        ),
        documents = listOf("Learner permit", "EDT logbook", "Road-legal car with L-plates, NCT, tax and insurance"),
        syllabus = coreSyllabus(LeftTraffic, listOf(mod("9. Irish rules", "Learner & N-plate rules", "Speed limits (120/100/80/50)", "Alcohol limits", "Hand signals"))),
        tips = listOf("Practise hand signals until they're automatic."),
    ),
    Country(
        id = "nz", name = "New Zealand", code = "NZ", flagColors = listOf(0xFF00247D, 0xFFFFFFFF, 0xFFCC142B),
        authority = "NZ Transport Agency Waka Kotahi (NZTA)", officialSite = "nzta.govt.nz",
        drivingSide = DrivingSide.LEFT, speedUnit = SpeedUnit.KMH, signStyle = SignStyle.DIAMOND,
        minAge = "16",
        licenceStages = listOf("Learner licence (theory test)", "Restricted licence (practical test)", "Full licence (practical test)"),
        theory = TheorySpec("Learner licence theory test", 35, 32, 30, listOf("35 questions from a pool of about 200; 32 correct to pass.", "You see whether each answer was right immediately.", "Fail twice in a day = wait 10 working days.")),
        hazard = practiceHazard,
        oral = oral("Pre-drive & examiner questions", "No formal oral test; the testing officer gives instructions and may ask about controls.", GenericOral),
        practical = PracticalSpec(
            name = "Restricted licence test", durationMinutes = 60,
            passRule = "Marked on critical and immediate-fail errors.",
            steps = listOf(ps("Stage 1", "Basic driving and a maneuver."), ps("Stage 2", "Complex traffic, if Stage 1 passed."), ps("Result", "Explained by the testing officer.")),
            maneuverIds = listOf("three_point_turn", "parallel_park", "roundabout", "lane_change"),
            commonFaults = listOf("Not giving way", "Missed head checks")
        ),
        documents = listOf("Identity documents", "Learner licence"),
        syllabus = coreSyllabus(LeftTraffic, listOf(mod("9. Road Code", "Give-way rules", "Zero alcohol under 20", "Learner supervision rules"))),
        tips = listOf("Study the official Road Code — the question pool comes from it."),
    ),
    Country(
        id = "za", name = "South Africa", code = "ZA", flagColors = listOf(0xFF007A4D, 0xFFFFB612, 0xFFDE3831),
        authority = "Driving Licence Testing Centres (DLTC) · RTMC", officialSite = "rtmc.co.za",
        drivingSide = DrivingSide.LEFT, speedUnit = SpeedUnit.KMH, signStyle = SignStyle.VIENNA,
        minAge = "17 for a learner's licence, 18 for a car licence",
        licenceStages = listOf("Eye test", "Learner's licence test", "K53 driving test (yard + road)", "Driving licence card"),
        theory = TheorySpec("Learner's licence test", 64, 51, 60, listOf("Three sections, each passed separately: road signs 23/28, rules of the road 22/28, vehicle controls 6/8.", "The learner's licence is valid for 24 months.")),
        hazard = practiceHazard,
        oral = oral("K53 pre-trip inspection", "The K53 test starts with a spoken and practical pre-trip inspection.", SouthAfricaOral),
        practical = PracticalSpec(
            name = "K53 driving test", durationMinutes = 45,
            passRule = "Penalty points for each fault; some faults are immediate failures.",
            steps = listOf(ps("Pre-trip inspection", "Exterior and interior checks."), ps("Yard test", "Alley docking, parallel parking, 3-point turn, incline start."), ps("Road test", "Observation routine at every manoeuvre."), ps("Result", "Explained at the end.")),
            maneuverIds = listOf("alley_dock", "parallel_park", "three_point_turn", "hill_start", "lane_change"),
            commonFaults = listOf("Missing K53 observation steps", "Rolling back on the incline", "Touching poles")
        ),
        documents = listOf("ID", "Learner's licence", "Photos", "Roadworthy vehicle"),
        syllabus = coreSyllabus(LeftTraffic, listOf(mod("9. K53 system", "Observation routine", "Four-way stops", "Speed limits 60/100/120"))),
        tips = listOf("K53 rewards exaggerated observation — make every check visible."),
    ),
    Country(
        id = "sg", name = "Singapore", code = "SG", flagColors = listOf(0xFFEF3340, 0xFFFFFFFF, 0xFFEF3340),
        authority = "Singapore Police Force — Traffic Police", officialSite = "police.gov.sg",
        drivingSide = DrivingSide.LEFT, speedUnit = SpeedUnit.KMH, signStyle = SignStyle.VIENNA,
        minAge = "18",
        licenceStages = listOf("Basic Theory Test (BTT)", "Provisional driving licence", "Final Theory Test (FTT)", "Practical driving test (TP)", "Probationary licence (1 year)"),
        theory = TheorySpec("Basic & Final Theory Tests", 50, 45, 50, listOf("BTT and FTT: 50 questions each, 45 to pass.", "FTT result is valid for 2 years.")),
        hazard = practiceHazard,
        oral = oral("Pre-drive & examiner questions", "No formal oral; testers give instructions in the circuit and on the road.", GenericOral),
        practical = PracticalSpec(
            name = "Practical driving test", durationMinutes = 60,
            passRule = "Fail with more than 18 demerit points (Class 3/3A) or any immediate-fail error.",
            steps = listOf(ps("Circuit", "Directional change, parking, ramp, S-course, crank course, emergency braking."), ps("Road", "Traffic driving with the tester."), ps("Result", "Given on the day.")),
            maneuverIds = listOf("s_course", "parallel_park", "garage_park", "three_point_turn", "hill_start", "emergency_stop"),
            commonFaults = listOf("Mounting the kerb in the S-course", "Rolling back on the ramp")
        ),
        documents = listOf("NRIC/FIN", "Provisional licence", "BTT & FTT results"),
        syllabus = coreSyllabus(LeftTraffic, listOf(mod("9. Singapore rules", "Demerit points", "Probation & P-plate", "Expressway rules"))),
        tips = listOf("Use the circuit sessions to master the crank course."),
    ),
    Country(
        id = "my", name = "Malaysia", code = "MY", flagColors = listOf(0xFF010066, 0xFFCC0001, 0xFFFFCC00),
        authority = "Jabatan Pengangkutan Jalan (JPJ)", officialSite = "jpj.gov.my",
        drivingSide = DrivingSide.LEFT, speedUnit = SpeedUnit.KMH, signStyle = SignStyle.VIENNA,
        minAge = "17",
        licenceStages = listOf("Road-safety course", "KPP01 computer theory test", "Learner licence (LDL)", "KPP02 circuit training & test", "KPP03 road test", "Probationary (P) licence for 2 years"),
        theory = TheorySpec("KPP01 computer test", 50, 42, 45, listOf("50 questions in 45 minutes, 42 to pass (84%).")),
        hazard = practiceHazard,
        oral = oral("Pre-drive & examiner questions", "No formal oral; JPJ examiners give instructions in the circuit.", GenericOral),
        practical = PracticalSpec(
            name = "KPP02 circuit + KPP03 road test", durationMinutes = 30,
            passRule = "Examiner-marked; touching lines or poles in the circuit fails that exercise.",
            steps = listOf(ps("Circuit", "Hill start (bukit), side parking, 3-point turn, Z/S course."), ps("Road", "Traffic driving with the JPJ examiner."), ps("Result", "P licence issued after passing.")),
            maneuverIds = listOf("hill_start", "parallel_park", "three_point_turn", "s_course"),
            commonFaults = listOf("Rolling back on the hill", "Touching poles")
        ),
        documents = listOf("MyKad", "Course certificates", "Photos"),
        syllabus = coreSyllabus(LeftTraffic, listOf(mod("9. JPJ essentials", "KEJARA demerit points", "P-licence rules", "Expressway limit 110 km/h"))),
        tips = listOf("Practise the bukit (hill) test until there's zero roll-back."),
    ),
    Country(
        id = "ph", name = "Philippines", code = "PH", flagColors = listOf(0xFF0038A8, 0xFFFFFFFF, 0xFFCE1126),
        authority = "Land Transportation Office (LTO)", officialSite = "lto.gov.ph",
        drivingSide = DrivingSide.RIGHT, speedUnit = SpeedUnit.KMH, signStyle = SignStyle.VIENNA,
        minAge = "17 for a non-professional licence (student permit from 16)",
        licenceStages = listOf("15-hour Theoretical Driving Course (TDC)", "Student permit", "8-hour Practical Driving Course (PDC)", "LTO written exam", "LTO practical exam → non-professional licence"),
        theory = TheorySpec("LTO written exam", 40, 30, null, listOf("40 multiple-choice questions, 30 correct to pass (75%).")),
        hazard = practiceHazard,
        oral = oral("Pre-drive & examiner questions", "No formal oral; the LTO examiner gives instructions during the practical test.", GenericOral),
        practical = PracticalSpec(
            name = "LTO practical driving test", durationMinutes = 15,
            passRule = "Scored out of 100; 70 needed to pass.",
            steps = listOf(ps("Pre-drive check", "Controls, mirrors, seatbelt."), ps("Maneuvers", "Parking, turning, hill start where available."), ps("Road drive", "Short supervised drive."), ps("Result", "Licence card released.")),
            maneuverIds = listOf("parallel_park", "three_point_turn", "hill_start", "lane_change", "bay_park"),
            commonFaults = listOf("No signal", "Hitting cones", "Poor observation")
        ),
        documents = listOf("Valid student permit", "TDC & PDC certificates", "Medical certificate", "Valid ID"),
        syllabus = coreSyllabus(RightTraffic, listOf(mod("9. Philippine laws", "Anti-Distracted Driving Act", "Child Safety in Motor Vehicles Act", "Seatbelt Use Act", "Helmet law"))),
        tips = listOf("Keep your TDC and PDC certificates — the LTO checks them online."),
    )
)

fun countryById(id: String?): Country? = Countries.firstOrNull { it.id == id }
