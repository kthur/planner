package com.planner.tracker.data

import java.io.OutputStream
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CsvExporter {
    fun export(entries: List<Entry>, outputStream: OutputStream) {
        val writer = OutputStreamWriter(outputStream, "UTF-8")
        // UTF-8 BOM을 헤더 맨 앞에 명시적으로 써주어, 엑셀에서 한글 인코딩이 정상적으로 디코딩되도록 처리합니다.
        writer.write('\ufeff'.code) 
        writer.write("날짜,카테고리,기록시간(분),메모,구분\n")
        
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        for (entry in entries) {
            val dateStr = sdf.format(Date(entry.date))
            val category = entry.category.replace(",", " ")
            val note = entry.note.replace(",", " ").replace("\n", " ")
            val type = entry.entryType
            writer.write("$dateStr,$category,${entry.minutes},$note,$type\n")
        }
        writer.flush()
        writer.close()
    }
}
