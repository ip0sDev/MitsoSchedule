package mitsoschedule.app.ui.components

import androidx.compose.ui.res.stringResource
import mitsoschedule.app.R
import mitsoschedule.app.ui.haptics.LocalBiolumeHaptics
import mitsoschedule.app.ui.theme.BiolumeTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import mitsoschedule.core.model.OptionItem
import mitsoschedule.core.model.UserSelection

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun GroupSelectionBottomSheet(
    faculties: List<OptionItem>,
    forms: List<OptionItem>,
    courses: List<OptionItem>,
    groups: List<OptionItem>,
    isLoadingOptions: Boolean,
    initialSelection: UserSelection,
    onFacultyChanged: (OptionItem) -> Unit,
    onFormChanged: (OptionItem) -> Unit,
    onCourseChanged: (OptionItem) -> Unit,
    onGroupChanged: (OptionItem) -> Unit,
    onApplySelection: (UserSelection) -> Unit,
    onDismiss: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    var draftSelection by remember(initialSelection) { mutableStateOf(initialSelection) }
    var groupSearchQuery by remember { mutableStateOf("") }

    val haptics = LocalBiolumeHaptics.current
    val depth = BiolumeTheme.depth
    val chipShape = RoundedCornerShape(14.dp)
    // §4.2 + §10: выбор — плотная selectionFill. primaryContainer в этой роли запрещён:
    // он полупрозрачный, поверх разных фонов даёт разный тон и путается с сигнальным слоем.
    val chipColors = FilterChipDefaults.filterChipColors(
        selectedContainerColor = depth.selectionFill,
        selectedLabelColor = MaterialTheme.colorScheme.onSurface,
        selectedLeadingIconColor = MaterialTheme.colorScheme.primary,
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            // Sheet Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.selection_title),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.selection_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(onClick = {
                    haptics.click()
                    onDismiss()
                }) {
                    Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.close))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (isLoadingOptions && faculties.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 1. Факультет
                    item {
                        SelectionSection(
                            title = stringResource(R.string.step_faculty),
                            icon = Icons.Outlined.AccountBalance
                        ) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                faculties.forEach { faculty ->
                                    val isSelected = draftSelection.facultyId == faculty.id
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            haptics.tick()
                                            draftSelection = draftSelection.copy(
                                                facultyId = faculty.id,
                                                facultyName = faculty.name,
                                                courseId = "",
                                                courseName = "",
                                                groupId = "",
                                                groupName = ""
                                            )
                                            onFacultyChanged(faculty)
                                        },
                                        label = { Text(faculty.name) },
                                        leadingIcon = if (isSelected) {
                                            { Icon(Icons.Outlined.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                        } else null,
                                        shape = RoundedCornerShape(14.dp),
                                        colors = chipColors
                                    )
                                }
                            }
                        }
                    }

                    // 2. Форма обучения (если есть выбор или по умолчанию)
                    if (forms.isNotEmpty() && forms.size > 1) {
                        item {
                            SelectionSection(
                                title = stringResource(R.string.step_form),
                                icon = Icons.Outlined.School
                            ) {
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    forms.forEach { form ->
                                        val isSelected = draftSelection.formId == form.id
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = {
                                                haptics.tick()
                                                draftSelection = draftSelection.copy(
                                                    formId = form.id,
                                                    formName = form.name,
                                                    courseId = "",
                                                    courseName = "",
                                                    groupId = "",
                                                    groupName = ""
                                                )
                                                onFormChanged(form)
                                            },
                                            label = { Text(form.name) },
                                            leadingIcon = if (isSelected) {
                                                { Icon(Icons.Outlined.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                            } else null,
                                            shape = RoundedCornerShape(14.dp),
                                            colors = chipColors
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 3. Курс
                    if (draftSelection.facultyId.isNotBlank()) {
                        item {
                            SelectionSection(
                                title = stringResource(R.string.step_course),
                                icon = Icons.Outlined.DateRange
                            ) {
                                if (isLoadingOptions && courses.isEmpty()) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                } else if (courses.isEmpty()) {
                                    Text(stringResource(R.string.courses_not_found), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                } else {
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        courses.forEach { course ->
                                            val isSelected = draftSelection.courseId == course.id
                                            FilterChip(
                                                selected = isSelected,
                                                onClick = {
                                                    haptics.tick()
                                                    draftSelection = draftSelection.copy(
                                                        courseId = course.id,
                                                        courseName = course.name,
                                                        groupId = "",
                                                        groupName = ""
                                                    )
                                                    onCourseChanged(course)
                                                },
                                                label = { Text(course.name) },
                                                leadingIcon = if (isSelected) {
                                                    { Icon(Icons.Outlined.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                                } else null,
                                                shape = RoundedCornerShape(14.dp),
                                                colors = chipColors
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 4. Группа
                    if (draftSelection.courseId.isNotBlank()) {
                        item {
                            SelectionSection(
                                title = stringResource(R.string.step_group),
                                icon = Icons.Outlined.Group
                            ) {
                                if (isLoadingOptions && groups.isEmpty()) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                } else if (groups.isEmpty()) {
                                    Text(stringResource(R.string.groups_not_found), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                } else {
                                    Column {
                                        if (groups.size > 6) {
                                            OutlinedTextField(
                                                value = groupSearchQuery,
                                                onValueChange = { groupSearchQuery = it },
                                                placeholder = { Text(stringResource(R.string.group_search_hint)) },
                                                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                                trailingIcon = if (groupSearchQuery.isNotEmpty()) {
                                                    {
                                                        IconButton(onClick = {
                                                            haptics.tick()
                                                            groupSearchQuery = ""
                                                        }) {
                                                            Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.clear), modifier = Modifier.size(16.dp))
                                                        }
                                                    }
                                                } else null,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(bottom = 10.dp),
                                                shape = RoundedCornerShape(16.dp),
                                                singleLine = true,
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                                                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow
                                                )
                                            )
                                        }

                                        val filteredGroups = groups.filter {
                                            groupSearchQuery.isBlank() || it.name.contains(groupSearchQuery, ignoreCase = true)
                                        }

                                        FlowRow(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            filteredGroups.forEach { group ->
                                                val isSelected = draftSelection.groupId == group.id
                                                FilterChip(
                                                    selected = isSelected,
                                                    onClick = {
                                                        haptics.tick()
                                                        draftSelection = draftSelection.copy(
                                                            groupId = group.id,
                                                            groupName = group.name
                                                        )
                                                        onGroupChanged(group)
                                                    },
                                                    label = { Text(group.name, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                                    leadingIcon = if (isSelected) {
                                                        { Icon(Icons.Outlined.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                                    } else null,
                                                    shape = RoundedCornerShape(14.dp),
                                                    colors = chipColors
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Apply button
                Button(
                    onClick = {
                        haptics.mediumClick()
                        onApplySelection(draftSelection)
                        onDismiss()
                    },
                    enabled = draftSelection.isComplete,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(100.dp)
                ) {
                    Text(
                        text = if (draftSelection.isComplete) stringResource(R.string.show_schedule) else stringResource(R.string.select_group),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun SelectionSection(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            content()
        }
    }
}
