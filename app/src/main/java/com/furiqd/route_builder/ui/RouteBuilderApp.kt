package com.furiqd.route_builder.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.furiqd.route_builder.data.AndroidNetworkAvailability
import com.furiqd.route_builder.model.RouteMode
import com.furiqd.route_builder.model.RouteValidator
import com.furiqd.route_builder.model.Stop

@Composable
fun RouteBuilderApp() {
    val appContext = LocalContext.current.applicationContext
    val viewModel: RouteViewModel = viewModel {
        RouteViewModel(networkAvailability = AndroidNetworkAvailability(appContext))
    }
    LaunchedEffect(Unit) { viewModel.loadStops() }

    when (viewModel.currentScreen) {
        RouteScreen.STOPS -> StopsScreen(viewModel)
        RouteScreen.INSTRUCTIONS -> InstructionsScreen(viewModel)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StopsScreen(viewModel: RouteViewModel) {
    var searchText by remember { mutableStateOf(viewModel.currentQuery) }
    LaunchedEffect(viewModel.currentQuery) {
        searchText = viewModel.currentQuery
    }
    val stopsByGroup = viewModel.availableStops
        .sortedBy { it.name.lowercase() }
        .groupBy(::stopGroupKey)
    val groupKeys = stopsByGroup.keys.sortedWith(compareBy<String> { it == "#" }.thenBy { it })

    Scaffold(
        modifier = Modifier.semantics { testTagsAsResourceId = true },
        topBar = {
            TopAppBar(title = { Text("Route Builder") })
        },
        bottomBar = {
            Surface(shadowElevation = 8.dp) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("${viewModel.selectedStops.size} selected")
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { viewModel.changeRouteMode(RouteMode.ONE_STOP) }) {
                                Text("One stop")
                            }
                            Button(onClick = { viewModel.changeRouteMode(RouteMode.MULTI_STOP) }) {
                                Text("Multi-stop")
                            }
                        }
                    }
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(viewModel.selectedStops, key = { it.stopId }) { selectedStop ->
                            AssistChip(
                                onClick = { viewModel.removeStop(selectedStop.stopId) },
                                label = { Text(selectedStop.stopName) },
                                modifier = Modifier.testTag("selected_stop_chip"),
                            )
                        }
                    }
                    Button(
                        onClick = { viewModel.openInstructions() },
                        enabled = viewModel.canContinue(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Continue (${viewModel.selectedStops.size})")
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = searchText,
                onValueChange = {
                    searchText = it
                    viewModel.loadStops(it)
                },
                label = { Text("Search stops") },
                enabled = !viewModel.isOffline,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("search_input_field")
            )

            if (viewModel.isOffline) {
                Text(
                    RouteViewModel.OFFLINE_MESSAGE,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.testTag("offline_message"),
                )
            }

            if (viewModel.isLoading) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.size(8.dp))
                    Text("Loading stops...")
                }
            }

            if (!viewModel.isOffline) viewModel.lastError?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
                TextButton(
                    onClick = { viewModel.loadStops() },
                    enabled = !viewModel.isLoading,
                ) {
                    Text("Retry loading stops")
                }
            }

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                groupKeys.forEach { group ->
                    item(key = "group_$group") {
                        Text(
                            group,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.testTag("stop_group_$group"),
                        )
                    }
                    items(stopsByGroup[group].orEmpty(), key = { it.id }) { stop ->
                        StopItem(
                            stop = stop,
                            selected = viewModel.isStopSelected(stop.id),
                            onClick = { viewModel.toggleStop(stop) },
                            onParcelClick = { newCount -> viewModel.setCount(stop.id, newCount) },
                            currentPackages = viewModel.selectedStops
                                .firstOrNull { it.stopId == stop.id }?.packages ?: 1,
                            packagesMax = stop.packagesMax,
                        )
                    }
                }
                if (!viewModel.isLoading && viewModel.lastError == null &&
                    viewModel.availableStops.isEmpty()
                ) {
                    item {
                        Text(if (viewModel.currentQuery.isBlank()) "No stops available" else "No stops found")
                    }
                }
            }
        }
    }
}

