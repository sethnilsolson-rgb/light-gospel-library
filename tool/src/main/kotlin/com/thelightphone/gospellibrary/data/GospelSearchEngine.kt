package com.thelightphone.gospellibrary.data

import com.thelightphone.gospellibrary.db.ConferenceTalkEntity
import com.thelightphone.gospellibrary.model.HymnBook
import com.thelightphone.gospellibrary.model.HymnItem
import com.thelightphone.gospellibrary.model.ScriptureBook
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.io.Reader
import java.util.Locale
import java.util.zip.GZIPInputStream
import kotlin.math.max
import kotlin.math.min

enum class SearchScope(val label: String) {
    ALL("All"),
    BOFM("BofM"),
    NT("NT"),
    OT("OT"),
    DC("D&C"),
    PGP("PGP"),
    CONFERENCE("Conf."),
    HYMNS("Hymns"),
}

enum class SearchMatchKind(val badge: String) {
    REFERENCE("Jump"),
    EXACT("Exact"),
    WORDS("Match"),
    CONCEPT("Related"),
}

data class GospelSearchResult(
    val contentId: String,
    val title: String,
    val subtitle: String,
    val snippet: String,
    val matchKind: SearchMatchKind,
    val topicLabel: String? = null,
    val scrollPermille: Int = 0,
    val score: Float = 0f,
)

data class GospelSearchResponse(
    val query: String,
    val matchedTopics: List<String>,
    val results: List<GospelSearchResult>,
)

/**
 * Fast, 100% offline search engine for Gospel Library.
 *
 * Supports:
 * 1. Direct scripture reference jumps (e.g. "Alma 32:21", "1 Ne 3:7", "D&C 121", "John 3:16")
 * 2. Exact phrase & multi-word/stemmed search across all 5 Standard Works and General Conference talks
 * 3. Conceptual / related-teachings search ("feeling anxious", "how to forgive", "strength in weakness")
 *    using a curated gospel concept & KJV-to-modern bridge graph.
 */
object GospelSearchEngine {

    private const val MAX_RESULTS = 60

    val SUGGESTED_TOPICS: List<String> = listOf(
        "peace and anxiety",
        "faith and doubt",
        "forgiveness and mercy",
        "strength in weakness",
        "prayer and revelation",
        "charity and service",
        "enduring trials",
        "covenants and temple",
    )

    private data class AnchorRef(
        val volumeId: String,
        val bookId: String,
        val chapter: Int,
        val verse: Int,
    )

    private data class ConceptCluster(
        val label: String,
        val triggers: Set<String>,
        val scriptureTerms: Set<String>,
        val anchors: List<AnchorRef>,
    )

    private val STOP_WORDS = setOf(
        "a", "an", "the", "and", "or", "but", "in", "on", "at", "to", "for", "of", "with",
        "by", "from", "up", "about", "into", "over", "after", "is", "are", "was", "were",
        "be", "been", "being", "have", "has", "had", "do", "does", "did", "will", "would",
        "shall", "should", "can", "could", "may", "might", "must", "i", "me", "my", "we",
        "our", "you", "your", "he", "him", "his", "she", "her", "it", "its", "they", "them",
        "their", "this", "that", "these", "those", "what", "which", "who", "whom", "how",
        "when", "where", "why", "all", "some", "no", "not", "so", "if", "then", "than",
        "too", "very", "just", "feeling", "feel", "feels", "felt", "get", "getting",
        "make", "making", "things", "thing", "someone", "something", "really", "much",
        "more", "most", "unto", "ye", "thee", "thou", "thy", "thine", "hath", "doth",
        "shalt", "wilt", "art", "yea", "behold", "came", "pass", "therefore", "wherefore",
    )

