package com.harrycodess.fueltracker.data

enum class CarCategory(val label: String) {
    HATCHBACK("Hatchback"),
    SEDAN("Sedan"),
    SUV("SUV"),
    PICKUP("Pickup"),
    HYBRID("Hybrid"),
    MOTORCYCLE("Motorcycle"),
}

/**
 * A popular vehicle with its typical real-world combined fuel economy, stored in km/L.
 * Values are rounded averages for guidance only; real mileage depends on driving
 * style, traffic, load, maintenance and the exact variant.
 */
data class CatalogCar(
    val make: String,
    val model: String,
    val engine: String,
    val category: CarCategory,
    val kmPerL: Double,
) {
    val name: String get() = "$make $model"
    val id: String get() = "$make|$model|$engine"
}

object CarCatalog {
    private fun car(make: String, model: String, engine: String, category: CarCategory, kmPerL: Double) =
        CatalogCar(make, model, engine, category, kmPerL)

    val cars: List<CatalogCar> = listOf(
        // Hatchbacks
        car("Suzuki", "Alto", "0.66 L petrol", CarCategory.HATCHBACK, 22.0),
        car("Suzuki", "Wagon R", "1.0 L petrol", CarCategory.HATCHBACK, 20.0),
        car("Suzuki", "Swift", "1.2 L petrol", CarCategory.HATCHBACK, 20.0),
        car("Suzuki", "Cultus", "1.0 L petrol", CarCategory.HATCHBACK, 19.0),
        car("Suzuki", "Baleno", "1.2 L petrol", CarCategory.HATCHBACK, 19.5),
        car("Toyota", "Yaris Hatchback", "1.5 L petrol", CarCategory.HATCHBACK, 17.0),
        car("Honda", "Jazz / Fit", "1.5 L petrol", CarCategory.HATCHBACK, 17.0),
        car("Hyundai", "i10", "1.2 L petrol", CarCategory.HATCHBACK, 19.0),
        car("Hyundai", "i20", "1.2 L petrol", CarCategory.HATCHBACK, 18.0),
        car("Kia", "Picanto", "1.0 L petrol", CarCategory.HATCHBACK, 20.0),
        car("Volkswagen", "Polo", "1.0 L TSI", CarCategory.HATCHBACK, 18.0),
        car("Volkswagen", "Golf", "1.5 L TSI", CarCategory.HATCHBACK, 16.5),
        car("Ford", "Focus", "1.0 L EcoBoost", CarCategory.HATCHBACK, 17.0),
        car("Renault", "Clio", "1.0 L petrol", CarCategory.HATCHBACK, 18.0),
        car("Peugeot", "208", "1.2 L petrol", CarCategory.HATCHBACK, 18.0),
        car("Fiat", "500", "1.0 L mild hybrid", CarCategory.HATCHBACK, 19.0),
        car("Perodua", "Myvi", "1.5 L petrol", CarCategory.HATCHBACK, 18.0),
        car("Mazda", "Mazda2", "1.5 L petrol", CarCategory.HATCHBACK, 18.5),

        // Sedans
        car("Toyota", "Corolla", "1.8 L petrol", CarCategory.SEDAN, 15.0),
        car("Toyota", "Camry", "2.5 L petrol", CarCategory.SEDAN, 13.5),
        car("Toyota", "Yaris Sedan", "1.5 L petrol", CarCategory.SEDAN, 17.0),
        car("Honda", "Civic", "1.5 L turbo", CarCategory.SEDAN, 15.5),
        car("Honda", "City", "1.5 L petrol", CarCategory.SEDAN, 17.0),
        car("Honda", "Accord", "1.5 L turbo", CarCategory.SEDAN, 13.5),
        car("Hyundai", "Elantra", "2.0 L petrol", CarCategory.SEDAN, 15.5),
        car("Hyundai", "Sonata", "2.5 L petrol", CarCategory.SEDAN, 13.5),
        car("Kia", "Rio", "1.6 L petrol", CarCategory.SEDAN, 15.5),
        car("Kia", "K5", "1.6 L turbo", CarCategory.SEDAN, 13.5),
        car("Nissan", "Sentra", "2.0 L petrol", CarCategory.SEDAN, 14.5),
        car("Nissan", "Altima", "2.5 L petrol", CarCategory.SEDAN, 13.5),
        car("Volkswagen", "Jetta", "1.5 L TSI", CarCategory.SEDAN, 15.0),
        car("Volkswagen", "Passat", "2.0 L TSI", CarCategory.SEDAN, 12.0),
        car("Mazda", "Mazda3", "2.5 L petrol", CarCategory.SEDAN, 13.0),
        car("Chevrolet", "Malibu", "1.5 L turbo", CarCategory.SEDAN, 13.5),
        car("BMW", "3 Series (330i)", "2.0 L turbo", CarCategory.SEDAN, 12.5),
        car("BMW", "5 Series (530i)", "2.0 L turbo", CarCategory.SEDAN, 12.0),
        car("Mercedes-Benz", "C-Class (C300)", "2.0 L turbo", CarCategory.SEDAN, 11.5),
        car("Mercedes-Benz", "E-Class (E350)", "2.0 L turbo", CarCategory.SEDAN, 11.0),
        car("Audi", "A4", "2.0 L TFSI", CarCategory.SEDAN, 12.0),
        car("Skoda", "Octavia", "1.5 L TSI", CarCategory.SEDAN, 16.5),
        car("Suzuki", "Dzire", "1.2 L petrol", CarCategory.SEDAN, 20.0),
        car("Changan", "Alsvin", "1.5 L petrol", CarCategory.SEDAN, 16.0),
        car("Proton", "Saga", "1.3 L petrol", CarCategory.SEDAN, 16.5),

        // SUVs
        car("Toyota", "RAV4", "2.5 L petrol", CarCategory.SUV, 12.8),
        car("Toyota", "Fortuner", "2.8 L diesel", CarCategory.SUV, 10.0),
        car("Toyota", "Land Cruiser 300", "3.3 L diesel", CarCategory.SUV, 9.0),
        car("Honda", "CR-V", "1.5 L turbo", CarCategory.SUV, 12.8),
        car("Honda", "HR-V", "2.0 L petrol", CarCategory.SUV, 12.0),
        car("Hyundai", "Tucson", "2.5 L petrol", CarCategory.SUV, 12.0),
        car("Hyundai", "Creta", "1.5 L petrol", CarCategory.SUV, 15.0),
        car("Hyundai", "Santa Fe", "2.5 L petrol", CarCategory.SUV, 10.5),
        car("Kia", "Sportage", "2.5 L petrol", CarCategory.SUV, 12.0),
        car("Kia", "Seltos", "2.0 L petrol", CarCategory.SUV, 13.0),
        car("Kia", "Sorento", "2.5 L petrol", CarCategory.SUV, 11.0),
        car("Nissan", "Rogue / X-Trail", "1.5 L turbo", CarCategory.SUV, 14.0),
        car("Nissan", "Kicks", "1.6 L petrol", CarCategory.SUV, 14.0),
        car("Nissan", "Patrol", "4.0 L V6 petrol", CarCategory.SUV, 6.5),
        car("Ford", "Escape / Kuga", "1.5 L EcoBoost", CarCategory.SUV, 12.8),
        car("Ford", "Explorer", "2.3 L EcoBoost", CarCategory.SUV, 10.0),
        car("Chevrolet", "Equinox", "1.5 L turbo", CarCategory.SUV, 12.0),
        car("Chevrolet", "Tahoe", "5.3 L V8", CarCategory.SUV, 7.2),
        car("Jeep", "Wrangler", "3.6 L V6", CarCategory.SUV, 8.0),
        car("Jeep", "Grand Cherokee", "3.6 L V6", CarCategory.SUV, 9.4),
        car("Jeep", "Compass", "2.0 L turbo", CarCategory.SUV, 10.5),
        car("Volkswagen", "Tiguan", "2.0 L TSI", CarCategory.SUV, 11.0),
        car("Mazda", "CX-5", "2.5 L petrol", CarCategory.SUV, 12.0),
        car("Mazda", "CX-30", "2.5 L petrol", CarCategory.SUV, 12.3),
        car("Subaru", "Forester", "2.5 L petrol", CarCategory.SUV, 12.3),
        car("Subaru", "Outback", "2.5 L petrol", CarCategory.SUV, 12.3),
        car("BMW", "X5 (xDrive40i)", "3.0 L turbo", CarCategory.SUV, 9.8),
        car("Mercedes-Benz", "GLC 300", "2.0 L turbo", CarCategory.SUV, 10.5),
        car("Audi", "Q5", "2.0 L TFSI", CarCategory.SUV, 10.5),
        car("Suzuki", "Vitara", "1.4 L turbo", CarCategory.SUV, 15.0),
        car("Suzuki", "Jimny", "1.5 L petrol", CarCategory.SUV, 14.0),
        car("Suzuki", "Ertiga", "1.5 L petrol", CarCategory.SUV, 17.0),
        car("Tata", "Nexon", "1.2 L turbo", CarCategory.SUV, 15.0),
        car("Mahindra", "Scorpio-N", "2.2 L diesel", CarCategory.SUV, 12.0),
        car("Mahindra", "XUV700", "2.2 L diesel", CarCategory.SUV, 13.0),
        car("Renault / Dacia", "Duster", "1.3 L turbo", CarCategory.SUV, 15.0),
        car("Peugeot", "3008", "1.2 L turbo", CarCategory.SUV, 15.0),
        car("MG", "ZS", "1.5 L petrol", CarCategory.SUV, 14.0),
        car("MG", "HS", "1.5 L turbo", CarCategory.SUV, 12.0),
        car("Haval", "Jolion", "1.5 L turbo", CarCategory.SUV, 13.0),
        car("Haval", "H6", "1.5 L turbo", CarCategory.SUV, 12.0),

        // Pickups
        car("Toyota", "Hilux", "2.8 L diesel", CarCategory.PICKUP, 10.5),
        car("Ford", "F-150", "3.5 L EcoBoost", CarCategory.PICKUP, 8.5),
        car("Ford", "Ranger", "2.3 L EcoBoost", CarCategory.PICKUP, 9.4),
        car("Chevrolet", "Silverado 1500", "5.3 L V8", CarCategory.PICKUP, 7.7),
        car("Ram", "1500", "5.7 L V8", CarCategory.PICKUP, 8.0),
        car("Isuzu", "D-Max", "1.9 L diesel", CarCategory.PICKUP, 12.0),
        car("Nissan", "Navara / Frontier", "2.3 L diesel", CarCategory.PICKUP, 11.0),

        // Hybrids
        car("Toyota", "Prius", "1.8 L hybrid", CarCategory.HYBRID, 24.0),
        car("Toyota", "Corolla Hybrid", "1.8 L hybrid", CarCategory.HYBRID, 21.0),
        car("Toyota", "Corolla Cross Hybrid", "1.8 L hybrid", CarCategory.HYBRID, 18.0),
        car("Toyota", "Camry Hybrid", "2.5 L hybrid", CarCategory.HYBRID, 22.0),
        car("Toyota", "RAV4 Hybrid", "2.5 L hybrid", CarCategory.HYBRID, 17.0),
        car("Toyota", "Yaris Hybrid", "1.5 L hybrid", CarCategory.HYBRID, 23.0),
        car("Honda", "Accord Hybrid", "2.0 L hybrid", CarCategory.HYBRID, 20.0),
        car("Honda", "CR-V Hybrid", "2.0 L hybrid", CarCategory.HYBRID, 17.0),
        car("Hyundai", "Tucson Hybrid", "1.6 L hybrid", CarCategory.HYBRID, 16.0),
        car("Hyundai", "Ioniq Hybrid", "1.6 L hybrid", CarCategory.HYBRID, 23.0),
        car("Kia", "Niro Hybrid", "1.6 L hybrid", CarCategory.HYBRID, 21.0),

        // Motorcycles & scooters
        car("Honda", "CD 70", "70 cc", CarCategory.MOTORCYCLE, 50.0),
        car("Honda", "CG 125", "125 cc", CarCategory.MOTORCYCLE, 40.0),
        car("Honda", "Activa", "110 cc scooter", CarCategory.MOTORCYCLE, 48.0),
        car("Honda", "PCX 160", "160 cc scooter", CarCategory.MOTORCYCLE, 40.0),
        car("Honda", "CBR650R", "650 cc", CarCategory.MOTORCYCLE, 20.0),
        car("Hero", "Splendor Plus", "100 cc", CarCategory.MOTORCYCLE, 60.0),
        car("Bajaj", "Pulsar 150", "150 cc", CarCategory.MOTORCYCLE, 45.0),
        car("Yamaha", "YBR 125", "125 cc", CarCategory.MOTORCYCLE, 40.0),
        car("Yamaha", "NMAX 155", "155 cc scooter", CarCategory.MOTORCYCLE, 40.0),
        car("Yamaha", "R15", "155 cc", CarCategory.MOTORCYCLE, 40.0),
        car("Royal Enfield", "Classic 350", "350 cc", CarCategory.MOTORCYCLE, 35.0),
        car("Kawasaki", "Ninja 400", "400 cc", CarCategory.MOTORCYCLE, 25.0),
        car("Harley-Davidson", "Street Glide", "1.9 L", CarCategory.MOTORCYCLE, 18.0),
    )
}
