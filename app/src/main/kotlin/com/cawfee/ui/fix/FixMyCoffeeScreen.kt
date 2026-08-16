package com.cawfee.ui.fix

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cawfee.domain.model.Recommendation
import com.cawfee.domain.model.Symptom

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FixMyCoffeeScreen(expertMode: Boolean, viewModel: FixMyCoffeeViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(topBar = { TopAppBar(title = { Text(if (expertMode) "Expert Mode" else "Fix My Coffee") }) }) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("What's wrong with the cup?", fontWeight = FontWeight.SemiBold)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Symptom.entries.forEach { symptom ->
                    FilterChip(
                        selected = symptom in state.selectedSymptoms,
                        onClick = { viewModel.toggle(symptom) },
                        label = { Text(symptom.displayName) },
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { viewModel.evaluate(novice = !expertMode) },
                    enabled = state.selectedSymptoms.isNotEmpty(),
                ) { Text("Get recommendation") }
                OutlinedButton(onClick = { viewModel.clearSymptoms() }) { Text("Clear") }
            }

            state.recommendation?.let { rec ->
                RecommendationCard(rec, onApply = { viewModel.applyPrimary() }, expert = expertMode)
            }
        }
    }
}

@Composable
private fun RecommendationCard(rec: Recommendation, onApply: () -> Unit, expert: Boolean) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (rec.suggestRevertToLastGood) {
                Text("Consider reverting", fontWeight = FontWeight.SemiBold)
                Text(rec.rationale)
                return@Column
            }
            rec.topCause?.let { Text(it.displayName, fontWeight = FontWeight.SemiBold) }
            LinearProgressIndicator(
                progress = { rec.confidence.toFloat() },
                modifier = Modifier.fillMaxWidth(),
            )
            Text("${(rec.confidence * 100).toInt()}% confident")
            Text(rec.rationale)
            rec.primary?.let { p ->
                Text("Primary: ${p.summary}", fontWeight = FontWeight.Medium)
                Text(p.reason)
                Button(onClick = onApply) { Text("Apply") }
            }
            rec.secondary?.let { s -> Text("Also: ${s.summary}") }

            if (expert) {
                Text("Rule contributions", fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 8.dp))
                rec.contributions.forEach { c ->
                    Text("• ${c.cause.displayName} — ${(c.confidence * 100).toInt()}% (${c.ruleIDs.joinToString()})")
                }
            }
        }
    }
}