    private val CONCEPT_CLUSTERS: List<ConceptCluster> = listOf(
        ConceptCluster(
            label = "Peace & Anxiety",
            triggers = setOf(
                "anxious", "anxiety", "worry", "worried", "worrying", "stress", "stressed",
                "overwhelmed", "overwhelm", "panic", "afraid", "fear", "fearful", "scared",
                "nervous", "uneasy", "restless", "peace", "calm", "quiet", "burden", "burdened",
                "heavy", "weary", "tired", "exhausted", "burnout", "troubled", "rest",
            ),
            scriptureTerms = setOf(
                "peace", "troubled", "afraid", "fear", "heavy laden", "burden", "burdens",
                "rest", "comfort", "still waters", "succor", "good cheer", "doubt not",
                "fear not", "consolation", "quiet", "weary", "careful for nothing",
            ),
            anchors = listOf(
                AnchorRef("nt", "john", 14, 27),
                AnchorRef("nt", "matt", 11, 28),
                AnchorRef("dc", "sections", 6, 36),
                AnchorRef("ot", "ps", 55, 22),
                AnchorRef("nt", "philip", 4, 6),
                AnchorRef("bofm", "mosiah", 24, 14),
                AnchorRef("bofm", "alma", 36, 3),
                AnchorRef("ot", "ps", 23, 2),
                AnchorRef("dc", "sections", 19, 23),
            ),
        ),
        ConceptCluster(
            label = "Faith & Doubt",
            triggers = setOf(
                "faith", "believe", "believing", "belief", "doubt", "doubting", "doubts",
                "uncertain", "uncertainty", "skeptical", "trust", "trusting", "testimony",
                "witness", "convinced", "question", "questions", "unsure", "seed", "hope",
            ),
            scriptureTerms = setOf(
                "faith", "believe", "believeth", "doubt", "trust", "seed", "hope",
                "testimony", "witness", "unbelief", "sure", "knowledge", "evidence",
                "unseen", "experiment", "assurance",
            ),
            anchors = listOf(
                AnchorRef("bofm", "alma", 32, 21),
                AnchorRef("bofm", "alma", 32, 27),
                AnchorRef("bofm", "ether", 12, 6),
                AnchorRef("nt", "heb", 11, 1),
                AnchorRef("nt", "mark", 9, 24),
                AnchorRef("ot", "prov", 3, 5),
                AnchorRef("bofm", "moroni", 10, 4),
                AnchorRef("dc", "sections", 6, 36),
            ),
        ),
        ConceptCluster(
            label = "Repentance & Forgiveness",
            triggers = setOf(
                "repent", "repentance", "forgive", "forgiving", "forgiveness", "forgiven",
                "guilt", "guilty", "shame", "ashamed", "sin", "sins", "sinned", "mistake",
                "mistakes", "regret", "sorry", "cleanse", "clean", "pure", "pardon", "mercy",
                "offend", "offended", "grudge", "resentment", "bitter", "bitterness",
            ),
            scriptureTerms = setOf(
                "repent", "repentance", "forgive", "forgiven", "forgiveness", "remission",
                "mercy", "merciful", "cleanse", "scarlet", "white as snow", "contrite",
                "broken heart", "remember them no more", "sin", "iniquity", "transgression",
            ),
            anchors = listOf(
                AnchorRef("dc", "sections", 58, 42),
                AnchorRef("dc", "sections", 64, 9),
                AnchorRef("ot", "isa", 1, 18),
                AnchorRef("bofm", "alma", 36, 18),
                AnchorRef("bofm", "enos", 1, 5),
                AnchorRef("nt", "matt", 18, 21),
                AnchorRef("bofm", "mosiah", 26, 30),
                AnchorRef("nt", "1-jn", 1, 9),
            ),
        ),
        ConceptCluster(
            label = "Grace & Atonement",
            triggers = setOf(
                "grace", "atonement", "redeem", "redemption", "redeemer", "savior", "save",
                "saved", "salvation", "ransom", "sacrifice", "healing", "heal", "healed",
                "suffering", "suffered", "gethsemane", "cross", "worthy", "unworthy", "enough",
            ),
            scriptureTerms = setOf(
                "grace", "atonement", "redeem", "redeemed", "redemption", "merits",
                "succor", "pains", "afflictions", "infirmities", "stripes", "healed",
                "mediator", "advocate", "mighty to save", "infinite",
            ),
            anchors = listOf(
                AnchorRef("bofm", "alma", 7, 11),
                AnchorRef("bofm", "2-ne", 25, 23),
                AnchorRef("bofm", "ether", 12, 27),
                AnchorRef("nt", "eph", 2, 8),
                AnchorRef("ot", "isa", 53, 4),
                AnchorRef("dc", "sections", 19, 16),
                AnchorRef("bofm", "moroni", 10, 32),
                AnchorRef("bofm", "2-ne", 2, 6),
            ),
        ),
        ConceptCluster(
            label = "Prayer & Revelation",
            triggers = setOf(
                "pray", "prayer", "praying", "prayers", "revelation", "inspiration", "spirit",
                "holy ghost", "guidance", "guide", "direction", "answer", "answers", "listening",
                "listen", "hear", "voice", "decision", "decisions", "confused", "confusion",
                "discern", "discernment", "prompting", "promptings", "ponder", "meditate",
            ),
            scriptureTerms = setOf(
                "pray", "prayer", "ask", "knock", "seek", "revelation", "holy ghost",
                "spirit", "still small voice", "mind and in thy heart", "counsel",
                "wisdom", "manifest", "whisper", "enlighten", "ponder",
            ),
            anchors = listOf(
                AnchorRef("nt", "james", 1, 5),
                AnchorRef("dc", "sections", 8, 2),
                AnchorRef("dc", "sections", 9, 8),
                AnchorRef("bofm", "2-ne", 32, 8),
                AnchorRef("bofm", "alma", 37, 37),
                AnchorRef("bofm", "moroni", 10, 4),
                AnchorRef("nt", "matt", 7, 7),
                AnchorRef("bofm", "3-ne", 18, 20),
                AnchorRef("ot", "1-kgs", 19, 12),
            ),
        ),
        ConceptCluster(
            label = "Charity, Love & Service",
            triggers = setOf(
                "charity", "love", "loving", "serve", "service", "serving", "kind", "kindness",
                "compassion", "minister", "ministering", "help", "helping", "neighbor",
                "poor", "needy", "generous", "generosity", "selfless", "caring", "care",
            ),
            scriptureTerms = setOf(
                "charity", "love", "serve", "service", "kind", "compassion", "neighbor",
                "poor", "needy", "bowels of mercy", "pure love of christ", "succor",
                "impart", "substance", "fellow beings", "good works",
            ),
            anchors = listOf(
                AnchorRef("bofm", "moroni", 7, 45),
                AnchorRef("bofm", "moroni", 7, 47),
                AnchorRef("bofm", "mosiah", 2, 17),
                AnchorRef("nt", "1-cor", 13, 4),
                AnchorRef("nt", "matt", 22, 37),
                AnchorRef("nt", "matt", 25, 40),
                AnchorRef("nt", "john", 13, 34),
                AnchorRef("bofm", "mosiah", 18, 8),
            ),
        ),
        ConceptCluster(
            label = "Trials, Sorrow & Enduring",
            triggers = setOf(
                "trial", "trials", "hard", "difficult", "struggle", "struggling", "pain",
                "suffering", "sorrow", "sad", "sadness", "grief", "grieving", "mourn",
                "mourning", "loss", "depressed", "depression", "discouraged", "discouragement",
                "despair", "hopeless", "adversity", "affliction", "endure", "enduring",
                "persevere", "perseverance", "give up", "quitting", "lonely", "loneliness", "alone",
            ),
            scriptureTerms = setOf(
                "affliction", "afflictions", "adversity", "sorrow", "mourn", "comfort",
                "comfortless", "endure", "tribulation", "trials", "consecrate", "small moment",
                "good cheer", "cast down", "forsake", "weeping", "valley",
            ),
            anchors = listOf(
                AnchorRef("dc", "sections", 121, 7),
                AnchorRef("dc", "sections", 122, 7),
                AnchorRef("bofm", "2-ne", 2, 2),
                AnchorRef("nt", "rom", 8, 28),
                AnchorRef("nt", "john", 14, 18),
                AnchorRef("nt", "john", 16, 33),
                AnchorRef("bofm", "ether", 12, 6),
                AnchorRef("ot", "isa", 41, 10),
                AnchorRef("ot", "ps", 34, 18),
            ),
        ),
        ConceptCluster(
            label = "Strength in Weakness",
            triggers = setOf(
                "weak", "weakness", "weaknesses", "inadequate", "failure", "failing", "fail",
                "strength", "strong", "strengthen", "courage", "brave", "overcome", "power",
                "capable", "ability", "confidence", "confident",
            ),
            scriptureTerms = setOf(
                "weak", "weakness", "strong", "strength", "strengthen", "strengtheneth",
                "mighty", "courage", "be strong", "grace is sufficient", "humble",
                "power", "overcome", "able",
            ),
            anchors = listOf(
                AnchorRef("bofm", "ether", 12, 27),
                AnchorRef("nt", "philip", 4, 13),
                AnchorRef("nt", "2-cor", 12, 9),
                AnchorRef("ot", "isa", 40, 31),
                AnchorRef("ot", "josh", 1, 9),
                AnchorRef("bofm", "1-ne", 3, 7),
                AnchorRef("bofm", "alma", 26, 12),
            ),
        ),
        ConceptCluster(
            label = "Humility & Pride",
            triggers = setOf(
                "humble", "humility", "meek", "meekness", "pride", "proud", "arrogant",
                "ego", "selfish", "stubborn", "teachable", "submissive", "childlike", "modest",
            ),
            scriptureTerms = setOf(
                "humble", "humility", "meek", "meekness", "pride", "proud", "lifted up",
                "puffed up", "submissive", "little child", "lowly", "contrite", "haughty",
            ),
            anchors = listOf(
                AnchorRef("bofm", "mosiah", 3, 19),
                AnchorRef("dc", "sections", 112, 10),
                AnchorRef("bofm", "ether", 12, 27),
                AnchorRef("nt", "matt", 5, 5),
                AnchorRef("bofm", "alma", 32, 16),
                AnchorRef("ot", "prov", 16, 18),
                AnchorRef("bofm", "jacob", 2, 13),
            ),
        ),
        ConceptCluster(
            label = "Temple, Covenants & Family",
            triggers = setOf(
                "temple", "covenant", "covenants", "sealing", "sealed", "endowment",
                "marriage", "marry", "spouse", "husband", "wife", "family", "parents",
                "mother", "father", "children", "child", "home", "eternal", "forever",
                "ancestors", "genealogy", "family history",
            ),
            scriptureTerms = setOf(
                "temple", "house of the lord", "covenant", "covenants", "seal", "sealed",
                "marriage", "husband", "wife", "parents", "children", "hearts of the children",
                "fathers", "ordinances", "holiness", "endowed",
            ),
            anchors = listOf(
                AnchorRef("dc", "sections", 109, 22),
                AnchorRef("ot", "mal", 4, 5),
                AnchorRef("dc", "sections", 132, 19),
                AnchorRef("nt", "1-cor", 11, 11),
                AnchorRef("ot", "prov", 22, 6),
                AnchorRef("bofm", "mosiah", 4, 14),
                AnchorRef("dc", "sections", 88, 119),
                AnchorRef("dc", "sections", 2, 2),
            ),
        ),
        ConceptCluster(
            label = "Agency & Freedom",
            triggers = setOf(
                "agency", "choice", "choices", "choose", "choosing", "free", "freedom",
                "liberty", "decision", "accountable", "accountability", "responsibility",
                "captivity", "bondage", "addiction", "habit", "habits",
            ),
            scriptureTerms = setOf(
                "free", "liberty", "choose", "agency", "act for themselves", "captivity",
                "bondage", "accountable", "moral agency", "truth shall make you free",
            ),
            anchors = listOf(
                AnchorRef("bofm", "2-ne", 2, 27),
                AnchorRef("bofm", "hel", 14, 30),
                AnchorRef("nt", "john", 8, 32),
                AnchorRef("ot", "josh", 24, 15),
                AnchorRef("bofm", "2-ne", 10, 23),
                AnchorRef("dc", "sections", 58, 27),
            ),
        ),
        ConceptCluster(
            label = "Hope, Joy & Gratitude",
            triggers = setOf(
                "joy", "joyful", "happy", "happiness", "glad", "rejoice", "cheerful",
                "optimism", "purpose", "meaning", "gratitude", "grateful", "thank",
                "thankful", "thanks", "thanksgiving", "appreciate", "praise", "blessings",
            ),
            scriptureTerms = setOf(
                "joy", "rejoice", "happiness", "glad", "thanksgiving", "thank", "praise",
                "hope", "brightness of hope", "good cheer", "blessed", "delight",
            ),
            anchors = listOf(
                AnchorRef("bofm", "2-ne", 2, 25),
                AnchorRef("bofm", "2-ne", 31, 20),
                AnchorRef("dc", "sections", 78, 19),
                AnchorRef("bofm", "mosiah", 2, 41),
                AnchorRef("nt", "1-thes", 5, 18),
                AnchorRef("bofm", "alma", 34, 38),
                AnchorRef("nt", "philip", 4, 4),
            ),
        ),
        ConceptCluster(
            label = "Temptation & Spiritual Protection",
            triggers = setOf(
                "temptation", "tempted", "tempt", "devil", "satan", "adversary", "evil",
                "resist", "armor", "protect", "protection", "purity", "chastity", "virtue",
                "clean thoughts", "watch", "stand", "foundation",
            ),
            scriptureTerms = setOf(
                "temptation", "tempted", "adversary", "devil", "armor", "shield of faith",
                "watch and pray", "rock", "foundation", "virtue", "garnish", "resist",
                "escape", "fiery darts",
            ),
            anchors = listOf(
                AnchorRef("nt", "1-cor", 10, 13),
                AnchorRef("nt", "eph", 6, 11),
                AnchorRef("bofm", "hel", 5, 12),
                AnchorRef("bofm", "alma", 13, 28),
                AnchorRef("bofm", "3-ne", 18, 18),
                AnchorRef("dc", "sections", 121, 45),
                AnchorRef("dc", "sections", 27, 15),
            ),
        ),
        ConceptCluster(
            label = "Unity, Anger & Peacemaking",
            triggers = setOf(
                "anger", "angry", "mad", "frustrated", "argue", "arguing", "argument",
                "contention", "conflict", "fight", "fighting", "peacemaker", "unity",
                "united", "one", "division", "quarrel", "patience", "patient", "gentle",
                "gentleness", "kind words", "gossip", "judge", "judging", "judgmental",
            ),
            scriptureTerms = setOf(
                "contention", "peacemakers", "anger", "wrath", "soft answer", "unity",
                "one heart", "one mind", "long-suffering", "gentleness", "patience",
                "judge not", "persuasion", "meekness",
            ),
            anchors = listOf(
                AnchorRef("bofm", "3-ne", 11, 29),
                AnchorRef("ot", "prov", 15, 1),
                AnchorRef("nt", "matt", 5, 9),
                AnchorRef("dc", "sections", 38, 27),
                AnchorRef("pgp", "moses", 7, 18),
                AnchorRef("dc", "sections", 121, 41),
                AnchorRef("nt", "matt", 7, 1),
            ),
        ),
        ConceptCluster(
            label = "Patience & God's Timing",
            triggers = setOf(
                "patience", "patient", "impatient", "wait", "waiting", "timing", "time",
                "delay", "delayed", "season", "slow", "still", "trust god",
            ),
            scriptureTerms = setOf(
                "patience", "patient", "wait upon the lord", "be still", "due time",
                "long-suffering", "season", "tarry", "his own time", "line upon line",
            ),
            anchors = listOf(
                AnchorRef("ot", "isa", 40, 31),
                AnchorRef("ot", "ps", 46, 10),
                AnchorRef("dc", "sections", 98, 2),
                AnchorRef("bofm", "mosiah", 24, 15),
                AnchorRef("ot", "eccl", 3, 1),
                AnchorRef("dc", "sections", 88, 68),
                AnchorRef("nt", "gal", 6, 9),
            ),
        ),
        ConceptCluster(
            label = "Obedience, Commandments & Diligence",
            triggers = setOf(
                "obey", "obedience", "obedient", "commandment", "commandments", "keep",
                "follow", "discipleship", "disciple", "diligent", "diligence", "work",
                "hard work", "lazy", "slothful", "procrastinate", "sacrifice", "consecration",
                "exactness", "small things", "simple",
            ),
            scriptureTerms = setOf(
                "obedience", "obedient", "commandments", "hearken", "keep my commandments",
                "diligence", "diligent", "small and simple", "prosper", "follow me",
                "endure to the end", "steward", "anxiously engaged",
            ),
            anchors = listOf(
                AnchorRef("bofm", "1-ne", 3, 7),
                AnchorRef("nt", "john", 14, 15),
                AnchorRef("dc", "sections", 130, 20),
                AnchorRef("bofm", "alma", 37, 6),
                AnchorRef("dc", "sections", 82, 10),
                AnchorRef("bofm", "mosiah", 2, 22),
                AnchorRef("dc", "sections", 58, 27),
            ),
        ),
        ConceptCluster(
            label = "Honesty, Truth & Integrity",
            triggers = setOf(
                "honest", "honesty", "truth", "truthful", "integrity", "lie", "lying",
                "deceive", "deception", "cheat", "trustworthy", "sincere", "authentic",
            ),
            scriptureTerms = setOf(
                "honest", "truth", "integrity", "lying", "deceive", "upright",
                "virtuous", "pure in heart", "true", "sincere",
            ),
            anchors = listOf(
                AnchorRef("pgp", "a-of-f", 1, 13),
                AnchorRef("dc", "sections", 93, 24),
                AnchorRef("nt", "john", 8, 32),
                AnchorRef("ot", "prov", 12, 22),
                AnchorRef("bofm", "alma", 53, 20),
                AnchorRef("dc", "sections", 124, 15),
            ),
        ),
        ConceptCluster(
            label = "Missionary Work & Being a Light",
            triggers = setOf(
                "mission", "missionary", "share", "sharing", "gospel", "preach", "invite",
                "example", "light", "shine", "gather", "gathering", "israel", "testify",
            ),
            scriptureTerms = setOf(
                "light of the world", "shine", "preach", "missionary", "gather",
                "worth of souls", "field is white", "marvelous work", "example",
                "witnesses", "stand as witnesses",
            ),
            anchors = listOf(
                AnchorRef("nt", "matt", 5, 14),
                AnchorRef("dc", "sections", 4, 2),
                AnchorRef("dc", "sections", 18, 10),
                AnchorRef("bofm", "mosiah", 18, 9),
                AnchorRef("bofm", "3-ne", 18, 24),
                AnchorRef("nt", "1-tim", 4, 12),
                AnchorRef("dc", "sections", 88, 81),
            ),
        ),
        ConceptCluster(
            label = "Resurrection, Death & Eternal Life",
            triggers = setOf(
                "death", "die", "dying", "dead", "resurrection", "resurrected", "afterlife",
                "heaven", "immortality", "eternal life", "spirit world", "grave", "celestial",
                "kingdom", "glory", "plan of salvation", "premortal",
            ),
            scriptureTerms = listOf(
                "resurrection", "immortality", "eternal life", "death", "sting", "grave",
                "spirit world", "paradise", "celestial", "mansions", "work and my glory",
                "restored to its proper",
            ).toSet(),
            anchors = listOf(
                AnchorRef("pgp", "moses", 1, 39),
                AnchorRef("bofm", "alma", 40, 23),
                AnchorRef("nt", "1-cor", 15, 22),
                AnchorRef("nt", "john", 11, 25),
                AnchorRef("bofm", "mosiah", 16, 7),
                AnchorRef("dc", "sections", 76, 22),
                AnchorRef("nt", "john", 17, 3),
            ),
        ),
        ConceptCluster(
            label = "Priesthood, Baptism & Sacrament",
            triggers = setOf(
                "priesthood", "baptism", "baptize", "baptized", "sacrament", "bread", "water",
                "ordinance", "ordinances", "keys", "authority", "calling", "holy ghost",
                "confirmation", "remember him", "always remember",
            ),
            scriptureTerms = setOf(
                "priesthood", "baptism", "baptized", "sacrament", "always remember him",
                "take upon them the name", "ordinances", "keys", "authority", "gift of the holy ghost",
            ),
            anchors = listOf(
                AnchorRef("bofm", "moroni", 4, 3),
                AnchorRef("bofm", "2-ne", 31, 17),
                AnchorRef("bofm", "mosiah", 18, 8),
                AnchorRef("dc", "sections", 20, 77),
                AnchorRef("dc", "sections", 121, 36),
                AnchorRef("dc", "sections", 84, 20),
                AnchorRef("nt", "john", 3, 5),
            ),
        ),
        ConceptCluster(
            label = "Sabbath, Fasting, Tithing & Health",
            triggers = setOf(
                "sabbath", "sunday", "fast", "fasting", "tithing", "tithe", "money", "finances",
                "health", "healthy", "body", "word of wisdom", "sleep", "run and not be weary",
            ),
            scriptureTerms = setOf(
                "sabbath", "holy day", "fast", "fasting", "tithing", "tithes", "windows of heaven",
                "temple of god", "word of wisdom", "health", "run and not be weary", "marrow",
            ),
            anchors = listOf(
                AnchorRef("ot", "mal", 3, 10),
                AnchorRef("dc", "sections", 59, 9),
                AnchorRef("dc", "sections", 89, 18),
                AnchorRef("ot", "isa", 58, 6),
                AnchorRef("ot", "isa", 58, 13),
                AnchorRef("nt", "1-cor", 6, 19),
            ),
        ),
        ConceptCluster(
            label = "Scripture Study & Learning",
            triggers = setOf(
                "scripture", "scriptures", "study", "studying", "read", "reading", "feast",
                "learn", "learning", "education", "knowledge", "wisdom", "intelligence", "liken",
            ),
            scriptureTerms = setOf(
                "scriptures", "feast upon the words", "search", "liken", "word of god",
                "iron rod", "rod of iron", "intelligence", "best books", "study", "ponder",
            ),
            anchors = listOf(
                AnchorRef("bofm", "2-ne", 32, 3),
                AnchorRef("bofm", "1-ne", 19, 23),
                AnchorRef("dc", "sections", 88, 118),
                AnchorRef("dc", "sections", 130, 18),
                AnchorRef("nt", "2-tim", 3, 16),
                AnchorRef("bofm", "1-ne", 15, 24),
                AnchorRef("ot", "ps", 119, 105),
            ),
        ),
    )

