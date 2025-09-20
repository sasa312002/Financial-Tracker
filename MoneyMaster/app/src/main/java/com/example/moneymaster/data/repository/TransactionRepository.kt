package com.example.moneymaster.data.repository

import com.example.moneymaster.data.dao.TransactionDao
import com.example.moneymaster.data.entity.TransactionEntity
import com.example.moneymaster.model.Transaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TransactionRepository(private val transactionDao: TransactionDao) {

    fun getAllTransactions(): Flow<List<Transaction>> {
        return transactionDao.getAllTransactions().map { entities ->
            entities.map { it.toTransaction() }
        }
    }

    fun getTransactionsByType(isExpense: Boolean): Flow<List<Transaction>> {
        return transactionDao.getTransactionsByType(isExpense).map { entities ->
            entities.map { it.toTransaction() }
        }
    }

    suspend fun insertTransaction(transaction: Transaction) {
        transactionDao.insertTransaction(transaction.toEntity())
    }

    suspend fun updateTransaction(transaction: Transaction) {
        transactionDao.updateTransaction(transaction.toEntity())
    }

    suspend fun deleteTransaction(transaction: Transaction) {
        transactionDao.deleteTransaction(transaction.toEntity())
    }

    suspend fun deleteAllTransactions() {
        transactionDao.deleteAllTransactions()
    }

    private fun TransactionEntity.toTransaction() = Transaction(
        id = id,
        title = title,
        amount = amount,
        date = date,
        category = category,
        notes = notes,
        isExpense = isExpense
    )

    private fun Transaction.toEntity() = TransactionEntity(
        id = id,
        title = title,
        amount = amount,
        date = date,
        category = category,
        notes = notes,
        isExpense = isExpense
    )
} 