package com.example.japanesedictionary.utils

import android.content.Context
import android.util.Log
import androidx.room.withTransaction
import com.example.japanesedictionary.R
import com.example.japanesedictionary.data.DictionaryDatabase
import com.example.japanesedictionary.data.dao.DictionaryDao
import com.example.japanesedictionary.data.dao.KanjiDao
import com.example.japanesedictionary.data.model.DictionaryEntry
import com.example.japanesedictionary.data.model.DictionaryFTS
import com.example.japanesedictionary.data.model.Example
import com.example.japanesedictionary.data.model.Field
import com.example.japanesedictionary.data.model.Kanji
import com.example.japanesedictionary.data.model.KanjiEntry
import com.example.japanesedictionary.data.model.KanjiReading
import com.example.japanesedictionary.data.model.Reading
import com.example.japanesedictionary.data.model.Sense
import com.example.japanesedictionary.data.model.SenseFieldCrossRef
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import java.io.InputStream
import java.nio.ByteBuffer
import javax.xml.stream.XMLInputFactory
import javax.xml.stream.XMLResolver
import javax.xml.stream.XMLStreamConstants

// Helper: InputStream từ ByteBuffer (Memory mapping)
class ByteBufferBackedInputStream(private val buffer: ByteBuffer) : InputStream() {
    override fun read(): Int {
        return if (!buffer.hasRemaining()) -1 else buffer.get().toInt() and 0xff
    }

    override fun read(bytes: ByteArray, off: Int, len: Int): Int {
        if (!buffer.hasRemaining()) return -1
        val n = minOf(len, buffer.remaining())
        buffer.get(bytes, off, n)
        return n
    }
}

object XmlToRoomImporter {
    private const val PHASE_PARSE_DICT = 0
    private const val PHASE_INSERT_DICT = 1
    private const val PHASE_PARSE_KANJI = 2
    private const val PHASE_INSERT_KANJI = 3

    // Ban đầu ta có hằng số, nhưng giờ sẽ dùng kích thước batch động
    private fun getDynamicBatchSize(): Int {
        val maxMemoryMB = Runtime.getRuntime().maxMemory() / (1024 * 1024)
        Log.d("MemoryInfo", "Max heap memory: $maxMemoryMB MB")


        return when {
            maxMemoryMB >= 1024 -> 16000
            maxMemoryMB >= 512 -> 8000
            maxMemoryMB >= 256 -> 4000
            maxMemoryMB >= 128 -> 2000
            maxMemoryMB >= 64 -> 1000
            else -> 500
        }
    }

    // Các hàm checkpoint để lưu tiến độ (error recovery)
    private fun saveCheckpoint(context: Context, phase: Int, progress: Int) {
        val prefs = context.getSharedPreferences("import_prefs", Context.MODE_PRIVATE)
        prefs.edit().putInt("checkpoint_phase_$phase", progress).apply()
    }

    private fun loadCheckpoint(context: Context, phase: Int): Int {
        val prefs = context.getSharedPreferences("import_prefs", Context.MODE_PRIVATE)
        return prefs.getInt("checkpoint_phase_$phase", 0)
    }

    /**
     * Import dictionary data using Flow for progress updates
     */
    private fun importDataFlow(context: Context): Flow<ImportProgress> = channelFlow {
        val db = DictionaryDatabase.getDatabase(context)
        val dao = db.dictionaryDao()
        if (dao.getCount() > 0) {
            send(ImportProgress.Completed(0))
            return@channelFlow
        }
        try {
            // Sử dụng memory mapping thay vì BufferedInputStream nếu có thể
//            val afd = context.resources.openRawResourceFd(R.raw.jmdict_e_examp)
//            val fileChannel = FileChannel.open(Paths.get(afd.fileDescriptor.toString()))
//            val mappedBuffer =
//                fileChannel.map(FileChannel.MapMode.READ_ONLY, afd.startOffset, afd.length)
//            val inputStream = ByteBufferBackedInputStream(mappedBuffer)
            val inputStream = ByteBufferBackedInputStream(
                ByteBuffer.wrap(context.resources.openRawResource(R.raw.jmdict_e_examp).readBytes())
            )

            val dynamicBatchSize = getDynamicBatchSize()
            val entriesChannel = Channel<List<ParsedEntry>>(Channel.BUFFERED)
            val parserJob = CoroutineScope(Dispatchers.Default).launch {
                parseXMLWithStAX(
                    context,
                    inputStream,
                    dynamicBatchSize
                ) { entries, current, total ->
                    entriesChannel.send(entries)
                    send(ImportProgress.Parsing(PHASE_PARSE_DICT, current, total))
                    // Lưu checkpoint sau mỗi batch parsing
                    saveCheckpoint(context, PHASE_PARSE_DICT, current)
                }
                entriesChannel.close()
            }
            var totalInserted = 0
            val inserterJob = CoroutineScope(Dispatchers.IO).launch {
                var totalEntries = 0
                for (entries in entriesChannel) {
                    totalEntries += entries.size
                    // Sử dụng bulk insert trong transaction (đã tối ưu sẵn)
                    insertEntriesToDatabase(db, dao, entries)
                    totalInserted += entries.size
                    send(ImportProgress.Inserting(PHASE_INSERT_DICT, totalInserted, totalEntries))
                    // Lưu checkpoint sau mỗi batch insert
                    saveCheckpoint(context, PHASE_INSERT_DICT, totalInserted)
                }
            }
            parserJob.join()
            inserterJob.join()
            send(ImportProgress.Completed(totalInserted))
        } catch (e: Exception) {
            Log.e("Import", "Critical error", e)
            send(ImportProgress.Failed(e))
        }
    }

