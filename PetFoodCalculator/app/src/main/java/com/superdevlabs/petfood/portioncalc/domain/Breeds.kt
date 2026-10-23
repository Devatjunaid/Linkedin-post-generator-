package com.superdevlabs.petfood.portioncalc.domain

enum class Species(val label: String) { DOG("Dog"), CAT("Cat") }

/** Dog size bands decide when a puppy is fully grown and when a dog counts as senior. */
enum class SizeClass(val label: String, val adultAtMonths: Int, val seniorAtYears: Int) {
    TOY("Toy", 10, 10),
    SMALL("Small", 10, 10),
    MEDIUM("Medium", 12, 8),
    LARGE("Large", 15, 7),
    GIANT("Giant", 18, 6);

    companion object {
        fun fromAdultWeight(kg: Double): SizeClass = when {
            kg < 5 -> TOY
            kg < 10 -> SMALL
            kg < 25 -> MEDIUM
            kg < 45 -> LARGE
            else -> GIANT
        }
    }
}

/**
 * A breed with its typical healthy adult weight range (breed standards) and the
 * English Wikipedia article used for its real photo and short description.
 */
data class Breed(
    val id: String,
    val name: String,
    val species: Species,
    val minKg: Double,
    val maxKg: Double,
    val wikiTitle: String,
) {
    val isMixed: Boolean get() = id.endsWith("mixed")

    /** Size from the breed standard; mixed breeds fall back to the pet's own weight. */
    fun sizeClassFor(currentKg: Double): SizeClass =
        SizeClass.fromAdultWeight(if (isMixed) currentKg else (minKg + maxKg) / 2)
}

object Breeds {
    private fun dog(id: String, name: String, min: Double, max: Double, wiki: String) =
        Breed(id, name, Species.DOG, min, max, wiki)

    private fun cat(id: String, name: String, min: Double, max: Double, wiki: String) =
        Breed(id, name, Species.CAT, min, max, wiki)

