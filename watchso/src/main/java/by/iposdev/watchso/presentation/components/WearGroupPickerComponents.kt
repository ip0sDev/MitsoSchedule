package by.iposdev.watchso.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.School
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.lazy.ScalingLazyListScope
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import by.iposdev.watchso.data.OptionItem
import by.iposdev.watchso.data.UserSelection
import by.iposdev.watchso.presentation.viewmodel.WearPickerStep

/**
 * Renders the multi-step group picker directly into ScalingLazyListScope on Wear OS.
 * This guarantees proper circular layout scaling, edge-snapping, rotary scroll and
 * eliminates viewport clipping bugs caused by embedding tall columns in a single item.
 */
fun ScalingLazyListScope.wearGroupPickerItems(
    step: WearPickerStep,
    faculties: List<OptionItem>,
    courses: List<OptionItem>,
    groups: List<OptionItem>,
    currentSelection: UserSelection,
    isLoading: Boolean,
    onFacultySelected: (OptionItem) -> Unit,
    onCourseSelected: (OptionItem) -> Unit,
    onGroupSelected: (OptionItem) -> Unit,
    onStepChange: (WearPickerStep) -> Unit,
    onRetryFaculties: () -> Unit,
    onBackToSchedule: () -> Unit
) {
    when (step) {
        WearPickerStep.FACULTY -> {
            item(key = "picker_faculty_header") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Факультет",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Шаг 1 из 3",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }

            if (isLoading && faculties.isEmpty()) {
                item(key = "picker_loading") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    }
                }
            } else if (faculties.isEmpty()) {
                item(key = "picker_empty_fac") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Не удалось загрузить факультеты",
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Chip(
                            onClick = onRetryFaculties,
                            label = { Text("Повторить", fontSize = 11.sp) },
                            icon = { Icon(Icons.Outlined.Refresh, contentDescription = null, modifier = Modifier.size(ChipDefaults.IconSize)) },
                            colors = ChipDefaults.primaryChipColors(),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            } else {
                items(
                    items = faculties,
                    key = { "fac_${it.id}" }
                ) { faculty ->
                    val isSelected = faculty.id == currentSelection.facultyId
                    Chip(
                        onClick = { onFacultySelected(faculty) },
                        label = {
                            Text(
                                text = faculty.name,
                                fontSize = 11.sp,
                                maxLines = 2,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        icon = if (isSelected) {
                            { Icon(Icons.Outlined.Check, contentDescription = null, modifier = Modifier.size(ChipDefaults.IconSize)) }
                        } else {
                            { Icon(Icons.Outlined.School, contentDescription = null, modifier = Modifier.size(ChipDefaults.IconSize)) }
                        },
                        colors = if (isSelected) ChipDefaults.primaryChipColors() else ChipDefaults.secondaryChipColors(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            if (currentSelection.isComplete) {
                item(key = "picker_fac_cancel") {
                    Spacer(modifier = Modifier.height(4.dp))
                    Chip(
                        onClick = onBackToSchedule,
                        label = { Text("Отмена", fontSize = 11.sp) },
                        icon = { Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = null, modifier = Modifier.size(ChipDefaults.IconSize)) },
                        colors = ChipDefaults.secondaryChipColors(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        WearPickerStep.COURSE -> {
            item(key = "picker_course_header") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Курс",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = currentSelection.facultyName.ifBlank { "Шаг 2 из 3" },
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        maxLines = 1
                    )
                }
            }

            item(key = "picker_course_back") {
                Chip(
                    onClick = { onStepChange(WearPickerStep.FACULTY) },
                    label = { Text("← К факультетам", fontSize = 11.sp) },
                    colors = ChipDefaults.secondaryChipColors(),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (isLoading) {
                item(key = "picker_course_loading") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    }
                }
            } else if (courses.isEmpty()) {
                item(key = "picker_course_empty") {
                    Text(
                        text = "Курсы не найдены",
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                    )
                }
            } else {
                items(
                    items = courses,
                    key = { "course_${it.id}" }
                ) { course ->
                    val isSelected = course.id == currentSelection.courseId
                    Chip(
                        onClick = { onCourseSelected(course) },
                        label = {
                            Text(
                                text = course.name,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        icon = if (isSelected) {
                            { Icon(Icons.Outlined.Check, contentDescription = null, modifier = Modifier.size(ChipDefaults.IconSize)) }
                        } else null,
                        colors = if (isSelected) ChipDefaults.primaryChipColors() else ChipDefaults.secondaryChipColors(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            if (currentSelection.isComplete) {
                item(key = "picker_course_cancel") {
                    Spacer(modifier = Modifier.height(4.dp))
                    Chip(
                        onClick = onBackToSchedule,
                        label = { Text("Отмена", fontSize = 11.sp) },
                        icon = { Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = null, modifier = Modifier.size(ChipDefaults.IconSize)) },
                        colors = ChipDefaults.secondaryChipColors(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        WearPickerStep.GROUP -> {
            item(key = "picker_group_header") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Группа",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "${currentSelection.facultyName} • ${currentSelection.courseName}",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        maxLines = 1
                    )
                }
            }

            item(key = "picker_group_back") {
                Chip(
                    onClick = { onStepChange(WearPickerStep.COURSE) },
                    label = { Text("← К курсам", fontSize = 11.sp) },
                    colors = ChipDefaults.secondaryChipColors(),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (isLoading) {
                item(key = "picker_group_loading") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    }
                }
            } else if (groups.isEmpty()) {
                item(key = "picker_group_empty") {
                    Text(
                        text = "Группы не найдены",
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                    )
                }
            } else {
                items(
                    items = groups,
                    key = { "grp_${it.id}" }
                ) { group ->
                    val isSelected = group.id == currentSelection.groupId
                    Chip(
                        onClick = { onGroupSelected(group) },
                        label = {
                            Text(
                                text = group.name,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        icon = if (isSelected) {
                            { Icon(Icons.Outlined.Check, contentDescription = null, modifier = Modifier.size(ChipDefaults.IconSize)) }
                        } else {
                            { Icon(Icons.Outlined.Group, contentDescription = null, modifier = Modifier.size(ChipDefaults.IconSize)) }
                        },
                        colors = if (isSelected) ChipDefaults.primaryChipColors() else ChipDefaults.secondaryChipColors(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            if (currentSelection.isComplete) {
                item(key = "picker_group_cancel") {
                    Spacer(modifier = Modifier.height(4.dp))
                    Chip(
                        onClick = onBackToSchedule,
                        label = { Text("Отмена", fontSize = 11.sp) },
                        icon = { Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = null, modifier = Modifier.size(ChipDefaults.IconSize)) },
                        colors = ChipDefaults.secondaryChipColors(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