    /**
     * Import kanji data using Flow for progress updates
     */
    private fun importKanjiDataFlow(context: Context): Flow<ImportProgress> = channelFlow {
        val db = DictionaryDatabase.getDatabase(context)
        val dao = db.kanjiDao()
        if (dao.getCount() > 0) {
            send(ImportProgress.Completed(0))
            return@channelFlow
        }
        try {
//            val afd = context.resources.openRawResourceFd(R.raw.kanjidic2)
//            val fileChannel = FileChannel.open(Paths.get(afd.fileDescriptor.toString()))
//            val mappedBuffer =
//                fileChannel.map(FileChannel.MapMode.READ_ONLY, afd.startOffset, afd.length)
//            val inputStream = ByteBufferBackedInputStream(mappedBuffer)
            val inputStream = ByteBufferBackedInputStream(
                ByteBuffer.wrap(context.resources.openRawResource(R.raw.kanjidic2).readBytes())
            )

            val dynamicBatchSize = getDynamicBatchSize()
            val entriesChannel = Channel<List<ParsedKanjiEntry>>(Channel.BUFFERED)
            val parserJob = CoroutineScope(Dispatchers.Default).launch {
                parseKanjiXMLWithStAX(inputStream, dynamicBatchSize) { entries, current, total ->
                    entriesChannel.send(entries)
                    send(ImportProgress.Parsing(PHASE_PARSE_KANJI, current, total))
                    saveCheckpoint(context, PHASE_PARSE_KANJI, current)
                }
                entriesChannel.close()
            }
            var totalInserted = 0
            val inserterJob = CoroutineScope(Dispatchers.IO).launch {
                var totalEntries = 0
                for (entries in entriesChannel) {
                    totalEntries += entries.size
                    insertKanjiEntriesToDatabase(db, dao, entries)
                    totalInserted += entries.size
                    send(ImportProgress.Inserting(PHASE_INSERT_KANJI, totalInserted, totalEntries))
                    saveCheckpoint(context, PHASE_INSERT_KANJI, totalInserted)
                }
            }
            parserJob.join()
            inserterJob.join()
            send(ImportProgress.Completed(totalInserted))
        } catch (e: Exception) {
            Log.e("Import", "Critical error", e)
            send(ImportProgress.Failed(e))
        }
    }

    /**
     * Parse XML using StAX with dynamic batch size
     */
    private suspend fun parseXMLWithStAX(
        context: Context,
        inputStream: InputStream,
        batchSize: Int,
        onBatchReady: suspend (List<ParsedEntry>, Int, Int) -> Unit
    ) {
        val factory = XMLInputFactory.newInstance().apply {
            setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, true)
            setProperty(XMLInputFactory.IS_COALESCING, true)
            setProperty(XMLInputFactory.SUPPORT_DTD, false)
            setProperty(XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES, false)
            xmlResolver = object : XMLResolver {
                override fun resolveEntity(
                    ns: String?, publicID: String?, systemID: String?, baseURI: String?
                ): Any = ""
            }
            try {
                setProperty("com.ctc.wstx.maxEntityCount", 1000000)
            } catch (e: Exception) {
                Log.w("Import", "Property com.ctc.wstx.maxEntityCount not supported", e)
            }
        }