    private val BOOK_ALIASES: Map<String, Pair<String, ScriptureBook>> by lazy {
        val map = LinkedHashMap<String, Pair<String, ScriptureBook>>()
        for (vol in StandardWorksData.volumes) {
            for (book in StandardWorksData.getBooksForVolume(vol.id)) {
                fun reg(alias: String) {
                    map[normalizeAlias(alias)] = vol.id to book
                }
                reg(book.id)
                reg(book.shortTitle)
                reg(book.title)
                reg(book.shortTitle.replace(" ", ""))
            }
        }
        fun addCustom(alias: String, volId: String, bookId: String) {
            val book = StandardWorksData.findBook(volId, bookId) ?: return
            map[normalizeAlias(alias)] = volId to book
        }
        // Book of Mormon abbreviations
        addCustom("1 ne", "bofm", "1-ne"); addCustom("1ne", "bofm", "1-ne"); addCustom("1 nephi", "bofm", "1-ne")
        addCustom("2 ne", "bofm", "2-ne"); addCustom("2ne", "bofm", "2-ne"); addCustom("2 nephi", "bofm", "2-ne")
        addCustom("3 ne", "bofm", "3-ne"); addCustom("3ne", "bofm", "3-ne"); addCustom("3 nephi", "bofm", "3-ne")
        addCustom("4 ne", "bofm", "4-ne"); addCustom("4ne", "bofm", "4-ne"); addCustom("4 nephi", "bofm", "4-ne")
        addCustom("jac", "bofm", "jacob")
        addCustom("w of m", "bofm", "w-of-m"); addCustom("wofm", "bofm", "w-of-m")
        addCustom("mos", "bofm", "mosiah")
        addCustom("alm", "bofm", "alma")
        addCustom("hel", "bofm", "hel")
        addCustom("morm", "bofm", "morm")
        addCustom("eth", "bofm", "ether")
        addCustom("moro", "bofm", "moroni")
        // D&C
        addCustom("d&c", "dc", "sections"); addCustom("dc", "dc", "sections")
        addCustom("sec", "dc", "sections"); addCustom("section", "dc", "sections")
        addCustom("doctrine and covenants", "dc", "sections")
        // New Testament
        addCustom("mat", "nt", "matt"); addCustom("mt", "nt", "matt")
        addCustom("mk", "nt", "mark"); addCustom("mrk", "nt", "mark")
        addCustom("lk", "nt", "luke"); addCustom("luk", "nt", "luke")
        addCustom("jn", "nt", "john"); addCustom("jhn", "nt", "john")
        addCustom("act", "nt", "acts")
        addCustom("rom", "nt", "rom")
        addCustom("1 cor", "nt", "1-cor"); addCustom("1cor", "nt", "1-cor")
        addCustom("2 cor", "nt", "2-cor"); addCustom("2cor", "nt", "2-cor")
        addCustom("gal", "nt", "gal"); addCustom("eph", "nt", "eph")
        addCustom("phil", "nt", "philip"); addCustom("php", "nt", "philip")
        addCustom("col", "nt", "col")
        addCustom("1 thes", "nt", "1-thes"); addCustom("2 thes", "nt", "2-thes")
        addCustom("1 tim", "nt", "1-tim"); addCustom("2 tim", "nt", "2-tim")
        addCustom("heb", "nt", "heb"); addCustom("jas", "nt", "james"); addCustom("jam", "nt", "james")
        addCustom("1 pet", "nt", "1-pet"); addCustom("2 pet", "nt", "2-pet")
        addCustom("1 jn", "nt", "1-jn"); addCustom("2 jn", "nt", "2-jn"); addCustom("3 jn", "nt", "3-jn")
        addCustom("rev", "nt", "rev")
        // Old Testament
        addCustom("gen", "ot", "gen"); addCustom("exod", "ot", "ex"); addCustom("ex", "ot", "ex")
        addCustom("lev", "ot", "lev"); addCustom("num", "ot", "num"); addCustom("deut", "ot", "deut")
        addCustom("josh", "ot", "josh"); addCustom("judg", "ot", "judg")
        addCustom("1 sam", "ot", "1-sam"); addCustom("2 sam", "ot", "2-sam")
        addCustom("1 kgs", "ot", "1-kgs"); addCustom("2 kgs", "ot", "2-kgs")
        addCustom("1 kings", "ot", "1-kgs"); addCustom("2 kings", "ot", "2-kgs")
        addCustom("ps", "ot", "ps"); addCustom("psa", "ot", "ps"); addCustom("psalm", "ot", "ps")
        addCustom("prov", "ot", "prov"); addCustom("pro", "ot", "prov")
        addCustom("eccl", "ot", "eccl"); addCustom("isa", "ot", "isa"); addCustom("jer", "ot", "jer")
        addCustom("ezek", "ot", "ezek"); addCustom("dan", "ot", "dan"); addCustom("mal", "ot", "mal")
        // Pearl of Great Price
        addCustom("moses", "pgp", "moses"); addCustom("abr", "pgp", "abr")
        addCustom("js-m", "pgp", "js-m"); addCustom("jsm", "pgp", "js-m")
        addCustom("js-h", "pgp", "js-h"); addCustom("jsh", "pgp", "js-h")
        addCustom("a of f", "pgp", "a-of-f"); addCustom("aoff", "pgp", "a-of-f"); addCustom("articles of faith", "pgp", "a-of-f")
        map
    }

