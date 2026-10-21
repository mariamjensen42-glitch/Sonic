@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package com.cycling.presentation.equalizer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ListItem
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import com.cycling.core.ui.theme.SonSpacing
import com.cycling.domain.model.AppliedAutoEq
import com.cycling.domain.model.AutoEqEntry
import com.cycling.domain.model.AutoEqFormFactor
import com.cycling.presentation.R

/**
 * 耳机校正档案选择面板。
 *
 * 目录里 8850 条按型号名搜索，选中即生效 —— 不做"预览确认"，因为校正本来就是听了才知道
 * 合不合口味，听完随时能取消，多一步确认只是多一次点击。
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AutoEqPickerSheet(
    applied: AppliedAutoEq?,
    catalogSize: Int,
    query: String,
    formFactor: AutoEqFormFactor?,
    results: List<AutoEqEntry>,
    isSearching: Boolean,
    onQueryChange: (String) -> Unit,
    onFormFactorChange: (AutoEqFormFactor?) -> Unit,
    onSelect: (String) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = SonSpacing.large)
        ) {
            Text(
                text = stringResource(R.string.autoeq_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(
                    start = SonSpacing.large,
                    end = SonSpacing.large,
                    bottom = SonSpacing.extraSmall
                )
            )
            Text(
                text = stringResource(R.string.autoeq_subtitle, catalogSize),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = SonSpacing.large)
            )
            Spacer(modifier = Modifier.height(SonSpacing.medium))

            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                placeholder = { Text(text = stringResource(R.string.autoeq_search_hint)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = SonSpacing.large)
            )

            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = SonSpacing.large,
                        end = SonSpacing.large,
                        top = SonSpacing.small
                    ),
                horizontalArrangement = Arrangement.spacedBy(SonSpacing.small)
            ) {
                FormFactorChip(
                    label = stringResource(R.string.autoeq_filter_all),
                    selected = formFactor == null,
                    onClick = { onFormFactorChange(null) }
                )
                FormFactorChip(
                    label = stringResource(R.string.autoeq_filter_in_ear),
                    selected = formFactor == AutoEqFormFactor.IN_EAR,
                    onClick = { onFormFactorChange(AutoEqFormFactor.IN_EAR) }
                )
                FormFactorChip(
                    label = stringResource(R.string.autoeq_filter_earbud),
                    selected = formFactor == AutoEqFormFactor.EARBUD,
                    onClick = { onFormFactorChange(AutoEqFormFactor.EARBUD) }
                )
                FormFactorChip(
                    label = stringResource(R.string.autoeq_filter_over_ear),
                    selected = formFactor == AutoEqFormFactor.OVER_EAR,
                    onClick = { onFormFactorChange(AutoEqFormFactor.OVER_EAR) }
                )
            }

            if (applied != null) {
                ListItem(
                    content = {
                        Text(
                            text = stringResource(R.string.autoeq_applied, applied.name),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    supportingContent = {
                        Text(
                            text = stringResource(
                                R.string.autoeq_applied_detail,
                                applied.source,
                                applied.preampDb
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    trailingContent = {
                        TextButton(onClick = onClear) {
                            Text(text = stringResource(R.string.autoeq_clear))
                        }
                    }
                )
            }

            when {
                isSearching -> Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = SonSpacing.extraLarge),
                    contentAlignment = Alignment.Center
                ) {
                    LoadingIndicator()
                }

                results.isEmpty() -> Text(
                    text = stringResource(R.string.autoeq_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(SonSpacing.large)
                )

                else -> LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = RESULTS_MAX_HEIGHT)
                ) {
                    items(results, key = { it.id }) { entry ->
                        ListItem(
                            content = {
                                Text(
                                    text = entry.name,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            supportingContent = {
                                Text(
                                    text = stringResource(
                                        R.string.autoeq_entry_detail,
                                        entry.source,
                                        formFactorLabel(entry.formFactor)
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            trailingContent = {
                                if (entry.id == applied?.id) {
                                    Text(
                                        text = stringResource(R.string.autoeq_in_use),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            },
                            onClick = { onSelect(entry.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FormFactorChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(text = label) }
    )
}

@Composable
private fun formFactorLabel(formFactor: AutoEqFormFactor): String = stringResource(
    when (formFactor) {
        AutoEqFormFactor.IN_EAR -> R.string.autoeq_filter_in_ear
        AutoEqFormFactor.EARBUD -> R.string.autoeq_filter_earbud
        AutoEqFormFactor.OVER_EAR -> R.string.autoeq_filter_over_ear
    }
)

/** 结果列表上限：约 6 屏条目，再多也没人翻。 */
private val RESULTS_MAX_HEIGHT: Dp = SonSpacing.huge * 6
