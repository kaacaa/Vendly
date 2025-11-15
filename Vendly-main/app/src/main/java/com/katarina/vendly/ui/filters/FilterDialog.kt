package com.katarina.vendly.ui.filters

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun FilterDialog(
    productType: String,
    status: String,
    updatedAfter: String,          // yyyy-MM-dd
    updatedBefore: String,         // yyyy-MM-dd
    onProductTypeChange: (String) -> Unit,
    onStatusChange: (String) -> Unit,
    onUpdatedAfterChange: (String) -> Unit,
    onUpdatedBeforeChange: (String) -> Unit,
    isFiltering: Boolean,
    onClear: () -> Unit,
    onApply: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Filter vending machines") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

                // Product type
                OutlinedTextField(
                    value = productType,
                    onValueChange = onProductTypeChange,
                    label = { Text("Product type") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Status selector
                Text("Status")
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatusRadio("full", "Full", status, onStatusChange)
                    StatusRadio("empty", "Empty", status, onStatusChange)
                    StatusRadio("out_of_order", "Out of order", status, onStatusChange)
                    StatusRadio("low", "Low", status, onStatusChange)
                }

                // Date filters
                Text("Last changed (yyyy-MM-dd)")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = updatedAfter,
                        onValueChange = onUpdatedAfterChange,
                        label = { Text("Updated after") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = updatedBefore,
                        onValueChange = onUpdatedBeforeChange,
                        label = { Text("Updated before") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TextButton(onClick = onClear, enabled = !isFiltering) {
                    Text("Clear")
                }
                Button(onClick = onApply, enabled = !isFiltering) {
                    if (isFiltering) {
                        CircularProgressIndicator(
                            modifier = Modifier.width(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text("Apply")
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isFiltering) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun StatusRadio(
    value: String,
    label: String,
    selectedStatus: String,
    onStatusChange: (String) -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        RadioButton(
            selected = selectedStatus == value,
            onClick = { onStatusChange(value) }
        )
        Spacer(Modifier.width(6.dp))
        Text(label)
    }
}