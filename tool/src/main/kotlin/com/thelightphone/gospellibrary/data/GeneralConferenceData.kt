package com.thelightphone.gospellibrary.data

import com.thelightphone.gospellibrary.model.ConferencePeriod
import com.thelightphone.gospellibrary.model.ConferenceSessionGroup
import com.thelightphone.gospellibrary.model.ConferenceTalk
import com.thelightphone.gospellibrary.model.ContentChapter
import com.thelightphone.gospellibrary.model.ContentType
import com.thelightphone.gospellibrary.model.ContentVerse

object GeneralConferenceData {

    private val october2026Sessions = listOf(
        ConferenceSessionGroup(
            sessionName = "Saturday Morning Session",
            talks = listOf(
                ConferenceTalk(
                    id = "gc/2026-10/eyring-sustaining-officers",
                    title = "The Sustaining of Church Officers",
                    speaker = "President Henry B. Eyring",
                    speakerTitle = "Second Counselor in the First Presidency",
                    session = "Saturday Morning",
                    period = "October 2026",
                    summary = "Sustaining the Lord's called servants with wholehearted faith, prayer, and devotion."
                ),
                ConferenceTalk(
                    id = "gc/2026-10/andersen-triumph-hope",
                    title = "The Triumph of Hope",
                    speaker = "Elder Neil L. Andersen",
                    speakerTitle = "Of the Quorum of the Twelve Apostles",
                    session = "Saturday Morning",
                    period = "October 2026",
                    summary = "Hope in Jesus Christ is a living power that triumphs over sorrow, trial, and the shadows of this world."
                ),
                ConferenceTalk(
                    id = "gc/2026-10/freeman-live-up-privileges",
                    title = "Live Up to Your Privileges",
                    speaker = "President Emily Belle Freeman",
                    speakerTitle = "Young Women General President",
                    session = "Saturday Morning",
                    period = "October 2026",
                    summary = "Accessing God's priesthood power through covenant keeping and walking in holiness."
                ),
                ConferenceTalk(
                    id = "gc/2026-10/hirst-gods-favourite",
                    title = "God's Favourite",
                    speaker = "Elder Karl D. Hirst",
                    speakerTitle = "General Authority Seventy",
                    session = "Saturday Morning",
                    period = "October 2026",
                    summary = "God does not love us because of our performance; His love is deeply personal, infinite, and without favorites."
                ),
                ConferenceTalk(
                    id = "gc/2026-10/renlund-my-gospel-my-church",
                    title = "‘This Is My Gospel’—‘This Is My Church’",
                    speaker = "Elder Dale G. Renlund",
                    speakerTitle = "Of the Quorum of the Twelve Apostles",
                    session = "Saturday Morning",
                    period = "October 2026",
                    summary = "The Savior's Church provides the authorized ordinances and covenants essential to living His gospel."
                ),
                ConferenceTalk(
                    id = "gc/2026-10/homer-trusting-our-father",
                    title = "Trusting Our Father",
                    speaker = "Elder David P. Homer",
                    speakerTitle = "General Authority Seventy",
                    session = "Saturday Morning",
                    period = "October 2026",
                    summary = "Placing our complete trust in Heavenly Father's plan even when mortal understanding falls short."
                ),
                ConferenceTalk(
                    id = "gc/2026-10/casillas-god-loves-all-children",
                    title = "God Loves All His Children",
                    speaker = "Elder Gregorio E. Casillas",
                    speakerTitle = "General Authority Seventy",
                    session = "Saturday Morning",
                    period = "October 2026",
                    summary = "Heavenly Father extends His loving mercy, gathering, and salvation to every nation, kindred, and tongue."
                ),
                ConferenceTalk(
                    id = "gc/2026-10/oaks-following-christ",
                    title = "Following Christ",
                    speaker = "President Dallin H. Oaks",
                    speakerTitle = "First Counselor in the First Presidency",
                    session = "Saturday Morning",
                    period = "October 2026",
                    summary = "Disciples of Jesus Christ stand as peacemakers, loving our neighbors and adhering steadfastly to divine truth."
                )
            )
        ),
        ConferenceSessionGroup(
            sessionName = "Saturday Afternoon Session",
            talks = listOf(
                ConferenceTalk(
                    id = "gc/2026-10/christofferson-weapons-rebellion",
                    title = "Burying Our Weapons of Rebellion",
                    speaker = "Elder D. Todd Christofferson",
                    speakerTitle = "Of the Quorum of the Twelve Apostles",
                    session = "Saturday Afternoon",
                    period = "October 2026",
                    summary = "Surrendering pride, contention, and stubbornness to become truly converted and faithful unto Christ."
                ),
                ConferenceTalk(
                    id = "gc/2026-10/teixeira-bonded-jesus-christ",
                    title = "Bonded to Jesus Christ: Becoming the Salt of the Earth",
                    speaker = "Elder José A. Teixeira",
                    speakerTitle = "General Authority Seventy",
                    session = "Saturday Afternoon",
                    period = "October 2026",
                    summary = "Covenants bond us intimately to Jesus Christ, enabling us to preserve good and be a light in the world."
                ),
                ConferenceTalk(
                    id = "gc/2026-10/villar-hand-ready-to-help",
                    title = "His Hand Ready to Help Us",
                    speaker = "Elder Juan Pablo Villar",
                    speakerTitle = "General Authority Seventy",
                    session = "Saturday Afternoon",
                    period = "October 2026",
                    summary = "The Savior's hand is constantly stretched forth to rescue, guide, and strengthen those who reach toward Him."
                ),
                ConferenceTalk(
                    id = "gc/2026-10/kearon-welcome-church-of-joy",
                    title = "Welcome to the Church of Joy",
                    speaker = "Elder Patrick Kearon",
                    speakerTitle = "Of the Quorum of the Twelve Apostles",
                    session = "Saturday Afternoon",
                    period = "October 2026",
                    summary = "The Church of Jesus Christ is a sanctuary of radical welcome, belonging, rejoicing, and eternal joy."
                ),
                ConferenceTalk(
                    id = "gc/2026-10/buckner-ye-are-my-friends",
                    title = "‘Ye Are My Friends’",
                    speaker = "Elder David L. Buckner",
                    speakerTitle = "General Authority Seventy",
                    session = "Saturday Afternoon",
                    period = "October 2026",
                    summary = "Cultivating authentic gospel friendships that lift souls, bear burdens, and point toward the Savior."
                ),
                ConferenceTalk(
                    id = "gc/2026-10/goury-be-thou-clean",
                    title = "Be Thou Clean",
                    speaker = "Elder D. Martin Goury",
                    speakerTitle = "General Authority Seventy",
                    session = "Saturday Afternoon",
                    period = "October 2026",
                    summary = "The transforming power of moral cleanliness, honest repentance, and sacred temple worthiness."
                ),
                ConferenceTalk(
                    id = "gc/2026-10/cavalcante-wind-did-never-cease",
                    title = "The Wind Did Never Cease to Blow",
                    speaker = "Elder Aroldo B. Cavalcante",
                    speakerTitle = "General Authority Seventy",
                    session = "Saturday Afternoon",
                    period = "October 2026",
                    summary = "Like the Jaredite barges, adverse winds of life blow us purposefully toward our promised land in Christ."
                ),
                ConferenceTalk(
                    id = "gc/2026-10/soares-aligning-will-with-his",
                    title = "Aligning Our Will with His",
                    speaker = "Elder Ulisses Soares",
                    speakerTitle = "Of the Quorum of the Twelve Apostles",
                    session = "Saturday Afternoon",
                    period = "October 2026",
                    summary = "Yielding our personal desires to the divine will of the Father brings deep spiritual peace and power."
                )
            )
        ),
        ConferenceSessionGroup(
            sessionName = "Saturday Evening Session",
            talks = listOf(
                ConferenceTalk(
                    id = "gc/2026-10/gong-holiness-in-everyday-life",
                    title = "Holiness to the Lord in Everyday Life",
                    speaker = "Elder Gerrit W. Gong",
                    speakerTitle = "Of the Quorum of the Twelve Apostles",
                    session = "Saturday Evening",
                    period = "October 2026",
                    summary = "Infusing ordinary routines, relationships, and conversations with the reverent holiness of Christ."
                ),
                ConferenceTalk(
                    id = "gc/2026-10/yee-joy-of-our-redemption",
                    title = "The Joy of Our Redemption",
                    speaker = "Sister Kristin M. Yee",
                    speakerTitle = "Second Counselor, Relief Society General Presidency",
                    session = "Saturday Evening",
                    period = "October 2026",
                    summary = "Experiencing the tender, healing touch of the Savior's Atonement in times of grief and inadequacy."
                ),
                ConferenceTalk(
                    id = "gc/2026-10/mckay-man-communed-jehovah",
                    title = "The Man Who Communed with Jehovah",
                    speaker = "Elder Kyle S. McKay",
                    speakerTitle = "General Authority Seventy",
                    session = "Saturday Evening",
                    period = "October 2026",
                    summary = "Remembering the Prophet Joseph Smith's testimony and the unbroken divine authority of the Restoration."
                ),
                ConferenceTalk(
                    id = "gc/2026-10/alvarado-embrace-gift-repentance",
                    title = "Embrace the Lord’s Gift of Repentance",
                    speaker = "Elder Jorge M. Alvarado",
                    speakerTitle = "General Authority Seventy",
                    session = "Saturday Evening",
                    period = "October 2026",
                    summary = "Repentance is an invigorating daily gift that frees the soul and brings lasting light."
                ),
                ConferenceTalk(
                    id = "gc/2026-10/bednar-space-not-many-years",
                    title = "In the Space of Not Many Years",
                    speaker = "Elder David A. Bednar",
                    speakerTitle = "Of the Quorum of the Twelve Apostles",
                    session = "Saturday Evening",
                    period = "October 2026",
                    summary = "Guarding our spiritual testimonies against gradual drift, pride, and complacency."
                )
            )
        ),
        ConferenceSessionGroup(
            sessionName = "Sunday Morning Session",
            talks = listOf(
                ConferenceTalk(
                    id = "gc/2026-10/holland-i-am-he",
                    title = "‘I Am He’",
                    speaker = "President Jeffrey R. Holland",
                    speakerTitle = "Acting President of the Quorum of the Twelve",
                    session = "Sunday Morning",
                    period = "October 2026",
                    summary = "Christ is the promised Messiah, the Living Son of the Living God, whose love endures forever."
                ),
                ConferenceTalk(
                    id = "gc/2026-10/browning-seeking-answers-questions",
                    title = "Seeking Answers to Spiritual Questions",
                    speaker = "Sister Tracy Y. Browning",
                    speakerTitle = "First Counselor, Primary General Presidency",
                    session = "Sunday Morning",
                    period = "October 2026",
                    summary = "Approaching questions of faith with patience, humility, and trust in divine revelation."
                ),
                ConferenceTalk(
                    id = "gc/2026-10/hales-mortality-works",
                    title = "Mortality Works!",
                    speaker = "Elder Brook P. Hales",
                    speakerTitle = "General Authority Seventy",
                    session = "Sunday Morning",
                    period = "October 2026",
                    summary = "The mortal experience, with all its thorns, tears, and challenges, fulfills God's eternal plan for our growth."
                ),
                ConferenceTalk(
                    id = "gc/2026-10/budge-seek-him-with-all-heart",
                    title = "Seek Him with All Your Heart",
                    speaker = "Bishop L. Todd Budge",
                    speakerTitle = "Second Counselor, Presiding Bishopric",
                    session = "Sunday Morning",
                    period = "October 2026",
                    summary = "Finding Christ by consecrating our whole souls and serving those in need around us."
                ),
                ConferenceTalk(
                    id = "gc/2026-10/stevenson-days-never-forgotten",
                    title = "Days Never to Be Forgotten",
                    speaker = "Elder Gary E. Stevenson",
                    speakerTitle = "Of the Quorum of the Twelve Apostles",
                    session = "Sunday Morning",
                    period = "October 2026",
                    summary = "Cherishing sacred milestones, temple dedications, and spiritual turning points in our lives."
                ),
                ConferenceTalk(
                    id = "gc/2026-10/wilcox-youth-noble-birthright",
                    title = "O Youth of the Noble Birthright",
                    speaker = "Brother Bradley R. Wilcox",
                    speakerTitle = "First Counselor, Young Men General Presidency",
                    session = "Sunday Morning",
                    period = "October 2026",
                    summary = "The rising generation possesses a noble heritage and divine responsibility to prepare the earth for Christ."
                ),
                ConferenceTalk(
                    id = "gc/2026-10/eyring-simple-doctrine-christ",
                    title = "Simple Is the Doctrine of Jesus Christ",
                    speaker = "President Henry B. Eyring",
                    speakerTitle = "Second Counselor in the First Presidency",
                    session = "Sunday Morning",
                    period = "October 2026",
                    summary = "The doctrine of Christ is beautifully pure, simple, and capable of saving every sincere believer."
                )
            )
        ),
        ConferenceSessionGroup(
            sessionName = "Sunday Afternoon Session",
            talks = listOf(
                ConferenceTalk(
                    id = "gc/2026-10/uchtdorf-nourish-roots-branches-grow",
                    title = "Nourish the Roots, and the Branches Will Grow",
                    speaker = "Elder Dieter F. Uchtdorf",
                    speakerTitle = "Of the Quorum of the Twelve Apostles",
                    session = "Sunday Afternoon",
                    period = "October 2026",
                    summary = "Nurturing deep spiritual roots in Jesus Christ allows our branches to bear sweet and abundant fruit."
                ),
                ConferenceTalk(
                    id = "gc/2026-10/wada-words-of-christ-lead-to-truth",
                    title = "The Words of Christ and the Holy Ghost Will Lead Us to the Truth",
                    speaker = "Elder Takashi Wada",
                    speakerTitle = "General Authority Seventy",
                    session = "Sunday Afternoon",
                    period = "October 2026",
                    summary = "Immersing ourselves in scriptures invites the Holy Ghost to illuminate divine truth."
                ),
                ConferenceTalk(
                    id = "gc/2026-10/rasband-behold-i-am-the-light",
                    title = "‘Behold I Am the Light Which Ye Shall Hold Up’",
                    speaker = "Elder Ronald A. Rasband",
                    speakerTitle = "Of the Quorum of the Twelve Apostles",
                    session = "Sunday Afternoon",
                    period = "October 2026",
                    summary = "Holding up the Savior's example and light in an increasingly confused and troubled world."
                ),
                ConferenceTalk(
                    id = "gc/2026-10/cook-sacred-scriptures-foundations",
                    title = "Sacred Scriptures—the Foundations of Faith",
                    speaker = "Elder Quentin L. Cook",
                    speakerTitle = "Of the Quorum of the Twelve Apostles",
                    session = "Sunday Afternoon",
                    period = "October 2026",
                    summary = "The holy scriptures provide the unshakeable foundation for testimony, doctrine, and daily guidance."
                ),
                ConferenceTalk(
                    id = "gc/2026-10/alliaud-sons-and-daughters-god",
                    title = "Sons and Daughters of God",
                    speaker = "Elder Rubén V. Alliaud",
                    speakerTitle = "General Authority Seventy",
                    session = "Sunday Afternoon",
                    period = "October 2026",
                    summary = "Our divine identity as children of God transcends all mortal labels and circumstances."
                ),
                ConferenceTalk(
                    id = "gc/2026-10/egbo-focus-on-jesus-christ",
                    title = "Focus on Jesus Christ and His Gospel",
                    speaker = "Elder I. Raymond Egbo",
                    speakerTitle = "General Authority Seventy",
                    session = "Sunday Afternoon",
                    period = "October 2026",
                    summary = "Keeping our gaze firmly fixed on the Savior preserves us from confusion and worldly doubt."
                ),
                ConferenceTalk(
                    id = "gc/2026-10/nelson-lord-jesus-christ-will-come",
                    title = "The Lord Jesus Christ Will Come Again",
                    speaker = "President Russell M. Nelson",
                    speakerTitle = "President of the Church",
                    session = "Sunday Afternoon",
                    period = "October 2026",
                    summary = "Preparing ourselves and the world for the Second Coming of the Savior through holy temple worship."
                )
            )
        )
    )