    private fun normalizeAlias(s: String): String =
        s.lowercase(Locale.US)
            .replace(".", "")
            .replace("—", "-")
            .replace("–", "-")
            .replace(Regex("\\s+"), " ")
            .trim()

    data class ParsedReference(
        val volumeId: String,
        val book: ScriptureBook,
        val chapter: Int,
        val verse: Int?,
    )

    fun parseReference(rawQuery: String): ParsedReference? {
        val cleaned = normalizeAlias(rawQuery)
        if (cleaned.length < 2) return null

        val match = Regex("""^(.+?)\s+(\d+)(?:\s*:\s*(\d+))?(?:-\d+)?$""").find(cleaned) ?: return null
        val bookPart = normalizeAlias(match.groupValues[1])
        val chapterNum = match.groupValues[2].toIntOrNull() ?: return null
        val verseNum = match.groupValues[3].toIntOrNull()

        val found = BOOK_ALIASES[bookPart] ?: return null
        val (volId, book) = found
        if (chapterNum !in 1..book.chapterCount) return null
        return ParsedReference(volId, book, chapterNum, verseNum)
    }

    fun search(
        rawQuery: String,
        scope: SearchScope,
        assetStreamProvider: ((String) -> InputStream?)?,
        conferenceTalks: List<ConferenceTalkEntity>,
        hymns: List<HymnItem> = emptyList(),
        isCancelled: () -> Boolean = { false },
    ): GospelSearchResponse {
        val query = rawQuery.trim()
        val hymnNumJump = parseHymnNumberQuery(query)
        if (query.length < 2 && hymnNumJump == null) {
            return GospelSearchResponse(query = query, matchedTopics = emptyList(), results = emptyList())
        }

        val queryLower = query.lowercase(Locale.US).replace(Regex("\\s+"), " ")
        val rawTokens = tokenize(queryLower)
        val meaningfulTokens = rawTokens.filter { it.length >= 2 && it !in STOP_WORDS }
            .ifEmpty { rawTokens.filter { it.isNotEmpty() } }
        val queryStems = meaningfulTokens.map { stemWord(it) }.distinct()

        // Identify matching conceptual clusters for "similar ideas & teachings"
        val matchedClusters = findMatchingConcepts(queryLower, rawTokens, meaningfulTokens, queryStems)
        val conceptTerms = linkedSetOf<String>()
        val anchorBoostMap = HashMap<String, Pair<String, Float>>() // "vol/book/ch/v" -> (topicLabel, boost)
        for ((cluster, relevance) in matchedClusters) {
            conceptTerms.addAll(cluster.scriptureTerms)
            for (a in cluster.anchors) {
                val key = "${a.volumeId}/${a.bookId}/${a.chapter}/${a.verse}"
                val prev = anchorBoostMap[key]
                val boost = 58f + relevance * 24f
                if (prev == null || boost > prev.second) {
                    anchorBoostMap[key] = cluster.label to boost
                }
            }
        }
        val conceptStems = conceptTerms
            .flatMap { tokenize(it) }
            .filter { it.length >= 3 && it !in STOP_WORDS }
            .map { stemWord(it) }
            .toSet()

        val allResults = ArrayList<GospelSearchResult>(96)

        // 1. Direct Scripture Reference Jump
        val parsedRef = parseReference(query)
        if (parsedRef != null) {
            val verses = ScriptureAssetLoader.loadVerses(
                assetStreamProvider = assetStreamProvider,
                volumeId = parsedRef.volumeId,
                bookShortTitle = parsedRef.book.shortTitle,
                chapterNum = parsedRef.chapter,
            )
            val targetVerse = parsedRef.verse?.let { vNum ->
                verses.firstOrNull { it.number == vNum }
            } ?: verses.firstOrNull()
            val verseIdx = if (targetVerse != null) verses.indexOf(targetVerse).coerceAtLeast(0) else 0
            val permille = if (verses.size > 1 && verseIdx > 0) {
                ((verseIdx.toFloat() / verses.size.toFloat()) * 1000f).toInt().coerceIn(0, 960)
            } else 0
            val volTitle = StandardWorksData.findVolume(parsedRef.volumeId)?.shortTitle.orEmpty()
            val refTitle = buildString {
                if (parsedRef.volumeId == "dc") append("D&C ${parsedRef.chapter}")
                else append("${parsedRef.book.shortTitle} ${parsedRef.chapter}")
                if (parsedRef.verse != null) append(":${parsedRef.verse}")
            }
            allResults.add(
                GospelSearchResult(
                    contentId = "scripture/${parsedRef.volumeId}/${parsedRef.book.id}/${parsedRef.chapter}",
                    title = refTitle,
                    subtitle = volTitle,
                    snippet = targetVerse?.text?.take(180) ?: "Open $refTitle",
                    matchKind = SearchMatchKind.REFERENCE,
                    scrollPermille = permille,
                    score = 1000f,
                )
            )
        }

        // 1b. Direct Hymn Number Jump (e.g. "1001", "#2", "hymn 193")
        if (hymnNumJump != null && (scope == SearchScope.ALL || scope == SearchScope.HYMNS) && hymns.isNotEmpty()) {
            hymns.filter { it.number == hymnNumJump }.forEach { hymn ->
                val book = HymnBook.fromId(hymn.bookId)
                allResults.add(
                    GospelSearchResult(
                        contentId = hymn.id,
                        title = "#${hymn.number} ${hymn.title}",
                        subtitle = "${book.shortTitle}${if (hymn.topic.isNotBlank()) " • ${hymn.topic}" else ""}",
                        snippet = hymn.stanzas.firstOrNull()?.replace("\n", " ")?.take(160) ?: hymn.title,
                        matchKind = SearchMatchKind.REFERENCE,
                        scrollPermille = 0,
                        score = 960f,
                    )
                )
            }
        }

        // 2. Search Standard Works (streaming JSON so memory stays tiny)
        val volumesToSearch = when (scope) {
            SearchScope.ALL -> listOf("bofm", "nt", "dc", "pgp", "ot")
            SearchScope.BOFM -> listOf("bofm")
            SearchScope.NT -> listOf("nt")
            SearchScope.OT -> listOf("ot")
            SearchScope.DC -> listOf("dc")
            SearchScope.PGP -> listOf("pgp")
            SearchScope.CONFERENCE, SearchScope.HYMNS -> emptyList()
        }

        val exactPhraseRegex = if (queryLower.length >= 2) buildWordBoundaryRegex(queryLower) else null
        val conceptTermRegexes = conceptTerms.associateWith { buildWordBoundaryRegex(it) }

        val directCandidates = ArrayList<GospelSearchResult>(64)
        val conceptCandidates = ArrayList<GospelSearchResult>(48)

        for (volId in volumesToSearch) {
            if (isCancelled()) break
            scanVolumeAsset(
                assetStreamProvider = assetStreamProvider,
                volumeId = volId,
                queryLower = queryLower,
                exactPhraseRegex = exactPhraseRegex,
                meaningfulTokens = meaningfulTokens,
                queryStems = queryStems,
                matchedClusters = matchedClusters.map { it.first },
                conceptTerms = conceptTerms,
                conceptTermRegexes = conceptTermRegexes,
                conceptStems = conceptStems,
                anchorBoostMap = anchorBoostMap,
                directOut = directCandidates,
                conceptOut = conceptCandidates,
                isCancelled = isCancelled,
            )
        }

        // 3. Search General Conference Talks (bundled + synced)
        if ((scope == SearchScope.ALL || scope == SearchScope.CONFERENCE) && !isCancelled()) {
            searchConferenceTalks(
                talks = conferenceTalks,
                queryLower = queryLower,
                exactPhraseRegex = exactPhraseRegex,
                meaningfulTokens = meaningfulTokens,
                queryStems = queryStems,
                matchedClusters = matchedClusters.map { it.first },
                conceptTerms = conceptTerms,
                conceptTermRegexes = conceptTermRegexes,
                directOut = directCandidates,
                conceptOut = conceptCandidates,
                isCancelled = isCancelled,
            )
        }

        // 4. Search Hymns (Home & Church + Classic 1985)
        if ((scope == SearchScope.ALL || scope == SearchScope.HYMNS) && hymns.isNotEmpty() && !isCancelled()) {
            searchHymns(
                hymns = hymns,
                queryLower = queryLower,
                exactPhraseRegex = exactPhraseRegex,
                meaningfulTokens = meaningfulTokens,
                queryStems = queryStems,
                directOut = directCandidates,
                isCancelled = isCancelled,
            )
        }

        // Combine & rank cleanly: Reference Jump -> Top Direct Matches -> Top Related Teachings
        val seenKeys = HashSet<String>()
        allResults.forEach { seenKeys.add("${it.contentId}|${it.title}") }

        val sortedDirect = directCandidates.sortedByDescending { it.score }
        val sortedConcept = conceptCandidates.sortedByDescending { it.score }

        val maxDirect = if (sortedConcept.isNotEmpty()) 42 else MAX_RESULTS
        var addedDirect = 0
        for (item in sortedDirect) {
            if (addedDirect >= maxDirect) break
            val key = "${item.contentId}|${item.title}"
            if (seenKeys.add(key)) {
                allResults.add(item)
                addedDirect++
            }
        }

        for (item in sortedConcept) {
            if (allResults.size >= MAX_RESULTS) break
            val key = "${item.contentId}|${item.title}"
            if (seenKeys.add(key)) {
                allResults.add(item)
            }
        }

        return GospelSearchResponse(
            query = query,
            matchedTopics = matchedClusters.take(3).map { it.first.label },
            results = allResults,
        )
    }

