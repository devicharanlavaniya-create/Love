package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.EventSeat
import androidx.compose.material.icons.filled.LocalLibrary
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.CampusFacility
import com.example.data.CampusNotice
import com.example.data.FacilityCategory
import com.example.data.FacilityStatus
import com.example.data.FacultyMember
import com.example.data.FacultyStatus
import com.example.data.FeeInvoice
import com.example.data.FeeStatus
import com.example.data.NoticeCategory
import com.example.ui.components.EmptyCampusCard
import com.example.ui.components.StatusPill
import com.example.ui.components.StudentAvatar
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalCampusStatusColors
import com.example.ui.viewmodel.SchoolDashboardData
import java.text.NumberFormat
import java.util.Locale

enum class CampusTab(val label: String) {
    FACILITIES("Facilities"),
    FACULTY("Faculty"),
    FEES("Fees"),
    NOTICES("Notices")
}

@Composable
fun CampusScreen(
    data: SchoolDashboardData,
    activeTab: CampusTab,
    onSelectTab: (CampusTab) -> Unit,
    onAddFacility: () -> Unit,
    onBookFacility: (CampusFacility) -> Unit,
    onDeleteFacility: (String) -> Unit,
    onAddFaculty: () -> Unit,
    onToggleFacultyStatus: (FacultyMember) -> Unit,
    onDeleteFaculty: (String) -> Unit,
    onAddFeeInvoice: () -> Unit,
    onRecordPayment: (FeeInvoice) -> Unit,
    onDeleteFeeInvoice: (String) -> Unit,
    onAddNotice: () -> Unit,
    onToggleNoticePin: (CampusNotice) -> Unit,
    onDeleteNotice: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val statusColors = LocalCampusStatusColors.current
    var facilityFilter by remember { mutableStateOf<FacilityCategory?>(null) }
    val currency = remember {
        NumberFormat.getCurrencyInstance(Locale.US).apply { maximumFractionDigits = 0 }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("campus_segmented_tabs")
            ) {
                CampusTab.entries.forEachIndexed { idx, tab ->
                    SegmentedButton(
                        selected = activeTab == tab,
                        onClick = { onSelectTab(tab) },
                        shape = SegmentedButtonDefaults.itemShape(index = idx, count = CampusTab.entries.size)
                    ) {
                        Text(tab.label, maxLines = 1)
                    }
                }
            }
        }

        when (activeTab) {
            CampusTab.FACILITIES -> {
                if (data.facilities.isNotEmpty()) {
                    item {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            item {
                                FilterChip(
                                    selected = facilityFilter == null,
                                    onClick = { facilityFilter = null },
                                    label = { Text("All (${data.facilities.size})") }
                                )
                            }
                            items(FacilityCategory.entries) { category ->
                                FilterChip(
                                    selected = facilityFilter == category,
                                    onClick = { facilityFilter = category },
                                    label = { Text(category.label) }
                                )
                            }
                        }
                    }
                }
                val visibleFacilities = data.facilities.filter {
                    facilityFilter == null || it.category == facilityFilter!!.name
                }
                if (visibleFacilities.isEmpty()) {
                    item {
                        EmptyCampusCard(
                            icon = Icons.Filled.Apartment,
                            title = "No Campus Facilities Yet",
                            description = "Manage science & computer labs, library halls, auditoriums, sports grounds, and school buses.",
                            primaryActionLabel = "Add Facility",
                            onPrimaryAction = onAddFacility
                        )
                    }
                } else {
                    items(visibleFacilities, key = { it.id }) { facility ->
                        val cat = FacilityCategory.fromRaw(facility.category)
                        val status = FacilityStatus.fromRaw(facility.status)
                        val (fg, bg) = when (status) {
                            FacilityStatus.AVAILABLE -> statusColors.present to statusColors.presentContainer
                            FacilityStatus.OCCUPIED -> statusColors.late to statusColors.lateContainer
                            FacilityStatus.MAINTENANCE -> statusColors.absent to statusColors.absentContainer
                        }
                        val icon = when (cat) {
                            FacilityCategory.LAB -> Icons.Filled.Science
                            FacilityCategory.LIBRARY -> Icons.Filled.LocalLibrary
                            FacilityCategory.SPORTS -> Icons.Filled.SportsSoccer
                            FacilityCategory.TRANSPORT -> Icons.Filled.DirectionsBus
                            FacilityCategory.HALL -> Icons.Filled.EventSeat
                            FacilityCategory.CLASSROOM -> Icons.Filled.MeetingRoom
                        }
                        Card(
                            shape = MaterialTheme.shapes.medium,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier.testTag("facility_card_${facility.id}")
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(26.dp)
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(facility.name, style = MaterialTheme.typography.titleMedium)
                                        Text(
                                            "${cat.label} · ${facility.building} · Cap ${facility.capacity}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    StatusPill(status.label, fg, bg)
                                }
                                if (facility.bookedBy.isNotBlank() || facility.scheduleSlot.isNotBlank()) {
                                    Text(
                                        text = "Assigned: ${listOf(facility.bookedBy, facility.scheduleSlot).filter { it.isNotBlank() }.joinToString(" · ")}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    FilledTonalButton(
                                        onClick = { onBookFacility(facility) },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            when (status) {
                                                FacilityStatus.AVAILABLE -> "Book / Assign"
                                                FacilityStatus.OCCUPIED -> "Update / Release"
                                                FacilityStatus.MAINTENANCE -> "Manage Status"
                                            }
                                        )
                                    }
                                    IconButton(onClick = { onDeleteFacility(facility.id) }) {
                                        Icon(Icons.Filled.DeleteOutline, contentDescription = "Delete facility ${facility.name}")
                                    }
                                }
                            }
                        }
                    }
                }
            }

            CampusTab.FACULTY -> {
                if (data.faculty.isEmpty()) {
                    item {
                        EmptyCampusCard(
                            icon = Icons.Filled.Badge,
                            title = "No Faculty Members Yet",
                            description = "Maintain your directory of teachers, departments, subjects, and leave status.",
                            primaryActionLabel = "Add Faculty",
                            onPrimaryAction = onAddFaculty
                        )
                    }
                } else {
                    items(data.faculty, key = { it.id }) { member ->
                        val status = FacultyStatus.fromRaw(member.status)
                        val (fg, bg) = if (status == FacultyStatus.ACTIVE) {
                            statusColors.present to statusColors.presentContainer
                        } else {
                            statusColors.late to statusColors.lateContainer
                        }
                        Card(
                            shape = MaterialTheme.shapes.medium,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    StudentAvatar(
                                        fullName = member.fullName,
                                        accentColor = MaterialTheme.colorScheme.secondary,
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(member.fullName, style = MaterialTheme.typography.titleMedium)
                                        Text(
                                            "${member.department} · ${member.subject}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    StatusPill(status.label, fg, bg)
                                }
                                if (member.assignedClass.isNotBlank() || member.email.isNotBlank()) {
                                    Text(
                                        text = listOf(member.assignedClass, member.email).filter { it.isNotBlank() }.joinToString(" · "),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = { onToggleFacultyStatus(member) },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(if (status == FacultyStatus.ACTIVE) "Mark On Leave" else "Mark Active")
                                    }
                                    IconButton(onClick = { onDeleteFaculty(member.id) }) {
                                        Icon(Icons.Filled.DeleteOutline, contentDescription = "Delete faculty ${member.fullName}")
                                    }
                                }
                            }
                        }
                    }
                }
            }

            CampusTab.FEES -> {
                if (data.fees.isNotEmpty()) {
                    item {
                        val totalBilled = data.fees.sumOf { it.amount }
                        val totalPaid = data.fees.sumOf { it.paidAmount }
                        val outstanding = (totalBilled - totalPaid).coerceAtLeast(0.0)
                        Card(
                            shape = MaterialTheme.shapes.medium,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("Collected", style = MaterialTheme.typography.labelMedium)
                                    Text(
                                        currency.format(totalPaid),
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontFamily = JetBrainsMonoFontFamily,
                                        color = statusColors.present
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Outstanding", style = MaterialTheme.typography.labelMedium)
                                    Text(
                                        currency.format(outstanding),
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontFamily = JetBrainsMonoFontFamily,
                                        color = if (outstanding > 0) statusColors.absent else statusColors.present
                                    )
                                }
                            }
                        }
                    }
                }
                if (data.fees.isEmpty()) {
                    item {
                        EmptyCampusCard(
                            icon = Icons.Filled.AttachMoney,
                            title = "No Fee Invoices Yet",
                            description = "Create tuition, lab, or shuttle invoices for enrolled students and record payments.",
                            primaryActionLabel = "Create Invoice",
                            onPrimaryAction = onAddFeeInvoice
                        )
                    }
                } else {
                    items(data.fees, key = { it.id }) { invoice ->
                        val feeStatus = FeeStatus.fromRaw(invoice.status)
                        val (fg, bg) = when (feeStatus) {
                            FeeStatus.PAID -> statusColors.present to statusColors.presentContainer
                            FeeStatus.PARTIAL -> statusColors.late to statusColors.lateContainer
                            FeeStatus.PENDING -> MaterialTheme.colorScheme.primary to MaterialTheme.colorScheme.primaryContainer
                            FeeStatus.OVERDUE -> statusColors.absent to statusColors.absentContainer
                        }
                        Card(
                            shape = MaterialTheme.shapes.medium,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(invoice.studentName, style = MaterialTheme.typography.titleMedium)
                                        Text(
                                            "${invoice.title} · Due ${invoice.dueDateKey}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    StatusPill(feeStatus.label, fg, bg)
                                }
                                LinearProgressIndicator(
                                    progress = { if (invoice.amount <= 0) 0f else (invoice.paidAmount / invoice.amount).toFloat().coerceIn(0f, 1f) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(999.dp)),
                                    color = statusColors.present,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "${currency.format(invoice.paidAmount)} / ${currency.format(invoice.amount)}",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontFamily = JetBrainsMonoFontFamily
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (feeStatus != FeeStatus.PAID) {
                                            Button(
                                                onClick = { onRecordPayment(invoice) },
                                                shape = RoundedCornerShape(10.dp)
                                            ) {
                                                Icon(Icons.Filled.Payments, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(Modifier.size(6.dp))
                                                Text("Pay")
                                            }
                                        }
                                        IconButton(onClick = { onDeleteFeeInvoice(invoice.id) }) {
                                            Icon(Icons.Filled.DeleteOutline, contentDescription = "Delete fee invoice")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            CampusTab.NOTICES -> {
                if (data.notices.isEmpty()) {
                    item {
                        EmptyCampusCard(
                            icon = Icons.Filled.Campaign,
                            title = "No Notices Published",
                            description = "Share exam schedules, facility alerts, and school event circulars.",
                            primaryActionLabel = "Post Notice",
                            onPrimaryAction = onAddNotice
                        )
                    }
                } else {
                    items(data.notices, key = { it.id }) { notice ->
                        val cat = NoticeCategory.fromRaw(notice.category)
                        val (fg, bg) = when (cat) {
                            NoticeCategory.URGENT -> statusColors.absent to statusColors.absentContainer
                            NoticeCategory.EXAM -> statusColors.late to statusColors.lateContainer
                            NoticeCategory.EVENT -> MaterialTheme.colorScheme.secondary to MaterialTheme.colorScheme.secondaryContainer
                            NoticeCategory.GENERAL -> MaterialTheme.colorScheme.primary to MaterialTheme.colorScheme.primaryContainer
                        }
                        Card(
                            shape = MaterialTheme.shapes.medium,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    StatusPill(cat.label, fg, bg)
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(onClick = { onToggleNoticePin(notice) }) {
                                            Icon(
                                                imageVector = if (notice.pinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                                                contentDescription = if (notice.pinned) "Unpin notice" else "Pin notice",
                                                tint = if (notice.pinned) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        IconButton(onClick = { onDeleteNotice(notice.id) }) {
                                            Icon(Icons.Filled.DeleteOutline, contentDescription = "Delete notice")
                                        }
                                    }
                                }
                                Text(notice.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                Text(notice.body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("Audience: ${notice.audience}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(72.dp)) }
    }
}
