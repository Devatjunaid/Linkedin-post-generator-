package com.devatjunaid.examclock.data.catalog

import java.time.DayOfWeek
import java.time.DayOfWeek.FRIDAY
import java.time.DayOfWeek.SATURDAY
import java.time.DayOfWeek.SUNDAY

enum class ExamCategory { SCHOOL, ENTRANCE, INTERNATIONAL }

/**
 * A well-known exam and its usual papers. Dates are intentionally NOT included: boards publish
 * them every year, so the student enters the date from the official date sheet.
 */
data class ExamTemplate(
    val id: String,
    val name: String,
    val board: String,
    val category: ExamCategory,
    val papers: List<String>,
    val paperMinutes: Int,
)

data class Country(
    val code: String,
    val name: String,
    val weekendDays: Set<DayOfWeek>,
    val templates: List<ExamTemplate>,
) {
    /** Short exam names for the country picker subtitle. */
    val highlights: String get() = templates.take(4).joinToString(" · ") { it.name }
}

object ExamCatalog {

    private val satSun = setOf(SATURDAY, SUNDAY)
    private val friSat = setOf(FRIDAY, SATURDAY)

    private fun t(id: String, name: String, board: String, category: ExamCategory, minutes: Int, vararg papers: String) =
        ExamTemplate(id, name, board, category, papers.toList(), minutes)

    /** Offered in every country. */
    val international: List<ExamTemplate> = listOf(
        t("cie_igcse", "Cambridge IGCSE / O Level", "Cambridge International", ExamCategory.INTERNATIONAL, 120,
            "English", "Mathematics", "Physics", "Chemistry", "Biology", "Computer Science", "Economics"),
        t("cie_alevel", "Cambridge A Level", "Cambridge International", ExamCategory.INTERNATIONAL, 120,
            "Mathematics", "Physics", "Chemistry", "Biology", "Computer Science", "Economics"),
        t("ib_dp", "IB Diploma", "International Baccalaureate", ExamCategory.INTERNATIONAL, 120,
            "Language A", "Language B", "Mathematics", "Sciences", "Individuals & Societies", "Theory of Knowledge"),
        t("sat", "SAT", "College Board", ExamCategory.INTERNATIONAL, 134, "Reading and Writing", "Math"),
        t("ielts", "IELTS", "British Council / IDP", ExamCategory.INTERNATIONAL, 165, "Listening", "Reading", "Writing", "Speaking"),
        t("toefl", "TOEFL iBT", "ETS", ExamCategory.INTERNATIONAL, 120, "Reading", "Listening", "Speaking", "Writing"),
        t("gre", "GRE General", "ETS", ExamCategory.INTERNATIONAL, 118, "Verbal Reasoning", "Quantitative Reasoning", "Analytical Writing"),
        t("gmat", "GMAT Focus", "GMAC", ExamCategory.INTERNATIONAL, 135, "Quantitative Reasoning", "Verbal Reasoning", "Data Insights"),
    )

