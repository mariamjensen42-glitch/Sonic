@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.cycling.presentation.equalizer

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.cycling.core.ui.apple.AppleChip
import com.cycling.core.ui.apple.AppleSearchField
import com.cycling.core.ui.apple.AppleSeparator
import com.cycling.core.ui.apple.AppleSheet
import com.cycling.core.ui.apple.AppleSheetRow
import com.cycling.core.ui.apple.AppleSheetTitle
import com.cycling.core.ui.apple.AppleTheme
import com.cycling.core.ui.apple.AppleType
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
    AppleSheet(onDismiss = onDismiss) {
        AppleSheetTitle(
            title = stringResource(R.string.autoeq_title),
            subtitle = stringResource(R.string.autoeq_subtitle, catalogSize)
        )

        AppleSearchField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = stringResource(R.string.autoeq_search_hint),
            onClear = { onQueryChange("") },
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(SonSpacing.small)
        ) {
            AppleChip(
                label = stringResource(R.string.autoeq_filter_all),
                selected = formFactor == null,
                onClick = { onFormFactorChange(null) }
            )
            AppleChip(
                label = stringResource(R.string.autoeq_filter_in_ear),
                selected = formFactor == AutoEqFormFactor.IN_EAR,
                onClick = { onFormFactorChange(AutoEqFormFactor.IN_EAR) }
            )
            AppleChip(
                label = stringResource(R.string.autoeq_filter_earbud),
                selected = formFactor == AutoEqFormFactor.EARBUD,
                onClick = { onFormFactorChange(AutoEqFormFactor.EARBUD) }
            )
            AppleChip(
                label = stringResource(R.string.autoeq_filter_over_ear),
                selected = formFactor == AutoEqFormFactor.OVER_EAR,
                onClick = { onFormFactorChange(AutoEqFormFactor.OVER_EAR) }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (applied != null) {
            AppleSheetRow(
                title = stringResource(R.string.autoeq_applied, applied.name),
                subtitle = stringResource(
                    R.string.autoeq_applied_detail,
                    applied.source,
                    applied.preampDb
                ),
                trailing = {
                    val interaction = remember { MutableInteractionSource() }
                    Text(
                        text = stringResource(R.string.autoeq_clear),
                        style = AppleType.body,
                        color = AppleTheme.colors.systemRed,
                        modifier = Modifier
                            .clickable(
                                interactionSource = interaction,
                                indication = null,
                                onClick = onClear
                            )
                            .padding(vertical = 4.dp, horizontal = 4.dp)
                    )
                }
            )
            AppleSeparator()
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
                style = AppleType.body,
                color = AppleTheme.colors.secondaryLabel,
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
                    AppleSheetRow(
                        title = entry.name,
                        subtitle = stringResource(
                            R.string.autoeq_entry_detail,
                            entry.source,
                            formFactorLabel(entry.formFactor)
                        ),
                        onClick = { onSelect(entry.id) },
                        trailing = {
                            if (entry.id == applied?.id) {
                                Text(
                                    text = stringResource(R.string.autoeq_in_use),
                                    style = AppleType.caption1Emphasized,
                                    color = AppleTheme.colors.key
                                )
                            }
                        }
                    )
                }
            }
        }
    }
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
