package by.iposdev.watchso.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import by.iposdev.watchso.data.OptionItem
import by.iposdev.watchso.data.UserSelection

@Composable
fun WearGroupPickerContent(
    faculties: List<OptionItem>,
    forms: List<OptionItem>,
    courses: List<OptionItem>,
    groups: List<OptionItem>,
    weeks: List<OptionItem>,
    currentSelection: UserSelection,
    isLoading: Boolean,
    onFacultySelected: (OptionItem) -> Unit,
    onFormSelected: (OptionItem) -> Unit,
    onCourseSelected: (OptionItem) -> Unit,
    onGroupSelected: (OptionItem) -> Unit,
    onWeekSelected: (OptionItem) -> Unit,
    onBackToSchedule: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = "Выбор группы",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center
        )

        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier
                    .size(24.dp)
                    .padding(vertical = 12.dp)
            )
        }

        // STEP 1: Faculty
        Text(
            text = "1. Факультет: ${currentSelection.facultyName.ifBlank { "выберите" }}",
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
        )
        faculties.forEach { faculty ->
            val isSelected = faculty.id == currentSelection.facultyId
            Chip(
                onClick = { onFacultySelected(faculty) },
                label = { Text(faculty.name, fontSize = 11.sp, maxLines = 2) },
                icon = if (isSelected) { { Icon(Icons.Outlined.Check, contentDescription = null, modifier = Modifier.size(ChipDefaults.IconSize)) } } else null,
                colors = if (isSelected) ChipDefaults.primaryChipColors() else ChipDefaults.secondaryChipColors(),
                modifier = Modifier.fillMaxWidth()
            )
        }

        // STEP 2: Course
        if (courses.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "2. Курс: ${currentSelection.courseName.ifBlank { "выберите" }}",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth()
            )
            courses.forEach { course ->
                val isSelected = course.id == currentSelection.courseId
                Chip(
                    onClick = { onCourseSelected(course) },
                    label = { Text(course.name, fontSize = 11.sp) },
                    icon = if (isSelected) { { Icon(Icons.Outlined.Check, contentDescription = null, modifier = Modifier.size(ChipDefaults.IconSize)) } } else null,
                    colors = if (isSelected) ChipDefaults.primaryChipColors() else ChipDefaults.secondaryChipColors(),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // STEP 3: Group
        if (groups.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "3. Группа: ${currentSelection.groupName.ifBlank { "выберите" }}",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth()
            )
            groups.forEach { group ->
                val isSelected = group.id == currentSelection.groupId
                Chip(
                    onClick = { onGroupSelected(group) },
                    label = { Text(group.name, fontSize = 11.sp) },
                    icon = if (isSelected) { { Icon(Icons.Outlined.Check, contentDescription = null, modifier = Modifier.size(ChipDefaults.IconSize)) } } else null,
                    colors = if (isSelected) ChipDefaults.primaryChipColors() else ChipDefaults.secondaryChipColors(),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // STEP 4: Week (optional)
        if (weeks.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "4. Неделя: ${currentSelection.weekName.ifBlank { "текущая" }}",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth()
            )
            weeks.take(5).forEach { week ->
                val isSelected = week.id == currentSelection.weekId
                Chip(
                    onClick = { onWeekSelected(week) },
                    label = { Text(week.name, fontSize = 10.sp, maxLines = 1) },
                    icon = if (isSelected) { { Icon(Icons.Outlined.Check, contentDescription = null, modifier = Modifier.size(ChipDefaults.IconSize)) } } else null,
                    colors = if (isSelected) ChipDefaults.primaryChipColors() else ChipDefaults.secondaryChipColors(),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Chip(
            onClick = onBackToSchedule,
            label = { Text("Назад к расписанию", fontSize = 11.sp) },
            icon = { Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = null, modifier = Modifier.size(ChipDefaults.IconSize)) },
            colors = ChipDefaults.secondaryChipColors(),
            modifier = Modifier.fillMaxWidth()
        )
    }
}
