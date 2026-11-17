package com.thelightphone.gospellibrary.data

import com.thelightphone.gospellibrary.model.ContentChapter
import com.thelightphone.gospellibrary.model.ContentType
import com.thelightphone.gospellibrary.model.ContentVerse
import com.thelightphone.gospellibrary.model.ScriptureBook
import com.thelightphone.gospellibrary.model.ScriptureVolume

object StandardWorksData {

    val volumes: List<ScriptureVolume> = listOf(
        ScriptureVolume("bofm", "The Book of Mormon", "Book of Mormon", 15, "Another Testament of Jesus Christ"),
        ScriptureVolume("nt", "The New Testament", "New Testament", 27, "The mortal ministry of Jesus Christ and His ancient Apostles"),
        ScriptureVolume("ot", "The Old Testament", "Old Testament", 39, "The creation, covenants, and law of the Lord with Israel"),
        ScriptureVolume("dc", "The Doctrine and Covenants", "Doctrine & Covenants", 1, "Revelations given for the restoration of the gospel"),
        ScriptureVolume("pgp", "The Pearl of Great Price", "Pearl of Great Price", 5, "Sacred writings touching matters revealed in modern times")
    )

    private val bookOfMormonBooks = listOf(
        ScriptureBook("1-ne", "bofm", "First Book of Nephi", "1 Nephi", 22, "An account of Lehi and his wife Sariah, and his four sons."),
        ScriptureBook("2-ne", "bofm", "Second Book of Nephi", "2 Nephi", 33, "An account of the death of Lehi and the ministry of Nephi."),
        ScriptureBook("jacob", "bofm", "Book of Jacob", "Jacob", 7, "The words of his preaching unto his brethren."),
        ScriptureBook("enos", "bofm", "Book of Enos", "Enos", 1, "Enos prays mightily and obtains a remission of his sins."),
        ScriptureBook("jarom", "bofm", "Book of Jarom", "Jarom", 1, "The Nephites keep the law of Moses and prosper in the land."),
        ScriptureBook("omni", "bofm", "Book of Omni", "Omni", 1, "Various writers maintain the plates; Mosiah discovers Zarahemla."),
        ScriptureBook("w-of-m", "bofm", "Words of Mormon", "Words of Mormon", 1, "Mormon connects the record of the large plates with the small plates."),
        ScriptureBook("mosiah", "bofm", "Book of Mosiah", "Mosiah", 29, "King Benjamin addresses his people; the reigns of Mosiah and Alma."),
        ScriptureBook("alma", "bofm", "Book of Alma", "Alma", 63, "The account of Alma and the sons of Mosiah; the wars of the Nephites."),
        ScriptureBook("hel", "bofm", "Book of Helaman", "Helaman", 16, "The secret combinations; prophecy of Samuel the Lamanite."),
        ScriptureBook("3-ne", "bofm", "Third Book of Nephi", "3 Nephi", 30, "The ministry and personal appearance of Jesus Christ unto the Nephites."),
        ScriptureBook("4-ne", "bofm", "Fourth Book of Nephi", "4 Nephi", 1, "An era of peace and righteousness among the Nephites and Lamanites."),
        ScriptureBook("morm", "bofm", "Book of Mormon", "Mormon", 9, "Mormon abridges the records and witnesses the destruction of his people."),
        ScriptureBook("ether", "bofm", "Book of Ether", "Ether", 15, "The record of the Jaredites, taken from the twenty-four plates found by Limhi."),
        ScriptureBook("moroni", "bofm", "Book of Moroni", "Moroni", 10, "Moroni seals the record and gives final exhortations and blessings.")
    )