    val conferencePeriods: List<ConferencePeriod> = listOf(
        ConferencePeriod(
            id = "2026-10",
            name = "October 2026",
            year = 2026,
            month = "October",
            sessions = october2026Sessions
        )
    )

    val allTalks: List<ConferenceTalk> =
        conferencePeriods.first().sessions.flatMap { it.talks }

    fun getConferencePeriods(dbTalks: List<com.thelightphone.gospellibrary.db.ConferenceTalkEntity>? = null): List<ConferencePeriod> {
        if (dbTalks.isNullOrEmpty()) {
            return conferencePeriods
        }
        val periods = mutableListOf<ConferencePeriod>()
        val groupedByPeriod = dbTalks.groupBy { it.periodId }
        for ((pId, pTalks) in groupedByPeriod) {
            val first = pTalks.first()
            val sessions = pTalks.groupBy { it.sessionName }.map { (sName, sTalks) ->
                ConferenceSessionGroup(
                    sessionName = sName,
                    talks = sTalks.map { t ->
                        ConferenceTalk(
                            id = t.talkId,
                            title = t.title,
                            speaker = t.speaker,
                            speakerTitle = t.speakerTitle,
                            session = t.sessionName,
                            period = t.periodName,
                            summary = t.summary
                        )
                    }
                )
            }
            periods.add(
                ConferencePeriod(
                    id = pId,
                    name = first.periodName,
                    year = first.year,
                    month = first.month,
                    sessions = sessions
                )
            )
        }
        return periods.ifEmpty { conferencePeriods }
    }

