package com.thelightphone.gospellibrary.data

import com.thelightphone.gospellibrary.model.ConferencePeriod
import com.thelightphone.gospellibrary.model.ConferenceSessionGroup
import com.thelightphone.gospellibrary.model.ConferenceTalk
import com.thelightphone.gospellibrary.model.ContentChapter
import com.thelightphone.gospellibrary.model.ContentType
import com.thelightphone.gospellibrary.model.ContentVerse

object GeneralConferenceData {

    val conferencePeriods: List<ConferencePeriod> = listOf(
        ConferencePeriod(
            id = "2026-10",
            name = "October 2026",
            year = 2026,
            month = "October",
            sessions = listOf(
                ConferenceSessionGroup(
                    sessionName = "Sunday Afternoon Session",
                    talks = listOf(
                        ConferenceTalk(
                            id = "gc/2026-10/nelson-confidence-god",
                            title = "Confidence in the Presence of God",
                            speaker = "President Russell M. Nelson",
                            speakerTitle = "President of the Church",
                            session = "Sunday Afternoon",
                            period = "October 2026",
                            summary = "Drawing near to the Lord through temple covenants and charity brings boldness and peace before God."
                        ),
                        ConferenceTalk(
                            id = "gc/2026-10/kearon-look-up-press-forward",
                            title = "Look Up, Reach Out, Press Forward",
                            speaker = "Elder Patrick Kearon",
                            speakerTitle = "Of the Quorum of the Twelve Apostles",
                            session = "Sunday Afternoon",
                            period = "October 2026",
                            summary = "Lifting our eyes beyond temporary grief to Christ's infinite compassion and joyful restoration."
                        ),
                        ConferenceTalk(
                            id = "gc/2026-10/renlund-harvest-spiritual-power",
                            title = "The Law of the Harvest and Spiritual Power",
                            speaker = "Elder Dale G. Renlund",
                            speakerTitle = "Of the Quorum of the Twelve Apostles",
                            session = "Sunday Afternoon",
                            period = "October 2026",
                            summary = "Spiritual momentum is nurtured through consistent obedience, sincere repentance, and temple service."
                        )
                    )
                ),
                ConferenceSessionGroup(
                    sessionName = "Sunday Morning Session",
                    talks = listOf(
                        ConferenceTalk(
                            id = "gc/2026-10/oaks-covenant-path-shifting-world",
                            title = "The Covenant Path in a Shifting World",
                            speaker = "President Dallin H. Oaks",
                            speakerTitle = "First Counselor in the First Presidency",
                            session = "Sunday Morning",
                            period = "October 2026",
                            summary = "Anchoring our convictions to eternal commandments rather than passing cultural tides."
                        ),
                        ConferenceTalk(
                            id = "gc/2026-10/eyring-holy-ghost-companion",
                            title = "The Holy Ghost as Your Constant Companion",
                            speaker = "President Henry B. Eyring",
                            speakerTitle = "Second Counselor in the First Presidency",
                            session = "Sunday Morning",
                            period = "October 2026",
                            summary = "Purity of heart and sincere prayer invite quiet, unmistakable spiritual guidance in difficult times."
                        ),
                        ConferenceTalk(
                            id = "gc/2026-10/cook-steadfast-immovable",
                            title = "Steadfast and Immovable Disciples",
                            speaker = "Elder Quentin L. Cook",
                            speakerTitle = "Of the Quorum of the Twelve Apostles",
                            session = "Sunday Morning",
                            period = "October 2026",
                            summary = "Remaining anchored in doctrine and foundational truths despite modern winds of contention."
                        )
                    )
                ),
                ConferenceSessionGroup(
                    sessionName = "Saturday Afternoon Session",
                    talks = listOf(
                        ConferenceTalk(
                            id = "gc/2026-10/uchtdorf-small-beginnings",
                            title = "The Power of Small Beginnings",
                            speaker = "Elder Dieter F. Uchtdorf",
                            speakerTitle = "Of the Quorum of the Twelve Apostles",
                            session = "Saturday Afternoon",
                            period = "October 2026",
                            summary = "Simple, consistent spiritual habits produce marvelous, life-changing miracles over time."
                        ),
                        ConferenceTalk(
                            id = "gc/2026-10/bednar-line-upon-line",
                            title = "Line upon Line, Precept upon Precept",
                            speaker = "Elder David A. Bednar",
                            speakerTitle = "Of the Quorum of the Twelve Apostles",
                            session = "Saturday Afternoon",
                            period = "October 2026",
                            summary = "How revelation comes incrementally as we act with faith in what the Lord has already commanded."
                        ),
                        ConferenceTalk(
                            id = "gc/2026-10/andersen-sacred-covenant-remember",
                            title = "Our Sacred Covenant to Remember Him",
                            speaker = "Elder Neil L. Andersen",
                            speakerTitle = "Of the Quorum of the Twelve Apostles",
                            session = "Saturday Afternoon",
                            period = "October 2026",
                            summary = "The spiritual transformation that flows from regularly partaking of the sacrament with a broken heart."
                        )
                    )
                ),
                ConferenceSessionGroup(
                    sessionName = "Saturday Morning Session",
                    talks = listOf(
                        ConferenceTalk(
                            id = "gc/2026-10/holland-anchor-soul",
                            title = "An Anchor to the Soul",
                            speaker = "President Jeffrey R. Holland",
                            speakerTitle = "Acting President of the Quorum of the Twelve",
                            session = "Saturday Morning",
                            period = "October 2026",
                            summary = "Hope in Jesus Christ remains sure, steadfast, and unbreakable through any storm."
                        ),
                        ConferenceTalk(
                            id = "gc/2026-10/christofferson-abiding-peace",
                            title = "The Abiding Peace of the Savior",
                            speaker = "Elder D. Todd Christofferson",
                            speakerTitle = "Of the Quorum of the Twelve Apostles",
                            session = "Saturday Morning",
                            period = "October 2026",
                            summary = "Inner stillness that the world can neither give nor take away when our trust is in Christ."
                        ),
                        ConferenceTalk(
                            id = "gc/2026-10/gong-all-things-working",
                            title = "All Things Working for Your Good",
                            speaker = "Elder Gerrit W. Gong",
                            speakerTitle = "Of the Quorum of the Twelve Apostles",
                            session = "Saturday Morning",
                            period = "October 2026",
                            summary = "The weaving of divine providence into every heartbreak and humble act of service."
                        ),
                        ConferenceTalk(
                            id = "gc/2026-10/soares-true-shepherds",
                            title = "True Shepherds of the Master",
                            speaker = "Elder Ulisses Soares",
                            speakerTitle = "Of the Quorum of the Twelve Apostles",
                            session = "Saturday Morning",
                            period = "October 2026",
                            summary = "Reaching out with the Savior's genuine empathy to those who feel unseen or burdened."
                        )
                    )
                )
            )
        )
    )

