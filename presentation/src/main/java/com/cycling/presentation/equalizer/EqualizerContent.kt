package com.cycling.presentation.equalizer

import androidx.annotation.StringRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cycling.core.ui.theme.LocalSonExtendedColors
import com.cycling.core.ui.theme.SonLayout
import com.cycling.core.ui.theme.SonSizes
import com.cycling.core.ui.theme.SonSpacing
import com.cycling.domain.audio.BiquadCoefficients
import com.cycling.domain.model.EqFilterSpec
import com.cycling.domain.model.EqualizerBands
import com.cycling.domain.model.EqualizerPreset
import com.cycling.presentation.R
import kotlin.math.pow

/**
 * 均衡器页内容。
 *
 * 结构：开关 → 响应曲线 → 预设 → 逐频段滑杆 → 预放大 → 复位，
 * 另加一个"播放页律动"开关控制节拍可视化（这两项本可分开，这里合并成一处
 * 音频外观设置，减少一个二级页）。
 *
 * 作为设置页的二级目的地渲染（外层 `SettingsScreen` 的 Scaffold 提供顶栏与返回），所以这里
 * 不自己建 Scaffold，只负责可滚动内容。
 */
@Composable
fun EqualizerContent(
    viewModel: EqualizerViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    // 曲线画的是实际生效的滤波器链：耳机校正在前，10 段图形增益在后。
    val filters = remember(state.bandGains, state.autoEq) {
        buildList {
            state.autoEq?.filters?.let { addAll(it) }
            addAll(EqualizerBands.filtersFor(state.bandGains))
        }
    }
    val effectivePreampDb = state.preampDb + (state.autoEq?.preampDb ?: 0f)

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = SonLayout.bottomBarClearance)
    ) {
        SwitchRow(
            title = stringResource(R.string.equalizer_enable),
            subtitle = stringResource(R.string.equalizer_enable_subtitle),
            checked = state.enabled,
            onCheckedChange = { viewModel.handleIntent(EqualizerIntent.SetEnabled(it)) }
        )

        HorizontalDivider(modifier = Modifier.padding(horizontal = SonLayout.screenMargin))

        SectionTitle(stringResource(R.string.equalizer_response))
        EqualizerCurve(
            filters = filters,
            preampDb = effectivePreampDb,
            enabled = state.enabled,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = SonLayout.screenMargin)
                .height(CURVE_HEIGHT)
        )

        SectionTitle(stringResource(R.string.autoeq_section))
        AutoEqRow(
            appliedName = state.autoEq?.name,
            detail = state.autoEq?.let {
                stringResource(R.string.autoeq_applied_detail, it.source, it.preampDb)
            } ?: stringResource(R.string.autoeq_none_subtitle),
            onPick = { viewModel.handleIntent(EqualizerIntent.OpenAutoEqPicker) }
        )

        SectionTitle(stringResource(R.string.equalizer_presets))
        PresetChips(
            selected = state.preset,
            chipEnabled = state.enabled,
            onSelect = { viewModel.handleIntent(EqualizerIntent.SetPreset(it)) }
        )

        SectionTitle(stringResource(R.string.equalizer_bands))
        Column(modifier = Modifier.padding(horizontal = SonLayout.screenMargin)) {
            EqualizerBands.frequenciesHz.forEachIndexed { index, frequencyHz ->
                BandSliderRow(
                    frequencyHz = frequencyHz,
                    gainDb = state.bandGains.getOrElse(index) { 0f },
                    sliderEnabled = state.enabled,
                    onGainChange = { gain ->
                        viewModel.handleIntent(EqualizerIntent.SetBandGain(index, gain))
                    }
                )
            }
        }

        SectionTitle(stringResource(R.string.equalizer_preamp))
        PreampSliderRow(
            preampDb = state.preampDb,
            sliderEnabled = state.enabled,
            onPreampChange = { viewModel.handleIntent(EqualizerIntent.SetPreamp(it)) }
        )

        Button(
            onClick = { viewModel.handleIntent(EqualizerIntent.Reset) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = SonLayout.screenMargin, vertical = SonSpacing.large)
        ) {
            Text(text = stringResource(R.string.equalizer_reset))
        }

        HorizontalDivider(modifier = Modifier.padding(horizontal = SonLayout.screenMargin))

        SwitchRow(
            title = stringResource(R.string.equalizer_visualization),
            subtitle = stringResource(R.string.equalizer_visualization_subtitle),
            checked = state.visualizationEnabled,
            onCheckedChange = {
                viewModel.handleIntent(EqualizerIntent.SetVisualizationEnabled(it))
            }
        )
    }

    if (state.autoEqPickerVisible) {
        AutoEqPickerSheet(
            applied = state.autoEq,
            catalogSize = state.autoEqCatalogSize,
            query = state.autoEqQuery,
            formFactor = state.autoEqFormFactor,
            results = state.autoEqResults,
            isSearching = state.autoEqSearching,
            onQueryChange = { viewModel.handleIntent(EqualizerIntent.SetAutoEqQuery(it)) },
            onFormFactorChange = {
                viewModel.handleIntent(EqualizerIntent.SetAutoEqFormFactor(it))
            },
            onSelect = { viewModel.handleIntent(EqualizerIntent.ApplyAutoEq(it)) },
            onClear = { viewModel.handleIntent(EqualizerIntent.ClearAutoEq) },
            onDismiss = { viewModel.handleIntent(EqualizerIntent.DismissAutoEqPicker) }
        )
    }
}

