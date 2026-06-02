package com.bank.banking.infra.mongo

import com.mongodb.kotlin.client.coroutine.MongoClient
import com.mongodb.kotlin.client.coroutine.MongoDatabase

object MongoBankingFactory {
    fun database(connectionString: String, databaseName: String = "banking"): MongoDatabase {
        val client = MongoClient.create(connectionString)
        return client.getDatabase(databaseName)
    }
}