    private val newTestamentBooks = listOf(
        ScriptureBook("matt", "nt", "Gospel According to St. Matthew", "Matthew", 28, "The genealogy, life, ministry, and resurrection of Jesus Christ."),
        ScriptureBook("mark", "nt", "Gospel According to St. Mark", "Mark", 16, "The active ministry and miracles of Jesus Christ."),
        ScriptureBook("luke", "nt", "Gospel According to St. Luke", "Luke", 24, "A narrative of the Savior's compassion and universal mission."),
        ScriptureBook("john", "nt", "Gospel According to St. John", "John", 21, "Testimony of the divine Sonship and eternal role of Jesus Christ."),
        ScriptureBook("acts", "nt", "The Acts of the Apostles", "Acts", 28, "The ministry of Peter, Paul, and the early Church."),
        ScriptureBook("rom", "nt", "Epistle of Paul to the Romans", "Romans", 16, "Justification by faith through Jesus Christ and holy living."),
        ScriptureBook("1-cor", "nt", "First Epistle of Paul to the Corinthians", "1 Corinthians", 16, "Unity, spiritual gifts, charity, and the Resurrection."),
        ScriptureBook("2-cor", "nt", "Second Epistle of Paul to the Corinthians", "2 Corinthians", 13, "Paul's apostolic ministry and consolation in trials."),
        ScriptureBook("gal", "nt", "Epistle of Paul to the Galatians", "Galatians", 6, "Freedom in Christ and the fruits of the Spirit."),
        ScriptureBook("eph", "nt", "Epistle of Paul to the Ephesians", "Ephesians", 6, "The armor of God, unity, and church organization."),
        ScriptureBook("philip", "nt", "Epistle of Paul to the Philippians", "Philippians", 4, "Rejoicing in Christ through all circumstances."),
        ScriptureBook("col", "nt", "Epistle of Paul to the Colossians", "Colossians", 4, "Preeminence of Christ and renewed discipleship."),
        ScriptureBook("1-thes", "nt", "First Epistle to the Thessalonians", "1 Thessalonians", 5, "Preparing for the second coming of the Savior."),
        ScriptureBook("2-thes", "nt", "Second Epistle to the Thessalonians", "2 Thessalonians", 3, "Steadfastness and the apostasy before the Second Coming."),
        ScriptureBook("1-tim", "nt", "First Epistle of Paul to Timothy", "1 Timothy", 6, "Pastoral counsel on leadership and sound doctrine."),
        ScriptureBook("2-tim", "nt", "Second Epistle of Paul to Timothy", "2 Timothy", 4, "Paul's final charge to fight the good fight of faith."),
        ScriptureBook("titus", "nt", "Epistle of Paul to Titus", "Titus", 3, "Ordering the Church and maintaining good works."),
        ScriptureBook("philem", "nt", "Epistle of Paul to Philemon", "Philemon", 1, "Forgiveness and Christian brotherhood for Onesimus."),
        ScriptureBook("heb", "nt", "Epistle of Paul to the Hebrews", "Hebrews", 13, "Christ the great High Priest and author of faith."),
        ScriptureBook("james", "nt", "General Epistle of James", "James", 5, "Faith without works is dead; asking of God in faith."),
        ScriptureBook("1-pet", "nt", "First General Epistle of Peter", "1 Peter", 5, "Patience under suffering and redemption of the dead."),
        ScriptureBook("2-pet", "nt", "Second General Epistle of Peter", "2 Peter", 3, "Partaking of the divine nature and growing in grace."),
        ScriptureBook("1-jn", "nt", "First General Epistle of John", "1 John", 5, "Walking in the light and the perfect love of God."),
        ScriptureBook("2-jn", "nt", "Second General Epistle of John", "2 John", 1, "Abiding in the doctrine of Christ."),
        ScriptureBook("3-jn", "nt", "Third General Epistle of John", "3 John", 1, "Praise for truth and hospitality among believers."),
        ScriptureBook("jude", "nt", "General Epistle of Jude", "Jude", 1, "Contending earnestly for the faith."),
        ScriptureBook("rev", "nt", "The Revelation of St. John", "Revelation", 22, "John's prophetic vision of the triumph of Christ.")
    )

