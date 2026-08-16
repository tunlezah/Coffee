package com.cawfee.ui.misc

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Lightweight placeholder for library/tool screens whose SwiftUI counterparts are
 * scheduled for a follow-up port (see docs/ANDROID_PORT.md "Screen status"). The
 * underlying domain models and Room scaffolding are already in place.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaceholderScreen(title: String) {
    Scaffold(topBar = { TopAppBar(title = { Text(title) }) }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
        ) {
            Text("$title — coming soon")
            Text("This screen's port is tracked in docs/ANDROID_PORT.md.")
        }
    }
}
