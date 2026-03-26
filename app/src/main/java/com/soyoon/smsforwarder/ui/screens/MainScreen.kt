package com.soyoon.smsforwarder.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.soyoon.smsforwarder.model.ForwardLog
import com.soyoon.smsforwarder.model.ForwardRule
import com.soyoon.smsforwarder.repository.SettingsRepository
import com.soyoon.smsforwarder.util.PhoneNumberUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MainScreen(repository: SettingsRepository) {
    var forwardingEnabled by remember { mutableStateOf(repository.isForwardingEnabled()) }
    var rules by remember { mutableStateOf(repository.getForwardRules()) }
    var logs by remember { mutableStateOf(repository.getForwardLogs()) }
    var showAddDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "SMS Forwarder",
                style = MaterialTheme.typography.headlineMedium
            )
        }

        item { NoticeSection() }

        item {
            ForwardingToggle(
                enabled = forwardingEnabled,
                onToggle = { enabled ->
                    forwardingEnabled = enabled
                    repository.setForwardingEnabled(enabled)
                }
            )
        }

        item {
            SectionHeader("전달 규칙")
        }

        items(rules.size) { index ->
            ForwardRuleCard(
                rule = rules[index],
                onToggle = { enabled ->
                    val updated = rules.toMutableList()
                    updated[index] = updated[index].copy(enabled = enabled)
                    rules = updated
                    repository.saveForwardRules(updated)
                },
                onDelete = {
                    val updated = rules.toMutableList()
                    updated.removeAt(index)
                    rules = updated
                    repository.saveForwardRules(updated)
                }
            )
        }

        item {
            OutlinedButton(
                onClick = { showAddDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("+ 규칙 추가")
            }
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
            SectionHeader("전달 로그")
        }

        if (logs.isEmpty()) {
            item {
                Text(
                    text = "전달 로그가 없습니다.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(logs.sortedByDescending { it.timestamp }) { log ->
                ForwardLogItem(log)
            }
            item {
                TextButton(
                    onClick = {
                        repository.clearForwardLogs()
                        logs = emptyList()
                    }
                ) {
                    Text("로그 삭제")
                }
            }
        }
    }

    if (showAddDialog) {
        AddRuleDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { rule ->
                val updated = rules + rule
                rules = updated
                repository.saveForwardRules(updated)
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun NoticeSection() {
    var expanded by remember { mutableStateOf(false) }
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "일부 기기에서 SMS 수신이 제한될 수 있습니다. 정상 동작을 위해 설정을 변경해주세요.",
                style = MaterialTheme.typography.bodySmall
            )
            TextButton(
                onClick = { expanded = !expanded },
                modifier = Modifier.padding(top = 4.dp)
            ) {
                Text(
                    text = if (expanded) "▼ 접기" else "▶ 상세 설정 보기",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier.padding(top = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 한국어 안내
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "한국어",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "1. 배터리 최적화 해제\n" +
                                "   설정 → 앱 → 해당 앱 → 배터리 → \"제한 없음\" 선택",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "2. 사용하지 않는 앱 관리 해제\n" +
                                "   설정 → 앱 → 해당 앱 → \"사용하지 않는 앱 관리\" 토글 끄기",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "3. 자동 절전 예외 앱 등록\n" +
                                "   설정 → 배터리 → 백그라운드 앱 사용 제한 → \"자동 절전 예외 앱\"에 추가",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    HorizontalDivider()
                    // English guide
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "English",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "1. Disable Battery Optimization\n" +
                                "   Settings → Apps → Select the app → Battery → Choose \"Unrestricted\"",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "2. Disable Manage app if unused\n" +
                                "   Settings → Apps → Select the app → Turn off \"Remove permissions if app is unused\"",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "3. Add to Never auto sleeping apps\n" +
                                "   Settings → Battery → Background usage limits → Add the app to \"Never auto sleeping apps\"",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ForwardingToggle(enabled: Boolean, onToggle: (Boolean) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (enabled) "전달 기능 ON" else "전달 기능 OFF",
                style = MaterialTheme.typography.titleMedium
            )
            Switch(checked = enabled, onCheckedChange = onToggle)
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Column {
        HorizontalDivider()
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun ForwardRuleCard(
    rule: ForwardRule,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = "발신번호: ${rule.senderNumber}", style = MaterialTheme.typography.bodyMedium)
            Text(
                text = "키워드: ${rule.keyword.ifEmpty { "(전체)" }}",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(text = "전달대상: ${rule.forwardTo}", style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(
                        checked = rule.enabled,
                        onCheckedChange = onToggle
                    )
                    Text(
                        text = if (rule.enabled) "활성" else "비활성",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
                TextButton(onClick = onDelete) {
                    Text("삭제")
                }
            }
        }
    }
}

@Composable
private fun AddRuleDialog(onDismiss: () -> Unit, onConfirm: (ForwardRule) -> Unit) {
    var senderNumber by remember { mutableStateOf("") }
    var keyword by remember { mutableStateOf("") }
    var forwardTo by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("규칙 추가") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = senderNumber,
                    onValueChange = { senderNumber = it },
                    label = { Text("발신번호") },
                    placeholder = { Text("01012345678") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = keyword,
                    onValueChange = { keyword = it },
                    label = { Text("키워드 (선택)") },
                    placeholder = { Text("비워두면 전체 전달") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = forwardTo,
                    onValueChange = { forwardTo = it },
                    label = { Text("전달 대상 번호") },
                    placeholder = { Text("01098765432") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val phonePattern = Regex("^[0-9+\\-]+$")
                when {
                    senderNumber.isBlank() -> {
                        errorMessage = "발신번호를 입력하세요."
                    }
                    !phonePattern.matches(senderNumber) -> {
                        errorMessage = "발신번호 형식이 올바르지 않습니다."
                    }
                    forwardTo.isBlank() -> {
                        errorMessage = "전달 대상 번호를 입력하세요."
                    }
                    !phonePattern.matches(forwardTo) -> {
                        errorMessage = "전달 대상 번호 형식이 올바르지 않습니다."
                    }
                    PhoneNumberUtils.normalize(senderNumber) ==
                        PhoneNumberUtils.normalize(forwardTo) -> {
                        errorMessage = "발신번호와 전달 대상이 같으면 루프가 발생합니다."
                    }
                    else -> {
                        onConfirm(
                            ForwardRule(
                                senderNumber = senderNumber.trim(),
                                keyword = keyword.trim(),
                                forwardTo = forwardTo.trim()
                            )
                        )
                    }
                }
            }) {
                Text("추가")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("취소")
            }
        }
    )
}

@Composable
private fun ForwardLogItem(log: ForwardLog) {
    val timeFormat = SimpleDateFormat("MM/dd HH:mm", Locale.getDefault())
    val timeStr = timeFormat.format(Date(log.timestamp))
    val statusStr = if (log.success) "성공" else "실패"

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (log.success)
                MaterialTheme.colorScheme.surfaceVariant
            else
                MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "$timeStr  ${log.senderNumber}",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = statusStr,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (log.success)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.error
                )
            }
            Text(
                text = "→ ${log.forwardedTo}",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = log.messageBody,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