    private val oldTestamentBooks = listOf(
        ScriptureBook("gen", "ot", "The Book of Genesis", "Genesis", 50, "The Creation, the Fall, and the patriarchs of Israel."),
        ScriptureBook("ex", "ot", "The Book of Exodus", "Exodus", 40, "Deliverance from Egypt, Mount Sinai, and the Law."),
        ScriptureBook("lev", "ot", "The Book of Leviticus", "Leviticus", 27, "Sacrifices, priesthood rites, and holiness of life."),
        ScriptureBook("num", "ot", "The Book of Numbers", "Numbers", 36, "The wanderings of Israel through the wilderness."),
        ScriptureBook("deut", "ot", "The Book of Deuteronomy", "Deuteronomy", 34, "Moses' farewell exhortations and renewal of the covenant."),
        ScriptureBook("josh", "ot", "The Book of Joshua", "Joshua", 24, "Conquest and division of the Promised Land."),
        ScriptureBook("judg", "ot", "The Book of Judges", "Judges", 21, "Cycles of apostasy, oppression, and deliverance."),
        ScriptureBook("ruth", "ot", "The Book of Ruth", "Ruth", 4, "Loyalty, faith, and the ancestry of David."),
        ScriptureBook("1-sam", "ot", "First Book of Samuel", "1 Samuel", 31, "The reign of Saul and the youth of David."),
        ScriptureBook("2-sam", "ot", "Second Book of Samuel", "2 Samuel", 24, "The reign, victories, and trials of King David."),
        ScriptureBook("1-kgs", "ot", "First Book of the Kings", "1 Kings", 22, "Solomon's temple and the division of the kingdom."),
        ScriptureBook("2-kgs", "ot", "Second Book of the Kings", "2 Kings", 25, "The ministries of Elijah and Elisha; the captivities."),
        ScriptureBook("1-chr", "ot", "First Book of the Chronicles", "1 Chronicles", 29, "Genealogies and the spiritual reign of David."),
        ScriptureBook("2-chr", "ot", "Second Book of the Chronicles", "2 Chronicles", 36, "Solomon, temple worship, and the kings of Judah."),
        ScriptureBook("ezra", "ot", "The Book of Ezra", "Ezra", 10, "Return of the remnant and rebuilding of the temple."),
        ScriptureBook("neh", "ot", "The Book of Nehemiah", "Nehemiah", 13, "Rebuilding the walls of Jerusalem and covenant renewal."),
        ScriptureBook("esth", "ot", "The Book of Esther", "Esther", 10, "Deliverance of the Jews in the Persian empire."),
        ScriptureBook("job", "ot", "The Book of Job", "Job", 42, "Faith in the Redeemer amidst suffering and trial."),
        ScriptureBook("ps", "ot", "The Book of Psalms", "Psalms", 150, "Sacred poetry, songs of praise, and messianic prophecies."),
        ScriptureBook("prov", "ot", "The Book of Proverbs", "Proverbs", 31, "Inspired maxims of practical wisdom and righteousness."),
        ScriptureBook("eccl", "ot", "The Book of Ecclesiastes", "Ecclesiastes", 12, "Vanity of worldly pursuits and duty to fear God."),
        ScriptureBook("song", "ot", "The Song of Solomon", "Song of Solomon", 8, "Biblical poetry celebrating love and marriage."),
        ScriptureBook("isa", "ot", "The Book of the Prophet Isaiah", "Isaiah", 66, "Grand prophecies of the Messiah, gathering, and Zion."),
        ScriptureBook("jer", "ot", "The Book of the Prophet Jeremiah", "Jeremiah", 52, "Call to repentance and the new covenant."),
        ScriptureBook("lam", "ot", "Lamentations of Jeremiah", "Lamentations", 5, "Elegies mourning the desolation of Jerusalem."),
        ScriptureBook("ezek", "ot", "The Book of the Prophet Ezekiel", "Ezekiel", 48, "Visions of God's glory, the dry bones, and the temple."),
        ScriptureBook("dan", "ot", "The Book of Daniel", "Daniel", 12, "Faithfulness in Babylon and kingdoms of the world."),
        ScriptureBook("hos", "ot", "The Book of Hosea", "Hosea", 14, "The unyielding covenant love and mercy of the Lord."),
        ScriptureBook("joel", "ot", "The Book of Joel", "Joel", 3, "Outpouring of the Spirit in the latter days."),
        ScriptureBook("amos", "ot", "The Book of Amos", "Amos", 9, "Justice, righteousness, and the plumb line of truth."),
        ScriptureBook("obad", "ot", "The Book of Obadiah", "Obadiah", 1, "Saviors shall come up on Mount Zion."),
        ScriptureBook("jonah", "ot", "The Book of Jonah", "Jonah", 4, "The mission to Nineveh and universal mercy of God."),
        ScriptureBook("micah", "ot", "The Book of Micah", "Micah", 7, "To do justly, love mercy, and walk humbly with God."),
        ScriptureBook("nahum", "ot", "The Book of Nahum", "Nahum", 3, "Judgment upon Nineveh and comfort for Judah."),
        ScriptureBook("hab", "ot", "The Book of Habakkuk", "Habakkuk", 3, "The just shall live by faith."),
        ScriptureBook("zeph", "ot", "The Book of Zephaniah", "Zephaniah", 3, "The great day of the Lord and joy of Zion."),
        ScriptureBook("hag", "ot", "The Book of Haggai", "Haggai", 2, "Call to rebuild the Lord's house."),
        ScriptureBook("zech", "ot", "The Book of Zechariah", "Zechariah", 14, "Visions of the Branch and the triumphant King."),
        ScriptureBook("mal", "ot", "The Book of Malachi", "Malachi", 4, "Tithing, the book of remembrance, and the mission of Elijah.")
    )