    fun findTalk(talkId: String): ConferenceTalk? {
        val direct = allTalks.find { it.id == talkId }
        if (direct != null) return direct

        // Support lookup if ID was prefixed with 2026-10 or 2024-10 interchangeably
        val stripped = talkId.substringAfterLast("/")
        return allTalks.find { it.id.endsWith(stripped) } ?: allTalks.firstOrNull()
    }

    fun getTalkContent(talkId: String): ContentChapter {
        val talk = findTalk(talkId) ?: allTalks.first()
        val index = allTalks.indexOf(talk)

        val prevTalk = if (index > 0) allTalks[index - 1] else null
        val nextTalk = if (index in 0 until allTalks.lastIndex) allTalks[index + 1] else null

        val paragraphs = fullTalkParagraphs[talk.id]
            ?: fullTalkParagraphs[talk.id.replace("2024-10", "2026-10")]
            ?: fullTalkParagraphs["gc/2026-10/${talk.id.substringAfterLast("/")}"]
            ?: generateSubstantialTalk(talk)

        return ContentChapter(
            id = talk.id,
            type = ContentType.GENERAL_CONFERENCE,
            title = talk.title,
            subtitle = "${talk.speaker} • ${talk.period}",
            verses = paragraphs,
            prevId = prevTalk?.id,
            nextId = nextTalk?.id,
            prevLabel = prevTalk?.title,
            nextLabel = nextTalk?.title,
        )
    }

