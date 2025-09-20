package com.example.moneymaster

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.progressindicator.LinearProgressIndicator
import java.text.NumberFormat
import java.util.Locale
import com.example.moneymaster.viewmodel.TransactionViewModel
import com.example.moneymaster.model.Transaction
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import androidx.lifecycle.lifecycleScope
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class BudgetFragment : Fragment() {
    private lateinit var budgetAmountInput: TextInputEditText
    private lateinit var saveBudgetButton: MaterialButton
    private lateinit var budgetProgressText: TextView
    private lateinit var spentAmountText: TextView
    private lateinit var remainingAmountText: TextView
    private lateinit var budgetProgressBar: LinearProgressIndicator
    private lateinit var budgetWarningText: TextView
    private val currencyFormatter = NumberFormat.getCurrencyInstance(Locale.US)
    private val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    private val viewModel: TransactionViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_budget, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize views
        budgetAmountInput = view.findViewById(R.id.budgetAmountInput)
        saveBudgetButton = view.findViewById(R.id.saveBudgetButton)
        budgetProgressText = view.findViewById(R.id.budgetProgressText)
        spentAmountText = view.findViewById(R.id.spentAmountText)
        remainingAmountText = view.findViewById(R.id.remainingAmountText)
        budgetProgressBar = view.findViewById(R.id.budgetProgressBar)
        budgetWarningText = view.findViewById(R.id.budgetWarningText)

        // Load saved budget
        loadSavedBudget()

        // Set up save button click listener
        saveBudgetButton.setOnClickListener {
            saveBudget()
        }

        // Observe transactions and update budget progress
        observeTransactions()
    }

    private fun loadSavedBudget() {
        val sharedPrefs = requireContext().getSharedPreferences("BudgetPrefs", 0)
        val savedBudget = sharedPrefs.getFloat("monthly_budget", 0f)
        if (savedBudget > 0) {
            budgetAmountInput.setText(savedBudget.toString())
        }
    }

    private fun saveBudget() {
        val budgetAmount = budgetAmountInput.text.toString().toFloatOrNull() ?: 0f
        val sharedPrefs = requireContext().getSharedPreferences("BudgetPrefs", 0)
        sharedPrefs.edit().putFloat("monthly_budget", budgetAmount).apply()
        updateBudgetProgress()
    }

    private fun observeTransactions() {
        lifecycleScope.launch {
            viewModel.expenses.collectLatest { transactions ->
                updateBudgetProgress(transactions)
            }
        }
    }

    private fun updateBudgetProgress(transactions: List<Transaction> = emptyList()) {
        val sharedPrefs = requireContext().getSharedPreferences("BudgetPrefs", 0)
        val monthlyBudget = sharedPrefs.getFloat("monthly_budget", 0f)

        if (monthlyBudget <= 0) {
            budgetProgressBar.progress = 0
            budgetProgressText.text = "0%"
            spentAmountText.text = currencyFormatter.format(0)
            remainingAmountText.text = currencyFormatter.format(0)
            budgetWarningText.visibility = View.GONE
            return
        }

        val currentMonth = LocalDate.now().format(DateTimeFormatter.ofPattern("MM/yyyy"))
        val monthlyExpenses = transactions
            .filter { it.date.endsWith(currentMonth) }
            .sumOf { it.amount }

        val progress = ((monthlyExpenses / monthlyBudget) * 100).toInt().coerceIn(0, 100)
        budgetProgressBar.progress = progress
        budgetProgressText.text = "$progress%"
        spentAmountText.text = currencyFormatter.format(monthlyExpenses)
        remainingAmountText.text = currencyFormatter.format(monthlyBudget - monthlyExpenses)

        // Show warning if expenses exceed 80% of budget
        if (progress >= 80) {
            budgetWarningText.visibility = View.VISIBLE
            budgetWarningText.text = if (progress >= 100) {
                "Budget exceeded!"
            } else {
                "Budget almost exceeded!"
            }
        } else {
            budgetWarningText.visibility = View.GONE
        }
    }

    companion object {
        fun newInstance() = BudgetFragment()
    }
} 