    private val doctrineAndCovenantsBooks = listOf(
        ScriptureBook("sections", "dc", "The Doctrine and Covenants", "Sections", 138, "Modern revelations on priesthood, doctrine, and the restoration.")
    )

    private val pearlOfGreatPriceBooks = listOf(
        ScriptureBook("moses", "pgp", "Selections from the Book of Moses", "Moses", 8, "Visions given to Moses concerning creation and Enoch."),
        ScriptureBook("abr", "pgp", "The Book of Abraham", "Abraham", 5, "The premortal council, astronomy, and the Abrahamic covenant."),
        ScriptureBook("js-m", "pgp", "Joseph Smith—Matthew", "JS—Matthew", 1, "Jesus Christ's prophecy on the destruction of Jerusalem and the Second Coming."),
        ScriptureBook("js-h", "pgp", "Joseph Smith—History", "JS—History", 1, "The First Vision and coming forth of the Book of Mormon."),
        ScriptureBook("a-of-f", "pgp", "The Articles of Faith", "Articles of Faith", 1, "Thirteen foundational declarations of belief.")
    )

    fun getBooksForVolume(volumeId: String): List<ScriptureBook> {
        return when (volumeId) {
            "bofm" -> bookOfMormonBooks
            "nt" -> newTestamentBooks
            "ot" -> oldTestamentBooks
            "dc" -> doctrineAndCovenantsBooks
            "pgp" -> pearlOfGreatPriceBooks
            else -> emptyList()
        }
    }

    fun findVolume(volumeId: String): ScriptureVolume? =
        volumes.find { it.id == volumeId }

    fun findBook(volumeId: String, bookId: String): ScriptureBook? =
        getBooksForVolume(volumeId).find { it.id == bookId }

    fun findBookAnyVolume(bookId: String): ScriptureBook? {
        val allBooks = bookOfMormonBooks + newTestamentBooks + oldTestamentBooks + doctrineAndCovenantsBooks + pearlOfGreatPriceBooks
        return allBooks.find { it.id == bookId }
    }

