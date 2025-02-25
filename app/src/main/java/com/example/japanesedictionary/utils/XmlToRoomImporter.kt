package com.example.japanesedictionary.utils

import android.content.Context
import android.util.Log
import androidx.room.withTransaction
import com.example.japanesedictionary.R
import com.example.japanesedictionary.data.DictionaryDatabase
import com.example.japanesedictionary.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xml.sax.Attributes
import org.xml.sax.InputSource
import org.xml.sax.helpers.DefaultHandler
import java.io.InputStream
import javax.xml.parsers.SAXParserFactory

object XmlToRoomImporter {
    private const val PHASE_PARSE_DICT = 0
    private const val PHASE_INSERT_DICT = 1
    private const val PHASE_PARSE_KANJI = 2
    private const val PHASE_INSERT_KANJI = 3

    suspend fun importData(
        context: Context,
        onProgress: (phase: Int, current: Int, total: Int) -> Unit
    ): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val db = DictionaryDatabase.getDatabase(context)
            val dao = db.dictionaryDao()

            if (dao.getCount() > 0) return@withContext Result.success(0)

            val inputStream = context.resources.openRawResource(R.raw.jmdict_e_examp)
            val parsedEntries = parseXML(inputStream) { current ->
                onProgress(PHASE_PARSE_DICT, current, 0)
            }
            onProgress(PHASE_PARSE_DICT, parsedEntries.size, parsedEntries.size)