        val reader = factory.createXMLStreamReader(inputStream)
        val entries = mutableListOf<ParsedEntry>()
        var totalParsed = loadCheckpoint(context, PHASE_PARSE_DICT)  // nếu có checkpoint trước đó
        var currentEntryId = ""
        val kanjiList = mutableListOf<String>()
        val readingList = mutableListOf<String>()
        val senses = mutableListOf<ParsedSense>()
        var currentSense: ParsedSenseBuilder? = null
        var currentElement: String
        val textBuffer = StringBuilder()
        try {
            while (reader.hasNext()) {
                val event = reader.next()
                when (event) {
                    XMLStreamConstants.START_ELEMENT -> {
                        currentElement = reader.localName
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

                    XMLStreamConstants.CHARACTERS -> {
                        textBuffer.append(reader.text)
                    }

                    XMLStreamConstants.END_ELEMENT -> {
                        val text = textBuffer.toString().trim()
                        when (reader.localName) {
                            "ent_seq" -> currentEntryId = text
                            "keb" -> if (text.isNotEmpty()) kanjiList.add(text)
                            "reb" -> if (text.isNotEmpty()) readingList.add(text)
                            "pos" -> currentSense?.pos?.add(text)
                            "gloss" -> currentSense?.glosses?.add(text)
                            "field" -> currentSense?.field?.add(text)
                            "misc" -> currentSense?.misc?.add(text)
                            "stagk" -> currentSense?.stagk?.add(text)
                            "stagr" -> currentSense?.stagr?.add(text)
                            "xref" -> currentSense?.xref?.add(text)
                            "ant" -> currentSense?.ant?.add(text)
                            "s_inf" -> currentSense?.sInf?.add(text)
                            "ex_text" -> currentSense?.currentExample?.exText = text
                            "ex_sent" -> {
                                if (currentSense?.currentExample?.exSentJpn.isNullOrEmpty())
                                    currentSense?.currentExample?.exSentJpn = text
                                else
                                    currentSense?.currentExample?.exSentEng = text
                            }

                            "example" -> {
                                currentSense?.currentExample?.let { ex ->
                                    currentSense?.examples?.add(ex.copy())
                                }
                                currentSense?.currentExample = ParsedExample("", "", "")
                            }

                            "sense" -> {
                                currentSense?.currentExample?.let { ex ->
                                    if (ex.exText.isNotEmpty() || ex.exSentJpn.isNotEmpty() || ex.exSentEng.isNotEmpty())
                                        currentSense?.examples?.add(ex.copy())
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
                                entries.add(entry)
                                totalParsed++
                                // Khi đủ kích thước batch động, gửi batch ra ngoài
                                if (entries.size >= batchSize) {
                                    onBatchReady(entries.toList(), totalParsed, totalParsed)
                                    entries.clear()
                                    yield() // nhường luồng cho các coroutine khác
                                }
                            }
                        }
                        textBuffer.setLength(0)
                    }
                }
            }
            if (entries.isNotEmpty()) {
                onBatchReady(entries.toList(), totalParsed, totalParsed)
            }
        } finally {
            reader.close()
            withContext(Dispatchers.IO) {
                inputStream.close()
            }
        }
    }

    /**
     * Parse Kanji XML using StAX with dynamic batch size
     */
    private suspend fun parseKanjiXMLWithStAX(
        inputStream: InputStream,
        batchSize: Int,
        onBatchReady: suspend (List<ParsedKanjiEntry>, Int, Int) -> Unit
    ) {
        val factory = XMLInputFactory.newInstance().apply {
            setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, true)
            setProperty(XMLInputFactory.IS_COALESCING, true)
            setProperty(XMLInputFactory.SUPPORT_DTD, false)
            setProperty(XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES, false)
            xmlResolver = object : XMLResolver {
                override fun resolveEntity(
                    ns: String?,
                    publicID: String?,
                    systemID: String?,
                    baseURI: String?
                ): Any = ""
            }
            try {
                setProperty("com.ctc.wstx.maxEntityCount", 1000000)
            } catch (e: Exception) {
                Log.w("Import", "Property com.ctc.wstx.maxEntityCount not supported", e)
            }
        }

        val reader = factory.createXMLStreamReader(inputStream)
        val entries = mutableListOf<ParsedKanjiEntry>()
        var totalParsed = 0
        var currentLiteral = ""
        var strokeCount = 0
        var freq: Int? = null
        var jlpt: Int? = null
        var fileSvgName: String? = null
        val meanings = mutableListOf<String>()
        val readings = mutableListOf<KanjiReading>()
        var currentMeaningLang: String? = null
        var currentCpType: String? = null
        var currentElement: String
        val textBuffer = StringBuilder()
        try {
            while (reader.hasNext()) {
                val event = reader.next()
                when (event) {
                    XMLStreamConstants.START_ELEMENT -> {
                        currentElement = reader.localName
                        textBuffer.setLength(0)
                        if (currentElement == "reading") {
                            for (i in 0 until reader.attributeCount) {
                                if (reader.getAttributeLocalName(i) == "r_type") {
                                    val type = reader.getAttributeValue(i)
                                    if (type == "ja_on" || type == "ja_kun") {
                                        readings.add(
                                            KanjiReading(
                                                kanjiLiteral = currentLiteral,
                                                reading = "",
                                                type = type
                                            )
                                        )
                                    }
                                }
                            }
                        }
                        if (currentElement == "meaning") {
                            for (i in 0 until reader.attributeCount) {
                                if (reader.getAttributeLocalName(i) == "m_lang") {
                                    currentMeaningLang = reader.getAttributeValue(i)
                                }
                            }
                        }
                        if (currentElement == "cp_value") {
                            for (i in 0 until reader.attributeCount) {
                                if (reader.getAttributeLocalName(i) == "cp_type") {
                                    currentCpType = reader.getAttributeValue(i)
                                }
                            }
                        }
                    }

                    XMLStreamConstants.CHARACTERS -> {
                        textBuffer.append(reader.text)
                    }

                    XMLStreamConstants.END_ELEMENT -> {
                        val text = textBuffer.toString().trim()
                        when (reader.localName) {
                            "literal" -> currentLiteral = text
                            "stroke_count" -> strokeCount = text.toIntOrNull() ?: 0
                            "freq" -> freq = text.toIntOrNull()
                            "jlpt" -> jlpt = text.toIntOrNull()
                            "cp_value" -> if (currentCpType == "ucs") fileSvgName = "0${text}.svg"
                            "meaning" -> {
                                if (currentMeaningLang == null) meanings.add(text)
                            }

                            "reading" -> {
                                if (readings.isNotEmpty() && readings.last().reading.isEmpty()) {
                                    readings[readings.size - 1] =
                                        readings.last().copy(reading = text)
                                }
                            }

                            "character" -> {
                                val kanjiEntry = KanjiEntry(
                                    literal = currentLiteral,
                                    strokeCount = strokeCount,
                                    freq = freq,
                                    jlpt = jlpt,
                                    meanings = meanings.toList(),
                                    fileSvgName = fileSvgName
                                )
                                entries.add(ParsedKanjiEntry(kanjiEntry, readings.toList()))
                                totalParsed++
                                meanings.clear()
                                readings.clear()
                                currentMeaningLang = null
                                currentCpType = null
                                if (entries.size >= batchSize) {
                                    onBatchReady(entries.toList(), totalParsed, totalParsed)
                                    entries.clear()
                                    yield()
                                }
                            }
                        }
                        textBuffer.setLength(0)
                    }
                }
            }
            if (entries.isNotEmpty()) {
                onBatchReady(entries.toList(), totalParsed, totalParsed)
            }
        } finally {
            reader.close()
            withContext(Dispatchers.IO) {
                inputStream.close()
            }
        }
    }