    private fun buildWordBoundaryRegex(term: String): Regex =
        Regex("(?<![\\p{L}\\p{N}_])${Regex.escape(term)}(?![\\p{L}\\p{N}_])")

    private fun containsWholeTerm(lowerText: String, term: String, regex: Regex?): Boolean =
        regex != null && lowerText.contains(term) && regex.containsMatchIn(lowerText)

    private fun findMatchingConcepts(
        queryLower: String,
        rawTokens: List<String>,
        meaningfulTokens: List<String>,
        queryStems: List<String>,
    ): List<Pair<ConceptCluster, Float>> {
        val rawSet = rawTokens.toSet()
        val stemSet = queryStems.toSet()
        val scored = ArrayList<Pair<ConceptCluster, Float>>()

        for (cluster in CONCEPT_CLUSTERS) {
            var hits = 0f
            for (trigger in cluster.triggers) {
                if (' ' in trigger) {
                    if (queryLower.contains(trigger)) hits += 2.2f
                } else {
                    if (trigger in rawSet) {
                        hits += 1.5f
                    } else if (trigger.length >= 4 && stemWord(trigger) in stemSet) {
                        hits += 1.1f
                    }
                }
            }
            if (hits > 0f) {
                // Normalize slightly by meaningful token count so focused queries rank tighter
                val denom = max(1f, meaningfulTokens.size.toFloat() * 0.6f)
                scored.add(cluster to (hits / denom))
            }
        }

        return scored.sortedByDescending { it.second }.take(3)
    }

    /**
     * Lightweight, pure-JVM streaming JSON reader so full-canon search never loads entire
     * multi-megabyte scripture JSON volumes into heap and works in both Android and JVM tests.
     */
    private class StreamingJsonReader(private val reader: Reader) {
        private var pushed: Int = -2

        private fun nextChar(): Int {
            if (pushed != -2) {
                val c = pushed
                pushed = -2
                return c
            }
            return reader.read()
        }

        private fun unread(c: Int) {
            pushed = c
        }

        private fun peekNonWs(): Int {
            var c = nextChar()
            while (c == ' '.code || c == '\n'.code || c == '\r'.code || c == '\t'.code) {
                c = nextChar()
            }
            unread(c)
            return c
        }

        private fun expect(ch: Char) {
            val c = peekNonWs()
            if (c != ch.code) {
                throw IllegalStateException("Expected '$ch' but got '${if (c == -1) "EOF" else c.toChar()}'")
            }
            nextChar()
        }

        fun beginObject() = expect('{')
        fun endObject() = expect('}')
        fun beginArray() = expect('[')
        fun endArray() = expect(']')

        fun hasNextInObject(): Boolean {
            val c = peekNonWs()
            if (c == '}'.code || c == -1) return false
            if (c == ','.code) {
                nextChar()
                return peekNonWs() != '}'.code
            }
            return true
        }

        fun hasNextInArray(): Boolean {
            val c = peekNonWs()
            if (c == ']'.code || c == -1) return false
            if (c == ','.code) {
                nextChar()
                return peekNonWs() != ']'.code
            }
            return true
        }

        fun nextName(): String {
            val name = nextString()
            expect(':')
            return name
        }

        fun nextString(): String {
            expect('"')
            val sb = StringBuilder()
            while (true) {
                val c = nextChar()
                if (c == -1) throw IllegalStateException("Unterminated JSON string")
                if (c == '"'.code) return sb.toString()
                if (c == '\\'.code) {
                    val esc = nextChar()
                    when (esc) {
                        '"'.code -> sb.append('"')
                        '\\'.code -> sb.append('\\')
                        '/'.code -> sb.append('/')
                        'b'.code -> sb.append('\b')
                        'f'.code -> sb.append('\u000C')
                        'n'.code -> sb.append('\n')
                        'r'.code -> sb.append('\r')
                        't'.code -> sb.append('\t')
                        'u'.code -> {
                            var hex = 0
                            repeat(4) {
                                val h = nextChar()
                                val digit = Character.digit(h, 16)
                                if (digit < 0) throw IllegalStateException("Invalid unicode escape")
                                hex = (hex shl 4) or digit
                            }
                            sb.append(hex.toChar())
                        }
                        else -> if (esc != -1) sb.append(esc.toChar())
                    }
                } else {
                    sb.append(c.toChar())
                }
            }
        }