            val batchSize = 5000
            parsedEntries.chunked(batchSize).forEachIndexed { batchIndex, batch ->
                db.withTransacction {
                    val dictEntry = batch.map { DictionaryEntry(it.id) }
                    dao.insertDictionaryEntries(dictEntry)

                    val kanjiList = batch.flatMap { entry ->
                        entry.kanjiList.map { Kanji(entryId = entry.id, kanji = it) }
                    }
                    dao.insertKanjiList(kanjiList)

                    val readingList = batch.flatMap { entry ->
                        entry.readingList.map { Reading(entryId = entry.id, reading = it) }
                    }
                    dao.insertReadingList(readingList)

                    val sensesWithExamples = mutableListOf<Pair<Sense, List<ParsedExample>>>()
                    val allFields = mutableListOf<Pair<Int, String>>()

                    batch.forEach { entry ->
                        entry.senses.forEach { parsedSense ->
                            val senseEntity = Sense(
                                entryId = entry.id,
                                pos = parsedSense.pos,
                                glosses = parsedSense.glosses,
                                misc = parsedSense.misc,
                                stagk = parsedSense.stagk,
                                stagr = parsedSense.stagr,
                                xref = parsedSense.xref,
                                ant = parsedSense.ant,
                                sInf = parsedSense.sInf
                            )
                            sensesWithExamples.add(senseEntity to parsedSense.examples)

                            parsedSense.field.forEach { field ->
                                allFields.add(sensesWithExamples.size - 1 to field)
                            }
                        }
                    }
                    val senseIds = dao.insertSenseList(sensesWithExamples.map { it.first })

                    val examples = sensesWithExamples.flatMapIndexed { index, (_, examples) ->
                        examples.map { example ->
                            Example(
                                senseId = senseIds[index].toInt(),
                                exText = example.exText,
                                exSentJpn = example.exSentJpn,
                                exSentEng = example.exSentEng
                            )
                        }
                    }
                    dao.insertExampleList(examples)

                    val fieldsMap = mutableMapOf<String, Int>()
                    val crossRefs = mutableListOf<SenseFieldCrossRef>()

                    allFields.forEach { (senseIndex, fieldName) ->
                        val senseId = senseIds[senseIndex]
                        val fieldId = fieldsMap.getOrPut(fieldName) {
                            dao.getFieldId(fieldName).takeIf { it != 0 } ?: dao.insertField(
                                Field(
                                    name = fieldName
                                )
                            ).toInt()
                        }
                        crossRefs.add(SenseFieldCrossRef(senseId.toInt(), fieldId))
                    }
                    dao.insertSenseFieldCrossRefs(crossRefs)
                }

                val current = (batchIndex + 1) * batchSize
                onProgress(PHASE_INSERT_DICT, current, parsedEntries.size)
            }
            Result.success(parsedEntries.size)
        } catch (e: Exception) {
            Log.e("Import", "Critical error", e)
            Result.failure(e)
        }
    }

    suspend fun importKanjiData(
        context: Context,
        onProgress: (phase: Int, current: Int, total: Int) -> Unit
    ): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val db = DictionaryDatabase.getDatabase(context)
            val dao = db.kanjiDao()

            if (dao.getCount() > 0) return@withContext Result.success(0)

            val inputStream = context.resources.openRawResource(R.raw.kanjidic2)
            val parsedEntries = parseKanjiXML(inputStream) { current ->
                onProgress(PHASE_PARSE_KANJI, current, 0)
            }
            onProgress(PHASE_PARSE_KANJI, parsedEntries.size, parsedEntries.size)

            // Sử dụng kỹ thuật batch insert
            val batchSize = 5000
            parsedEntries.chunked(batchSize).forEachIndexed { batchIndex, batch ->
                db.withTransaction {
                    // Batch insert cho KanjiEntry
                    val entries = batch.map { it.kanjiEntry }
                    dao.insertKanjiEntries(entries)

                    // Batch insert cho KanjiReading
                    val readings = batch.flatMap { it.readings }
                    dao.insertKanjiReadings(readings)
                }
                // Update progress
                val current = (batchIndex + 1) * batchSize
                onProgress(
                    PHASE_INSERT_KANJI,
                    current.coerceAtMost(parsedEntries.size),
                    parsedEntries.size
                )
            }
            onProgress(PHASE_INSERT_KANJI, parsedEntries.size, parsedEntries.size)
            Result.success(parsedEntries.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }


    private fun parseXML(
        inputStream: InputStream,
        onProgressUpdate: (Int) -> Unit
    ): List<ParsedEntry> {
        val parsedEntries = mutableListOf<ParsedEntry>()
        val factory = SAXParserFactory.newInstance()
        val saxParser = factory.newSAXParser()

        var currentEntryId = ""
        val kanjiList = mutableListOf<String>()
        val readingList = mutableListOf<String>()
        val senses = mutableListOf<ParsedSense>()

        var currentSense: ParsedSenseBuilder? = null

        val handler = object : DefaultHandler() {
            var currentElement = ""
            val textBuffer = StringBuilder()

            override fun startElement(
                uri: String?,
                localName: String?,
                qName: String?,
                attributes: Attributes?
            ) {
                currentElement = qName ?: ""
                textBuffer.setLength(0)
                when (currentElement) {
                    "entry" -> {
                        currentEntryId = ""
                        kanjiList.clear()
                        readingList.clear()
                        senses.clear()
                    }

                    "sense" -> {
                        currentSense = ParsedSenseBuilder()
                    }

                    "example" -> {
                        currentSense?.currentExample = ParsedExample("", "", "")
                    }
                }
            }

            override fun characters(ch: CharArray?, start: Int, length: Int) {
                textBuffer.appendRange(ch!!, start, start + length)
            }

            override fun endElement(uri: String?, localName: String?, qName: String?) {
                val text = textBuffer.toString().trim()
                when (qName) {
                    "ent_seq" -> currentEntryId = text
                    "keb" -> if (text.isNotEmpty()) kanjiList.add(text)
                    "reb" -> if (text.isNotEmpty()) readingList.add(text)
                    "pos" -> currentSense?.pos?.add(text)
                    "gloss" -> currentSense?.glosses?.add(text)
                    "field" -> {
                        currentSense?.field?.add(text)
                        Log.d(
                            "XMLParser",
                            "Added field: $text for sense ${currentSense?.pos?.joinToString()}"
                        )
                    }

                    "misc" -> currentSense?.misc?.add(text)
                    "stagk" -> currentSense?.stagk?.add(text)
                    "stagr" -> currentSense?.stagr?.add(text)
                    "xref" -> currentSense?.xref?.add(text)
                    "ant" -> currentSense?.ant?.add(text)
                    "s_inf" -> currentSense?.sInf?.add(text)
                    "ex_text" -> currentSense?.currentExample?.exText = text
                    "ex_sent" -> {
                        if (currentSense?.currentExample?.exSentJpn.isNullOrEmpty()) {
                            currentSense?.currentExample?.exSentJpn = text
                        } else {
                            currentSense?.currentExample?.exSentEng = text
                        }
                    }

                    "example" -> {
                        currentSense?.currentExample?.let { ex ->
                            currentSense?.examples?.add(ex.copy())
                        }
                        currentSense?.currentExample = ParsedExample("", "", "")
                    }

                    "sense" -> {
                        currentSense?.currentExample?.let { ex ->
                            if (ex.exText.isNotEmpty() || ex.exSentJpn.isNotEmpty() || ex.exSentEng.isNotEmpty()) {
                                currentSense?.examples?.add(ex.copy())
                            }
                        }
                        currentSense?.let { senses.add(it.build()) }
                        currentSense = null
                    }

                    "entry" -> {
                        val entry = ParsedEntry(
                            id = currentEntryId,
                            kanjiList = kanjiList.toList(),
                            readingList = readingList.toList(),
                            senses = senses.toList()
                        )
                        parsedEntries.add(entry)
                        onProgressUpdate(parsedEntries.size)
                        Log.d("XmlToRoomImporter", "Parsed entry: $entry")
                    }
                }
                textBuffer.setLength(0)
            }
        }
        saxParser.parse(InputSource(inputStream), handler)
        Log.d("XmlToRoomImporter", "Total parsed entries: ${parsedEntries.size}")
        return parsedEntries
    }

    private fun parseKanjiXML(
        inputStream: InputStream,
        onProgressUpdate: (Int) -> Unit
    ): List<ParsedKanjiEntry> {
        val parsedEntries = mutableListOf<ParsedKanjiEntry>()
        val factory = SAXParserFactory.newInstance()
        val saxParser = factory.newSAXParser()

        var currentLiteral = ""
        var strokeCount = 0
        var freq: Int? = null
        var jlpt: Int? = null
        var fileSvgName: String? = null
        val meanings = mutableListOf<String>()
        val readings = mutableListOf<KanjiReading>()
        var currentMeaningLang: String? = null
        var currentCpType: String? = null

        val handler = object : DefaultHandler() {
            var currentElement = ""
            val textBuffer = StringBuilder()

            override fun startElement(
                uri: String?,
                localName: String?,
                qName: String?,
                attributes: Attributes?
            ) {
                currentElement = qName ?: ""
                textBuffer.setLength(0)

                if (attributes != null) {
                    if (currentElement == "reading" && (attributes.getValue("r_type") == "ja_on" || attributes.getValue(
                            "r_type"
                        ) == "ja_kun")
                    ) {
                        val type = attributes.getValue("r_type") ?: ""
                        readings.add(
                            KanjiReading(
                                kanjiLiteral = currentLiteral,
                                reading = "",
                                type = type
                            )
                        )
                    }
                    if (currentElement == "meaning") {
                        currentMeaningLang = attributes.getValue("m_lang")
                    }

                    if (currentElement == "cp_value") {
                        currentCpType = attributes.getValue("cp_type")
                    }
                }
            }

            override fun characters(ch: CharArray?, start: Int, length: Int) {
                textBuffer.appendRange(ch!!, start, start + length)
            }

            override fun endElement(uri: String?, localName: String?, qName: String?) {
                val text = textBuffer.toString().trim()
                when (qName) {
                    "literal" -> currentLiteral = text
                    "stroke_count" -> strokeCount = text.toInt()
                    "freq" -> freq = text.toIntOrNull()
                    "jlpt" -> jlpt = text.toIntOrNull()
                    "cp_value" -> if (currentCpType == "ucs") fileSvgName = "0${text}.svg"


                    "meaning" -> {
                        if (currentMeaningLang == null) {
                            meanings.add(text)
                        }
                    }

                    "reading" -> {
                        if (readings.isNotEmpty() && readings.last().reading.isEmpty()) {
                            readings[readings.size - 1] = readings.last().copy(reading = text)
                        }
                    }

                    "character" -> {
                        val kanjiEntry = KanjiEntry(
                            literal = currentLiteral,
                            strokeCount = strokeCount,
                            freq = freq,
                            jlpt = jlpt,
                            meanings = meanings.toList(),
                            fileSvgName = fileSvgName ?: ""
                        )
                        parsedEntries.add(ParsedKanjiEntry(kanjiEntry, readings.toList()))
                        onProgressUpdate(parsedEntries.size)
                        meanings.clear()
                        readings.clear()
//                        Log.d("XmlToRoomImporter", "Parsed kanji entry: $kanjiEntry")
                    }
                }
                textBuffer.setLength(0)
            }
        }
        saxParser.parse(InputSource(inputStream), handler)
        Log.d("XmlToRoomImporter", "Total parsed kanji entries: ${parsedEntries.size}")
        return parsedEntries
    }

    data class ParsedEntry(
        val id: String,
        val kanjiList: List<String>,
        val readingList: List<String>,
        val senses: List<ParsedSense>
    )

    data class ParsedSense(
        val pos: List<String>,
        val glosses: List<String>,
        val field: List<String>,
        val misc: List<String>,
        val stagk: List<String>,
        val stagr: List<String>,
        val xref: List<String>,
        val ant: List<String>,
        val sInf: List<String>,
        val examples: List<ParsedExample>
    )

    data class ParsedExample(
        var exText: String,
        var exSentJpn: String,
        var exSentEng: String
    )

    data class ParsedSenseBuilder(
        var pos: MutableList<String> = mutableListOf(),
        var glosses: MutableList<String> = mutableListOf(),
        var field: MutableList<String> = mutableListOf(),
        var misc: MutableList<String> = mutableListOf(),
        var stagk: MutableList<String> = mutableListOf(),
        var stagr: MutableList<String> = mutableListOf(),
        var xref: MutableList<String> = mutableListOf(),
        var ant: MutableList<String> = mutableListOf(),
        var sInf: MutableList<String> = mutableListOf(),
        var examples: MutableList<ParsedExample> = mutableListOf(),
        var currentExample: ParsedExample? = ParsedExample("", "", "")
    ) {
        fun build() = ParsedSense(
            pos,
            glosses.toList(),
            field.toList(),
            misc.toList(),
            stagk.toList(),
            stagr.toList(),
            xref.toList(),
            ant.toList(),
            sInf.toList(),
            examples.toList()
        )
    }

    data class ParsedKanjiEntry(
        val kanjiEntry: KanjiEntry,
        val readings: List<KanjiReading>
    )
}