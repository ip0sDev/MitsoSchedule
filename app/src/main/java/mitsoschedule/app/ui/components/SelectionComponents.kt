package mitsoschedule.app.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import mitsoschedule.app.ui.theme.biolumeNeumorphicRaised
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import mitsoschedule.app.data.OptionItem
import mitsoschedule.app.data.UserSelection

@Composable
fun GroupHeaderCard(
    currentSelection: UserSelection,
    weeksList: List<OptionItem>,
    onOpenSelectionClick: () -> Unit,
    onWeekSelected: (OptionItem) -> Unit,
    modifier: Modifier = Modifier,
    canGoPrevious: Boolean = false,
    canGoNext: Boolean = false,
    onPreviousWeekClick: () -> Unit = {},
    onNextWeekClick: () -> Unit = {}
) {
    var weekDropdownExpanded by remember { mutableStateOf(false) }
    val isDark = isSystemInDarkTheme()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .biolumeNeumorphicRaised(shape = RoundedCornerShape(26.dp), isDark = isDark)
            .background(MaterialTheme.colorScheme.surfaceContainer, shape = RoundedCornerShape(26.dp))
            .padding(18.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        modifier = Modifier.size(46.dp),
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Outlined.Group,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column {
                        Text(
                            text = if (currentSelection.groupName.isNotBlank()) currentSelection.groupName else "Группа не выбрана",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (currentSelection.facultyName.isNotBlank()) "${currentSelection.facultyName} • ${currentSelection.courseName}" else "Нажмите для выбора",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                FilledTonalButton(
                    onClick = onOpenSelectionClick,
                    shape = RoundedCornerShape(100.dp),
                    contentPadding = ButtonDefaults.ButtonWithIconContentPadding
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Edit,
                        contentDescription = "Изменить группу",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (currentSelection.isComplete) "Сменить" else "Выбрать",
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }

            // Weeks selector chip if weeks are available
            if (weeksList.isNotEmpty()) {
                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start
                ) {
                    IconButton(
                        onClick = onPreviousWeekClick,
                        enabled = canGoPrevious,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Предыдущая неделя",
                            tint = if (canGoPrevious) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.38f),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(2.dp))

                    Box {
                        Surface(
                            onClick = { weekDropdownExpanded = true },
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            shape = RoundedCornerShape(100.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.DateRange,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = currentSelection.weekName.ifBlank { "Период обучения" },
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Outlined.ArrowDropDown,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = weekDropdownExpanded,
                            onDismissRequest = { weekDropdownExpanded = false }
                        ) {
                            weeksList.forEach { weekOption ->
                                DropdownMenuItem(
                                    text = { Text(weekOption.name) },
                                    onClick = {
                                        onWeekSelected(weekOption)
                                        weekDropdownExpanded = false
                                    },
                                    leadingIcon = {
                                        if (weekOption.id == currentSelection.weekId) {
                                            Icon(Icons.Outlined.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(2.dp))

                    IconButton(
                        onClick = onNextWeekClick,
                        enabled = canGoNext,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                            contentDescription = "Следующая неделя",
                            tint = if (canGoNext) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.38f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

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
                        text = "Выбор расписания",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Укажите факультет, курс и группу",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(onClick = onDismiss) {
                    Icon(Icons.Outlined.Close, contentDescription = "Закрыть")
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
                            title = "1. Факультет",
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
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    )
                                }
                            }
                        }
                    }

                    // 2. Форма обучения (если есть выбор или по умолчанию)
                    if (forms.isNotEmpty() && forms.size > 1) {
                        item {
                            SelectionSection(
                                title = "2. Форма обучения",
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
                                            shape = RoundedCornerShape(14.dp)
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
                                title = "3. Курс",
                                icon = Icons.Outlined.DateRange
                            ) {
                                if (isLoadingOptions && courses.isEmpty()) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                } else if (courses.isEmpty()) {
                                    Text("Курсы не найдены", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                                    selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                                                )
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
                                title = "4. Группа",
                                icon = Icons.Outlined.Group
                            ) {
                                if (isLoadingOptions && groups.isEmpty()) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                } else if (groups.isEmpty()) {
                                    Text("Группы не найдены", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                } else {
                                    Column {
                                        if (groups.size > 6) {
                                            OutlinedTextField(
                                                value = groupSearchQuery,
                                                onValueChange = { groupSearchQuery = it },
                                                placeholder = { Text("Поиск группы (напр. 2423)...") },
                                                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                                trailingIcon = if (groupSearchQuery.isNotEmpty()) {
                                                    {
                                                        IconButton(onClick = { groupSearchQuery = "" }) {
                                                            Icon(Icons.Outlined.Close, contentDescription = "Очистить", modifier = Modifier.size(16.dp))
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
                                                    colors = FilterChipDefaults.filterChipColors(
                                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                                    )
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
                        text = if (draftSelection.isComplete) "Показать расписание" else "Выберите группу",
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