        fun nextInt(): Int {
            peekNonWs()
            val sb = StringBuilder()
            while (true) {
                val c = nextChar()
                if (c == -1) break
                val ch = c.toChar()
                if (ch in '0'..'9' || ch == '-' || ch == '+' || ch == '.' || ch == 'e' || ch == 'E') {
                    sb.append(ch)
                } else {
                    unread(c)
                    break
                }
            }
            return sb.toString().toDoubleOrNull()?.toInt() ?: 0
        }

        fun skipValue() {
            when (peekNonWs()) {
                '{'.code -> {
                    beginObject()
                    while (hasNextInObject()) {
                        nextName()
                        skipValue()
                    }
                    endObject()
                }
                '['.code -> {
                    beginArray()
                    while (hasNextInArray()) {
                        skipValue()
                    }
                    endArray()
                }
                '"'.code -> nextString()
                else -> {
                    while (true) {
                        val c = nextChar()
                        if (c == -1 || c == ','.code || c == '}'.code || c == ']'.code ||
                            c == ' '.code || c == '\n'.code || c == '\r'.code || c == '\t'.code
                        ) {
                            if (c != -1) unread(c)
                            break
                        }
                    }
                }
            }
        }
    }

    private fun readVersesStreaming(reader: StreamingJsonReader): List<Pair<Int, String>> {
        val verses = ArrayList<Pair<Int, String>>(32)
        reader.beginArray()
        while (reader.hasNextInArray()) {
            reader.beginObject()
            var verseNum = 0
            var text = ""
            while (reader.hasNextInObject()) {
                when (reader.nextName()) {
                    "verse" -> verseNum = reader.nextInt()
                    "text" -> text = reader.nextString()
                    else -> reader.skipValue()
                }
            }
            reader.endObject()
            if (text.isNotBlank()) {
                verses.add(verseNum to text)
            }
        }
        reader.endArray()
        return verses
    }

    private fun scanVolumeAsset(
        assetStreamProvider: ((String) -> InputStream?)?,
        volumeId: String,
        queryLower: String,
        exactPhraseRegex: Regex?,
        meaningfulTokens: List<String>,
        queryStems: List<String>,
        matchedClusters: List<ConceptCluster>,
        conceptTerms: Set<String>,
        conceptTermRegexes: Map<String, Regex>,
        conceptStems: Set<String>,
        anchorBoostMap: Map<String, Pair<String, Float>>,
        directOut: MutableList<GospelSearchResult>,
        conceptOut: MutableList<GospelSearchResult>,
        isCancelled: () -> Boolean,
    ) {
        if (assetStreamProvider == null) return
        val assetPath = when (volumeId) {
            "bofm" -> "scriptures/book-of-mormon.json"
            "nt" -> "scriptures/new-testament.json"
            "ot" -> "scriptures/old-testament.bin"
            "dc" -> "scriptures/doctrine-and-covenants.json"
            "pgp" -> "scriptures/pearl-of-great-price.json"
            else -> return
        }
        val volumeTitle = StandardWorksData.findVolume(volumeId)?.shortTitle ?: volumeId
        val booksInVol = StandardWorksData.getBooksForVolume(volumeId)

        val rawStream = try {
            assetStreamProvider(assetPath)
        } catch (_: Exception) {
            null
        } ?: return

        try {
            rawStream.use { raw ->
                val stream = if (assetPath.endsWith(".bin")) GZIPInputStream(raw) else raw
                BufferedReader(InputStreamReader(stream, Charsets.UTF_8), 16384).use { buffered ->
                    val jsonReader = StreamingJsonReader(buffered)
                    jsonReader.beginObject()
                    while (jsonReader.hasNextInObject()) {
                        if (isCancelled()) return
                        when (jsonReader.nextName()) {
                            "sections" -> {
                                if (volumeId != "dc" || booksInVol.isEmpty()) {
                                    jsonReader.skipValue()
                                } else {
                                    val dcBook = booksInVol.first()
                                    jsonReader.beginArray()
                                    while (jsonReader.hasNextInArray()) {
                                        if (isCancelled()) return
                                        jsonReader.beginObject()
                                        var sectionNum = 0
                                        var verses: List<Pair<Int, String>> = emptyList()
                                        while (jsonReader.hasNextInObject()) {
                                            when (jsonReader.nextName()) {
                                                "section" -> sectionNum = jsonReader.nextInt()
                                                "verses" -> verses = readVersesStreaming(jsonReader)
                                                else -> jsonReader.skipValue()
                                            }
                                        }
                                        jsonReader.endObject()
                                        if (verses.isNotEmpty()) {
                                            scoreChapterVerses(
                                                volumeId = volumeId,
                                                volumeTitle = volumeTitle,
                                                book = dcBook,
                                                chapterNum = sectionNum,
                                                verses = verses,
                                                queryLower = queryLower,
                                                exactPhraseRegex = exactPhraseRegex,
                                                meaningfulTokens = meaningfulTokens,
                                                queryStems = queryStems,
                                                matchedClusters = matchedClusters,
                                                conceptTerms = conceptTerms,
                                                conceptTermRegexes = conceptTermRegexes,
                                                conceptStems = conceptStems,
                                                anchorBoostMap = anchorBoostMap,
                                                directOut = directOut,
                                                conceptOut = conceptOut,
                                            )
                                        }
                                    }
                                    jsonReader.endArray()
                                }
                            }
                            "books" -> {
                                if (volumeId == "dc") {
                                    jsonReader.skipValue()
                                } else {
                                    jsonReader.beginArray()
                                    while (jsonReader.hasNextInArray()) {
                                        if (isCancelled()) return
                                        jsonReader.beginObject()
                                        var book: ScriptureBook? = null
                                        while (jsonReader.hasNextInObject()) {
                                            if (isCancelled()) return
                                            when (jsonReader.nextName()) {
                                                "book" -> {
                                                    val bookName = jsonReader.nextString()
                                                    book = matchBookInVolume(bookName, booksInVol)
                                                }
                                                "chapters" -> {
                                                    val matchedBook = book
                                                    if (matchedBook == null) {
                                                        jsonReader.skipValue()
                                                    } else {
                                                        jsonReader.beginArray()
                                                        while (jsonReader.hasNextInArray()) {
                                                            if (isCancelled()) return
                                                            jsonReader.beginObject()
                                                            var chapterNum = 0
                                                            var verses: List<Pair<Int, String>> = emptyList()
                                                            while (jsonReader.hasNextInObject()) {
                                                                when (jsonReader.nextName()) {
                                                                    "chapter" -> chapterNum = jsonReader.nextInt()
                                                                    "verses" -> verses = readVersesStreaming(jsonReader)
                                                                    else -> jsonReader.skipValue()
                                                                }
                                                            }
                                                            jsonReader.endObject()
                                                            if (verses.isNotEmpty()) {
                                                                scoreChapterVerses(
                                                                    volumeId = volumeId,
                                                                    volumeTitle = volumeTitle,
                                                                    book = matchedBook,
                                                                    chapterNum = chapterNum,
                                                                    verses = verses,
                                                                    queryLower = queryLower,
                                                                    exactPhraseRegex = exactPhraseRegex,
                                                                    meaningfulTokens = meaningfulTokens,
                                                                    queryStems = queryStems,
                                                                    matchedClusters = matchedClusters,
                                                                    conceptTerms = conceptTerms,
                                                                    conceptTermRegexes = conceptTermRegexes,
                                                                    conceptStems = conceptStems,
                                                                    anchorBoostMap = anchorBoostMap,
                                                                    directOut = directOut,
                                                                    conceptOut = conceptOut,
                                                                )
                                                            }
                                                        }
                                                        jsonReader.endArray()
                                                    }
                                                }
                                                else -> jsonReader.skipValue()
                                            }
                                        }
                                        jsonReader.endObject()
                                    }
                                    jsonReader.endArray()
                                }
                            }
                            else -> jsonReader.skipValue()
                        }
                    }
                    jsonReader.endObject()
                }
            }
        } catch (_: Exception) {
            // Gracefully skip unreadable asset
        }
    }

    private fun matchBookInVolume(bookInJson: String, books: List<ScriptureBook>): ScriptureBook? {
        val normJson = bookInJson.replace("—", "-").replace(" ", "").lowercase(Locale.US)
        return books.firstOrNull { b ->
            val normTarget = b.shortTitle.replace("—", "-").replace(" ", "").lowercase(Locale.US)
            bookInJson.equals(b.shortTitle, ignoreCase = true) ||
                normJson == normTarget ||
                (normTarget.contains("solomon") && normJson.contains("solomon")) ||
                (normTarget.startsWith("js") && normJson.contains("josephsmith") &&
                    ((normTarget.contains("matthew") && normJson.contains("matthew")) ||
                        (!normTarget.contains("matthew") && normJson.contains("history"))))
        }
    }

