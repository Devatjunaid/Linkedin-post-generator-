package com.voltora.evcalculator.data

/** The test cycle a rated range comes from. Real-world consumption differs per cycle. */
enum class Cycle(val label: String, val realWorldFactor: Double) {
    WLTP("WLTP", 1.12),
    EPA("EPA", 1.00),
    CLTC("CLTC", 1.30),
    MIDC("MIDC", 1.35),
}

/**
 * One EV variant.
 *
 * batteryKwh  – usable (net) capacity
 * rangeKm     – official rated range for [cycle]
 * maxAcKw     – on-board charger limit
 * maxDcKw     – peak DC charging power
 * dc10to80Min – manufacturer-quoted 10→80 % DC time at a sufficiently fast charger
 */
data class EvModel(
    val id: String,
    val brand: String,
    val name: String,
    val batteryKwh: Double,
    val rangeKm: Int,
    val cycle: Cycle,
    val maxAcKw: Double,
    val maxDcKw: Double,
    val dc10to80Min: Int,
) {
    val fullName: String get() = "$brand $name"

    /** Consumption implied by the official figure, in kWh/100 km. */
    val ratedKwhPer100: Double get() = batteryKwh / rangeKm * 100.0

    /** Typical mixed real-world consumption, in kWh/100 km. */
    val realKwhPer100: Double get() = ratedKwhPer100 * cycle.realWorldFactor
}

object EvDatabase {
    private fun ev(
        brand: String, name: String, kwh: Double, range: Int, cycle: Cycle,
        ac: Double, dc: Double, min: Int,
    ) = EvModel(
        id = (brand + "-" + name).lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-'),
        brand = brand, name = name, batteryKwh = kwh, rangeKm = range, cycle = cycle,
        maxAcKw = ac, maxDcKw = dc, dc10to80Min = min,
    )

    private val W = Cycle.WLTP
    private val E = Cycle.EPA
    private val C = Cycle.CLTC
    private val M = Cycle.MIDC

