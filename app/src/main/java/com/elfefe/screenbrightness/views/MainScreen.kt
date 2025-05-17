package com.elfefe.screenbrightness.views

import android.content.Intent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateOffsetAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.elfefe.screenbrightness.ActionKeys
import com.elfefe.screenbrightness.AdsScreen
import com.elfefe.screenbrightness.MainActivity
import com.elfefe.screenbrightness.OverlayService
import com.elfefe.screenbrightness.R
import com.elfefe.screenbrightness.resString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MainActivity.MainScreen() {
    val scope = rememberCoroutineScope()
    val configuration = LocalConfiguration.current

    var showMenu by remember { mutableStateOf(false) }
    val pages by remember {
        mutableStateOf(
            listOf(
                resString(R.string.brightness) to R.drawable.baseline_settings_brightness_24,
                resString(R.string.alarm) to R.drawable.baseline_alarm_24,
                resString(R.string.color) to R.drawable.baseline_color_lens_24
            )
        )
    }

    val pagerState = rememberPagerState(pageCount = { pages.size })
    val buttonSize by animateDpAsState(targetValue =
        if (pagerState.currentPage == 0) 192.dp
        else 64.dp, label = "buttonSize"
    )
    val buttonOffset by animateOffsetAsState(targetValue =
        if (pagerState.currentPage == 0) Offset(x = 0f, y = -32f)
        else Offset(x = ((configuration.screenWidthDp / 2) - (buttonSize.value / 2) - 8), y = 48f),
        label = "buttonOffset"
    )
    val buttonElevation by animateDpAsState(targetValue =
        if (pagerState.currentPage == 0) 8.dp
        else 1.dp, label = "buttonElevation"
    )

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Scaffold(
            floatingActionButton = {
                FloatingActionButton(
                    onClick = {
                        startForegroundService(
                            Intent(this, OverlayService::class.java).apply {
                                action = ActionKeys.TOGGLE_OVERLAY
                            }
                        )
                    },
                    shape = CircleShape,
                    containerColor =
                        if (isOverlayEnabled.value) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surface,
                    contentColor =
                        if (isOverlayEnabled.value) MaterialTheme.colorScheme.surface
                        else MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .offset(buttonOffset.x.dp, buttonOffset.y.dp)
                        .size(buttonSize),
                    elevation = FloatingActionButtonDefaults.elevation(buttonElevation)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.brightness_high_24dp),
                        contentDescription = "Toggle overlay",
                        modifier = Modifier
                            .size(buttonSize * .6f),
                    )
                }
            },
            floatingActionButtonPosition = FabPosition.Center,
            topBar = {
                TopAppBar(
                    title = { Text(
                        text = pages[pagerState.currentPage].first,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary
                    ) },
                    actions = {
                        IconButton(
                            onClick = {
                                showMenu = !showMenu
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Toggle overlay"
                            )
                        }

                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = "Donate ❤",
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.clickable {

                                    }
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Watch ads",
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.clickable {

                                    }
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                        actionIconContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            },
            bottomBar = {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    BottomAppBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ) {
                        LazyRow(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            items(pages) { page ->
                                IconButton(
                                    onClick = {
                                        scope.launch(Dispatchers.Default) {
                                            pagerState.animateScrollToPage(
                                                pages.indexOf(page)
                                            )
                                        }
                                    },
                                    modifier = Modifier.padding(8.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(page.second),
                                        contentDescription = page.first,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                            }
                        }
                    }
                }
            }
        ) {
            HorizontalPager(state = pagerState) { page ->
                Column(
                    modifier = Modifier
                        .padding(
                            16.dp,
                            it.calculateTopPadding() + 16.dp,
                            16.dp,
                            it.calculateBottomPadding()
                        )
                        .fillMaxSize(),
                    verticalArrangement = Arrangement.Top,
                    horizontalAlignment = Alignment.Start
                ) {
                    when (page) {
                        0 -> BrightnessScreen(
                            initialBrightness = brightnessAlpha.value,
                            onBrightnessChange = { newBrightness ->
                                adjustBrightness(newBrightness)
                            },
                            initialBrighnessStep = brightnessStep.value,
                            onBrightnessStepChange = { step ->
                                adjustBrightnessStep(step)
                            }
                        )

                        1 -> ScheduleScreen()
                        2 -> ColorScreen()
                    }
                }
            }
        }
    }
}
