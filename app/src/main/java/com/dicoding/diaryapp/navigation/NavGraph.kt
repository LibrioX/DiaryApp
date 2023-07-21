package com.dicoding.diaryapp.navigation

import android.util.Log
import android.widget.Toast
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.dicoding.diaryapp.data.repository.MongoDB
import com.dicoding.diaryapp.model.GalleryImage
import com.dicoding.diaryapp.model.Mood
import com.dicoding.diaryapp.presentation.components.DisplayAlertDialog
import com.dicoding.diaryapp.presentation.screens.auth.AuthenticationScreen
import com.dicoding.diaryapp.presentation.screens.auth.AuthenticationViewModel
import com.dicoding.diaryapp.presentation.screens.home.HomeScreen
import com.dicoding.diaryapp.presentation.screens.home.HomeViewModel
import com.dicoding.diaryapp.presentation.screens.write.WriteScreen
import com.dicoding.diaryapp.presentation.screens.write.WriteViewModel
import com.dicoding.diaryapp.util.Constants.APP_ID
import com.dicoding.diaryapp.util.Constants.WRITE_SCREEN_ARGUMENT_KEY
import com.dicoding.diaryapp.model.RequestState
import com.dicoding.diaryapp.model.rememberGalleryState
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.rememberPagerState
import com.stevdzasan.messagebar.rememberMessageBarState
import com.stevdzasan.onetap.rememberOneTapSignInState
import io.realm.kotlin.mongodb.App
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SetupNavGraph(
    startDestination: String,
    navController: NavHostController,
    onDataLoaded: () -> Unit
) {
    NavHost(
        startDestination = startDestination,
        navController = navController
    ) {
        authenticationRoute(
            navigateToHome = {
                navController.popBackStack()
                navController.navigate(Screen.Home.route)
            },
            onDataLoaded = onDataLoaded
        )
        homeRoute(
            navigateToWrite = {
                navController.navigate(Screen.Write.route)
            },
            navigateToWriteWithArgs = {
                navController.navigate(Screen.Write.passDiaryId(it))
            },
            navigateToAuth = {
                navController.popBackStack()
                navController.navigate(Screen.Authentication.route)
            },
            onDataLoaded = onDataLoaded
        )
        writeRoute(onBackPressed = {
            navController.popBackStack()
        })
    }

}

fun NavGraphBuilder.authenticationRoute(navigateToHome: () -> Unit, onDataLoaded: () -> Unit) {
    composable(route = Screen.Authentication.route) {
        val viewModel: AuthenticationViewModel = viewModel()
        val authenticated by viewModel.authenticated
        val loadingState by viewModel.loadingState
        val oneTapState = rememberOneTapSignInState()
        val messageBarState = rememberMessageBarState()

        LaunchedEffect(key1 = Unit) {
            onDataLoaded()
        }

        AuthenticationScreen(
            authenticated = authenticated,
            loadingState = loadingState,
            oneTapState = oneTapState,
            messageBarState = messageBarState,
            onSuccessfullFirebaseSignIn = { token ->
                viewModel.signInWithMongoAtlas(
                    tokenId = token,
                    onSuccess = {
                        messageBarState.addSuccess("Successfully signed in")
                        viewModel.setLoading(false)
                    },
                    onError = {
                        messageBarState.addError(it)
                        viewModel.setLoading(false)
                    }
                )

            },
            onFailedFirebaseSignIn = {
                messageBarState.addError(it)
                viewModel.setLoading(false)
            },
            onDialogDismissed = {
                messageBarState.addError(Exception(it))
                viewModel.setLoading(false)
            },
            onButtonClicked = {
                oneTapState.open()
                viewModel.setLoading(true)
            },
            navigateToHome = navigateToHome
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
fun NavGraphBuilder.homeRoute(
    navigateToWrite: () -> Unit,
    navigateToWriteWithArgs: (String) -> Unit,
    navigateToAuth: () -> Unit,
    onDataLoaded: () -> Unit
) {
    composable(route = Screen.Home.route) {
        val viewModel: HomeViewModel = viewModel()
        val diaries by viewModel.diaries
        val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
        var signOutDialogOpened by remember { mutableStateOf(false) }
        val scope = rememberCoroutineScope()

        LaunchedEffect(key1 = diaries) {
            if (diaries !is RequestState.Loading) {
                onDataLoaded()
            }
        }

        HomeScreen(
            diaries = diaries,
            onMenuClicked = {
                scope.launch {
                    drawerState.open()
                }
            },
            onNavigateToWrite = navigateToWrite,
            drawerState = drawerState,
            onSignOutClicked = {
                signOutDialogOpened = true
            },
            navigateToWriteWithArgs = navigateToWriteWithArgs,
        )

        LaunchedEffect(key1 = Unit) {
            MongoDB.configureTheRealm()
        }

        DisplayAlertDialog(
            title = "Sign Out",
            message = "Are you sure you want to sign out?",
            dialogOpened = signOutDialogOpened,
            onDialogClosed = { signOutDialogOpened = false },
            onYesClicked = {
                scope.launch(Dispatchers.IO) {
                    val user = App.create(APP_ID).currentUser
                    user?.apply {
                        logOut()
                        withContext(Dispatchers.Main) {
                            navigateToAuth()
                        }

                    }
                }
            },
        )

    }
}

@OptIn(ExperimentalPagerApi::class)
fun NavGraphBuilder.writeRoute(onBackPressed: () -> Unit) {

    composable(
        route = Screen.Write.route,
        arguments = listOf(navArgument(name = WRITE_SCREEN_ARGUMENT_KEY) {
            type = NavType.StringType
            nullable = true
            defaultValue = null
        })
    ) {
        val viewModel: WriteViewModel = viewModel()
        val context = LocalContext.current
        val uiState = viewModel.uiState
        val pagerState = rememberPagerState()
        val galleryState = viewModel.galleryState
        val pageNumber by remember {
            derivedStateOf {
                pagerState.currentPage
            }
        }

        LaunchedEffect(key1 = uiState) {
            Log.d("WriteRoute", "uiState: ${uiState.selectedDiaryId}")
        }

        WriteScreen(
            uiState = uiState,
            pagerState = pagerState,
            galleryState = galleryState,
            onDeleteConfirmed = {
                viewModel.deleteDiary(
                    onSuccess = {
                        Toast.makeText(context, "Successfully deleted", Toast.LENGTH_SHORT).show()
                        onBackPressed()
                    },
                    onError = {
                        Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                    }
                )
            },
            onBackPressed = onBackPressed,
            onTitleChange = {
                viewModel.setTitle(it)
            },
            onDescriptionChanged = {
                viewModel.setDescription(it)
            },
            updatedDateTime = {
                viewModel.updateDateTime(it)
            },
            moodName = { Mood.values()[pageNumber].name },
            onSaveClicked = { diary ->
                viewModel.upsertDiary(diary,
                    onSuccess = {
                        onBackPressed()
                    },
                    onError = {
                        Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                    }

                )
            },
            onImageSelect = {
                val type = context.contentResolver.getType(it)?.split("/")?.last() ?: "jpg"
                viewModel.addImage(
                    image = it,
                    imageType =type
                )
            }
        )
    }
}