    /**
     * Insert entries to database with optimized batch processing (Bulk Insert)
     */
    private suspend fun insertEntriesToDatabase(
        db: DictionaryDatabase,
        dao: DictionaryDao,
        entries: List<ParsedEntry>
    ) {
        db.withTransaction {
            val dictEntries = entries.map { DictionaryEntry(it.id) }
            dao.insertDictionaryEntries(dictEntries)
            val kanjiList = entries.flatMap { entry ->
                entry.kanjiList.map { Kanji(entryId = entry.id, kanji = it) }
            }
            dao.insertKanjiList(kanjiList)
            val readingList = entries.flatMap { entry ->
                entry.readingList.map { Reading(entryId = entry.id, reading = it) }
            }
            dao.insertReadingList(readingList)

            val sensesWithExamples = mutableListOf<Pair<Sense, List<ParsedExample>>>()
            val allFields = mutableListOf<Pair<Int, String>>()
            entries.forEach { entry ->
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
                    dao.getFieldId(fieldName).takeIf { it != 0 }
                        ?: dao.insertField(Field(name = fieldName)).toInt()
                }
                crossRefs.add(SenseFieldCrossRef(senseId.toInt(), fieldId))
            }
            dao.insertSenseFieldCrossRefs(crossRefs)

            // Insert into FTS table
            val ftsEntries = entries.map { parsedEntry ->
                val kanjiText = parsedEntry.kanjiList.joinToString(" ") { tokenizeJapaneseText(it) }
                val readingText =
                    parsedEntry.readingList.joinToString(" ") { tokenizeJapaneseText(it) }
                val readingHiraganaText = parsedEntry.readingList
                    .map { it.convertKatakanaToHiragana() }
                    .joinToString(" ") { tokenizeJapaneseText(it) }
                val glossesText = parsedEntry.senses
                    .flatMap { it.glosses }
                    .joinToString(" ")
                DictionaryFTS(
                    entryId = parsedEntry.id,
                    kanji = kanjiText,
                    reading = readingText,
                    reading_hiragana = readingHiraganaText,
                    glosses = glossesText
                )
            }
            dao.insertFTSEntries(ftsEntries)
        }
    }

    /**
     * Insert kanji entries to database with optimized batch processing
     */
    private suspend fun insertKanjiEntriesToDatabase(
        db: DictionaryDatabase,
        dao: KanjiDao,
        entries: List<ParsedKanjiEntry>
    ) {
        db.withTransaction {
            val kanjiEntries = entries.map { it.kanjiEntry }
            dao.insertKanjiEntries(kanjiEntries)
            val readings = entries.flatMap { it.readings }
            dao.insertKanjiReadings(readings)
        }
    }

    // For backward compatibility
    suspend fun importData(
        context: Context,
        onProgress: (phase: Int, current: Int, total: Int) -> Unit
    ): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val progress = MutableStateFlow<ImportProgress>(ImportProgress.NotStarted)
            val job = launch { importDataFlow(context).collect { progress.value = it } }
            val progressJob = launch {
                while (isActive) {
                    when (val currentProgress = progress.value) {
                        is ImportProgress.Parsing -> onProgress(
                            currentProgress.phase,
                            currentProgress.current,
                            currentProgress.total
                        )

                        is ImportProgress.Inserting -> onProgress(
                            currentProgress.phase,
                            currentProgress.current,
                            currentProgress.total
                        )

                        is ImportProgress.Completed -> {
                            onProgress(
                                PHASE_INSERT_DICT,
                                currentProgress.count,
                                currentProgress.count
                            )
                            break
                        }

                        is ImportProgress.Failed -> throw currentProgress.error
                        else -> {}
                    }
                    delay(100)
                }
            }
            job.join()
            progressJob.join()
            when (val finalProgress = progress.value) {
                is ImportProgress.Failed -> Result.failure(finalProgress.error)
                is ImportProgress.Completed -> Result.success(finalProgress.count)
                else -> Result.success(0)
            }
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
            val progress = MutableStateFlow<ImportProgress>(ImportProgress.NotStarted)
            val job = launch { importKanjiDataFlow(context).collect { progress.value = it } }
            val progressJob = launch {
                while (isActive) {
                    when (val currentProgress = progress.value) {
                        is ImportProgress.Parsing -> onProgress(
                            currentProgress.phase,
                            currentProgress.current,
                            currentProgress.total
                        )

                        is ImportProgress.Inserting -> onProgress(
                            currentProgress.phase,
                            currentProgress.current,
                            currentProgress.total
                        )

                        is ImportProgress.Completed -> {
                            onProgress(
                                PHASE_INSERT_KANJI,
                                currentProgress.count,
                                currentProgress.count
                            )
                            break
                        }

                        is ImportProgress.Failed -> throw currentProgress.error
                        else -> {}
                    }
                    delay(100)
                }
            }
            job.join()
            progressJob.join()
            when (val finalProgress = progress.value) {
                is ImportProgress.Failed -> Result.failure(finalProgress.error)
                is ImportProgress.Completed -> Result.success(finalProgress.count)
                else -> Result.success(0)
            }
        } catch (e: Exception) {
            Log.e("Import", "Critical error", e)
            Result.failure(e)
        }
    }

    // Data classes
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

    sealed class ImportProgress {
        data object NotStarted : ImportProgress()
        data class Parsing(val phase: Int, val current: Int, val total: Int) : ImportProgress()
        data class Inserting(val phase: Int, val current: Int, val total: Int) : ImportProgress()
        data class Completed(val count: Int) : ImportProgress()
        data class Failed(val error: Exception) : ImportProgress()
    }
}