    private fun scoreChapterVerses(
        volumeId: String,
        volumeTitle: String,
        book: ScriptureBook,
        chapterNum: Int,
        verses: List<Pair<Int, String>>,
        queryLower: String,
        exactPhraseRegex: Regex?,
        meaningfulTokens: List<String>,
        queryStems: List<String>,
        matchedClusters: List<ConceptCluster>,
        conceptTerms: Set<String>,
        conceptTermRegexes: Map<String, Regex>,
        conceptStems: Set<String>,
        anchorBoostMap: Map<String, Pair<String, Float>>,
        directOut: MutableList<GospelSearchResult>,
        conceptOut: MutableList<GospelSearchResult>,
    ) {
        val totalVerses = verses.size.coerceAtLeast(1)
        val hasPhraseQuery = meaningfulTokens.size >= 2

        for ((idx, pair) in verses.withIndex()) {
            val (verseNum, rawText) = pair
            val lower = rawText.lowercase(Locale.US)

            val anchorKey = "$volumeId/${book.id}/$chapterNum/$verseNum"
            val anchorHit = anchorBoostMap[anchorKey]

            // Fast pre-filter before tokenizing to keep full-canon scans super fast & low-CPU
            val exactPhraseHit = containsWholeTerm(lower, queryLower, exactPhraseRegex)
            var quickTokenHit = exactPhraseHit || anchorHit != null
            if (!quickTokenHit) {
                for (tok in meaningfulTokens) {
                    if (tok.length >= 2 && lower.contains(tok)) {
                        quickTokenHit = true
                        break
                    }
                }
            }
            if (!quickTokenHit) {
                for (stem in queryStems) {
                    if (stem.length >= 3 && lower.contains(stem)) {
                        quickTokenHit = true
                        break
                    }
                }
            }
            var quickConceptHit = false
            if (!quickTokenHit && conceptStems.isNotEmpty()) {
                var cHits = 0
                for (term in conceptTerms) {
                    if (containsWholeTerm(lower, term, conceptTermRegexes[term])) {
                        cHits++
                        if (cHits >= 2) {
                            quickConceptHit = true
                            break
                        }
                    }
                }
            }
            if (!quickTokenHit && !quickConceptHit) continue

            val verseWords = tokenize(lower)
            val verseStems = verseWords.mapTo(HashSet(verseWords.size)) { stemWord(it) }

            var matchedQueryTokens = 0
            for (tok in meaningfulTokens) {
                val st = stemWord(tok)
                if (tok in verseWords || (st.length >= 3 && st in verseStems)) {
                    matchedQueryTokens++
                }
            }

            val refTitle = if (volumeId == "dc") {
                "D&C $chapterNum:$verseNum"
            } else {
                "${book.shortTitle} $chapterNum:$verseNum"
            }
            val contentId = "scripture/$volumeId/${book.id}/$chapterNum"
            val permille = if (idx > 0 && totalVerses > 1) {
                ((idx.toFloat() / totalVerses.toFloat()) * 1000f).toInt().coerceIn(0, 960)
            } else 0

            val minRequiredForDirect = when {
                meaningfulTokens.size <= 1 -> 1
                meaningfulTokens.size == 2 -> 2
                else -> max(2, (meaningfulTokens.size * 0.6f).toInt())
            }

            if (exactPhraseHit || matchedQueryTokens >= minRequiredForDirect) {
                var score = 0f
                if (exactPhraseHit) score += 120f
                val coverage = matchedQueryTokens.toFloat() / max(1, meaningfulTokens.size).toFloat()
                score += coverage * 65f + matchedQueryTokens * 8f
                if (anchorHit != null) score += 18f
                // Slight preference for concise, punchy verses
                if (rawText.length in 40..280) score += 4f

                val kind = if (exactPhraseHit && hasPhraseQuery) SearchMatchKind.EXACT else SearchMatchKind.WORDS
                val highlightAnchor = if (exactPhraseHit) queryLower else meaningfulTokens.firstOrNull().orEmpty()
                directOut.add(
                    GospelSearchResult(
                        contentId = contentId,
                        title = refTitle,
                        subtitle = volumeTitle,
                        snippet = buildSnippet(rawText, highlightAnchor),
                        matchKind = kind,
                        topicLabel = anchorHit?.first,
                        scrollPermille = permille,
                        score = score,
                    )
                )
            } else if (anchorHit != null || quickConceptHit) {
                var conceptScore = anchorHit?.second ?: 0f
                var conceptTermMatches = 0
                var matchedTermForSnippet = ""
                for (term in conceptTerms) {
                    if (containsWholeTerm(lower, term, conceptTermRegexes[term])) {
                        conceptTermMatches++
                        if (matchedTermForSnippet.isEmpty()) matchedTermForSnippet = term
                    }
                }
                conceptScore += conceptTermMatches * 14f + matchedQueryTokens * 12f
                if (conceptScore >= 28f) {
                    val topicLabel = anchorHit?.first ?: matchedClusters.firstOrNull { cluster ->
                        cluster.scriptureTerms.any { containsWholeTerm(lower, it, conceptTermRegexes[it]) }
                    }?.label ?: matchedClusters.firstOrNull()?.label
                    conceptOut.add(
                        GospelSearchResult(
                            contentId = contentId,
                            title = refTitle,
                            subtitle = volumeTitle,
                            snippet = buildSnippet(rawText, matchedTermForSnippet.ifEmpty { queryLower }),
                            matchKind = SearchMatchKind.CONCEPT,
                            topicLabel = topicLabel,
                            scrollPermille = permille,
                            score = conceptScore,
                        )
                    )
                }
            }
        }
    }

    private fun searchConferenceTalks(
        talks: List<ConferenceTalkEntity>,
        queryLower: String,
        exactPhraseRegex: Regex?,
        meaningfulTokens: List<String>,
        queryStems: List<String>,
        matchedClusters: List<ConceptCluster>,
        conceptTerms: Set<String>,
        conceptTermRegexes: Map<String, Regex>,
        directOut: MutableList<GospelSearchResult>,
        conceptOut: MutableList<GospelSearchResult>,
        isCancelled: () -> Boolean,
    ) {
        val hasPhraseQuery = meaningfulTokens.size >= 2

        for (talk in talks) {
            if (isCancelled()) break
            val headerLower = "${talk.title} ${talk.speaker} ${talk.summary}".lowercase(Locale.US)
            val paragraphs = talk.paragraphs.split("\n\n").filter { it.isNotBlank() }
            val totalParas = paragraphs.size.coerceAtLeast(1)

            var bestDirectScore = 0f
            var bestDirectKind = SearchMatchKind.WORDS
            var bestDirectSnippet = talk.summary.ifBlank { paragraphs.firstOrNull().orEmpty() }
            var bestDirectPermille = 0

            var bestConceptScore = 0f
            var bestConceptSnippet = talk.summary.ifBlank { paragraphs.firstOrNull().orEmpty() }
            var bestConceptTopic: String? = null
            var bestConceptPermille = 0

            // Check talk title + speaker + summary first (strong signal!)
            val headerExact = containsWholeTerm(headerLower, queryLower, exactPhraseRegex)
            val headerWords = tokenize(headerLower)
            val headerStems = headerWords.mapTo(HashSet()) { stemWord(it) }
            val headerHits = meaningfulTokens.count { tok ->
                tok in headerWords || (stemWord(tok).length >= 3 && stemWord(tok) in headerStems)
            }
            if (headerExact || (meaningfulTokens.isNotEmpty() && headerHits >= meaningfulTokens.size)) {
                bestDirectScore = (if (headerExact) 140f else 85f) + headerHits * 10f
                bestDirectKind = if (headerExact && hasPhraseQuery) SearchMatchKind.EXACT else SearchMatchKind.WORDS
                bestDirectSnippet = talk.summary.ifBlank { paragraphs.firstOrNull().orEmpty().take(180) }
                bestDirectPermille = 0
            }

            // Scan talk paragraphs for exact/keyword & conceptual teaching matches
            for ((pIdx, para) in paragraphs.withIndex()) {
                val pLower = para.lowercase(Locale.US)
                val exactHit = containsWholeTerm(pLower, queryLower, exactPhraseRegex)
                var hasQuickHit = exactHit
                if (!hasQuickHit) {
                    for (tok in meaningfulTokens) {
                        if (tok.length >= 2 && pLower.contains(tok)) {
                            hasQuickHit = true
                            break
                        }
                    }
                }
                if (!hasQuickHit) {
                    for (st in queryStems) {
                        if (st.length >= 3 && pLower.contains(st)) {
                            hasQuickHit = true
                            break
                        }
                    }
                }
                var conceptHits = 0
                var firstConceptTerm = ""
                if (conceptTerms.isNotEmpty()) {
                    for (term in conceptTerms) {
                        if (containsWholeTerm(pLower, term, conceptTermRegexes[term])) {
                            conceptHits++
                            if (firstConceptTerm.isEmpty()) firstConceptTerm = term
                        }
                    }
                }
                if (!hasQuickHit && conceptHits < 2) continue

                val pWords = tokenize(pLower)
                val pStems = pWords.mapTo(HashSet(pWords.size)) { stemWord(it) }
                val matchedCount = meaningfulTokens.count { tok ->
                    val st = stemWord(tok)
                    tok in pWords || (st.length >= 3 && st in pStems)
                }
                val permille = if (pIdx > 0 && totalParas > 1) {
                    ((pIdx.toFloat() / totalParas.toFloat()) * 1000f).toInt().coerceIn(0, 960)
                } else 0

                val minRequired = when {
                    meaningfulTokens.size <= 1 -> 1
                    meaningfulTokens.size == 2 -> 2
                    else -> max(2, (meaningfulTokens.size * 0.6f).toInt())
                }

                if (exactHit || matchedCount >= minRequired) {
                    val coverage = matchedCount.toFloat() / max(1, meaningfulTokens.size).toFloat()
                    val sc = (if (exactHit) 115f else 0f) + coverage * 62f + matchedCount * 8f + headerHits * 12f
                    if (sc > bestDirectScore) {
                        bestDirectScore = sc
                        bestDirectKind = if (exactHit && hasPhraseQuery) SearchMatchKind.EXACT else SearchMatchKind.WORDS
                        bestDirectSnippet = buildSnippet(para, if (exactHit) queryLower else meaningfulTokens.firstOrNull().orEmpty())
                        bestDirectPermille = permille
                    }
                } else if (conceptHits >= 2) {
                    val cScore = conceptHits * 15f + matchedCount * 10f + headerHits * 8f
                    if (cScore > bestConceptScore) {
                        bestConceptScore = cScore
                        bestConceptSnippet = buildSnippet(para, firstConceptTerm)
                        bestConceptTopic = matchedClusters.firstOrNull { cl ->
                            cl.scriptureTerms.any { containsWholeTerm(pLower, it, conceptTermRegexes[it]) }
                        }?.label ?: matchedClusters.firstOrNull()?.label
                        bestConceptPermille = permille
                    }
                }
            }

            val subtitle = "${talk.speaker} • ${talk.periodName}"
            if (bestDirectScore > 0f) {
                directOut.add(
                    GospelSearchResult(
                        contentId = talk.talkId,
                        title = talk.title,
                        subtitle = subtitle,
                        snippet = bestDirectSnippet,
                        matchKind = bestDirectKind,
                        topicLabel = matchedClusters.firstOrNull()?.label,
                        scrollPermille = bestDirectPermille,
                        score = bestDirectScore,
                    )
                )
            } else if (bestConceptScore >= 30f) {
                conceptOut.add(
                    GospelSearchResult(
                        contentId = talk.talkId,
                        title = talk.title,
                        subtitle = subtitle,
                        snippet = bestConceptSnippet,
                        matchKind = SearchMatchKind.CONCEPT,
                        topicLabel = bestConceptTopic,
                        scrollPermille = bestConceptPermille,
                        score = bestConceptScore,
                    )
                )
            }
        }
    }

