package com.provigz.avtotest

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.provigz.avtotest.ui.theme.AvtoTestTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.min

class QuestionActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ComposeQuestionActivity(context = this)
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
fun ComposeQuestionActivity(
    context: QuestionActivity = QuestionActivity(),
    question: String = "Водачът на ППС е задължен да подаде сигнал към останалите участници в движението:",
    imageID: String = "",
    videoID: String = "",
    answers: Array<String> = arrayOf(
        "Lorem Ipsum is simply dummy text of the printing and typesetting industry. Lorem Ipsum has been the industry's standard dummy text ever since the 1500s, when an unknown printer took a galley of type and scrambled it to",
        "Lorem Ipsum is simply dummy text of the printing and typesetting industry. Lorem Ipsum has been the industry's standard dummy text ever since the 1500s, when an unknown printer took a galley of type and scrambled it to",
        "Lorem Ipsum is simply dummy text of the printing and typesetting industry. Lorem Ipsum has been the industry's standard dummy text ever since the 1500s, when an unknown printer took a galley of type and scrambled it to",
        "Lorem Ipsum is simply dummy text of the printing and typesetting industry. Lorem Ipsum has been the industry's standard dummy text ever since the 1500s, when an unknown printer took a galley of type and scrambled it to"
    ),
    points: Int = 1,
    correctAnswers: Int = 1
) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

    AvtoTestTheme {
        ComposeQuizNavigationDrawer(
            drawerState = drawerState,
            coroutineScope = coroutineScope
        ) {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                topBar = {
                    TopAppBar(
                        colors = topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            titleContentColor = MaterialTheme.colorScheme.primary,
                        ),
                        navigationIcon = {
                            IconButton(
                                onClick = {
                                    coroutineScope.launch { drawerState.open() }
                                }
                            ) {
                                Icon(Icons.Default.Menu, contentDescription = "Въпроси")
                            }
                        },
                        title = {
                            Text(
                                text = "Въпрос 4/45 (#45325245)",
                                fontFamily = FontFamily.Serif,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        actions = {
                            IconButton(onClick = {
                                // TODO: Submit solution
                            }) {
                                Icon(Icons.Default.Done, contentDescription = "Предай")
                            }
                        }
                    )
                },
                bottomBar = {
                    BottomAppBar(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.primary,
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "Точки: $points",
                                fontSize = 20.sp,
                                modifier = Modifier.padding(horizontal = 40.dp)
                            )
                            Text(
                                text = "Верни: $correctAnswers",
                                fontSize = 20.sp,
                                modifier = Modifier.padding(horizontal = 40.dp)
                            )
                        }
                    }
                },
                floatingActionButton = {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            FloatingActionButton(
                                onClick = {
                                    // TODO: Set previous question in this activity
                                },
                                modifier = Modifier.padding(start = 32.dp, end = 16.dp)
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Предишен"
                                )
                            }
                            FloatingActionButton(
                                onClick = {
                                    // TODO: Set next question in this activity
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = "Следващ"
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .padding(innerPadding)
                        .fillMaxWidth()
                        .fillMaxHeight(fraction = 0.88f)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(fraction = if (imageID.isEmpty() && videoID.isEmpty()) 0.3f else 0.5f)
                            .align(Alignment.TopCenter),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = question,
                            textAlign = TextAlign.Center,
                            lineHeight = 28.sp,
                            fontSize = 18.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp)
                                .padding(end = 10.dp)
                        )
                    }
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(fraction = if (imageID.isEmpty() && videoID.isEmpty()) 0.7f else 0.5f)
                            .align(Alignment.BottomCenter),
                        contentPadding = PaddingValues(8.dp),
                        verticalArrangement = Arrangement.spacedBy(15.dp)
                    ) {
                        items(count = min(a = answers.size, b = 4)) { index ->
                            ComposeQuestionAnswerCard(
                                text = answers[index],
                                pictureID = "55265"
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ComposeQuizNavigationDrawer(
    drawerState: DrawerState,
    coroutineScope: CoroutineScope,
    content: @Composable () -> Unit = {}
) {
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(300.dp)
            ) {
                LazyColumn(
                    modifier = Modifier
                        .padding(vertical = 12.dp)
                        .fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    item {
                        Text(
                            text = "Листовка #45325245",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                    }
                    item {
                        Text(
                            text = "0/45 отговорени",
                            fontSize = 20.sp,
                            modifier = Modifier.padding(vertical = 15.dp)
                        )
                    }
                    items(count = 23 /* TODO: Questions / 2 */) { index ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            ComposeQuizNavigationQuestionCard(
                                coroutineScope = coroutineScope,
                                index = index * 2 + 1,
                                thumbnailID = "1123577637"
                            )
                            Spacer(
                                modifier = Modifier.width(32.dp)
                            )
                            if (index * 2 + 1 < 45 /* TODO: Questions */) {
                                ComposeQuizNavigationQuestionCard(
                                    coroutineScope = coroutineScope,
                                    index = index * 2 + 2
                                )
                            } else {
                                Spacer(
                                    modifier = Modifier.fillMaxWidth(fraction = 0.7f)
                                )
                            }
                        }
                    }
                }
            }
        }
    ) {
        content()
    }
}

@Composable
fun ComposeQuizNavigationQuestionCard(
    coroutineScope: CoroutineScope,
    index: Int,
    thumbnailID: String = ""
) {
    Card(
        modifier = Modifier.fillMaxWidth(fraction = if (index % 2 == 0) 0.7f else 0.36f),
        onClick = {
            // TODO: Switch to chosen question in this activity
        },
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = if (index < 10) " $index.  " else "$index. ",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            if (thumbnailID.isNotEmpty()) {
                AsyncImage(
                    model = "https://avtoizpit.com/api/pictures/$thumbnailID.png?thumbnail=true",
                    contentDescription = "Thumbnail",
                    modifier = Modifier.size(26.dp)
                )
            } else {
                Text(
                    text = "...",
                    fontSize = 20.sp
                )
            }
        }
    }
}

@Composable
fun ComposeQuestionAnswerCard(
    text: String = "",
    pictureID: String = ""
) {
    var isPressed by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = {
            isPressed = !isPressed
        },
        colors = CardDefaults.cardColors(containerColor = if (isPressed) MaterialTheme.colorScheme.inversePrimary else MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (pictureID.isNotEmpty()) {
                AsyncImage(
                    model = "https://avtoizpit.com/api/pictures/$pictureID.png?quality=4",
                    contentDescription = text,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                Text(
                    text,
                    fontSize = 16.sp,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 3
                )
            }
        }
    }
}