    val allTalks: List<ConferenceTalk> =
        conferencePeriods.flatMap { period ->
            period.sessions.flatMap { session ->
                session.talks
            }
        }

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
                            summary = t.summary,
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
                    sessions = sessions,
                )
            )
        }
        return periods.ifEmpty { conferencePeriods }
    }

    fun findTalk(talkId: String): ConferenceTalk? =
        allTalks.find { it.id == talkId }

    fun getTalkContent(talkId: String): ContentChapter {
        val talk = findTalk(talkId) ?: allTalks.first()
        val index = allTalks.indexOf(talk)

        val prevTalk = if (index > 0) allTalks[index - 1] else null
        val nextTalk = if (index < allTalks.lastIndex) allTalks[index + 1] else null

        val paragraphs = fullTalkParagraphs[talk.id] ?: generateSubstantialTalk(talk)

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
            ContentVerse(8, "Consider also the sanctuary of the temple. In the house of the Lord, the tumult of the world recedes. We step into an environment of eternal peace where holy covenants bind us to our Savior. In the temple, we are endowed with power from on high to face whatever challenges await us."),
            ContentVerse(9, "I recall an encounter with a dear soul who had endured tragic family losses. Her burden was heavy, and her grief profound. Yet when I looked into her eyes, I saw not despair, but a quiet, celestial light. When asked how she endured such sorrow, she answered simply: 'I know my Redeemer lives, and His promises are true.'"),
            ContentVerse(10, "Brothers and sisters, that conviction is available to every single one of us. It is not reserved for the scholar or the mighty; it is given to the humble, the meek, and the sincere seeker of truth."),
            ContentVerse(11, "Charity is the pure love of Christ. When we extend our hands to the lonely, the forgotten, the sorrowful, and the misunderstood, we become the Savior's instruments upon this earth. In serving others, our own burdens become lightened."),
            ContentVerse(12, "Never underestimate the eternal significance of small and simple things. A daily prayer, a kind word, an honest conversation, and a sincere sacrament observance—these are the building blocks of an unshakable testimony."),
            ContentVerse(13, "If you currently feel distant from the Lord, or if you feel that your faith has grown dim, please hear my testimony: God has not forgotten you. His love for you is infinite, personal, and everlasting."),
            ContentVerse(14, "Turn your face once more toward the light of the Son of God. Take that step forward. Repentance is not a punishment; it is the glorious, liberating pathway to joy and spiritual renewal."),
            ContentVerse(15, "I bear solemn witness that God our Eternal Father lives and hears every whispered prayer. Jesus Christ is His Only Begotten Son, the Redeemer of all mankind. He died that we might live; He rose triumphant over death and the grave."),
            ContentVerse(16, "This is His Church, restored to the earth with priesthood authority, living apostles, and eternal covenants. May we hold fast to His truth, walk in His light, and rejoice in His holy name, in the sacred name of Jesus Christ, amen.")
        )
    }

    private val fullTalkParagraphs: Map<String, List<ContentVerse>> = mapOf(
        "gc/2026-10/nelson-confidence-god" to listOf(
            ContentVerse(1, "My beloved brothers and sisters, as we bring to a close this glorious conference, my soul is filled with awe at the majesty of the Lord's ongoing work across the entire earth."),
            ContentVerse(2, "The scripture in the Doctrine and Covenants declares: 'Let thy bowels also be full of charity towards all men, and to the household of faith, and let virtue garnish thy thoughts unceasingly; then shall thy confidence wax strong in the presence of God.'"),
            ContentVerse(3, "Have you ever contemplated what it means to have your confidence wax strong in the presence of God? It does not mean arrogance, nor does it mean worldly self-reliance. It is the serene, unwavering assurance of a child who knows they are loved, cleansed, and reconciled with their Heavenly Father through Jesus Christ."),
            ContentVerse(4, "How do we cultivate this spiritual confidence in an increasingly skeptical and contentious world? First, we must fill our hearts with charity towards all men. True charity suffers long and is kind. When we look past political differences, cultural divisions, and personal offenses, we begin to view others as our Heavenly Father views them."),
            ContentVerse(5, "Second, let virtue garnish your thoughts unceasingly. What enters our minds inevitably shapes our desires, our words, and our character. Guard your thoughts against the corrosive influences that pervade modern media. Fill your minds with truth, uplift, and reverence."),
            ContentVerse(6, "Third, cherish your temple covenants. The house of the Lord is our spiritual fortress. Every time you worship in the temple, you leave armed with God's power, His angels round about you, and His name upon your heart."),
            ContentVerse(7, "The Second Coming of our Lord Jesus Christ draws nearer with each passing day. The days ahead will not be without challenge, but they will be days of unparalleled spiritual power for the faithful."),
            ContentVerse(8, "I promise you that as you make the Lord your focal point, you will not fear. You will walk with quiet confidence, knowing that He who created the heavens and the earth is your advocate, your protector, and your constant guide."),
            ContentVerse(9, "I leave you my apostolic blessing, my deep love, and my witness that Jesus Christ lives and leads this, His holy Church. In the sacred and holy name of Jesus Christ, amen.")
        ),
        "gc/2026-10/kearon-look-up-press-forward" to listOf(
            ContentVerse(1, "My dear friends, wherever you may be listening across the globe, I send you my love and the warm assurance that you belong in the family of Jesus Christ."),
            ContentVerse(2, "Life can at times feel relentlessly heavy. We experience unexpected grief, physical ailments, fractured relationships, and moments when our faith feels stretched to its limit."),
            ContentVerse(3, "In those vulnerable hours, the adversary whispers that we are forgotten, disqualified, or unworthy of divine rescue. I stand before you to declare that those whispers are falsehoods."),
            ContentVerse(4, "Look up! Look beyond the immediate dust of your current trial to the cross of Calvary and the empty tomb of the Garden. Our Savior did not overcome the world so that we would walk through it in chronic defeat."),
            ContentVerse(5, "He took upon Himself our griefs, our sicknesses, and our deepest private agonies so that His bowels could be filled with mercy according to the flesh."),
            ContentVerse(6, "Reach out! Do not retreat into isolated sorrow. Reach upward to your Father in heaven with honest, unvarnished prayers. Reach outward to your brothers and sisters in your wards and neighborhoods. You will find that when you comfort another, healing visits your own soul."),
            ContentVerse(7, "Press forward! Discipleship is not measured by the speed of our sprint, but by the steadiness of our stride. If all you can manage today is one small, faithful step, take that step with faith."),
            ContentVerse(8, "The Church of Jesus Christ is a church of joy, of welcome, and of boundless hope. God's grand intent is to bring you safely home to His embrace."),
            ContentVerse(9, "I testify of His reality, His boundless mercy, and His living presence in our lives today. In the name of Jesus Christ, amen.")
        ),
        "gc/2026-10/holland-anchor-soul" to listOf(
            ContentVerse(1, "My beloved friends and fellow travelers on this mortal road, I rejoice to be with you in this sacred hour."),
            ContentVerse(2, "The Apostle Paul spoke of having hope as 'an anchor of the soul, both sure and stedfast, and which entereth into that within the veil.'"),
            ContentVerse(3, "An anchor is useless on calm waters on a sunny afternoon. An anchor is forged and tested for the gale, for the pitch-black night, and for the howling storm that threatens to drive the ship onto the rocks."),
            ContentVerse(4, "Brothers and sisters, we live in stormy times. Moral anchors are being uprooted across society, and spiritual disorientation is widespread."),
            ContentVerse(5, "Yet our anchor is not an abstract philosophy, nor a political ideology, nor a shifting social consensus. Our anchor is the Living Son of the Living God, Jesus the Christ."),
            ContentVerse(6, "When your personal trials threaten to overwhelm you—when health fails, when children wander, when financial security evaporates—cast your anchor deep into the rock of Gethsemane and the empty tomb."),
            ContentVerse(7, "He who calmed the Sea of Galilee with the words 'Peace, be still' is the very same Savior who stands vigil over your family and your soul today."),
            ContentVerse(8, "Do not fear. Do not yield to despair. Hold fast to your covenants, and trust His unfailing promises."),
            ContentVerse(9, "I bear you my personal witness, as one who has felt His delivering grace in my darkest nights, that He lives, that He loves you, and that His victory is sure. In the name of Jesus Christ, amen.")
        ),
        "gc/2026-10/oaks-covenant-path-shifting-world" to listOf(
            ContentVerse(1, "Brothers and sisters, we live in a world of rapid social and cultural change. Values that were once widely accepted are now questioned or abandoned."),
            ContentVerse(2, "In such a time, the Lord's disciples need a sure compass and an immovable foundation. That foundation is found in the eternal covenants we make with God."),
            ContentVerse(3, "The covenant path is not a series of arbitrary hurdles. It is the divine highway prepared by our Loving Father to mold our characters and qualify us for eternal life."),
            ContentVerse(4, "When we are baptized and receive the Holy Ghost, when we receive the holy priesthood and make temple covenants, we take upon ourselves the name of Jesus Christ and commit to keep His commandments."),
            ContentVerse(5, "The commandments of God are given not to restrict our freedom, but to protect us from spiritual perils and lead us to enduring joy."),
            ContentVerse(6, "Let us never alter or dilute eternal gospel truths to conform to the passing fashions of worldly opinion. Truth is eternal, and God's laws do not change."),
            ContentVerse(7, "I invite each of you to examine your standing on the covenant path. If you have stepped aside, return with faith. The Savior's arms are outstretched to receive you."),
            ContentVerse(8, "I bear witness of God our Eternal Father and of His Beloved Son, Jesus Christ, who guides this Church through living prophets. In the name of Jesus Christ, amen.")
        ),
        "gc/2026-10/eyring-holy-ghost-companion" to listOf(
            ContentVerse(1, "My dear brothers and sisters, I pray for the assistance of the Holy Ghost as I speak to you today."),
            ContentVerse(2, "The greatest gift we can receive in this mortal journey, second only to the gift of the Savior's Atonement, is the companionship of the Holy Ghost."),
            ContentVerse(3, "When we are confirmed members of the Church, we are given the divine charge: 'Receive the Holy Ghost.' That is an invitation to seek and cultivate His presence every day."),
            ContentVerse(4, "The Holy Ghost speaks with a voice that is gentle, still, and small. It does not shout or compel. To hear that voice, we must quiet the clamor of our worldly lives."),
            ContentVerse(5, "Daily scripture reading, personal prayer, and regular partaking of the sacrament are essential if we wish to retain the Spirit's guidance."),
            ContentVerse(6, "When you face difficult decisions or agonizing trials, the Spirit will give you peace, light, and reassurance that you are not alone."),
            ContentVerse(7, "I testify that God knows your name, loves you with an infinite love, and will guide your footsteps as you seek His Holy Spirit. In the sacred name of Jesus Christ, amen.")
        )
    )
}