    val countries: List<Country> = listOf(
        Country("PK", "Pakistan", setOf(SUNDAY), listOf(
            t("pk_ssc", "SSC (Matric)", "FBISE / BISE", ExamCategory.SCHOOL, 180,
                "English", "Urdu", "Mathematics", "Physics", "Chemistry", "Biology", "Computer Science", "Islamiyat", "Pakistan Studies"),
            t("pk_hssc_med", "HSSC (FSc Pre-Medical)", "FBISE / BISE", ExamCategory.SCHOOL, 180,
                "English", "Urdu", "Physics", "Chemistry", "Biology", "Islamiyat", "Pakistan Studies"),
            t("pk_hssc_eng", "HSSC (FSc Pre-Engineering)", "FBISE / BISE", ExamCategory.SCHOOL, 180,
                "English", "Urdu", "Physics", "Chemistry", "Mathematics", "Islamiyat", "Pakistan Studies"),
            t("pk_ics", "HSSC (ICS)", "FBISE / BISE", ExamCategory.SCHOOL, 180,
                "English", "Urdu", "Mathematics", "Computer Science", "Physics / Statistics"),
            t("pk_mdcat", "MDCAT", "PMDC", ExamCategory.ENTRANCE, 210, "Biology", "Chemistry", "Physics", "English", "Logical Reasoning"),
            t("pk_ecat", "ECAT", "UET", ExamCategory.ENTRANCE, 100, "Mathematics", "Physics", "Chemistry / Computer Science", "English"),
            t("pk_nat", "NAT", "NTS", ExamCategory.ENTRANCE, 120, "Verbal", "Analytical", "Quantitative", "Subject"),
            t("pk_css", "CSS", "FPSC", ExamCategory.ENTRANCE, 180, "English Essay", "English Précis & Composition", "General Science & Ability", "Current Affairs", "Pakistan Affairs", "Islamic Studies"),
        )),
        Country("IN", "India", setOf(SUNDAY), listOf(
            t("in_cbse10", "CBSE Class 10", "CBSE", ExamCategory.SCHOOL, 180, "English", "Hindi", "Mathematics", "Science", "Social Science"),
            t("in_cbse12", "CBSE Class 12 (Science)", "CBSE", ExamCategory.SCHOOL, 180, "English", "Physics", "Chemistry", "Mathematics", "Biology"),
            t("in_icse", "ICSE Class 10", "CISCE", ExamCategory.SCHOOL, 120, "English", "Mathematics", "Physics", "Chemistry", "Biology", "History & Civics", "Geography"),
            t("in_jee_main", "JEE Main", "NTA", ExamCategory.ENTRANCE, 180, "Physics", "Chemistry", "Mathematics"),
            t("in_jee_adv", "JEE Advanced", "IITs", ExamCategory.ENTRANCE, 180, "Paper 1", "Paper 2"),
            t("in_neet", "NEET UG", "NTA", ExamCategory.ENTRANCE, 180, "Physics", "Chemistry", "Botany", "Zoology"),
            t("in_cuet", "CUET UG", "NTA", ExamCategory.ENTRANCE, 60, "Language", "Domain Subject 1", "Domain Subject 2", "General Test"),
            t("in_gate", "GATE", "IITs", ExamCategory.ENTRANCE, 180, "General Aptitude", "Engineering Mathematics", "Core Subject"),
            t("in_upsc", "UPSC CSE Prelims", "UPSC", ExamCategory.ENTRANCE, 120, "General Studies I", "CSAT"),
        )),
        Country("BD", "Bangladesh", setOf(FRIDAY, SATURDAY), listOf(
            t("bd_ssc", "SSC", "Education Boards", ExamCategory.SCHOOL, 180, "Bangla", "English", "Mathematics", "Physics", "Chemistry", "Biology", "ICT"),
            t("bd_hsc", "HSC", "Education Boards", ExamCategory.SCHOOL, 180, "Bangla", "English", "ICT", "Physics", "Chemistry", "Biology", "Higher Mathematics"),
            t("bd_admission", "University Admission", "Public universities", ExamCategory.ENTRANCE, 90, "Physics", "Chemistry", "Mathematics / Biology", "English"),
        )),
        Country("NG", "Nigeria", satSun, listOf(
            t("ng_waec", "WAEC WASSCE", "WAEC", ExamCategory.SCHOOL, 180, "English Language", "Mathematics", "Physics", "Chemistry", "Biology", "Economics", "Civic Education"),
            t("ng_neco", "NECO SSCE", "NECO", ExamCategory.SCHOOL, 180, "English Language", "Mathematics", "Physics", "Chemistry", "Biology", "Economics"),
            t("ng_jamb", "JAMB UTME", "JAMB", ExamCategory.ENTRANCE, 120, "Use of English", "Subject 2", "Subject 3", "Subject 4"),
        )),
        Country("GH", "Ghana", satSun, listOf(
            t("gh_wassce", "WASSCE", "WAEC", ExamCategory.SCHOOL, 180, "English Language", "Core Mathematics", "Integrated Science", "Social Studies", "Elective 1", "Elective 2"),
            t("gh_bece", "BECE", "WAEC", ExamCategory.SCHOOL, 120, "English Language", "Mathematics", "Integrated Science", "Social Studies"),
        )),
        Country("KE", "Kenya", satSun, listOf(
            t("ke_kcse", "KCSE", "KNEC", ExamCategory.SCHOOL, 150, "English", "Kiswahili", "Mathematics", "Biology", "Chemistry", "Physics"),
        )),
        Country("ZA", "South Africa", satSun, listOf(
            t("za_nsc", "NSC (Matric)", "DBE / IEB", ExamCategory.SCHOOL, 180, "Home Language", "First Additional Language", "Mathematics", "Physical Sciences", "Life Sciences", "Life Orientation"),
        )),
        Country("EG", "Egypt", friSat, listOf(
            t("eg_thanaweya", "Thanaweya Amma", "Ministry of Education", ExamCategory.SCHOOL, 180, "Arabic", "English", "Second Language", "Physics", "Chemistry", "Biology / Mathematics"),
        )),
        Country("SA", "Saudi Arabia", friSat, listOf(
            t("sa_qudurat", "Qudurat (GAT)", "ETEC", ExamCategory.ENTRANCE, 150, "Verbal", "Quantitative"),
            t("sa_tahsili", "Tahsili (SAAT)", "ETEC", ExamCategory.ENTRANCE, 150, "Mathematics", "Physics", "Chemistry", "Biology"),
        )),
        Country("AE", "United Arab Emirates", satSun, listOf(
            t("ae_cbse12", "CBSE Class 12", "CBSE", ExamCategory.SCHOOL, 180, "English", "Physics", "Chemistry", "Mathematics", "Biology"),
        )),
        Country("TR", "Türkiye", satSun, listOf(
            t("tr_yks", "YKS", "ÖSYM", ExamCategory.ENTRANCE, 165, "TYT", "AYT", "YDT"),
            t("tr_lgs", "LGS", "MEB", ExamCategory.SCHOOL, 135, "Verbal", "Numerical"),
        )),
        Country("GB", "United Kingdom", satSun, listOf(
            t("gb_gcse", "GCSE", "AQA / Edexcel / OCR", ExamCategory.SCHOOL, 105, "English Language", "English Literature", "Mathematics", "Combined Science", "History", "Geography"),
            t("gb_alevel", "A Level", "AQA / Edexcel / OCR", ExamCategory.SCHOOL, 120, "Mathematics", "Further Mathematics", "Physics", "Chemistry", "Biology", "Economics"),
            t("gb_highers", "Scottish Highers", "SQA", ExamCategory.SCHOOL, 150, "English", "Mathematics", "Physics", "Chemistry", "Biology"),
            t("gb_ucat", "UCAT", "UCAT Consortium", ExamCategory.ENTRANCE, 120, "Verbal Reasoning", "Decision Making", "Quantitative Reasoning", "Situational Judgement"),
        )),
        Country("IE", "Ireland", satSun, listOf(
            t("ie_lc", "Leaving Certificate", "SEC", ExamCategory.SCHOOL, 150, "English", "Irish", "Mathematics", "Biology", "Chemistry", "Physics"),
            t("ie_jc", "Junior Cycle", "SEC", ExamCategory.SCHOOL, 120, "English", "Irish", "Mathematics", "Science"),
        )),
        Country("US", "United States", satSun, listOf(
            t("us_sat", "SAT", "College Board", ExamCategory.ENTRANCE, 134, "Reading and Writing", "Math"),
            t("us_act", "ACT", "ACT", ExamCategory.ENTRANCE, 125, "English", "Math", "Reading", "Science"),
            t("us_ap", "AP Exams", "College Board", ExamCategory.SCHOOL, 180, "AP Calculus", "AP Biology", "AP Chemistry", "AP Physics", "AP US History", "AP English Language"),
            t("us_mcat", "MCAT", "AAMC", ExamCategory.ENTRANCE, 375, "Chem/Phys", "CARS", "Bio/Biochem", "Psych/Soc"),
            t("us_lsat", "LSAT", "LSAC", ExamCategory.ENTRANCE, 140, "Logical Reasoning", "Reading Comprehension", "Writing"),
        )),
        Country("CA", "Canada", satSun, listOf(
            t("ca_provincial", "Provincial exams", "Provincial ministry", ExamCategory.SCHOOL, 120, "English", "Mathematics", "Science"),
            t("ca_mcat", "MCAT", "AAMC", ExamCategory.ENTRANCE, 375, "Chem/Phys", "CARS", "Bio/Biochem", "Psych/Soc"),
        )),
        Country("AU", "Australia", satSun, listOf(
            t("au_hsc", "HSC (NSW)", "NESA", ExamCategory.SCHOOL, 180, "English", "Mathematics Advanced", "Physics", "Chemistry", "Biology"),
            t("au_vce", "VCE (VIC)", "VCAA", ExamCategory.SCHOOL, 150, "English", "Mathematical Methods", "Physics", "Chemistry", "Biology"),
        )),
        Country("NZ", "New Zealand", satSun, listOf(
            t("nz_ncea", "NCEA", "NZQA", ExamCategory.SCHOOL, 180, "English", "Mathematics", "Physics", "Chemistry", "Biology"),
        )),
        Country("MY", "Malaysia", satSun, listOf(
            t("my_spm", "SPM", "Lembaga Peperiksaan", ExamCategory.SCHOOL, 150, "Bahasa Melayu", "English", "Mathematics", "Sejarah", "Physics", "Chemistry", "Biology"),
            t("my_stpm", "STPM", "MPM", ExamCategory.SCHOOL, 180, "Pengajian Am", "Mathematics", "Physics", "Chemistry", "Biology"),
        )),
        Country("SG", "Singapore", satSun, listOf(
            t("sg_alevel", "GCE A-Level", "SEAB", ExamCategory.SCHOOL, 180, "General Paper", "H2 Mathematics", "H2 Physics", "H2 Chemistry", "H2 Biology"),
            t("sg_psle", "PSLE", "SEAB", ExamCategory.SCHOOL, 120, "English", "Mother Tongue", "Mathematics", "Science"),
        )),
        Country("ID", "Indonesia", satSun, listOf(
            t("id_utbk", "UTBK-SNBT", "SNPMB", ExamCategory.ENTRANCE, 195, "Potensi Kognitif", "Literasi Bahasa Indonesia", "Literasi Bahasa Inggris", "Penalaran Matematika"),
        )),
        Country("CN", "China", satSun, listOf(
            t("cn_gaokao", "Gaokao", "Provincial exam authority", ExamCategory.ENTRANCE, 150, "Chinese", "Mathematics", "Foreign Language", "Elective 1", "Elective 2", "Elective 3"),
        )),
        Country("KR", "South Korea", satSun, listOf(
            t("kr_csat", "CSAT (Suneung)", "KICE", ExamCategory.ENTRANCE, 100, "Korean", "Mathematics", "English", "Korean History", "Inquiry 1", "Inquiry 2"),
        )),
        Country("DE", "Germany", satSun, listOf(
            t("de_abitur", "Abitur", "Landesministerium", ExamCategory.SCHOOL, 270, "Deutsch", "Mathematik", "Englisch", "Leistungskurs 1", "Leistungskurs 2"),
        )),
        Country("FR", "France", satSun, listOf(
            t("fr_bac", "Baccalauréat", "Ministère de l'Éducation", ExamCategory.SCHOOL, 240, "Philosophie", "Spécialité 1", "Spécialité 2", "Grand oral", "Français"),
        )),
        Country("BR", "Brazil", satSun, listOf(
            t("br_enem", "ENEM", "INEP", ExamCategory.ENTRANCE, 330, "Linguagens", "Ciências Humanas", "Ciências da Natureza", "Matemática", "Redação"),
        )),
    ).sortedBy { it.name }

    /** Country with only the international exams, for anywhere not listed. */
    val other = Country("ZZ", "Other country", satSun, emptyList())

    fun country(code: String): Country = countries.firstOrNull { it.code == code } ?: other

    fun templatesFor(code: String): List<ExamTemplate> {
        val local = country(code).templates
        return local + international.filterNot { intl -> local.any { it.name == intl.name } }
    }

    fun template(id: String): ExamTemplate? = (countries.flatMap { it.templates } + international).firstOrNull { it.id == id }
}