/**
 * 耳机校正条目：左边是当前档案（或"未应用"），右边是入口。
 */
@Composable
private fun AutoEqRow(
    appliedName: String?,
    detail: String,
    onPick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = SonLayout.screenMargin, vertical = SonSpacing.medium),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = appliedName ?: stringResource(R.string.autoeq_none),
                style = MaterialTheme.typography.titleMediumEmphasized,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = detail,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.width(SonSpacing.medium))
        TextButton(onClick = onPick) {
            Text(
                text = stringResource(
                    if (appliedName == null) R.string.autoeq_pick else R.string.autoeq_change
                )
            )
        }
    }
}

@Composable
private fun SwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = SonLayout.screenMargin, vertical = SonSpacing.medium),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMediumEmphasized,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(SonSpacing.medium))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        modifier = Modifier.padding(
            start = SonSizes.listItemHorizontalPadding,
            top = SonSpacing.large,
            bottom = SonSpacing.small
        ),
        style = MaterialTheme.typography.titleSmallEmphasized,
        color = MaterialTheme.colorScheme.primary
    )
}

/**
 * 幅频响应曲线：把当前滤波器组在 20Hz–20kHz 上的真实响应画出来，拖滑杆时曲线同步变形。
 *
 * 计算用 domain 的 [BiquadCoefficients]，和音频线程跑的是同一份系数，界面不会"画的和听的不一致"。
 */
