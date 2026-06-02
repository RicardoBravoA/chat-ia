package com.bank.banking.infra.mongo

import com.bank.banking.domain.model.Account
import com.bank.banking.domain.model.AccountId
import com.bank.banking.domain.model.Money
import com.bank.banking.domain.model.UserId
import com.bank.banking.domain.port.AccountRepository
import com.mongodb.client.model.Filters
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList
import org.bson.Document

class MongoAccountRepository(
    private val db: MongoDatabase,
) : AccountRepository {
    private val col get() = db.getCollection<Document>(Collections.ACCOUNTS)

    override suspend fun findPrimaryByUserId(userId: UserId): Account? =
        col.find(Filters.eq("userId", userId.value)).limit(1).toList().firstOrNull()?.toAccount()

    private fun Document.toAccount(): Account =
        Account(
            id = AccountId(getString("_id")),
            userId = UserId(getString("userId")),
            balance = Money(getNumericDouble("amount") ?: error("amount"), getString("currency")),
            nickname = (get("nickname") as? String)?.takeIf { it.isNotBlank() } ?: "",
        )
}