    fun getChapter(
        volumeId: String,
        bookId: String,
        chapterNum: Int,
        loadedVerses: List<ContentVerse>? = null
    ): ContentChapter {
        val volume = findVolume(volumeId) ?: volumes.first()
        val book = findBook(volumeId, bookId) ?: getBooksForVolume(volumeId).firstOrNull() ?: bookOfMormonBooks.first()
        val validChapter = chapterNum.coerceIn(1, book.chapterCount)
        val canonicalId = "scripture/${volume.id}/${book.id}/$validChapter"

        val booksInVolume = getBooksForVolume(volume.id)
        val currentBookIndex = booksInVolume.indexOf(book)

        // Previous link
        val prevInfo = when {
            validChapter > 1 -> {
                val prevId = "scripture/${volume.id}/${book.id}/${validChapter - 1}"
                val prevLabel = "${book.shortTitle} ${validChapter - 1}"
                prevId to prevLabel
            }
            currentBookIndex > 0 -> {
                val prevBook = booksInVolume[currentBookIndex - 1]
                val prevId = "scripture/${volume.id}/${prevBook.id}/${prevBook.chapterCount}"
                val prevLabel = "${prevBook.shortTitle} ${prevBook.chapterCount}"
                prevId to prevLabel
            }
            else -> null
        }

        // Next link
        val nextInfo = when {
            validChapter < book.chapterCount -> {
                val nextId = "scripture/${volume.id}/${book.id}/${validChapter + 1}"
                val nextLabel = "${book.shortTitle} ${validChapter + 1}"
                nextId to nextLabel
            }
            currentBookIndex < booksInVolume.lastIndex -> {
                val nextBook = booksInVolume[currentBookIndex + 1]
                val nextId = "scripture/${volume.id}/${nextBook.id}/1"
                val nextLabel = "${nextBook.shortTitle} 1"
                nextId to nextLabel
            }
            else -> null
        }

        val chapterTitle = if (volume.id == "dc") "Section $validChapter" else "${book.shortTitle} $validChapter"
        val subtitle = "${book.title} • ${volume.shortTitle}"

        val verses = if (!loadedVerses.isNullOrEmpty()) {
            loadedVerses
        } else {
            curatedVerses[canonicalId] ?: generateVersesForChapter(book, validChapter)
        }

        return ContentChapter(
            id = canonicalId,
            type = ContentType.SCRIPTURE,
            title = chapterTitle,
            subtitle = subtitle,
            verses = verses,
            prevId = prevInfo?.first,
            nextId = nextInfo?.first,
            prevLabel = prevInfo?.second,
            nextLabel = nextInfo?.second,
        )
    }

    private fun generateVersesForChapter(book: ScriptureBook, chapter: Int): List<ContentVerse> {
        return listOf(
            ContentVerse(1, "The words of the holy scripture according to ${book.shortTitle}, recorded in chapter $chapter."),
            ContentVerse(2, "Hearken, O ye people, and give ear to the voice of the Lord your God, who is from eternity to eternity, the Great I AM."),
            ContentVerse(3, "Search these commandments diligently, for they are true and faithful, and the prophecies and promises which are in them shall all be fulfilled."),
            ContentVerse(4, "What I the Lord have spoken, I have spoken, and I excuse not myself; and though the heavens and the earth pass away, my word shall not pass away."),
            ContentVerse(5, "Wherefore, lift up your hearts and rejoice, and gird up your loins, and take upon you my whole armor, that ye may be able to withstand the evil day.")
        )
    }

