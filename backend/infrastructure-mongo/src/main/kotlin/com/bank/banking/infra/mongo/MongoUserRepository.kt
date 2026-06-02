package com.bank.banking.infra.mongo

import com.bank.banking.domain.model.User
import com.bank.banking.domain.model.UserId
import com.bank.banking.domain.port.UserRepository
import com.mongodb.client.model.Filters
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList
import org.bson.Document

class MongoUserRepository(
    private val db: MongoDatabase,
) : UserRepository {
    private val col get() = db.getCollection<Document>(Collections.USERS)

    override suspend fun findByEmail(email: String): User? =
        col.find(Filters.eq("email", email)).limit(1).toList().firstOrNull()?.toUser()

    override suspend fun findById(id: UserId): User? =
        col.find(Filters.eq("_id", id.value)).limit(1).toList().firstOrNull()?.toUser()

    private fun Document.toUser(): User =
        User(
            id = UserId(getString("_id")),
            email = getString("email"),
        )
}
