package com.cycling.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSerializable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.runtime.serialization.NavKeySerializer
import androidx.savedstate.compose.serialization.serializers.MutableStateSerializer

/**
 * Create a navigation state that persists config changes and process death.
 */
@Composable
fun rememberNavigationState(
    startRoute: NavKey,
    topLevelRoutes: Set<NavKey>
): NavigationState {

    val topLevelRoute = rememberSerializable(
        startRoute, topLevelRoutes,
        serializer = MutableStateSerializer(NavKeySerializer())
    ) {
        mutableStateOf(startRoute)
    }

    val backStacks = topLevelRoutes.associateWith { key -> rememberNavBackStack(key) }

    return remember(startRoute, topLevelRoutes) {
        NavigationState(
            startRoute = startRoute,
            topLevelRoute = topLevelRoute,
            backStacks = backStacks,
            topLevelRoutes = topLevelRoutes
        )
    }
}

/**
 * State holder for navigation state.
 *
 * @param startRoute - the start route. The user will exit the app through this route.
 * @param topLevelRoute - the current top level route
 * @param backStacks - the back stacks for each top level route
 * @param topLevelRoutes - all routes that are considered "top level" (tabs)
 */
class NavigationState(
    val startRoute: NavKey,
    topLevelRoute: MutableState<NavKey>,
    val backStacks: Map<NavKey, NavBackStack<NavKey>>,
    val topLevelRoutes: Set<NavKey>
) {
    var topLevelRoute: NavKey by topLevelRoute

    /** 栈顶是不是 tab 根 —— 导航层面自己的判断，只由 [recomputeBottomBarVisibility] 改写。 */
    private val routeShowsBottomBar = mutableStateOf(true)

    /**
     * 页面自己请求收起底栏，给 tab 内部的「全屏子视图」用（当前只有「音乐库 → 我喜欢」）。
     *
     * 这类子视图不是一条导航路由，导航层看不出它和 tab 根的区别，只能由页面在进入/退出时
     * 置位；页面必须负责复位，否则底栏会一直藏着 —— 而底栏藏着的时候用户连 tab 都切不了。
     */
    private val screenHidesBottomBar = mutableStateOf(false)

    /** 底栏最终可见性：路由允许，且当前页面没有主动收起。 */
    val showBottomBar: Boolean
        get() = routeShowsBottomBar.value && !screenHidesBottomBar.value

    /** 页面请求收起 / 放回底栏。 */
    fun requestBottomBarHidden(hidden: Boolean) {
        screenHidesBottomBar.value = hidden
    }

    val stacksInUse: List<NavKey>
        get() = if (topLevelRoute == startRoute) {
            listOf(startRoute)
        } else {
            listOf(startRoute, topLevelRoute)
        }

    /**
     * Check whether the route currently displayed (pushed on the active stack)
     * is a top-level (tab) route. Returns false when a pushed screen (like Player)
     * is on top, so the bottom bar can be hidden.
     */
    fun recomputeBottomBarVisibility() {
        val stack = backStacks[topLevelRoute]
        val top = stack?.lastOrNull() ?: topLevelRoute
        routeShowsBottomBar.value = top in topLevelRoutes
    }
}

/**
 * Convert NavigationState into NavEntries.
 */
@Composable
fun NavigationState.toEntries(
    entryProvider: (NavKey) -> NavEntry<NavKey>
): SnapshotStateList<NavEntry<NavKey>> {

    val decoratedEntries = backStacks.mapValues { (_, stack) ->
        val decorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator<NavKey>(),
            rememberViewModelStoreNavEntryDecorator<NavKey>(),
        )
        rememberDecoratedNavEntries(
            backStack = stack,
            entryDecorators = decorators,
            entryProvider = entryProvider
        )
    }

    return stacksInUse
        .flatMap { decoratedEntries[it] ?: emptyList() }
        .toMutableStateList()
}