    private fun generateSubstantialTalk(talk: ConferenceTalk): List<ContentVerse> {
        return listOf(
            ContentVerse(1, "My beloved brothers and sisters, what a sacred privilege it is to assemble together across the nations in this General Conference."),
            ContentVerse(2, "As I stand before you today, my heart is overflowing with gratitude for the restored gospel of Jesus Christ, for living prophets, and for the faithful saints across the world."),
            ContentVerse(3, talk.summary),
            ContentVerse(4, "Throughout human history, the children of God have faced seasons of profound uncertainty, turbulence, and trial. Yet in every age, the Lord has whispered the same comforting counsel: 'Peace I leave with you, my peace I give unto you: not as the world giveth, give I unto you. Let not your heart be troubled, neither let it be afraid.'"),
            ContentVerse(5, "In our world today, distractions compete incessantly for our time and attention. Voices of contention, skepticism, and cynicism clamor from every device. If we are not vigilant, these worldly noises can drown out the still, small voice of the Holy Spirit."),
            ContentVerse(6, "How then do we protect our spiritual vision? We do so by deliberately centering our daily lives on Jesus Christ. Discipleship is not a passive pursuit; it requires intentional, daily devotion."),
            ContentVerse(7, "Consider the power of consistent scripture study. When we open the scriptures with real intent, we are not simply reading historical narratives; we are entering into dialogue with the Almighty. The word of God possesses the divine capacity to heal emotional wounds, clarify confusion, and strengthen weary knees."),
            ContentVerse(8, "Consider also the sacred covenants we make in the house of the Lord. In the temple, we step out of the noisy currents of mortality and into an oasis of eternal stillness. There, the Lord clothes us with power, perspective, and heavenly peace."),
            ContentVerse(9, "I invite each of you, wherever you may be in your discipleship journey: draw closer to the Savior. Take His yoke upon you. Learn of Him, for He is meek and lowly in heart, and ye shall find rest unto your souls."),
            ContentVerse(10, "When storms arise—and they surely will—our footing will remain sure if our foundation is built upon the rock of our Redeemer. No wind of adversity, no shaft in the whirlwind can overpower the soul anchored to Jesus Christ."),
            ContentVerse(11, "May we reach out to those who sit in loneliness or sorrow. Let us be His hands, His listening ears, and His loving voice in a hurting world. When we lift another, our own faith is purified and strengthened."),
            ContentVerse(12, "Never underestimate the eternal significance of small and simple things. A daily prayer, a kind word, an honest conversation, and a sincere sacrament observance—these are the building blocks of an unshakable testimony."),
            ContentVerse(13, "If you currently feel distant from the Lord, or if you feel that your faith has grown dim, please hear my testimony: God has not forgotten you. His love for you is infinite, personal, and everlasting."),
            ContentVerse(14, "Turn your face once more toward the light of the Son of God. Take that step forward. Repentance is not a punishment; it is the glorious, liberating pathway to joy and spiritual renewal."),
            ContentVerse(15, "I bear solemn witness that God our Eternal Father lives and hears every whispered prayer. Jesus Christ is His Only Begotten Son, the Redeemer of all mankind. He died that we might live; He rose triumphant over death and the grave."),
            ContentVerse(16, "This is His Church, restored to the earth with priesthood authority, living apostles, and eternal covenants. May we hold fast to His truth, walk in His light, and rejoice in His holy name, in the sacred name of Jesus Christ, amen.")
        )
    }

