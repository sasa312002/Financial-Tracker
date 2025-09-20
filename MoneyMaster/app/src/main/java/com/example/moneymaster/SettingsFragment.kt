package com.example.moneymaster

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.viewModels
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.moneymaster.utils.BackupUtils
import com.example.moneymaster.viewmodel.TransactionViewModel
import com.example.moneymaster.model.Currency
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class SettingsFragment : Fragment() {
    private lateinit var currencySpinner: Spinner
    private val viewModel: TransactionViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupCurrencySpinner(view)

        // Setup backup button
        view.findViewById<MaterialButton>(R.id.backupButton).setOnClickListener {
            backupData()
        }

        // Setup restore button
        view.findViewById<MaterialButton>(R.id.restoreButton).setOnClickListener {
            restoreData()
        }
    }

    private fun setupCurrencySpinner(view: View) {
        currencySpinner = view.findViewById(R.id.currencySpinner)
        
        // Create adapter for currency spinner
        val currencies = Currency.values()
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            currencies
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        currencySpinner.adapter = adapter

        // Set current selection
        val currentCurrency = getSelectedCurrency()
        val position = currencies.indexOf(currentCurrency)
        currencySpinner.setSelection(position)

        // Handle currency selection
        currencySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedCurrency = currencies[position]
                if (selectedCurrency != currentCurrency) {
                    saveCurrency(selectedCurrency)
                    Toast.makeText(context, "Currency updated to ${selectedCurrency.code}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Do nothing
            }
        }
    }

    private fun backupData() {
        try {
            val transactions = runBlocking { viewModel.allTransactions.first() }
            val backupPath = BackupUtils.exportTransactions(requireContext(), transactions)
            Toast.makeText(context, "Data backed up successfully to: $backupPath", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to backup data: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun restoreData() {
        try {
            val backupFiles = BackupUtils.listBackupFiles(requireContext())
            if (backupFiles.isEmpty()) {
                Toast.makeText(context, "No backup files found", Toast.LENGTH_SHORT).show()
                return
            }

            // Get the most recent backup file
            val latestBackup = backupFiles.first()
            val transactions = BackupUtils.importTransactions(requireContext(), latestBackup.absolutePath)
            
            // Clear existing data and insert restored transactions
            runBlocking {
                viewModel.deleteAllTransactions()
                transactions.forEach { transaction ->
                    viewModel.insertTransaction(transaction)
                }
            }
            
            Toast.makeText(context, "Data restored successfully", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to restore data: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getSelectedCurrency(): Currency {
        val prefs = requireContext().getSharedPreferences("money_master_prefs", 0)
        val currencyCode = prefs.getString("selected_currency", null)
        return if (currencyCode != null) {
            Currency.fromCode(currencyCode)
        } else {
            Currency.getDefault()
        }
    }

    private fun saveCurrency(currency: Currency) {
        val prefs = requireContext().getSharedPreferences("money_master_prefs", 0)
        prefs.edit().putString("selected_currency", currency.code).apply()
    }

    companion object {
        fun newInstance() = SettingsFragment()
    }
} 