    private val curatedVerses: Map<String, List<ContentVerse>> = mapOf(
        // Book of Mormon Key Chapters
        "scripture/bofm/1-ne/1" to listOf(
            ContentVerse(1, "I, Nephi, having been born of goodly parents, therefore I was taught somewhat in all the learning of my father; and having seen many afflictions in the course of my days, nevertheless, having been highly favored of the Lord in all my days; yea, having had a great knowledge of the goodness and the mysteries of God, therefore I make a record of my proceedings in my days."),
            ContentVerse(2, "Yea, I make a record in the language of my father, which consists of the learning of the Jews and the language of the Egyptians."),
            ContentVerse(3, "And I know that the record which I make is true; and I make it with mine own hand; and I make it according to my knowledge."),
            ContentVerse(4, "For it came to pass in the commencement of the first year of the reign of Zedekiah, king of Judah, my father, Lehi, having dwelt at Jerusalem in all his days; and there came many prophets, prophesying unto the people that they must repent, or the great city Jerusalem must be destroyed."),
            ContentVerse(5, "Wherefore it came to pass that my father, Lehi, as he went forth prayed unto the Lord, yea, even with all his heart, in behalf of his people."),
            ContentVerse(6, "And it came to pass as he prayed unto the Lord, there came a pillar of fire and dwelt upon a rock before him; and he saw and heard much; and because of the things which he saw and heard he did quake and tremble exceedingly."),
            ContentVerse(7, "And it came to pass that he returned to his own house at Jerusalem; and he cast himself upon his bed, being overcome with the Spirit and the things which he had seen."),
            ContentVerse(8, "And being thus overcome with the Spirit, he was carried away in a vision, even that he saw the heavens open, and he thought he saw God sitting upon his throne, surrounded with numberless concourses of angels in the attitude of singing and praising their God."),
            ContentVerse(9, "And it came to pass that he saw One descending out of the midst of heaven, and he beheld that his luster was above that of the sun at noon-day."),
            ContentVerse(10, "And he also saw twelve others following him, and their brightness did exceed that of the stars in the firmament."),
            ContentVerse(11, "And they came down and went forth upon the face of the earth; and the first came and stood before my father, and gave unto him a book, and bade him that he should read."),
            ContentVerse(12, "And it came to pass that as he read, he was filled with the Spirit of the Lord."),
            ContentVerse(13, "And he read, saying: Wo, wo, unto Jerusalem, for I have seen thine abominations! Yea, and many things did my father read concerning Jerusalem—that it should be destroyed, and the inhabitants thereof; many should perish by the sword, and many should be carried away captive into Babylon."),
            ContentVerse(14, "And it came to pass that when my father had read and seen many great and marvelous things, he did exclaim many things unto the Lord; such as: Great and marvelous are thy works, O Lord God Almighty! Thy throne is high in the heavens, and thy power, and goodness, and mercy are over all the inhabitants of the earth; and, because thou art merciful, thou wilt not suffer those who come unto thee that they shall perish!"),
            ContentVerse(15, "And after this manner was the language of my father in the praising of his God; for his soul did rejoice, and his whole heart was filled, because of the things which he had seen, yea, which the Lord had shown unto him.")
        ),
        "scripture/bofm/1-ne/3" to listOf(
            ContentVerse(1, "And it came to pass that I, Nephi, returned from speaking with the Lord, to the tent of my father."),
            ContentVerse(2, "And it came to pass that he spake unto me, saying: Behold I have dreamed a dream, in the which the Lord hath commanded me that thou and thy brethren shall return to Jerusalem."),
            ContentVerse(3, "For behold, Laban hath the record of the Jews and also a genealogy of my forefathers, and they are engraven upon plates of brass."),
            ContentVerse(4, "Wherefore, the Lord hath commanded me that thou and thy brothers should go unto the house of Laban, and seek the records, and bring them down hither into the wilderness."),
            ContentVerse(5, "And now, behold thy brothers murmur, saying it is a hard thing which I have required of them; but behold I have not required it of them, but it is a commandment of the Lord."),
            ContentVerse(6, "Therefore go, my son, and thou shalt be favored of the Lord, because thou hast not murmured."),
            ContentVerse(7, "And it came to pass that I, Nephi, said unto my father: I will go and do the things which the Lord hath commanded, for I know that the Lord giveth no commandments unto the children of men, save he shall prepare a way for them that they may accomplish the thing which he commandeth them.")
        ),
        "scripture/bofm/2-ne/2" to listOf(
            ContentVerse(25, "Adam fell that men might be; and men are, that they might have joy."),
            ContentVerse(26, "And the Messiah cometh in the fulness of time, that he may redeem the children of men from the fall. And because that they are redeemed from the fall they have become free forever, knowing good from evil; to act for themselves and not to be acted upon, save it be by the punishment of the law at the great and last day, according to the commandments which God hath given."),
            ContentVerse(27, "Wherefore, men are free according to the flesh; and all things are given them which are expedient unto man. And they are free to choose liberty and eternal life, through the great Mediator of all men, or to choose captivity and death, according to the captivity and power of the devil; for he seeketh that all men might be miserable like unto himself.")
        ),
        "scripture/bofm/2-ne/31" to listOf(
            ContentVerse(19, "And now, my beloved brethren, after ye have gotten into this strait and narrow path, I would ask if all is done? Behold, I say unto you, Nay; for ye have not come thus far save it were by the word of Christ with unshaken faith in him, relying wholly upon the merits of him who is mighty to save."),
            ContentVerse(20, "Wherefore, ye must press forward with a steadfastness in Christ, having a perfect brightness of hope, and a love of God and of all men. Wherefore, if ye shall press forward, feasting upon the word of Christ, and endure to the end, behold, thus saith the Father: Ye shall have eternal life.")
        ),
        "scripture/bofm/alma/32" to listOf(
            ContentVerse(21, "And now as I said concerning faith—faith is not to have a perfect knowledge of things; therefore if ye have faith ye hope for things which are not seen, which are true."),
            ContentVerse(28, "Now, we will compare the word unto a seed. Now, if ye give place, that a seed may be planted in your heart, behold, if it be a true seed, or a good seed, if ye do not cast it out by your unbelief, that ye will resist the Spirit of the Lord, behold, it will begin to swell within your breasts; and when you feel these swelling motions, ye will begin to say within yourselves—It must needs be that this is a good seed, or that the word is good, for it beginneth to enlarge my soul; yea, it beginneth to enlighten my understanding, yea, it beginneth to be delicious to me.")
        ),
        "scripture/bofm/3-ne/11" to listOf(
            ContentVerse(8, "And it came to pass, as they understood they cast their eyes up again towards heaven; and behold, they saw a Man descending out of heaven; and he was clothed in a white robe; and he came down and stood in the midst of them; and the eyes of the whole multitude were turned upon him."),
            ContentVerse(9, "And it came to pass that he stretched forth his hand and spake unto the people, saying:"),
            ContentVerse(10, "Behold, I am Jesus Christ, whom the prophets testified shall come into the world."),
            ContentVerse(11, "And behold, I am the light and the life of the world; and I have drunk out of that bitter cup which the Father hath given me, and have glorified the Father in taking upon me the sins of the world.")
        ),
        "scripture/bofm/moroni/10" to listOf(
            ContentVerse(3, "Behold, I would exhort you that when ye shall read these things, if it be wisdom in God that ye should read them, that ye would remember how merciful the Lord hath been unto the children of men, from the creation of Adam even down until the time that ye shall receive these things, and ponder it in your hearts."),
            ContentVerse(4, "And when ye shall receive these things, I would exhort you that ye would ask God, the Eternal Father, in the name of Christ, if these things are not true; and if ye shall ask with a sincere heart, with real intent, having faith in Christ, he will manifest the truth of it unto you, by the power of the Holy Ghost."),
            ContentVerse(5, "And by the power of the Holy Ghost ye may know the truth of all things.")
        ),

        // New Testament Key Chapters
        "scripture/nt/matt/5" to listOf(
            ContentVerse(1, "And seeing the multitudes, he went up into a mountain: and when he was set, his disciples came unto him:"),
            ContentVerse(2, "And he opened his mouth, and taught them, saying,"),
            ContentVerse(3, "Blessed are the poor in spirit: for theirs is the kingdom of heaven."),
            ContentVerse(4, "Blessed are they that mourn: for they shall be comforted."),
            ContentVerse(5, "Blessed are the meek: for they shall inherit the earth."),
            ContentVerse(6, "Blessed are they which do hunger and thirst after righteousness: for they shall be filled."),
            ContentVerse(7, "Blessed are the merciful: for they shall obtain mercy."),
            ContentVerse(8, "Blessed are the pure in heart: for they shall see God."),
            ContentVerse(9, "Blessed are the peacemakers: for they shall be called the children of God."),
            ContentVerse(14, "Ye are the light of the world. A city that is set on an hill cannot be hid."),
            ContentVerse(16, "Let your light so shine before men, that they may see your good works, and glorify your Father which is in heaven.")
        ),
        "scripture/nt/john/3" to listOf(
            ContentVerse(16, "For God so loved the world, that he gave his only begotten Son, that whosoever believeth in him should not perish, but have everlasting life."),
            ContentVerse(17, "For God sent not his Son into the world to condemn the world; but that the world through him might be saved.")
        ),
        "scripture/nt/1-cor/13" to listOf(
            ContentVerse(4, "Charity suffereth long, and is kind; charity envieth not; charity vaunteth not itself, is not puffed up,"),
            ContentVerse(8, "Charity never faileth: but whether there be prophecies, they shall fail; whether there be tongues, they shall cease; whether there be knowledge, it shall vanish away."),
            ContentVerse(13, "And now abideth faith, hope, charity, these three; but the greatest of these is charity.")
        ),

        // Old Testament Key Chapters
        "scripture/ot/gen/1" to listOf(
            ContentVerse(1, "In the beginning God created the heaven and the earth."),
            ContentVerse(2, "And the earth was without form, and void; and darkness was upon the face of the deep. And the Spirit of God moved upon the face of the waters."),
            ContentVerse(3, "And God said, Let there be light: and there was light."),
            ContentVerse(27, "So God created man in his own image, in the image of God created he him; male and female created he them.")
        ),
        "scripture/ot/ps/23" to listOf(
            ContentVerse(1, "The Lord is my shepherd; I shall not want."),
            ContentVerse(2, "He maketh me to lie down in green pastures: he leadeth me beside the still waters."),
            ContentVerse(3, "He restoreth my soul: he leadeth me in the paths of righteousness for his name's sake."),
            ContentVerse(4, "Yea, though I walk through the valley of the shadow of death, I will fear no evil: for thou art with me; thy rod and thy staff they comfort me."),
            ContentVerse(6, "Surely goodness and mercy shall follow me all the days of my life: and I will dwell in the house of the Lord for ever.")
        ),

        // Doctrine and Covenants Key Sections
        "scripture/dc/sections/4" to listOf(
            ContentVerse(1, "Now behold, a marvelous work is about to come forth among the children of men."),
            ContentVerse(2, "Therefore, O ye that embark in the service of God, see that ye serve him with all your heart, might, mind and strength, that ye may stand blameless before God at the last day."),
            ContentVerse(5, "And faith, hope, charity and love, with an eye single to the glory of God, qualify him for the work."),
            ContentVerse(6, "Remember faith, virtue, knowledge, temperance, patience, brotherly kindness, godliness, charity, humility, diligence.")
        ),
        "scripture/dc/sections/76" to listOf(
            ContentVerse(22, "And now, after the many testimonies which have been given of him, this is the testimony, last of all, which we give of him: That he lives!"),
            ContentVerse(23, "For we saw him, even on the right hand of God; and we heard the voice bearing record that he is the Only Begotten of the Father—"),
            ContentVerse(24, "That by him, and through him, and of him, the worlds are and were created, and the inhabitants thereof are begotten sons and daughters unto God.")
        ),

        // Pearl of Great Price Key Chapters
        "scripture/pgp/moses/1" to listOf(
            ContentVerse(39, "For behold, this is my work and my glory—to bring to pass the immortality and eternal life of man.")
        ),
        "scripture/pgp/a-of-f/1" to listOf(
            ContentVerse(1, "We believe in God, the Eternal Father, and in His Son, Jesus Christ, and in the Holy Ghost."),
            ContentVerse(2, "We believe that men will be punished for their own sins, and not for Adam's transgression."),
            ContentVerse(3, "We believe that through the Atonement of Christ, all mankind may be saved, by obedience to the laws and ordinances of the Gospel."),
            ContentVerse(4, "We believe that the first principles and ordinances of the Gospel are: first, Faith in the Lord Jesus Christ; second, Repentance; third, Baptism by immersion for the remission of sins; fourth, Laying on of hands for the gift of the Holy Ghost."),
            ContentVerse(13, "We believe in being honest, true, chaste, benevolent, virtuous, and in doing good to all men; indeed, we may say that we follow the admonition of Paul—We believe all things, we hope all things, we have endured many things, and hope to be able to endure all things. If there is anything virtuous, lovely, or of good report or praiseworthy, we seek after these things.")
        )
    )
}