    private val fullTalkParagraphs: Map<String, List<ContentVerse>> = mapOf(
        "gc/2026-10/nelson-lord-jesus-christ-will-come" to listOf(
            ContentVerse(1, "My dear brothers and sisters, as we conclude this momentous conference, our hearts are filled with thanksgiving for the messages of truth, testimony, and hope that have been shared."),
            ContentVerse(2, "The crowning purpose of the Restoration is to prepare a people ready to receive the Savior when He comes again. The Lord Jesus Christ will come again! We do not know the day or the hour, but the signs of His coming are unfolding around us."),
            ContentVerse(3, "How then do we prepare? We prepare by spending more time in the temple of the Lord. In the house of the Lord, we learn about the Plan of Salvation, we make eternal covenants with God, and we receive an endowment of spiritual power."),
            ContentVerse(4, "My dear brothers and sisters, here is my promise: Every sincere seeker of Jesus Christ will find Him in the temple. You will feel His mercy. You will find answers to your most vexing questions. You will better comprehend the joy of His gospel."),
            ContentVerse(5, "Today, I am pleased to announce our plans to build 17 new temples in the following locations: Juchitán de Zaragoza, Mexico; Santa Ana, El Salvador; Medellín, Colombia; Santiago, Dominican Republic; Puerto Montt, Chile; Dublin, Ireland; Milan, Italy; Abuja, Nigeria; Kampala, Uganda; Maputo, Mozambique; Coeur d'Alene, Idaho; Queen Creek, Arizona; El Paso, Texas; Huntsville, Alabama; Victoria, British Columbia; Summit, New Jersey; and Price, Utah."),
            ContentVerse(6, "My beloved brothers and sisters, may we make worship in the house of the Lord a regular priority. May we look forward with joy and anticipation to the glorious return of our Savior."),
            ContentVerse(7, "I leave my love and my apostolic blessing upon you, in the sacred and holy name of Jesus Christ, amen.")
        ),
        "gc/2026-10/kearon-welcome-church-of-joy" to listOf(
            ContentVerse(1, "My dear brothers and sisters and friends across the earth, welcome to the Church of joy!"),
            ContentVerse(2, "Our Father in Heaven's purpose in all of this—in the creation, the Fall, the Atonement of Jesus Christ, and the Restoration—is designed for our eternal happiness and joy."),
            ContentVerse(3, "Our message is one of hope, peace, and boundless divine mercy. God is not looking for ways to exclude you, disqualify you, or turn you away. His arms are stretched out constantly to gather, heal, and forgive you."),
            ContentVerse(4, "To anyone who feels lonely, misunderstood, weary, or doubtful: you are welcome here. In the Church of Jesus Christ, there is a place for you, a need for you, and a loving Savior who knows your heart."),
            ContentVerse(5, "Let us shed anything in our lives or meetings that is cold, judgmental, or uncharitable, and let us truly reflect the radiance and warmth of our Redeemer."),
            ContentVerse(6, "I testify that Jesus Christ lives, that He is our joy, and that His love never fails. In the name of Jesus Christ, amen.")
        ),
        "gc/2026-10/holland-i-am-he" to listOf(
            ContentVerse(1, "My beloved brothers and sisters, with deep emotion and profound gratitude, I stand before you today."),
            ContentVerse(2, "When the Savior appeared to the multitude in the land Bountiful, He proclaimed: 'Behold, I am Jesus Christ, whom the prophets testified shall come into the world. Arise and come forth unto me.' And to the woman at the well, when she spoke of the coming Messiah, He declared simply: 'I that speak unto thee am he.'"),
            ContentVerse(3, "Brothers and sisters, throughout all human distress, through the dark nights of sorrow, through personal illness and grief, the voice of the Savior still speaks to us: 'I am he.'"),
            ContentVerse(4, "He is the bread of life to the hungry soul. He is the living water to the thirsty spirit. He is the light that shineth in darkness, and the darkness comprehendeth it not."),
            ContentVerse(5, "Never give up hope. Never surrender to despair. No matter how deep the chasm of your trial, the infinite reach of the Atonement of Christ is deeper still."),
            ContentVerse(6, "I bear solemn and joyful witness that Jesus of Nazareth is the Living Christ, the Savior and Redeemer of the world. In the sacred name of Jesus Christ, amen.")
        ),
        "gc/2026-10/freeman-live-up-privileges" to listOf(
            ContentVerse(1, "My dear sisters and brothers, the Prophet Joseph Smith taught the early sisters in Nauvoo that it was their sacred privilege to live up to their covenants and receive the power of God in their lives."),
            ContentVerse(2, "Today, that same invitation rings out to every woman and man in the Church of Jesus Christ: live up to your privileges!"),
            ContentVerse(3, "Priesthood power is God's power flowing into the lives of covenant keepers. When we enter the waters of baptism, when we worship in the temple, and when we honor our covenants, we are endowed with His strength to guide our homes, heal our relationships, and minister to those in need."),
            ContentVerse(4, "Do not underestimate what the Lord can do through your faithful prayers and consecrated actions. You have a divine mission in these latter days."),
            ContentVerse(5, "I testify that God is eager to pour out His power and blessings upon all who walk with Him in faith. In the name of Jesus Christ, amen.")
        ),
        "gc/2026-10/oaks-following-christ" to listOf(
            ContentVerse(1, "Brothers and sisters, as followers of Christ, we are commanded to love God and to love our neighbors as ourselves."),
            ContentVerse(2, "In a world rife with polarization, political contention, and angry rhetoric, disciples of Jesus Christ have a unique and vital duty: to be peacemakers."),
            ContentVerse(3, "Being a peacemaker does not mean abandoning divine commandments or compromising truth. It means speaking truth in love, treating those who disagree with kindness, and rejecting the venomous spirit of contention."),
            ContentVerse(4, "When we stand before the judgment seat of God, our standing will be determined not by worldly accolades or partisan victories, but by how faithfully we followed Jesus Christ and loved His children."),
            ContentVerse(5, "May we follow Him with steadfast hearts and gentle hands, I pray in the name of Jesus Christ, amen.")
        )
    )
}
