package com.dicoding.diaryapp.presentation.screens.write

import android.annotation.SuppressLint
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.dicoding.diaryapp.model.Diary
import com.dicoding.diaryapp.model.Mood
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.PagerState
import java.time.ZonedDateTime

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class, ExperimentalPagerApi::class)
@Composable
fun WriteScreen(
    uiState: uiState,
    pagerState: PagerState,
    moodName: () -> String,
    onDeleteConfirmed: () -> Unit,
    onBackPressed: () -> Unit,
    onTitleChange : (String) -> Unit,
    onDescriptionChanged : (String) -> Unit,
    onSaveClicked: (Diary) -> Unit,
    updatedDateTime: (ZonedDateTime) -> Unit
) {
    LaunchedEffect(key1 = uiState.mood){
        pagerState.scrollToPage(Mood.valueOf(uiState.mood.name).ordinal)
    }
    Scaffold(
        topBar = {
            WriteTopBar(
                onBackPressed = onBackPressed,
                onDeleteConfirmed = onDeleteConfirmed,
                selectedDiary = uiState.selectedDiary,
                moodName = moodName,
                updatedDateTime =  updatedDateTime
            )
        },
        content = {
            WriteContent(
                uiState = uiState,
                paddingValues = it,
                pagerState = pagerState,
                title = uiState.title,
                onTitleChange = onTitleChange,
                description = uiState.description,
                onDescriptionChanged = onDescriptionChanged,
                onSaveClicked = onSaveClicked
            )
        }
    )

}