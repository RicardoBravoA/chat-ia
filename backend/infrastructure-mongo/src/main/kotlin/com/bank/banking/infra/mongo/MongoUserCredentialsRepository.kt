package com.bank.banking.infra.mongo

import com.bank.banking.domain.model.UserId
import com.bank.banking.domain.port.UserCredentialsRepository
import com.mongodb.client.model.Filters
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList
import org.bson.Document

class MongoUserCredentialsRepository(
    private val db: MongoDatabase,
) : UserCredentialsRepository {
    private val col get() = db.getCollection<Document>(Collections.USERS)

    override suspend fun findPasswordHashByUserId(userId: UserId): String? {
        val doc = col.find(Filters.eq("_id", userId.value)).limit(1).toList().firstOrNull() ?: return null
        return doc.getString("passwordHash")
    }
}