    val all: List<Breed> = listOf(
        dog("dog_mixed", "Mixed breed dog", 2.0, 70.0, "Mixed-breed dog"),
        dog("akita", "Akita", 32.0, 59.0, "Akita (dog)"),
        dog("malamute", "Alaskan Malamute", 34.0, 39.0, "Alaskan Malamute"),
        dog("aussie", "Australian Shepherd", 16.0, 32.0, "Australian Shepherd"),
        dog("basset", "Basset Hound", 20.0, 29.0, "Basset Hound"),
        dog("beagle", "Beagle", 9.0, 14.0, "Beagle"),
        dog("malinois", "Belgian Malinois", 20.0, 30.0, "Malinois dog"),
        dog("bernese", "Bernese Mountain Dog", 36.0, 52.0, "Bernese Mountain Dog"),
        dog("bichon", "Bichon Frise", 5.0, 8.0, "Bichon Frisé"),
        dog("border_collie", "Border Collie", 14.0, 20.0, "Border Collie"),
        dog("boston", "Boston Terrier", 5.0, 11.0, "Boston Terrier"),
        dog("boxer", "Boxer", 25.0, 32.0, "Boxer (dog)"),
        dog("bulldog", "Bulldog (English)", 18.0, 25.0, "Bulldog"),
        dog("cane_corso", "Cane Corso", 40.0, 50.0, "Cane Corso"),
        dog("cavalier", "Cavalier King Charles Spaniel", 6.0, 8.0, "Cavalier King Charles Spaniel"),
        dog("chihuahua", "Chihuahua", 1.5, 3.0, "Chihuahua (dog)"),
        dog("cocker", "Cocker Spaniel", 9.0, 14.0, "English Cocker Spaniel"),
        dog("corgi", "Corgi (Pembroke Welsh)", 10.0, 14.0, "Pembroke Welsh Corgi"),
        dog("dachshund", "Dachshund (Standard)", 7.0, 15.0, "Dachshund"),
        dog("dachshund_mini", "Dachshund (Miniature)", 4.0, 5.0, "Dachshund"),
        dog("dobermann", "Dobermann", 27.0, 45.0, "Dobermann"),
        dog("frenchie", "French Bulldog", 8.0, 13.0, "French Bulldog"),
        dog("gsd", "German Shepherd", 22.0, 40.0, "German Shepherd"),
        dog("gsp", "German Shorthaired Pointer", 20.0, 32.0, "German Shorthaired Pointer"),
        dog("golden", "Golden Retriever", 25.0, 34.0, "Golden Retriever"),
        dog("goldendoodle", "Goldendoodle", 15.0, 35.0, "Goldendoodle"),
        dog("great_dane", "Great Dane", 50.0, 80.0, "Great Dane"),
        dog("havanese", "Havanese", 3.0, 6.0, "Havanese dog"),
        dog("husky", "Siberian Husky", 16.0, 27.0, "Siberian Husky"),
        dog("jack_russell", "Jack Russell Terrier", 6.0, 8.0, "Jack Russell Terrier"),
        dog("labrador", "Labrador Retriever", 25.0, 36.0, "Labrador Retriever"),
        dog("labradoodle", "Labradoodle", 15.0, 30.0, "Labradoodle"),
        dog("maltese", "Maltese", 2.0, 4.0, "Maltese dog"),
        dog("mastiff", "Mastiff", 54.0, 100.0, "English Mastiff"),
        dog("schnauzer_mini", "Miniature Schnauzer", 5.0, 9.0, "Miniature Schnauzer"),
        dog("pitbull", "Pit Bull Terrier", 14.0, 32.0, "American Pit Bull Terrier"),
        dog("pomeranian", "Pomeranian", 1.5, 3.5, "Pomeranian dog"),
        dog("poodle_std", "Poodle (Standard)", 18.0, 32.0, "Poodle"),
        dog("poodle_mini", "Poodle (Miniature)", 5.0, 8.0, "Poodle"),
        dog("poodle_toy", "Poodle (Toy)", 2.0, 3.0, "Toy Poodle"),
        dog("pug", "Pug", 6.0, 8.0, "Pug"),
        dog("rottweiler", "Rottweiler", 35.0, 60.0, "Rottweiler"),
        dog("st_bernard", "Saint Bernard", 54.0, 82.0, "St. Bernard (dog)"),
        dog("samoyed", "Samoyed", 16.0, 30.0, "Samoyed dog"),
        dog("sheltie", "Shetland Sheepdog", 6.0, 12.0, "Shetland Sheepdog"),
        dog("shiba", "Shiba Inu", 7.0, 11.0, "Shiba Inu"),
        dog("shih_tzu", "Shih Tzu", 4.0, 7.0, "Shih Tzu"),
        dog("vizsla", "Vizsla", 20.0, 30.0, "Vizsla"),
        dog("weimaraner", "Weimaraner", 25.0, 40.0, "Weimaraner"),
        dog("yorkie", "Yorkshire Terrier", 2.0, 3.5, "Yorkshire Terrier"),

        cat("cat_mixed", "Domestic / mixed cat", 3.0, 6.0, "Domestic short-haired cat"),
        cat("abyssinian", "Abyssinian", 3.0, 5.0, "Abyssinian cat"),
        cat("bengal", "Bengal", 3.5, 7.0, "Bengal cat"),
        cat("birman", "Birman", 3.0, 6.0, "Birman"),
        cat("bombay", "Bombay", 3.0, 5.0, "Bombay cat"),
        cat("british_sh", "British Shorthair", 4.0, 8.0, "British Shorthair"),
        cat("dlh", "Domestic Longhair", 3.0, 6.5, "Domestic long-haired cat"),
        cat("exotic_sh", "Exotic Shorthair", 3.0, 6.0, "Exotic Shorthair"),
        cat("maine_coon", "Maine Coon", 5.0, 11.0, "Maine Coon"),
        cat("norwegian", "Norwegian Forest Cat", 4.0, 9.0, "Norwegian Forest cat"),
        cat("persian", "Persian", 3.0, 5.5, "Persian cat"),
        cat("ragdoll", "Ragdoll", 4.5, 9.0, "Ragdoll"),
        cat("russian_blue", "Russian Blue", 3.0, 5.5, "Russian Blue"),
        cat("scottish_fold", "Scottish Fold", 3.0, 6.0, "Scottish Fold"),
        cat("siamese", "Siamese", 3.0, 5.5, "Siamese cat"),
        cat("sphynx", "Sphynx", 3.0, 5.5, "Sphynx cat"),
    )

    private val byId = all.associateBy { it.id }

    fun get(id: String): Breed? = byId[id]

    fun forSpecies(species: Species): List<Breed> =
        all.filter { it.species == species }.sortedWith(compareBy({ !it.isMixed }, { it.name }))

    fun defaultFor(species: Species): Breed =
        byId.getValue(if (species == Species.DOG) "dog_mixed" else "cat_mixed")
}
