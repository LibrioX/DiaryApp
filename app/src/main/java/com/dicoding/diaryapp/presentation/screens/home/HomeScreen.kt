package com.dicoding.diaryapp.presentation.screens.home

import android.annotation.SuppressLint
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role.Companion.Image
import androidx.compose.ui.unit.dp
import com.dicoding.diaryapp.R
import com.dicoding.diaryapp.data.repository.Diaries
import com.dicoding.diaryapp.util.RequestState

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    diaries: Diaries,
    drawerState: DrawerState,
    onMenuClicked: () -> Unit,
    onSignOutClicked: () -> Unit,
    onNavigateToWrite: () -> Unit
) {
    NavigationDrawer(drawerState = drawerState, onSignOutClicked = onSignOutClicked) {
    val scrollBehaviour = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

        Scaffold(
            modifier = Modifier.nestedScroll(scrollBehaviour.nestedScrollConnection),
            topBar = {
                HomeTopBar(scrollBehavior = scrollBehaviour,onMenuClicked = onMenuClicked)
            },
            floatingActionButton = {
                FloatingActionButton(onClick = onNavigateToWrite) {
                    Icon(imageVector = Icons.Default.Edit, contentDescription = "New Diary Icon")
                }
            },
            content = { paddingValues ->
                when (diaries) {
                    is RequestState.Success -> {
                        Log.d("HomeScreen", "HomeScreen: ${diaries.data}")
                        HomeContent(paddingValues = paddingValues, diaries.data, onClick = {})
                    }

                    is RequestState.Error -> {
                        EmptyPage(title = "Error", subtitle = diaries.exception.message ?: "")
                    }

                    is RequestState.Loading -> {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }

                    else -> {}
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavigationDrawer(
    drawerState: DrawerState,
    onSignOutClicked: () -> Unit,
    content: @Composable () -> Unit
) {
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp),
                    contentAlignment = Alignment.Center
                )
                {
                    Image(
                        modifier = Modifier.size(250.dp),
                        painter = painterResource(id = R.drawable.logo),
                        contentDescription = "Logo image"
                    )

                }
                NavigationDrawerItem(label = {
                    Row(modifier = Modifier.padding(horizontal = 12.dp)) {
                        Image(
                            painter = painterResource(id = R.drawable.google_logo),
                            contentDescription = "Google Logo"
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Sign Out", color = MaterialTheme.colorScheme.onSurface)
                    }
                }, selected = false, onClick = onSignOutClicked)
            }

        },
        content = content
    )

}