    val all: List<EvModel> = listOf(
        // Tesla
        ev("Tesla", "Model 3 RWD", 57.5, 513, W, 11.0, 170.0, 25),
        ev("Tesla", "Model 3 Long Range AWD", 75.0, 629, W, 11.0, 250.0, 27),
        ev("Tesla", "Model 3 Performance", 75.0, 528, W, 11.0, 250.0, 27),
        ev("Tesla", "Model Y RWD", 60.0, 500, W, 11.0, 175.0, 27),
        ev("Tesla", "Model Y Long Range AWD", 75.0, 568, W, 11.0, 250.0, 27),
        ev("Tesla", "Model S Long Range", 95.0, 634, W, 11.0, 250.0, 30),
        ev("Tesla", "Model X Long Range", 95.0, 576, W, 11.0, 250.0, 30),
        ev("Tesla", "Cybertruck AWD", 123.0, 512, E, 11.5, 325.0, 30),
        // BYD
        ev("BYD", "Dolphin Surf", 43.2, 322, W, 11.0, 85.0, 30),
        ev("BYD", "Dolphin 60 kWh", 60.4, 427, W, 11.0, 88.0, 40),
        ev("BYD", "Atto 3", 60.5, 420, W, 11.0, 88.0, 45),
        ev("BYD", "Seal AWD Excellence", 82.5, 520, W, 11.0, 150.0, 32),
        ev("BYD", "Seal U Design", 87.0, 500, W, 11.0, 140.0, 35),
        ev("BYD", "Han EV", 85.4, 521, W, 11.0, 120.0, 30),
        // Hyundai / Genesis
        ev("Hyundai", "Inster Long Range", 49.0, 370, W, 11.0, 85.0, 30),
        ev("Hyundai", "Kona Electric 65 kWh", 64.8, 514, W, 11.0, 102.0, 41),
        ev("Hyundai", "Ioniq 5 Long Range RWD", 80.0, 570, W, 11.0, 235.0, 18),
        ev("Hyundai", "Ioniq 6 Long Range RWD", 74.0, 614, W, 11.0, 235.0, 18),
        ev("Genesis", "GV60", 74.0, 470, W, 11.0, 233.0, 18),
        // Kia
        ev("Kia", "Niro EV", 64.8, 460, W, 11.0, 80.0, 45),
        ev("Kia", "EV3 Long Range", 78.0, 605, W, 11.0, 128.0, 31),
        ev("Kia", "EV6 Long Range RWD", 80.0, 582, W, 11.0, 258.0, 18),
        ev("Kia", "EV9 AWD", 96.0, 505, W, 11.0, 210.0, 24),
        // Volkswagen group
        ev("Volkswagen", "ID.3 Pro S", 77.0, 559, W, 11.0, 175.0, 30),
        ev("Volkswagen", "ID.4 Pro", 77.0, 550, W, 11.0, 175.0, 28),
        ev("Volkswagen", "ID.7 Pro S", 86.0, 709, W, 11.0, 200.0, 26),
        ev("Volkswagen", "ID. Buzz Pro", 79.0, 423, W, 11.0, 185.0, 26),
        ev("Skoda", "Enyaq 85", 77.0, 580, W, 11.0, 175.0, 28),
        ev("Skoda", "Elroq 85", 77.0, 580, W, 11.0, 175.0, 28),
        ev("Cupra", "Born 77 kWh", 77.0, 548, W, 11.0, 185.0, 30),
        ev("Audi", "Q4 e-tron 45", 77.0, 530, W, 11.0, 175.0, 28),
        ev("Audi", "Q8 e-tron 55", 106.0, 582, W, 11.0, 170.0, 31),
        ev("Audi", "e-tron GT quattro", 97.0, 609, W, 11.0, 320.0, 18),
        ev("Porsche", "Taycan (2025)", 97.0, 678, W, 11.0, 320.0, 18),
        ev("Porsche", "Macan 4 Electric", 95.0, 613, W, 11.0, 270.0, 21),
        // BMW / Mini
        ev("BMW", "i4 eDrive40", 81.3, 590, W, 11.0, 205.0, 31),
        ev("BMW", "i5 eDrive40", 81.2, 582, W, 11.0, 205.0, 30),
        ev("BMW", "iX1 eDrive20", 64.7, 475, W, 11.0, 130.0, 29),
        ev("BMW", "iX3", 74.0, 460, W, 11.0, 150.0, 32),
        ev("BMW", "iX xDrive50", 105.2, 633, W, 22.0, 195.0, 35),
        ev("Mini", "Cooper SE", 49.2, 402, W, 11.0, 95.0, 30),
        // Mercedes-Benz
        ev("Mercedes-Benz", "EQA 250+", 70.5, 528, W, 11.0, 100.0, 35),
        ev("Mercedes-Benz", "EQE 350+", 89.0, 654, W, 11.0, 170.0, 32),
        ev("Mercedes-Benz", "EQS 450+", 108.4, 780, W, 11.0, 200.0, 31),
        // Volvo / Polestar
        ev("Volvo", "EX30 Extended Range", 64.0, 476, W, 11.0, 153.0, 26),
        ev("Volvo", "EX40 Single Motor ER", 79.0, 575, W, 11.0, 200.0, 28),
        ev("Volvo", "EX90 Twin Motor", 107.0, 600, W, 11.0, 250.0, 30),
        ev("Polestar", "Polestar 2 LR Single Motor", 79.0, 654, W, 11.0, 205.0, 28),
        ev("Polestar", "Polestar 4 LR Single Motor", 94.0, 610, W, 22.0, 200.0, 30),
        // Stellantis / Renault
        ev("Peugeot", "e-208", 48.1, 410, W, 11.0, 100.0, 30),
        ev("Peugeot", "e-3008 73 kWh", 73.0, 527, W, 11.0, 160.0, 30),
        ev("Fiat", "500e 42 kWh", 37.3, 320, W, 11.0, 85.0, 35),
        ev("Renault", "5 E-Tech 52 kWh", 52.0, 410, W, 11.0, 100.0, 30),
        ev("Renault", "Megane E-Tech 60 kWh", 60.0, 450, W, 22.0, 130.0, 30),
        // Japan
        ev("Nissan", "Leaf 40 kWh", 39.0, 270, W, 6.6, 50.0, 60),
        ev("Nissan", "Leaf e+ 59 kWh", 59.0, 385, W, 6.6, 100.0, 50),
        ev("Nissan", "Ariya 87 kWh", 87.0, 533, W, 7.4, 130.0, 35),
        ev("Toyota", "bZ4X FWD", 64.0, 513, W, 11.0, 150.0, 30),
        ev("Honda", "e:Ny1", 61.9, 412, W, 11.0, 78.0, 45),
        // US brands
        ev("Ford", "Mustang Mach-E Extended RWD", 91.0, 600, W, 11.0, 150.0, 37),
        ev("Ford", "Explorer EV Extended RWD", 77.0, 602, W, 11.0, 135.0, 26),
        ev("Ford", "F-150 Lightning Extended", 131.0, 515, E, 19.2, 155.0, 41),
        ev("Chevrolet", "Bolt EV", 65.0, 417, E, 11.0, 55.0, 60),
        ev("Chevrolet", "Equinox EV FWD", 85.0, 513, E, 11.5, 150.0, 40),
        ev("Rivian", "R1T Large Pack", 135.0, 563, E, 11.5, 220.0, 36),
        ev("Rivian", "R1S Large Pack", 135.0, 566, E, 11.5, 220.0, 36),
        ev("Lucid", "Air Grand Touring", 112.0, 824, E, 19.2, 300.0, 30),
        // China
        ev("MG", "MG4 Long Range", 74.4, 520, W, 11.0, 144.0, 35),
        ev("MG", "ZS EV Long Range", 68.3, 440, W, 11.0, 92.0, 42),
        ev("Xpeng", "G6 Long Range", 87.5, 570, W, 11.0, 280.0, 20),
        ev("NIO", "ET5 100 kWh", 90.0, 560, W, 11.0, 180.0, 30),
        ev("Zeekr", "001 Long Range", 100.0, 620, W, 22.0, 200.0, 20),
        ev("Smart", "#1 Pro+", 62.0, 420, W, 22.0, 150.0, 30),
        ev("Leapmotor", "C10", 69.9, 420, W, 11.0, 84.0, 30),
        ev("BYD", "Song Plus EV", 71.8, 520, C, 7.0, 90.0, 35),
        // India / Vietnam
        ev("Tata", "Punch EV Long Range", 35.0, 421, M, 7.2, 50.0, 56),
        ev("Tata", "Nexon EV 45", 45.0, 489, M, 7.2, 60.0, 40),
        ev("Mahindra", "BE 6 Pack Two", 79.0, 682, M, 11.0, 175.0, 20),
        ev("Mahindra", "XEV 9e Pack Two", 79.0, 656, M, 11.0, 175.0, 20),
        ev("VinFast", "VF 8 Eco", 87.7, 471, W, 11.0, 150.0, 31),
        // Others
        ev("Jaguar", "I-Pace EV400", 84.7, 470, W, 11.0, 104.0, 45),
    ).sortedWith(compareBy({ it.brand }, { it.name }))

    val brands: List<String> = all.map { it.brand }.distinct().sorted()

    fun byId(id: String?): EvModel? = all.firstOrNull { it.id == id }

    val default: EvModel get() = byId("tesla-model-3-long-range-awd") ?: all.first()
}