@Composable
private fun EqualizerCurve(
    filters: List<EqFilterSpec>,
    preampDb: Float,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    val curveColor = LocalSonExtendedColors.current.waveformActive
    val baseColor = LocalSonExtendedColors.current.waveform

    Canvas(modifier = modifier) {
        val middleY = size.height / 2f
        val thinStroke = SonSpacing.extraSmall.toPx() / 2f

        // 0dB 基线
        drawLine(
            color = baseColor.copy(alpha = 0.45f),
            start = Offset(0f, middleY),
            end = Offset(size.width, middleY),
            strokeWidth = thinStroke
        )
        if (!enabled) return@Canvas

        val path = Path()
        for (index in 0..CURVE_POINTS) {
            val fraction = index.toFloat() / CURVE_POINTS
            val frequency = MIN_CURVE_FREQUENCY_HZ *
                (MAX_CURVE_FREQUENCY_HZ / MIN_CURVE_FREQUENCY_HZ).pow(fraction.toDouble())
            val gainDb = BiquadCoefficients.magnitudeDb(
                filters = filters,
                frequencyHz = frequency,
                sampleRate = CURVE_SAMPLE_RATE,
                preampDb = preampDb
            )
            val x = size.width * fraction
            val y = middleY - (gainDb / CURVE_DB_RANGE).toFloat() * middleY
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path = path, color = curveColor, style = Stroke(width = thinStroke * 2f))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PresetChips(
    selected: EqualizerPreset,
    chipEnabled: Boolean,
    onSelect: (EqualizerPreset) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = SonLayout.screenMargin),
            horizontalArrangement = Arrangement.spacedBy(SonSpacing.small),
            verticalArrangement = Arrangement.spacedBy(SonSpacing.small)
        ) {
            // CUSTOM 只是"用户改过"的标记，不作为可点选的预设。
            EqualizerPreset.entries.filter { it != EqualizerPreset.CUSTOM }.forEach { preset ->
                FilterChip(
                    selected = preset == selected,
                    onClick = { onSelect(preset) },
                    enabled = chipEnabled,
                    label = { Text(text = stringResource(preset.labelRes())) }
                )
            }
        }
        if (selected == EqualizerPreset.CUSTOM) {
            Text(
                text = stringResource(R.string.equalizer_custom_active),
                modifier = Modifier.padding(
                    start = SonSizes.listItemHorizontalPadding,
                    top = SonSpacing.small
                ),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun BandSliderRow(
    frequencyHz: Int,
    gainDb: Float,
    sliderEnabled: Boolean,
    onGainChange: (Float) -> Unit
) {
    // 拖动过程只改本地值，松手才落盘 —— 否则一次拖动会触发几十次 DataStore 写入。
    var localGain by remember(gainDb) { mutableStateOf(gainDb) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = frequencyLabel(frequencyHz),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(FREQUENCY_LABEL_WIDTH)
        )
        Slider(
            value = localGain,
            onValueChange = { localGain = EqualizerBands.clampGain(it) },
            onValueChangeFinished = { onGainChange(localGain) },
            valueRange = EqualizerBands.MIN_GAIN_DB..EqualizerBands.MAX_GAIN_DB,
            steps = GAIN_SLIDER_STEPS,
            enabled = sliderEnabled,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = SonSpacing.small)
        )
        Text(
            text = stringResource(R.string.equalizer_band_gain, localGain),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.End,
            modifier = Modifier.width(GAIN_LABEL_WIDTH)
        )
    }
}

@Composable
private fun PreampSliderRow(
    preampDb: Float,
    sliderEnabled: Boolean,
    onPreampChange: (Float) -> Unit
) {
    var localPreamp by remember(preampDb) { mutableStateOf(preampDb) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = SonLayout.screenMargin),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Slider(
            value = localPreamp,
            onValueChange = {
                localPreamp = it.coerceIn(
                    EqualizerBands.MIN_PREAMP_DB,
                    EqualizerBands.MAX_PREAMP_DB
                )
            },
            onValueChangeFinished = { onPreampChange(localPreamp) },
            valueRange = EqualizerBands.MIN_PREAMP_DB..EqualizerBands.MAX_PREAMP_DB,
            steps = PREAMP_SLIDER_STEPS,
            enabled = sliderEnabled,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = stringResource(R.string.equalizer_preamp_value, localPreamp),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.End,
            modifier = Modifier.width(GAIN_LABEL_WIDTH)
        )
    }
}

private fun frequencyLabel(frequencyHz: Int): String = when {
    frequencyHz >= 1_000 -> "${frequencyHz / 1_000}k"
    else -> frequencyHz.toString()
}

@StringRes
private fun EqualizerPreset.labelRes(): Int = when (this) {
    EqualizerPreset.FLAT -> R.string.eq_preset_flat
    EqualizerPreset.BASS_BOOST -> R.string.eq_preset_bass_boost
    EqualizerPreset.VOCAL -> R.string.eq_preset_vocal
    EqualizerPreset.ROCK -> R.string.eq_preset_rock
    EqualizerPreset.POP -> R.string.eq_preset_pop
    EqualizerPreset.CLASSICAL -> R.string.eq_preset_classical
    EqualizerPreset.JAZZ -> R.string.eq_preset_jazz
    EqualizerPreset.ELECTRONIC -> R.string.eq_preset_electronic
    EqualizerPreset.DANCE -> R.string.eq_preset_dance
    EqualizerPreset.CUSTOM -> R.string.equalizer_custom_active
}

private val FREQUENCY_LABEL_WIDTH: Dp = SonSpacing.extraExtraLarge
private val GAIN_LABEL_WIDTH: Dp = SonSpacing.huge
private val CURVE_HEIGHT: Dp = SonSpacing.huge + SonSpacing.large

/** (-12dB..12dB) / 0.5dB 步长 → 中间 47 个刻度。 */
private const val GAIN_SLIDER_STEPS = 47

/** (-12dB..12dB) / 0.5dB 步长。 */
private const val PREAMP_SLIDER_STEPS = 47

private const val CURVE_POINTS = 64
private const val MIN_CURVE_FREQUENCY_HZ = 20.0
private const val MAX_CURVE_FREQUENCY_HZ = 20_000.0

/** 曲线纵向取 ±18dB 满量程，给 +12dB 留一点余量看得到形状。 */
private const val CURVE_DB_RANGE = 18.0

/** 画曲线用的采样率假设，不影响实际播放（音频线程用真实采样率）。 */
private const val CURVE_SAMPLE_RATE = 48_000.0
