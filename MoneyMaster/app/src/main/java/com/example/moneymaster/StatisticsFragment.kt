package com.example.moneymaster

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.moneymaster.model.Transaction
import com.example.moneymaster.viewmodel.TransactionViewModel
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.PercentFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import androidx.lifecycle.lifecycleScope

class StatisticsFragment : Fragment() {

    private lateinit var allTransactionsPieChart: PieChart
    private lateinit var incomePieChart: PieChart
    private lateinit var expensePieChart: PieChart
    private val viewModel: TransactionViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_statistics, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize charts
        allTransactionsPieChart = view.findViewById(R.id.allTransactionsPieChart)
        incomePieChart = view.findViewById(R.id.incomePieChart)
        expensePieChart = view.findViewById(R.id.expensePieChart)

        // Setup charts
        setupPieChart(allTransactionsPieChart, "All Transactions")
        setupPieChart(incomePieChart, "Income")
        setupPieChart(expensePieChart, "Expenses")

        // Observe transactions and update charts
        observeTransactions()
    }

    private fun observeTransactions() {
        lifecycleScope.launch {
            viewModel.allTransactions.collectLatest { transactions ->
                updateCharts(transactions)
            }
        }
    }

    private fun setupPieChart(chart: PieChart, title: String) {
        chart.apply {
            description.isEnabled = false
            setUsePercentValues(true)
            setEntryLabelColor(Color.WHITE)
            setEntryLabelTextSize(12f)
            legend.textSize = 12f
            legend.textColor = Color.WHITE
            setDrawHoleEnabled(true)
            setHoleColor(Color.TRANSPARENT)
            setTransparentCircleRadius(61f)
            setDrawCenterText(true)
            centerText = title
            setCenterTextSize(16f)
            setCenterTextColor(Color.WHITE)
            setDrawEntryLabels(true)
            setEntryLabelColor(Color.WHITE)
            setEntryLabelTextSize(12f)
        }
    }

    private fun updateCharts(transactions: List<Transaction>) {
        // Update all transactions chart
        val allEntries = transactions.groupBy { it.category }
            .map { PieEntry(it.value.sumOf { transaction -> transaction.amount }.toFloat(), it.key) }
        updatePieChart(allTransactionsPieChart, allEntries)

        // Update income chart
        val incomeEntries = transactions.filter { !it.isExpense }
            .groupBy { it.category }
            .map { PieEntry(it.value.sumOf { transaction -> transaction.amount }.toFloat(), it.key) }
        updatePieChart(incomePieChart, incomeEntries)

        // Update expense chart
        val expenseEntries = transactions.filter { it.isExpense }
            .groupBy { it.category }
            .map { PieEntry(it.value.sumOf { transaction -> transaction.amount }.toFloat(), it.key) }
        updatePieChart(expensePieChart, expenseEntries)
    }

    private fun updatePieChart(chart: PieChart, entries: List<PieEntry>) {
        if (entries.isEmpty()) {
            chart.clear()
            return
        }

        val dataSet = PieDataSet(entries, "").apply {
            colors = ColorTemplate.MATERIAL_COLORS.toList()
            valueTextSize = 12f
            valueTextColor = Color.WHITE
            valueFormatter = PercentFormatter(chart)
        }

        chart.data = PieData(dataSet)
        chart.invalidate()
    }

    companion object {
        fun newInstance() = StatisticsFragment()
    }
} 