    internal fun parseHymnNumberQuery(query: String): Int? {
        val m = Regex("""^(?:hymn\s*#?|#)?\s*(\d{1,4})$""", RegexOption.IGNORE_CASE).matchEntire(query.trim())
            ?: return null
        return m.groupValues[1].toIntOrNull()?.takeIf { it in 1..2999 }
    }

    private fun searchHymns(
        hymns: List<HymnItem>,
        queryLower: String,
        exactPhraseRegex: Regex?,
        meaningfulTokens: List<String>,
        queryStems: List<String>,
        directOut: MutableList<GospelSearchResult>,
        isCancelled: () -> Boolean,
    ) {
        if (meaningfulTokens.isEmpty()) return
        val hasPhraseQuery = meaningfulTokens.size >= 2
        val minRequired = when {
            meaningfulTokens.size <= 1 -> 1
            meaningfulTokens.size == 2 -> 2
            else -> max(2, (meaningfulTokens.size * 0.6f).toInt())
        }

        for (hymn in hymns) {
            if (isCancelled()) break
            val titleLower = "${hymn.number} ${hymn.title} ${hymn.topic}".lowercase(Locale.US)
            val titleExact = containsWholeTerm(titleLower, queryLower, exactPhraseRegex)
            val titleWords = tokenize(titleLower)
            val titleStems = titleWords.mapTo(HashSet()) { stemWord(it) }
            val titleHits = meaningfulTokens.count { tok ->
                val st = stemWord(tok)
                tok in titleWords || (st.length >= 3 && st in titleStems)
            }

            var bestScore = 0f
            var bestKind = SearchMatchKind.WORDS
            var bestSnippet = hymn.stanzas.firstOrNull()?.replace("\n", " ")?.take(160) ?: hymn.title

            if (titleExact || titleHits >= minRequired) {
                bestScore = (if (titleExact) 145f else 90f) + titleHits * 12f
                bestKind = if (titleExact && hasPhraseQuery) SearchMatchKind.EXACT else SearchMatchKind.WORDS
            }

            for (stanza in hymn.stanzas) {
                val sLower = stanza.lowercase(Locale.US)
                val exactHit = containsWholeTerm(sLower, queryLower, exactPhraseRegex)
                var quickHit = exactHit
                if (!quickHit) {
                    for (tok in meaningfulTokens) {
                        if (tok.length >= 2 && sLower.contains(tok)) {
                            quickHit = true
                            break
                        }
                    }
                }
                if (!quickHit) {
                    for (st in queryStems) {
                        if (st.length >= 3 && sLower.contains(st)) {
                            quickHit = true
                            break
                        }
                    }
                }
                if (!quickHit) continue

                val sWords = tokenize(sLower)
                val sStems = sWords.mapTo(HashSet(sWords.size)) { stemWord(it) }
                val hits = meaningfulTokens.count { tok ->
                    val st = stemWord(tok)
                    tok in sWords || (st.length >= 3 && st in sStems)
                }
                if (exactHit || hits >= minRequired) {
                    val coverage = hits.toFloat() / max(1, meaningfulTokens.size).toFloat()
                    val sc = (if (exactHit) 118f else 0f) + coverage * 62f + hits * 8f + titleHits * 14f
                    if (sc > bestScore) {
                        bestScore = sc
                        bestKind = if (exactHit && hasPhraseQuery) SearchMatchKind.EXACT else SearchMatchKind.WORDS
                        bestSnippet = buildSnippet(stanza.replace("\n", " / "), if (exactHit) queryLower else meaningfulTokens.first())
                    }
                }
            }

            if (bestScore > 0f) {
                val book = HymnBook.fromId(hymn.bookId)
                directOut.add(
                    GospelSearchResult(
                        contentId = hymn.id,
                        title = "#${hymn.number} ${hymn.title}",
                        subtitle = "${book.shortTitle}${if (hymn.topic.isNotBlank()) " • ${hymn.topic}" else ""}",
                        snippet = bestSnippet,
                        matchKind = bestKind,
                        scrollPermille = 0,
                        score = bestScore,
                    )
                )
            }
        }
    }

    private fun buildSnippet(text: String, needle: String): String {
        val clean = text.replace(Regex("\\s+"), " ").trim()
        if (clean.length <= 165) return clean
        val idx = if (needle.isNotBlank()) clean.lowercase(Locale.US).indexOf(needle.lowercase(Locale.US)) else -1
        if (idx < 0 || idx < 60) {
            return clean.take(162).trimEnd() + "…"
        }
        val start = (idx - 45).coerceAtLeast(0)
        val end = min(clean.length, start + 162)
        val prefix = if (start > 0) "…" else ""
        val suffix = if (end < clean.length) "…" else ""
        return prefix + clean.substring(start, end).trim() + suffix
    }

    internal fun tokenize(text: String): List<String> {
        val out = ArrayList<String>(16)
        val sb = StringBuilder()
        for (ch in text) {
            if (ch.isLetterOrDigit() || ch == '\'') {
                sb.append(ch.lowercaseChar())
            } else if (sb.isNotEmpty()) {
                val word = sb.toString().trim('\'')
                if (word.isNotEmpty()) out.add(word)
                sb.clear()
            }
        }
        if (sb.isNotEmpty()) {
            val word = sb.toString().trim('\'')
            if (word.isNotEmpty()) out.add(word)
        }
        return out
    }

    internal fun stemWord(word: String): String {
        var w = word.lowercase(Locale.US)
        if (w.length <= 3) return w
        // KJV verb suffixes + standard English suffixes
        val suffixes = listOf(
            "fulness", "lessness", "ations", "ation", "ments", "ment", "ness",
            "ings", "ing", "eth", "est", "ied", "ies", "ed", "es", "ly", "s",
        )
        for (suf in suffixes) {
            if (w.endsWith(suf) && w.length - suf.length >= 3) {
                w = w.dropLast(suf.length)
                break
            }
        }
        if (w.length > 4 && w.endsWith("e")) {
            w = w.dropLast(1)
        }
        return w
    }
}
