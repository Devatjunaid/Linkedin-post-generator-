package com.driveready.testprep.data.content

import com.driveready.testprep.data.model.Region

/**
 * Knowledge (permit) test format per US state. Compiled from multiple 2025–2026
 * public guides; several states revise counts often, so the UI always shows a
 * "confirm with your agency" note next to these numbers.
 */
val UsStates: List<Region> = listOf(
    Region("al", "Alabama", "ALEA Driver License Division", 30, 24, "15"),
    Region("ak", "Alaska", "Alaska DMV", 20, 16, "14"),
    Region("az", "Arizona", "ADOT Motor Vehicle Division", 30, 24, "15½"),
    Region("ar", "Arkansas", "Arkansas State Police / DFA", 25, 20, "14"),
    Region("ca", "California", "California DMV", 46, 38, "15½", "Under-18 applicants: 46 questions. Adults (18+) take a 36-question test and need 30 correct."),
    Region("co", "Colorado", "Colorado DMV", 25, 20, "15"),
    Region("ct", "Connecticut", "Connecticut DMV", 25, 20, "16"),
    Region("de", "Delaware", "Delaware DMV", 30, 24, "16"),
    Region("dc", "District of Columbia", "DC DMV", 25, 20, "16", "Some guides list a 70–75% pass mark; confirm with DC DMV."),
    Region("fl", "Florida", "FLHSMV", 50, 40, "15", "Test can end early once you reach 40 correct or 11 incorrect."),
    Region("ga", "Georgia", "Department of Driver Services (DDS)", 40, 30, "15", "Two parts: 20 road-sign and 20 road-rule questions. You need 15 correct in EACH part."),
    Region("hi", "Hawaii", "County DMV / Hawaii DOT", 30, 24, "15½"),
    Region("id", "Idaho", "ITD Division of Motor Vehicles", 40, 34, "14½", "85% pass mark — one of the strictest in the US."),
    Region("il", "Illinois", "Illinois Secretary of State", 35, 28, "15", "15 road-sign and 20 road-rule questions; both sections count."),
    Region("in", "Indiana", "Indiana BMV", 50, 42, "15", "Separate road-sign (16) and rules (34) sections; about 84% overall."),
    Region("ia", "Iowa", "Iowa DOT", 35, 28, "14"),
    Region("ks", "Kansas", "Kansas DOR Division of Vehicles", 25, 20, "14"),
    Region("ky", "Kentucky", "Kentucky State Police / KYTC", 40, 32, "16"),
    Region("la", "Louisiana", "Louisiana OMV", 40, 32, "15"),
    Region("me", "Maine", "Maine BMV", 30, 24, "15"),
    Region("md", "Maryland", "Maryland MVA", 25, 22, "15¾", "88% pass mark — the highest bar in the country."),
    Region("ma", "Massachusetts", "Massachusetts RMV", 25, 18, "16", "25-minute time limit."),
    Region("mi", "Michigan", "Michigan Secretary of State", 50, 40, "14¾"),
    Region("mn", "Minnesota", "Driver and Vehicle Services (DVS)", 40, 32, "15", "Test ends early once you pass (32 correct) or fail (9 incorrect)."),
    Region("ms", "Mississippi", "DPS Driver Service Bureau", 30, 24, "15"),
    Region("mo", "Missouri", "Missouri DOR / Highway Patrol", 25, 20, "15"),
    Region("mt", "Montana", "Montana MVD", 33, 27, "14½"),
    Region("ne", "Nebraska", "Nebraska DMV", 25, 20, "15"),
    Region("nv", "Nevada", "Nevada DMV", 50, 40, "15½"),
    Region("nh", "New Hampshire", "New Hampshire DMV", 40, 32, "15½", "No separate learner permit: supervised driving from 15½."),
    Region("nj", "New Jersey", "New Jersey MVC", 50, 40, "16"),
    Region("nm", "New Mexico", "New Mexico MVD", 25, 18, "15"),
    Region("ny", "New York", "New York DMV", 20, 14, "16", "You must also answer at least 2 of the 4 road-sign questions correctly."),
    Region("nc", "North Carolina", "NCDMV", 25, 20, "15"),
    Region("nd", "North Dakota", "NDDOT Driver License", 25, 20, "14"),
    Region("oh", "Ohio", "Ohio BMV", 40, 30, "15½", "Two 20-question sections (signs and rules); 15 correct needed in each."),
    Region("ok", "Oklahoma", "Service Oklahoma", 50, 40, "15½"),
    Region("or", "Oregon", "Oregon DMV", 35, 28, "15"),
    Region("pa", "Pennsylvania", "PennDOT", 18, 15, "16", "Shortest test in the US — only 3 mistakes allowed."),
    Region("ri", "Rhode Island", "Rhode Island DMV", 25, 20, "16"),
    Region("sc", "South Carolina", "SCDMV", 30, 24, "15"),
    Region("sd", "South Dakota", "South Dakota DPS", 25, 20, "14"),
    Region("tn", "Tennessee", "Tennessee Driver Services", 30, 24, "15"),
    Region("tx", "Texas", "Texas DPS", 30, 21, "15", "70% pass mark. Teens may take the test through an approved driver-ed course."),
    Region("ut", "Utah", "Utah Driver License Division", 50, 40, "15"),
    Region("vt", "Vermont", "Vermont DMV", 20, 16, "15"),
    Region("va", "Virginia", "Virginia DMV", 40, 34, "15½", "Part 1: 10 road-sign questions — all 10 must be correct. Part 2: 30 general questions, 24 correct to pass."),
    Region("wa", "Washington", "Washington DOL", 40, 32, "15", "45-minute time limit."),
    Region("wv", "West Virginia", "West Virginia DMV", 25, 19, "15"),
    Region("wi", "Wisconsin", "Wisconsin DMV", 50, 40, "15½"),
    Region("wy", "Wyoming", "WYDOT Driver Services", 25, 20, "15")
)

val CanadaProvinces: List<Region> = listOf(
    Region("on", "Ontario", "MTO / DriveTest", 40, 32, "16", "G1 test: 20 sign + 20 rule questions, 16 correct needed in EACH. Then G2 road test, then G road test."),
    Region("bc", "British Columbia", "ICBC", 50, 40, "16", "Knowledge test for the Learner (L) licence, then Class 7 (N) and Class 5 road tests."),
    Region("ab", "Alberta", "Alberta Registries", 30, 25, "14", "Class 7 learner knowledge test, then Class 5 GDL and advanced road tests."),
    Region("qc", "Quebec", "SAAQ", 24, 18, "16", "Learner licence knowledge exam (75% pass) after the theory modules of the driving course.")
)

val AustraliaStates: List<Region> = listOf(
    Region("nsw", "New South Wales", "Transport for NSW / Service NSW", 45, 41, "16", "Driver Knowledge Test: 15 general (12 correct) + 30 road safety (29 correct). Hazard Perception Test before the P1 driving test."),
    Region("vic", "Victoria", "VicRoads", 32, 25, "16", "Learner Permit Test, then Hazard Perception Test and Drive Test for the P plate."),
    Region("qld", "Queensland", "TMR Queensland", 30, 27, "16", "Written road rules test (90%), then the practical driving test."),
    Region("wa", "Western Australia", "Department of Transport WA", 30, 24, "16", "Computerised theory test, then Hazard Perception Test and practical assessment.")
)
