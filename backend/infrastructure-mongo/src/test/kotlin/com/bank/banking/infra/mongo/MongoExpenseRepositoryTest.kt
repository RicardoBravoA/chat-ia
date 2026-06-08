package com.bank.banking.infra.mongo

import org.bson.Document
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.YearMonth
import java.time.ZoneOffset
import java.util.Date

class MongoExpenseRepositoryTest {
    @Test
    fun `reads occurredAt from date and epoch millis`() {
        val june2024 = Date.from(YearMonth.of(2024, 6).atDay(15).atStartOfDay(ZoneOffset.UTC).toInstant())
        val fromDate = ExpenseDocumentSupport.readOccurredAtEpochMs(Document("occurredAt", june2024))
        val fromLong = ExpenseDocumentSupport.readOccurredAtEpochMs(Document("occurredAt", june2024.time))

        assertEquals(june2024.time, fromDate)
        assertEquals(june2024.time, fromLong)
        assertNull(ExpenseDocumentSupport.readOccurredAtEpochMs(Document()))
    }
}
