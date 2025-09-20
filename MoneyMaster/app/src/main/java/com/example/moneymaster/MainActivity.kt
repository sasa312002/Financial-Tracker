package com.example.moneymaster

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.moneymaster.adapter.TransactionAdapter
import com.example.moneymaster.model.Transaction
import com.example.moneymaster.utils.BackupUtils
import com.example.moneymaster.viewmodel.TransactionViewModel
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

class MainActivity : AppCompatActivity() {
    private lateinit var transactionAdapter: TransactionAdapter
    private val currencyFormatter = NumberFormat.getCurrencyInstance(Locale.US)
    private val dateFormatter = java.text.SimpleDateFormat("MMM yyyy", Locale.US)
    private var isBalanceVisible = true
    private var currentTransactionType = TransactionType.ALL
    private val viewModel: TransactionViewModel by viewModels()

    private enum class TransactionType {
        EXPENSE, INCOME, ALL
    }

    private val editTransactionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val updatedTransaction = result.data?.getSerializableExtra(
                EditTransactionActivity.EXTRA_UPDATED_TRANSACTION
            ) as? Transaction
            if (updatedTransaction != null) {
                viewModel.updateTransaction(updatedTransaction)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupViews()
        setupRecyclerView()
        setupTabLayout()
        setupCurrentDate()
        observeTransactions()
    }

    private fun setupViews() {
        // Setup FAB
        findViewById<FloatingActionButton>(R.id.fabAddTransaction).setOnClickListener {
            startActivity(Intent(this, AddEditTransactionActivity::class.java))
        }

        // Setup balance visibility toggle
        findViewById<ImageButton>(R.id.toggleBalanceVisibility).setOnClickListener {
            isBalanceVisible = !isBalanceVisible
            updateTotalAmount()
        }

        // Setup bottom navigation
        findViewById<BottomNavigationView>(R.id.bottomNavigation).setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> true
                R.id.navigation_statistics -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragmentContainer, StatisticsFragment.newInstance())
                        .commit()
                    true
                }
                R.id.navigation_budget -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragmentContainer, BudgetFragment.newInstance())
                        .commit()
                    true
                }
                R.id.navigation_settings -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragmentContainer, SettingsFragment.newInstance())
                        .commit()
                    true
                }
                else -> false
            }
        }
    }

    private fun setupRecyclerView() {
        transactionAdapter = TransactionAdapter(
            onItemClick = { transaction ->
                val intent = Intent(this, EditTransactionActivity::class.java).apply {
                    putExtra(EditTransactionActivity.EXTRA_TRANSACTION, transaction)
                }
                editTransactionLauncher.launch(intent)
            }
        )

        findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.transactionsRecyclerView).apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = transactionAdapter
        }
    }

    private fun setupTabLayout() {
        val tabLayout = findViewById<com.google.android.material.tabs.TabLayout>(R.id.tabLayout)
        tabLayout.addOnTabSelectedListener(object : com.google.android.material.tabs.TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: com.google.android.material.tabs.TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> currentTransactionType = TransactionType.ALL
                    1 -> currentTransactionType = TransactionType.INCOME
                    2 -> currentTransactionType = TransactionType.EXPENSE
                }
                filterTransactions()
            }

            override fun onTabUnselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {}
            override fun onTabReselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {}
        })
    }

    private fun observeTransactions() {
        lifecycleScope.launch {
            when (currentTransactionType) {
                TransactionType.ALL -> viewModel.allTransactions.collectLatest { transactions ->
                    transactionAdapter.setTransactions(transactions)
                    updateTotalAmount()
                }
                TransactionType.EXPENSE -> viewModel.expenses.collectLatest { transactions ->
                    transactionAdapter.setTransactions(transactions)
                    updateTotalAmount()
                }
                TransactionType.INCOME -> viewModel.income.collectLatest { transactions ->
                    transactionAdapter.setTransactions(transactions)
                    updateTotalAmount()
                }
            }
        }
    }

    private fun filterTransactions() {
        observeTransactions()
    }

    private fun setupCurrentDate() {
        val dateButton = findViewById<Button>(R.id.dateButton)
        dateButton.text = dateFormatter.format(java.util.Date())
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == ADD_TRANSACTION_REQUEST && resultCode == RESULT_OK) {
            data?.let {
                val title = it.getStringExtra(EXTRA_TITLE) ?: return
                val amount = it.getDoubleExtra(EXTRA_AMOUNT, 0.0)
                val date = it.getStringExtra(EXTRA_DATE) ?: return
                val category = it.getStringExtra(EXTRA_CATEGORY) ?: return
                val notes = it.getStringExtra(EXTRA_NOTES) ?: ""
                val isExpense = it.getBooleanExtra(EXTRA_IS_EXPENSE, true)

                val transaction = Transaction(
                    title = title,
                    amount = amount,
                    date = date,
                    category = category,
                    notes = notes,
                    isExpense = isExpense
                )

                viewModel.insertTransaction(transaction)
            }
        }
    }

    private fun updateTotalAmount() {
        val transactions = transactionAdapter.getTransactions()
        var totalIncome = 0.0
        var totalExpenditure = 0.0

        transactions.forEach { transaction ->
            if (transaction.isExpense) {
                totalExpenditure += transaction.amount
            } else {
                totalIncome += transaction.amount
            }
        }

        findViewById<TextView>(R.id.balanceAmount).text = if (isBalanceVisible) {
            currencyFormatter.format(totalIncome - totalExpenditure)
        } else {
            "******"
        }

        val expenditureCard = findViewById<androidx.cardview.widget.CardView>(R.id.expenditureCard)
        val expenditureTitle = expenditureCard.findViewById<TextView>(R.id.totalExpenditureTitle)
        val expenditureAmount = expenditureCard.findViewById<TextView>(R.id.totalExpenditure)

        expenditureTitle.text = when (currentTransactionType) {
            TransactionType.EXPENSE -> "Total Expenses"
            TransactionType.INCOME -> "Total Income"
            TransactionType.ALL -> "Total Balance"
        }

        expenditureAmount.text = when (currentTransactionType) {
            TransactionType.EXPENSE -> currencyFormatter.format(totalExpenditure)
            TransactionType.INCOME -> currencyFormatter.format(totalIncome)
            TransactionType.ALL -> currencyFormatter.format(totalIncome - totalExpenditure)
        }
    }

    companion object {
        const val ADD_TRANSACTION_REQUEST = 1
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_AMOUNT = "extra_amount"
        const val EXTRA_DATE = "extra_date"
        const val EXTRA_CATEGORY = "extra_category"
        const val EXTRA_NOTES = "extra_notes"
        const val EXTRA_IS_EXPENSE = "extra_is_expense"
    }
}