private fun stopGroupKey(stop: Stop): String =
    stop.name.firstOrNull()?.takeIf { it.isLetter() }?.uppercaseChar()?.toString() ?: "#"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StopItem(
    stop: Stop,
    selected: Boolean,
    onClick: () -> Unit,
    onParcelClick: (Int) -> Unit,
    currentPackages: Int,
    packagesMax: Int?,
) {
    var editingPackages by remember { mutableStateOf(false) }
    var packageValue by remember(stop.id, currentPackages) { mutableStateOf(currentPackages.toString()) }
    val maximumPackages = packagesMax ?: Int.MAX_VALUE
    val validPackageValue = packageValue.toIntOrNull()?.takeIf { it in 1..maximumPackages }
    val displayedPackages = validPackageValue ?: currentPackages

    Card(
        modifier = Modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onClick)
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), CircleShape)
                            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(stop.initials, fontWeight = FontWeight.Bold)
                    }
                    Text(stop.name, fontWeight = FontWeight.Bold)
                }
                Text(stop.address)
                stop.phone?.let { Text(it) }
            }

            Column(
                modifier = Modifier.widthIn(max = 170.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                if (editingPackages) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        TextButton(
                            onClick = {
                                packageValue = (displayedPackages - 1).coerceAtLeast(1).toString()
                            },
                            enabled = displayedPackages > 1,
                            modifier = Modifier
                                .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                                .testTag("parcel_minus_${stop.id}"),
                        ) {
                            Text("−")
                        }
                        OutlinedTextField(
                            value = packageValue,
                            onValueChange = { value ->
                                if (value.isEmpty() || value.all(Char::isDigit)) {
                                    val parsedValue = value.toIntOrNull()
                                    if (value.isEmpty() ||
                                        (parsedValue != null && parsedValue in 1..maximumPackages)
                                    ) {
                                        packageValue = value
                                    }
                                }
                            },
                            modifier = Modifier
                                .width(64.dp)
                                .testTag("parcel_value_${stop.id}"),
                            singleLine = true,
                        )
                        TextButton(
                            onClick = {
                                packageValue = if (displayedPackages < maximumPackages) {
                                    (displayedPackages + 1).toString()
                                } else {
                                    displayedPackages.toString()
                                }
                            },
                            enabled = displayedPackages < maximumPackages,
                            modifier = Modifier
                                .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                                .testTag("parcel_plus_${stop.id}"),
                        ) {
                            Text("+")
                        }
                    }
                    Button(
                        onClick = {
                            validPackageValue?.let(onParcelClick)
                            editingPackages = false
                        },
                        enabled = validPackageValue != null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 48.dp)
                            .testTag("parcel_save_${stop.id}"),
                    ) {
                        Text("Save")
                    }
                } else {
                    TextButton(
                        onClick = {
                            packageValue = currentPackages.toString()
                            editingPackages = true
                        },
                        enabled = selected,
                        modifier = Modifier
                            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                            .testTag("parcel_count_${stop.id}"),
                    ) {
                        Text("${currentPackages}×", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InstructionsScreen(viewModel: RouteViewModel) {
    Scaffold(
        modifier = Modifier.semantics { testTagsAsResourceId = true },
        topBar = {
            TopAppBar(title = { Text("Instructions") }, actions = {
                TextButton(onClick = { viewModel.returnToStops() }) { Text("Back") }
            })
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (viewModel.isOffline) {
                    Text(
                        RouteViewModel.OFFLINE_MESSAGE,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.testTag("offline_message"),
                    )
                }
                if (viewModel.retryWarning) {
                    Text(
                        "Route submission failed or timed out. Retrying may create a duplicate route. Do you want to proceed?",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.testTag("duplicate_risk_warning"),
                    )
                }
                if (!viewModel.isOffline) {
                    viewModel.lastError?.let {
                        Text(it, color = MaterialTheme.colorScheme.error)
                    }
                }
                Button(
                    onClick = { viewModel.submitRoute(viewModel.retryWarning) },
                    enabled = !viewModel.submitting &&
                        !viewModel.isOffline &&
                        viewModel.selectedStops.isNotEmpty() &&
                        viewModel.confirmationRouteId == null,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (viewModel.submitting) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.size(8.dp))
                    }
                    Text(if (viewModel.retryWarning) "Retry route" else "Dispatch Route")
                }
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (viewModel.selectedStops.isEmpty()) {
                item {
                    Text("No stops selected.")
                    TextButton(onClick = { viewModel.returnToStops() }) {
                        Text("Back to Stops")
                    }
                }
            }

            if (viewModel.confirmationRouteId != null) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Route Dispatched Successfully.", fontWeight = FontWeight.Bold)
                            Text("Route ID: ${viewModel.confirmationRouteId}")
                        }
                    }
                }
            }

            items(viewModel.selectedStops, key = { it.stopId }) { stop ->
                val invalid = !RouteValidator.isStopValid(stop)
                val instructionRequired = stop.instructions.trim().isEmpty()
                val instructionTooLong = !RouteValidator.hasSafeInstructionLength(stop.instructions)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (viewModel.showValidationErrors && invalid) {
                            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f)
                        } else {
                            MaterialTheme.colorScheme.surface
                        }
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(stop.stopName, fontWeight = FontWeight.Bold)
                            TextButton(onClick = { viewModel.removeStop(stop.stopId) }) { Text("Remove") }
                        }
                        Text(stop.stopAddress)
                        Text("Parcel count: ${stop.packages}")
                        OutlinedTextField(
                            value = stop.instructions,
                            onValueChange = {
                                if (RouteValidator.isInstructionWithinInputLimit(it)) {
                                    viewModel.setInstruction(stop.stopId, it)
                                }
                            },
                            label = { Text("Delivery instruction") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("instruction_note_input_${stop.stopId}")
                                .height(100.dp),
                            minLines = 2,
                            maxLines = 4,
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            TextButton(onClick = { viewModel.clearInstruction(stop.stopId) }) { Text("Clear all") }
                            Text("${stop.instructions.length}/${RouteValidator.MAX_INSTRUCTION_LENGTH}", textAlign = TextAlign.End)
                        }

                        if (viewModel.showValidationErrors && instructionRequired) {
                            Text(
                                "Delivery instruction is required for this stop.",
                                color = MaterialTheme.colorScheme.error,
                            )
                        } else if (viewModel.showValidationErrors && instructionTooLong) {
                            Text(
                                "Instructions must be 200 characters or fewer before dispatch.",
                                color = MaterialTheme.colorScheme.error,
                            )
                        } else if (viewModel.showValidationErrors && invalid) {
                            Text("Please correct this stop before dispatch.", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}
