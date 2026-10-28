package com.cycling.presentation.home

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cycling.core.ui.apple.AppleAlphabetIndex
import com.cycling.core.ui.apple.AppleDims
import com.cycling.core.ui.apple.AppleIconButton
import com.cycling.core.ui.apple.AppleLargeTitle
import com.cycling.core.ui.apple.AppleTheme
import com.cycling.core.ui.apple.AppleType
import com.cycling.core.ui.apple.appleBottomBarClearance
import com.cycling.core.ui.apple.applePress
import com.cycling.core.ui.components.SymbolIconName
import com.cycling.presentation.R
import com.cycling.presentation.components.SongListItem
import com.cycling.presentation.components.SongQueueActions
import com.cycling.presentation.components.formatDuration
import com.cycling.presentation.components.sectionSongsByAlphabet
import com.cycling.presentation.components.songMenuItems
import kotlinx.coroutines.launch

/**
 * 首页 = 全部歌曲。
 *
 * 顶部是大标题 + 设置入口，下面按首字母分区（中文走 ICU 转拼音），右侧挂 Apple 的
 * A–Z 索引条：点或拖都能跳，拖动时浮出字母气泡。点一行即播并进播放页。
 *
 * 分区字母与排序由 [sectionSongsByAlphabet] 一次算好 —— 索引条要跳得准，
 * 列表就必须真按同一个键排过序。
 */
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    onNavigateToSettings: () -> Unit,
    onNavigateToPlayer: (Long) -> Unit,
    onNavigateToSongDetail: (Long) -> Unit = {},
    bottomPadding: Dp = 0.dp
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = AppleTheme.colors
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    // 长按哪一行就把菜单挂在哪一行；只给打开的那一行造菜单项，其余行不建弹层。
    var menuSongId by remember { mutableStateOf<Long?>(null) }

    // 队列动作来自本页 VM，详情来自导航，收藏走本页 VM：三件事在每一行上都可达。
    val queueActions = SongQueueActions(
        onPlayNext = { viewModel.handleIntent(HomeIntent.PlayNext(it)) },
        onAddToQueue = { viewModel.handleIntent(HomeIntent.AddToQueue(it)) },
        onViewDetail = { onNavigateToSongDetail(it.id) }
    )

    LaunchedEffect(Unit) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                is HomeEffect.NavigateToPlayer -> onNavigateToPlayer(effect.songId)
                is HomeEffect.NavigateToSettings -> onNavigateToSettings()
                is HomeEffect.ShowToast -> {
                    Toast.makeText(context, context.getString(effect.message), Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }
    }

    val sections = remember(uiState.songs) { sectionSongsByAlphabet(uiState.songs) }

    // 每个分区在 LazyList 里的起始下标：大标题占 1 格，之后每个分区只占该区歌曲数
    // （分区头不再画，字母只出现在右侧索引条上）。
    val sectionStartIndex = remember(sections) {
        var index = 1
        buildMap {
            sections.forEach { section ->
                put(section.letter, index)
                index += section.songs.size
            }
        }
    }

    // 反过来查：某首歌在 LazyList 里的下标。「跳到正在播放」要它。
    val indexBySongId = remember(sections) {
        var index = 1
        buildMap {
            sections.forEach { section ->
                section.songs.forEach { song -> put(song.id, index++) }
            }
        }
    }

    // 首个可见行落在哪个分区 → 索引条高亮哪个字母。
    val currentLetter by remember(listState, sectionStartIndex) {
        derivedStateOf {
            val visible = listState.firstVisibleItemIndex
            sectionStartIndex.entries
                .filter { it.value <= visible }
                .maxByOrNull { it.value }
                ?.key
        }
    }

    // 正在播放的那一行现在可见吗？只读 layoutInfo 的 key，不随滚动重组整页
    // （derivedStateOf 只在 true/false 翻转时才推一次）。
    val playingKey = uiState.currentSongId?.let { "song-$it" }
    val playingVisible by remember(listState, playingKey) {
        derivedStateOf {
            playingKey != null &&
                listState.layoutInfo.visibleItemsInfo.any { it.key == playingKey }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            // 大标题自己不吃 inset，这里补回状态栏高度。
            .statusBarsPadding()
    ) {
        // 加载中留空，避免空态文案闪一下。
        if (!uiState.isLoading) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    // 悬浮胶囊不吃布局高度，这里要把导航栏 inset 一并让出来
                    bottom = appleBottomBarClearance() + bottomPadding
                )
            ) {
                item(key = "title") {
                    AppleLargeTitle(
                        title = stringResource(R.string.mymusic_all_songs_label),
                        actions = {
                            AppleIconButton(
                                icon = SymbolIconName.Settings,
                                contentDescription = stringResource(R.string.home_settings_desc),
                                onClick = {
                                    viewModel.handleIntent(HomeIntent.NavigateToSettings)
                                }
                            )
                        }
                    )
                }

                if (sections.isEmpty()) {
                    item(key = "empty") {
                        Text(
                            text = stringResource(R.string.songs_empty_title),
                            style = AppleType.body,
                            color = colors.secondaryLabel,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = AppleDims.padding * 2, vertical = 48.dp)
                        )
                    }
                } else {
                    sections.forEach { section ->
                        items(section.songs, key = { song -> "song-${song.id}" }) { song ->
                            val menuExpanded = menuSongId == song.id
                            SongListItem(
                                song = song,
                                isCurrentSong = song.id == uiState.currentSongId,
                                // quiet surface：列表不画分割线，行与行之间交给留白。
                                showDivider = false,
                                subtitle = "${song.artist} · ${formatDuration(song.duration)}",
                                showDuration = false,
                                onClick = {
                                    viewModel.handleIntent(HomeIntent.SongClick(song))
                                },
                                // 长按整行弹菜单（行尾没有 ⋮，长按是唯一入口）。
                                onLongClick = { menuSongId = song.id },
                                moreMenuExpanded = menuExpanded,
                                onMoreDismiss = { menuSongId = null },
                                moreMenuItems = if (menuExpanded) {
                                    songMenuItems(
                                        song = song,
                                        queueActions = queueActions,
                                        onToggleFavorite = {
                                            viewModel.handleIntent(HomeIntent.ToggleFavorite(it))
                                        }
                                    )
                                } else {
                                    emptyList()
                                }
                            )
                        }
                    }
                }
            }

            AppleAlphabetIndex(
                letters = sections.map { it.letter },
                current = currentLetter,
                onLetterSelected = { letter ->
                    sectionStartIndex[letter]?.let { target ->
                        scope.launch { listState.scrollToItem(target) }
                    }
                },
                modifier = Modifier.align(Alignment.CenterEnd)
            )

            // 跳回正在播放：当前曲目滚出可视区时才出现，点一下滚到它那一行。
            val jumpTarget = uiState.currentSongId?.let { indexBySongId[it] }
            if (jumpTarget != null && !playingVisible) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(
                            end = AppleDims.padding,
                            bottom = appleBottomBarClearance() + bottomPadding + AppleDims.padding
                        )
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(colors.material)
                        .border(AppleDims.hairline, colors.materialStroke, CircleShape)
                        .applePress {
                            // 直接落位，不做动画：全库几百首时 animateScrollToItem 会滚上一两秒，
                            // 中间还可能先跳到别的位置，观感很差。索引条也是瞬间跳。
                            scope.launch { listState.scrollToItem(jumpTarget) }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_jump_to_playing),
                        contentDescription = stringResource(R.string.home_jump_to_playing),
                        tint = colors.label,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}
