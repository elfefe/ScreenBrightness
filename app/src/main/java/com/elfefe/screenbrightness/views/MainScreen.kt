package com.elfefe.screenbrightness.views

import android.content.Intent
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.elfefe.screenbrightness.ActionKeys
import com.elfefe.screenbrightness.BrightnessScreen
import com.elfefe.screenbrightness.MainActivity
import com.elfefe.screenbrightness.OverlayService
import com.elfefe.screenbrightness.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MainActivity.MainScreen() {
    val scope = rememberCoroutineScope()

    var showMenu by remember { mutableStateOf(false) }
    val pages by remember {
        mutableStateOf(
            listOf(
                "brightness" to R.drawable.baseline_settings_brightness_24,
                "alarm" to R.drawable.baseline_alarm_24,
                "color" to R.drawable.baseline_color_lens_24
            )
        )
    }

    val pageScrollState = rememberScrollState()
    val pagerState = rememberPagerState(pageCount = { pages.size })

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
                    modifier = Modifier
                        .size(64.dp)
                        .offset(y = 48.dp),
                ) {
                    Icon(
                        painter = painterResource(R.mipmap.ic_lower_brightness_monochrome),
                        contentDescription = "Toggle overlay"
                    )
                }
            },
            floatingActionButtonPosition = FabPosition.End,
            topBar = {
                TopAppBar(
                    title = { Text(text = "Lower Brightness") },
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
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Watch ads",
                                    color = MaterialTheme.colorScheme.onSurface
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