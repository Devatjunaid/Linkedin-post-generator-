package com.glowdash.warningidentifier.data

import com.glowdash.warningidentifier.data.LightColor.AMBER
import com.glowdash.warningidentifier.data.LightColor.BLUE
import com.glowdash.warningidentifier.data.LightColor.GREEN
import com.glowdash.warningidentifier.data.LightColor.RED
import com.glowdash.warningidentifier.data.Severity.CAUTION
import com.glowdash.warningidentifier.data.Severity.INFO
import com.glowdash.warningidentifier.data.Severity.STOP

/**
 * Offline database of dashboard symbols. Everything here ships inside the app,
 * so lookups are instant and need no network or API.
 *
 * List order doubles as "how common is this light", which is used to rank
 * candidates when the camera only detects a colour.
 */
object LightsCatalog {

    private const val BRAKE_ARCS = "M4 7a10 10 0 0 0 0 10M20 7a10 10 0 0 1 0 10"
    private const val CAR = "M5 11l2-5h10l2 5v4H5zM7 15v2M17 15v2M5 20c1.5-1.5 3-1.5 4.5 0M14.5 20c1.5-1.5 3-1.5 4.5 0"

    val all: List<WarningLight> = listOf(
        WarningLight(
            id = "check_engine", name = "Check Engine", aka = "Malfunction Indicator Lamp (MIL)",
            color = AMBER, severity = CAUTION,
            iconPath = "M3 10v6M3 13h2M5 9h3V7h5v2h3l2 2h2v-2h1v8h-1v-2h-2v2l-2 2H8l-3-3zM9.5 5h4",
            summary = "Engine or emissions fault",
            meaning = "The engine computer found a fault in the engine, ignition, fuel or emissions system. Causes range from a loose fuel cap to a failing oxygen sensor, a misfire or the catalytic converter.",
            steps = listOf(
                "Check the fuel cap is tight, then drive normally for a few trips.",
                "Have the OBD-II fault code read — many parts stores do this for free.",
                "Book a service if the light stays on.",
            ),
            canDrive = "Usually yes, for a short time. Avoid heavy loads and long trips until it is checked.",
            flashingNote = "Flashing means a severe misfire that can damage the catalytic converter. Slow down and stop driving as soon as it is safe.",
            keywords = "engine mil obd emissions motor",
        ),
        WarningLight(
            id = "oil_pressure", name = "Oil Pressure", aka = "Low engine oil pressure",
            color = RED, severity = STOP,
            iconPath = "M2 13l4-2h6l2-2 8 3-7 5H6zM20 16v2M8 11V8h3M6.5 8h3",
            summary = "Stop engine, check oil",
            meaning = "Oil pressure has dropped too low to protect the engine. It can be a low oil level, a leak or a failed oil pump.",
            steps = listOf(
                "Pull over safely and switch the engine off immediately.",
                "Wait a few minutes, then check the oil level with the dipstick.",
                "Top up if low. If the level is fine or the light comes back, do not drive — call for assistance.",
            ),
            canDrive = "No. Driving without oil pressure can destroy the engine within minutes.",
            keywords = "oil can lubrication",
        ),
        WarningLight(
            id = "battery", name = "Battery / Charging", aka = "Charging system warning",
            color = RED, severity = CAUTION,
            iconPath = "M3 7h18v12H3zM7 4v3M17 4v3M6 13h4M15.5 11v4M13.5 13h4",
            summary = "Alternator not charging",
            meaning = "The charging system isn't working properly — usually the alternator, its drive belt, or the battery and its connections.",
            steps = listOf(
                "Turn off non-essential electrics (A/C, heated seats, radio).",
                "Drive straight to a garage or home, avoiding switching the engine off on the way.",
                "Watch the temperature gauge — on many cars the same belt drives the water pump.",
                "Have the alternator, belt and battery tested.",
            ),
            canDrive = "Only a short distance. The car runs on battery power until it is drained, then stops.",
            keywords = "battery alternator charging electrical volt",
        ),
        WarningLight(
            id = "coolant_temp", name = "Engine Temperature", aka = "Coolant temperature warning",
            color = RED, severity = STOP,
            iconPath = "M12 3v10M12 4h3M12 7h3M12 10h3M10 15a2 2 0 1 0 4 0a2 2 0 1 0-4 0M3 20c2-1.5 4-1.5 6 0s4 1.5 6 0 4-1.5 6 0",
            summary = "Overheating — pull over",
            meaning = "The engine is overheating. Low coolant, a leak, or a failed fan, thermostat or water pump are common causes. (If this symbol is blue, the engine is simply still cold.)",
            steps = listOf(
                "Pull over safely and turn the engine off.",
                "Let it cool for at least 30 minutes. Never open a hot radiator or coolant cap.",
                "Once cool, check the coolant level and top up if needed.",
                "If it overheats again, call for assistance.",
            ),
            canDrive = "No. Continuing can warp the cylinder head and wreck the engine.",
            keywords = "temperature coolant overheating radiator thermometer",
        ),
        WarningLight(
            id = "brake", name = "Brake System", aka = "Brake warning light",
            color = RED, severity = STOP,
            iconPath = "M6 12a6 6 0 1 0 12 0a6 6 0 1 0-12 0M12 9v3M12 15h.01$BRAKE_ARCS",
            summary = "Low fluid or brake fault",
            meaning = "Either the parking brake is applied, the brake fluid is low, or there is a fault in the brake system.",
            steps = listOf(
                "Make sure the parking brake is fully released.",
                "If the light stays on, check the brake fluid level.",
                "If the pedal feels soft or the fluid is low, stop driving and have the car towed.",
            ),
            canDrive = "Not if the parking brake is off and the light stays on — your brakes may not work properly.",
            keywords = "brake fluid exclamation circle",
        ),
        WarningLight(
            id = "abs", name = "ABS", aka = "Anti-lock Braking System",
            color = AMBER, severity = CAUTION,
            iconPath = "M5 12a7 7 0 1 0 14 0a7 7 0 1 0-14 0M2.5 7a11 11 0 0 0 0 10M21.5 7a11 11 0 0 1 0 10",
            iconLabel = "ABS", iconLabelSize = 5.5f,
            summary = "Anti-lock brakes off",
            meaning = "The Anti-lock Braking System has a fault and is switched off. Normal braking still works, but wheels may lock under hard braking.",
            steps = listOf(
                "Drive gently and leave extra stopping distance.",
                "Have the ABS sensors and module checked soon.",
            ),
            canDrive = "Yes, carefully. If the red brake light is also on, stop driving.",
            keywords = "abs antilock brakes",
        ),
        WarningLight(
            id = "tpms", name = "Tire Pressure", aka = "TPMS — tyre pressure monitor",
            color = AMBER, severity = CAUTION,
            iconPath = "M7 20c-2-2-3-4.5-3-7.5S5 6 7 4M17 20c2-2 3-4.5 3-7.5S19 6 17 4M5 20h14M12 8v5M12 16h.01",
            summary = "One or more tires low",
            meaning = "One or more tires are significantly under-inflated. If the light flashes at start-up and then stays on, the monitoring system itself has a fault.",
            steps = listOf(
                "Slow down and check all tires visually.",
                "Check pressures with a gauge and inflate to the value on the driver's door sticker.",
                "Reset the system if your car requires it.",
            ),
            canDrive = "Yes, slowly, to the nearest air pump. Do not drive on a flat tire.",
            keywords = "tyre tire pressure tpms flat horseshoe",
        ),
        WarningLight(
            id = "airbag", name = "Airbag", aka = "SRS — Supplemental Restraint System",
            color = RED, severity = CAUTION,
            iconPath = "M5 4a2 2 0 1 0 4 0a2 2 0 1 0-4 0M7 7v7h5l2 6M5 10h4M13 10a4 4 0 1 0 8 0a4 4 0 1 0-8 0",
            summary = "Airbag system fault",
            meaning = "The airbag system has a fault. Airbags or seat-belt pre-tensioners may not deploy in a crash.",
            steps = listOf(
                "Make sure nothing is blocking the seat sensors or wiring under the seats.",
                "Have the SRS system diagnosed as soon as possible.",
            ),
            canDrive = "The car drives normally, but you may not be protected in a crash — get it fixed promptly.",
            keywords = "airbag srs seat person",
        ),
        WarningLight(
            id = "power_steering", name = "Power Steering", aka = "EPS / steering assist fault",
            color = AMBER, severity = CAUTION,
            iconPath = "M5 12a7 7 0 1 0 14 0a7 7 0 1 0-14 0M12 8v4M12 15h.01M1.5 9v6M22.5 9v6",
            summary = "Steering may go heavy",
            meaning = "Power steering assistance has a fault. Steering can suddenly become much heavier.",
            steps = listOf(
                "Grip the wheel firmly and drive slowly.",
                "Restart the engine once safely stopped — this can clear an electrical glitch.",
                "On hydraulic systems, check the power steering fluid, then book a service.",
            ),
            canDrive = "Yes with care, but steering will be heavy, especially at low speed.",
            keywords = "steering wheel eps",
        ),
        WarningLight(
            id = "stability", name = "Stability Control", aka = "ESC / ESP / Traction control",
            color = AMBER, severity = CAUTION,
            iconPath = CAR,
            summary = "Flashing = active, steady = fault",
            meaning = "Flashing: the system is actively controlling wheel spin on a slippery surface. Steady: the system has a fault or is disabled.",
            steps = listOf(
                "If it flashes, ease off the accelerator and drive smoothly.",
                "If it stays on, restart the car. If it remains lit, have it checked.",
            ),
            canDrive = "Yes. Take extra care on wet or icy roads without stability control.",
            keywords = "esc esp traction skid car squiggle slip",
        ),
        WarningLight(
            id = "stability_off", name = "Stability Control Off", aka = "ESC OFF / TCS OFF",
            color = AMBER, severity = INFO,
            iconPath = "${CAR}M3 3l18 18",
            summary = "Traction aid switched off",
            meaning = "Traction or stability control has been switched off, usually with the button on the dashboard.",
            steps = listOf(
                "Press the ESC/TCS OFF button to turn it back on.",
                "If it won't turn back on, have the system checked.",
            ),
            canDrive = "Yes, but the car has less grip control in bad conditions.",
            keywords = "esc off tcs off traction off",
        ),
        WarningLight(
            id = "low_fuel", name = "Low Fuel", aka = "Fuel reserve warning",
            color = AMBER, severity = INFO,
            iconPath = "M4 21V5a2 2 0 0 1 2-2h6a2 2 0 0 1 2 2v16M3 21h12M6 8h6M14 10h2a2 2 0 0 1 2 2v4a1.5 1.5 0 0 0 3 0V8l-3-3",
            summary = "Refuel soon",
            meaning = "The fuel level is low — typically around 50–80 km (30–50 miles) of range remains, depending on the car.",
            steps = listOf(
                "Refuel at the next opportunity.",
                "Avoid running the tank dry; it can damage the fuel pump.",
            ),
            canDrive = "Yes, to the nearest fuel station.",
            keywords = "fuel gas petrol diesel pump tank",
        ),
        WarningLight(
            id = "seat_belt", name = "Seat Belt Reminder", aka = "Unfastened seat belt",
            color = RED, severity = INFO,
            iconPath = "M10 4a2 2 0 1 0 4 0a2 2 0 1 0-4 0M8 21v-6l-2-6h12l-2 6v6M9 9l7 8",
            summary = "Someone isn't buckled up",
            meaning = "The driver or a passenger isn't wearing a seat belt.",
            steps = listOf(
                "Fasten all seat belts.",
                "If it stays on with belts fastened, check nothing heavy is resting on a passenger seat.",
            ),
            canDrive = "Buckle up first.",
            keywords = "seatbelt belt person",
        ),
        WarningLight(
            id = "door_open", name = "Door Open", aka = "Door / boot ajar",
            color = RED, severity = CAUTION,
            iconPath = "M8 3h8l1 4v13H7V7zM7 10l-4 4M17 10l4 4",
            summary = "A door isn't fully shut",
            meaning = "A door, the boot/trunk or the tailgate is not fully closed.",
            steps = listOf(
                "Stop safely and close every door firmly, including the boot.",
                "If all doors are shut, a door switch may be faulty.",
            ),
            canDrive = "Not until every door is properly closed.",
            keywords = "door ajar trunk boot tailgate",
        ),
        WarningLight(
            id = "hood_open", name = "Hood Open", aka = "Bonnet not latched",
            color = RED, severity = CAUTION,
            iconPath = "M3 16l2-5h14l2 5v3H3zM5 11l4-6h8M7 19v2M17 19v2",
            summary = "Bonnet not latched",
            meaning = "The hood/bonnet is not fully latched and could fly up while driving.",
            steps = listOf(
                "Stop safely and close the hood firmly until it clicks.",
                "If it is closed, the latch sensor may need adjusting.",
            ),
            canDrive = "Not until the hood is securely latched.",
            keywords = "hood bonnet open",
        ),
        WarningLight(
            id = "parking_brake", name = "Parking Brake", aka = "Handbrake / EPB",
            color = RED, severity = CAUTION,
            iconPath = "M6 12a6 6 0 1 0 12 0a6 6 0 1 0-12 0$BRAKE_ARCS",
            iconLabel = "P", iconLabelSize = 8f,
            summary = "Handbrake is on",
            meaning = "The parking (hand) brake is applied, or the electronic parking brake has a fault.",
            steps = listOf(
                "Release the parking brake fully before driving.",
                "If it won't release or keeps flashing, have the system checked.",
            ),
            canDrive = "Not with the parking brake applied.",
            keywords = "handbrake parking brake p epb",
        ),
        WarningLight(
            id = "transmission_temp", name = "Transmission Temperature", aka = "Gearbox overheating",
            color = AMBER, severity = CAUTION,
            iconPath = "M12 7a5 5 0 1 0 0 10a5 5 0 1 0 0-10M12 2v3M12 19v3M2 12h3M19 12h3M4.9 4.9L7 7M17 17l2.1 2.1M4.9 19.1L7 17M17 7l2.1-2.1M12 10v3",
            summary = "Gearbox running hot",
            meaning = "The automatic gearbox fluid is overheating, often from towing, stop-and-go traffic or low fluid.",
            steps = listOf(
                "Pull over and let the engine idle in Park so the fluid cools.",
                "Check the transmission fluid if your car has a dipstick.",
                "Have it inspected if it happens again.",
            ),
            canDrive = "Not until it has cooled down.",
            keywords = "transmission gearbox gear temperature",
        ),
        WarningLight(
            id = "glow_plug", name = "Glow Plug", aka = "Diesel pre-heat",
            color = AMBER, severity = CAUTION,
            iconPath = "M3 6h4M17 6h4M7 6c0 3 3 3 3 6s-3 3-3 6M17 6c0 3-3 3-3 6s3 3 3 6M7 18h10",
            summary = "Wait before starting (diesel)",
            meaning = "Steady after switching on: the glow plugs are warming the engine — wait for it to go out before starting. Flashing or on while driving: an engine management fault.",
            steps = listOf(
                "Wait for the light to go out before cranking the engine.",
                "If it flashes while driving, have the engine checked soon.",
            ),
            canDrive = "Yes once it goes out. If it flashes, drive gently to a garage.",
            flashingNote = "A flashing glow plug light usually means an engine fault — reduce load and get it diagnosed.",
            keywords = "diesel glow plug coil preheat",
        ),
        WarningLight(
            id = "dpf", name = "Diesel Particulate Filter", aka = "DPF warning",
            color = AMBER, severity = CAUTION,
            iconPath = "M3 7h18v10H3zM7 12h.01M11 12h.01M15 12h.01M9 10h.01M13 10h.01M9 14h.01M13 14h.01M21 10h2M1 14h2",
            summary = "Filter needs to clean itself",
            meaning = "The DPF is filling with soot and needs to regenerate (burn it off).",
            steps = listOf(
                "Drive at a steady 60+ km/h (40+ mph) for 15–20 minutes on a main road.",
                "If the light stays on, visit a garage before the filter blocks completely.",
            ),
            canDrive = "Yes — a sustained drive is often the fix.",
            keywords = "dpf diesel particulate filter soot exhaust",
        ),
        WarningLight(
            id = "adblue", name = "AdBlue / DEF Low", aka = "Diesel exhaust fluid",
            color = AMBER, severity = CAUTION,
            iconPath = "M12 3c3 4 6 7 6 10a6 6 0 0 1-12 0c0-3 3-6 6-10z",
            iconLabel = "DEF", iconLabelSize = 4.2f,
            summary = "Refill exhaust fluid",
            meaning = "Diesel exhaust fluid is running low. Many cars refuse to restart once it runs out.",
            steps = listOf(
                "Refill AdBlue/DEF at a fuel station or from a bottle bought at an auto store.",
                "Never put AdBlue in the fuel tank.",
            ),
            canDrive = "Yes, but refill soon to avoid a no-start.",
            keywords = "adblue def urea diesel exhaust fluid drop",
        ),
        WarningLight(
            id = "key_missing", name = "Key Not Detected", aka = "Smart key warning",
            color = AMBER, severity = CAUTION,
            iconPath = "M3 12a4 4 0 1 0 8 0a4 4 0 1 0-8 0M11 12h10M18 12v3M15 12v2",
            summary = "Key fob not found",
            meaning = "The car can't detect the smart key — very often the key fob battery is weak.",
            steps = listOf(
                "Hold the key against the start button or the marked spot on the steering column.",
                "Replace the key fob battery (commonly a CR2032 coin cell).",
            ),
            canDrive = "The car keeps running, but it may not restart after you switch it off.",
            keywords = "key fob immobilizer smart key",
        ),
        WarningLight(
            id = "service", name = "Service Reminder", aka = "Maintenance due",
            color = AMBER, severity = INFO,
            iconPath = "M14.7 6.3a4 4 0 0 0-5.4 5.4L3 18l3 3 6.3-6.3a4 4 0 0 0 5.4-5.4l-2.5 2.5-2.4-.6-.6-2.4z",
            summary = "Scheduled service due",
            meaning = "Scheduled maintenance such as an oil change or inspection is due.",
            steps = listOf(
                "Book a service soon.",
                "Have the reminder reset after the service.",
            ),
            canDrive = "Yes.",
            keywords = "service wrench spanner maintenance oil change",
        ),
        WarningLight(
            id = "bulb_failure", name = "Exterior Bulb Failure", aka = "Lamp out",
            color = AMBER, severity = INFO,
            iconPath = "M9 18h6M10 21h4M12 3a6 6 0 0 0-4 10.5c.7.7 1 1.5 1 2.5h6c0-1 .3-1.8 1-2.5A6 6 0 0 0 12 3zM12 7v4M12 13h.01",
            summary = "A light bulb has failed",
            meaning = "One of the exterior lights (brake, indicator, headlight, number plate) has failed.",
            steps = listOf(
                "Walk around the car with the lights on to find the failed bulb.",
                "Replace it — a failed brake light or headlight is a safety and legal issue.",
            ),
            canDrive = "Yes, but replace the bulb soon.",
            keywords = "bulb lamp light failure",
        ),
        WarningLight(
            id = "washer_fluid", name = "Washer Fluid Low", aka = "Screen wash low",
            color = AMBER, severity = INFO,
            iconPath = "M3 18c5-4 13-4 18 0M12 14V8M8 9L6 5M16 9l2-4M12 5V3M5 21h14",
            summary = "Top up screen wash",
            meaning = "The windscreen washer reservoir is low.",
            steps = listOf("Top up with screen-wash fluid (use winter fluid in freezing weather)."),
            canDrive = "Yes.",
            keywords = "washer windscreen windshield wiper fluid",
        ),
        WarningLight(
            id = "frost", name = "Frost Warning", aka = "Low outside temperature",
            color = AMBER, severity = INFO,
            iconPath = "M12 2v20M3.3 7l17.4 10M3.3 17l17.4-10M9 4l3 2 3-2M9 20l3-2 3 2",
            summary = "Possible ice on the road",
            meaning = "The outside temperature is near or below freezing (around 3 °C / 37 °F).",
            steps = listOf("Watch for black ice, slow down and increase your following distance."),
            canDrive = "Yes, with extra care.",
            keywords = "frost ice snowflake cold",
        ),
        WarningLight(
            id = "rear_fog", name = "Rear Fog Light", aka = "Rear fog lamp on",
            color = AMBER, severity = INFO,
            iconPath = "M10 6c-4 0-6 3-6 6s2 6 6 6zM13 8h8M13 12h8M13 16h8M16.5 6c-1 4-1 8 0 12",
            summary = "Rear fog lamp is on",
            meaning = "The rear fog light is switched on.",
            steps = listOf("Switch it off once visibility improves — it can dazzle drivers behind you."),
            canDrive = "Yes.",
            keywords = "fog rear lamp",
        ),
        WarningLight(
            id = "high_beam", name = "High Beam", aka = "Main beam headlights",
            color = BLUE, severity = INFO,
            iconPath = "M10 6c-4 0-6 3-6 6s2 6 6 6zM14 7h7M14 10h7M14 14h7M14 17h7",
            summary = "Main headlights active",
            meaning = "Your main-beam (high-beam) headlights are on.",
            steps = listOf("Dip to low beam for oncoming traffic or when following another car."),
            canDrive = "Yes.",
            keywords = "high beam headlight full beam blue",
        ),
        WarningLight(
            id = "low_beam", name = "Low Beam", aka = "Dipped headlights",
            color = GREEN, severity = INFO,
            iconPath = "M14 6c4 0 6 3 6 6s-2 6-6 6zM3 8l8 2M3 12l8 2M3 16l8 2",
            summary = "Headlights are on",
            meaning = "Your dipped (low-beam) headlights are on.",
            steps = listOf("No action needed."),
            canDrive = "Yes.",
            keywords = "low beam dipped headlight",
        ),
        WarningLight(
            id = "front_fog", name = "Front Fog Lights", aka = "Front fog lamps on",
            color = GREEN, severity = INFO,
            iconPath = "M14 6c4 0 6 3 6 6s-2 6-6 6zM3 8h8M3 12h8M3 16h8M7.5 6c1 4 1 8 0 12",
            summary = "Front fog lamps are on",
            meaning = "The front fog lights are switched on.",
            steps = listOf("Switch them off when visibility improves."),
            canDrive = "Yes.",
            keywords = "fog front lamp",
        ),
        WarningLight(
            id = "turn_signal", name = "Turn Signals", aka = "Indicators / hazards",
            color = GREEN, severity = INFO,
            iconPath = "M2 12l5-5v3h3v4H7v3zM22 12l-5-5v3h-3v4h3v3z",
            summary = "Indicator is on",
            meaning = "A turn signal or the hazard lights are on. Flashing much faster than normal usually means a bulb has failed.",
            steps = listOf("Cancel the indicator after turning. Replace the bulb if it flashes fast."),
            canDrive = "Yes.",
            keywords = "indicator turn signal blinker hazard arrow",
        ),
        WarningLight(
            id = "cruise", name = "Cruise Control", aka = "Cruise active",
            color = GREEN, severity = INFO,
            iconPath = "M4 17a8 8 0 1 1 16 0M12 15l4-5M7 20h10",
            summary = "Cruise control is set",
            meaning = "Cruise control is switched on or holding a set speed.",
            steps = listOf("Tap the brake or the cancel button to disengage."),
            canDrive = "Yes.",
            keywords = "cruise speed gauge",
        ),
        WarningLight(
            id = "lane_assist", name = "Lane Assist", aka = "Lane keeping",
            color = GREEN, severity = INFO,
            iconPath = "M5 21L9 3M19 21L15 3M10 15l2-3 2 3M12 12v6",
            summary = "Lane keeping is active",
            meaning = "Green: lane keeping is active. Amber: it is unavailable, often because the lane markings or camera view are unclear.",
            steps = listOf("Keep the windscreen camera area clean. Stay alert — it is an aid, not autopilot."),
            canDrive = "Yes.",
            keywords = "lane assist keeping departure",
        ),
    )

    private val byId = all.associateBy { it.id }

    fun find(id: String): WarningLight? = byId[id]

    fun search(query: String, severity: Severity?): List<WarningLight> {
        val q = query.trim().lowercase()
        return all.filter { light ->
            (severity == null || light.severity == severity) &&
                (q.isEmpty() || light.name.lowercase().contains(q) ||
                    light.aka.lowercase().contains(q) || light.keywords.contains(q))
        }
    }

    fun byColors(colors: List<LightColor>): List<WarningLight> =
        colors.flatMap { c -> all.filter { it.color == c } }
}
