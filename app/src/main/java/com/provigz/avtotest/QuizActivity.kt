package com.provigz.avtotest

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.AsyncImage
import com.provigz.avtotest.db.TestSetDao
import com.provigz.avtotest.db.TestSetDatabase
import com.provigz.avtotest.db.entity.Answer
import com.provigz.avtotest.db.entity.Property
import com.provigz.avtotest.db.entity.Question
import com.provigz.avtotest.db.entity.QuestionQueried
import com.provigz.avtotest.db.entity.QuestionState
import com.provigz.avtotest.db.entity.TestSet
import com.provigz.avtotest.db.entity.TestSetQueried
import com.provigz.avtotest.model.TestSetAssessment
import com.provigz.avtotest.model.TestSetAssessmentRequest
import com.provigz.avtotest.model.TestSetRequest
import com.provigz.avtotest.model.TestSetSubCategory
import com.provigz.avtotest.ui.theme.AvtoTestTheme
import com.provigz.avtotest.util.AsyncVideoPlayer
import com.provigz.avtotest.viewmodel.NetworkRequestViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

class QuizActivity : ComponentActivity() {
    @SuppressLint("StateFlowValueCalledInComposition", "CoroutineCreationDuringComposition")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val db = TestSetDatabase.getDatabase(context = this)
        val testSetDao = db.testSetDao()

        enableEdgeToEdge()
        setContent {
            AvtoTestTheme {
                val testSetIDState by produceState<Pair<Boolean, String?>>(false to null) {
                    val data = testSetDao.getProperty("startedTestSet")
                    value = true to data
                }
                val (hasLoadedTestSetID, startedTestSetID) = testSetIDState

                if (!hasLoadedTestSetID) {
                    ComposeLoadingPrompt()
                } else if (startedTestSetID.isNullOrEmpty()) {
                    val viewModel = remember {
                        NetworkRequestViewModel(
                            endpoint = "test-sets",
                            method = "POST",
                            request = TestSetRequest(
                                subCategoryID = 3,
                                learningPlanID = 227,
                                languageID = 1
                            ),
                            responseClass = com.provigz.avtotest.model.TestSet::class.java
                        )
                    }
                    LaunchedEffect(Unit) {
                        viewModel.fetch()
                    }

                    val response by viewModel.responseState.collectAsState()
                    val finished by viewModel.finishedState.collectAsState()
                    val error by viewModel.errorState.collectAsState()

                    if (response != null) {
                        val testSet = remember { TestSet(response!!) }
                        var testSetReady by remember { mutableStateOf(false) }
                        LaunchedEffect(Unit) {
                            testSet.insertQuestions(
                                testSetDao,
                                questions = response!!.questions
                            )
                            testSetDao.insertTestSet(testSet)
                            testSetDao.setProperty(
                                Property(
                                    name = "startedTestSet",
                                    value = testSet.id.toString()
                                )
                            )
                            testSetReady = true
                        }

                        if (testSetReady) {
                            ComposeQueryQuizActivity(
                                testSetDao,
                                testSet
                            )
                        } else {
                            ComposeLoadingPrompt()
                        }
                    } else if (finished) {
                        AlertDialog(
                            onDismissRequest = {},
                            title = {
                                Text(
                                    text = "Листовката не може да бъде заредена!"
                                )
                            },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        finish()
                                    }
                                ) {
                                    Text(text = "OK")
                                }
                            }
                        )
                    } else if (error != null) {
                        AlertDialog(
                            onDismissRequest = {},
                            title = {
                                Text(
                                    text = "Грешка при зареждането на листовка!"
                                )
                            },
                            text = {
                                Text(
                                    text = error!!
                                )
                            },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        finish()
                                    }
                                ) {
                                    Text(text = "OK")
                                }
                            }
                        )
                    } else {
                        ComposeLoadingPrompt()
                    }
                } else {
                    val testSetState by produceState<Pair<Boolean, TestSet?>>(false to null) {
                        val data = testSetDao.getTestSetByID(startedTestSetID.toInt())
                        value = true to data
                    }
                    val (hasLoadedTestSet, testSet) = testSetState

                    if (hasLoadedTestSet) {
                        if (testSet != null) {
                            ComposeQueryQuizActivity(
                                testSetDao,
                                testSet
                            )
                        } else {
                            LaunchedEffect(Unit) {
                                testSetDao.setProperty(
                                    Property(
                                        name = "startedTestSet",
                                        value = ""
                                    )
                                )
                            }
                            AlertDialog(
                                onDismissRequest = {},
                                title = {
                                    Text(
                                        text = "Грешка при зареждането на започната листовка!"
                                    )
                                },
                                text = {
                                    Text(
                                        text = "Започната листовка не е налична!"
                                    )
                                },
                                confirmButton = {
                                    Button(
                                        onClick = {
                                            finish()
                                        }
                                    ) {
                                        Text(text = "OK")
                                    }
                                }
                            )
                        }
                    } else {
                        ComposeLoadingPrompt()
                    }
                }
            }
        }
    }
}

@Composable
fun ComposeLoadingPrompt(
    text: String = "Зареждане на листовка..."
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(30.dp)
        )
        Spacer(
            modifier = Modifier.width(16.dp)
        )
        Text(
            text,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun ComposeQueryQuizActivity(
    testSetDao: TestSetDao,
    testSet: TestSet?
) {
    var testSetQueried by remember { mutableStateOf<TestSetQueried?>(null) }
    LaunchedEffect(Unit) {
        val result = testSet!!.query(testSetDao)
        testSetQueried = result
    }

    if (testSetQueried != null) {
        ComposeQuizTimer(
            testSetQueried!!
        )

        val testSetUpdateCount by testSetQueried!!.updateCount.collectAsState() // Used to trigger recompositions
        Log.i("QuizActivity", "testSet was updated. Individual updates: $testSetUpdateCount")

        if (testSetQueried!!.base.stateTimeFinished != null && !testSetQueried!!.base.stateAssessed) {
            /* Request assessment and await it */
            val testSetID = testSet!!.id
            val viewModel = remember {
                NetworkRequestViewModel(
                    endpoint = "test-sets/$testSetID/assessment",
                    method = "POST",
                    request = TestSetAssessmentRequest(
                        testSetQueried!!
                    ),
                    responseClass = TestSetAssessment::class.java
                )
            }
            LaunchedEffect(Unit) {
                viewModel.fetch()
            }

            val response by viewModel.responseState.collectAsState()
            val finished by viewModel.finishedState.collectAsState()
            val error by viewModel.errorState.collectAsState()

            if (response != null) {
                var testSetReady by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) {
                    testSetQueried!!.setAssessment(response!!)
                    testSetDao.deleteProperty(name = "startedTestSet")
                    testSetReady = true
                }

                if (testSetReady) {
                    ComposeQuizActivity(
                        testSetQueried!!
                    )
                } else {
                    ComposeLoadingPrompt(
                        text = "Зареждане на резултат..."
                    )
                }
            } else if (finished) {
                AlertDialog(
                    onDismissRequest = {
                        CoroutineScope(Dispatchers.IO).launch {
                            testSetQueried!!.retractSubmit()
                        }
                    },
                    title = {
                        Text(
                            text = "Резултат на листовката не може да бъде зареден!"
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                CoroutineScope(Dispatchers.IO).launch {
                                    testSetQueried!!.retractSubmit()
                                }
                            }
                        ) {
                            Text(text = "OK")
                        }
                    }
                )
            } else if (error != null) {
                AlertDialog(
                    onDismissRequest = {
                        CoroutineScope(Dispatchers.IO).launch {
                            testSetQueried!!.retractSubmit()
                        }
                    },
                    title = {
                        Text(
                            text = "Грешка при зареждането на резултат на листовката!"
                        )
                    },
                    text = {
                        Text(
                            text = error!!
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                CoroutineScope(Dispatchers.IO).launch {
                                    testSetQueried!!.retractSubmit()
                                }
                            }
                        ) {
                            Text(text = "OK")
                        }
                    }
                )
            } else {
                ComposeLoadingPrompt(
                    text = "Зареждане на резултат..."
                )
            }
        } else {
            ComposeQuizActivity(
                testSetQueried!!
            )
        }
    } else {
        ComposeLoadingPrompt()
    }
}

@Composable
fun ComposeQuizTimer(
    testSet: TestSetQueried
) {
    val coroutineScope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current

    var job by remember { mutableStateOf<Job?>(null) }

    if (testSet.base.stateTimeFinished != null) {
        job?.cancel()
        return
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    job = coroutineScope.launch {
                        while (true) {
                            testSet.incrementSecondsPassed()
                            if (testSet.base.stateSecondsPassed >= testSet.base.durationMinutes * 60) {
                                job?.cancel()
                                CoroutineScope(Dispatchers.IO).launch {
                                    testSet.requestSubmit(timeout = true)
                                }
                            } else {
                                delay(timeMillis = 1000L)
                            }
                        }
                    }
                }
                Lifecycle.Event.ON_PAUSE -> {
                    job?.cancel()
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            job?.cancel()
        }
    }
}

@Preview(
    name = "Light",
    showBackground = true
)
@Preview(
    name = "Dark",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
fun ComposeQuizActivityPreview() {
    val sampleTestSet = TestSetQueried(
        testSetDao = null,
        base = TestSet(
            com.provigz.avtotest.model.TestSet(
                id = 216782909,
                subCategory = TestSetSubCategory(
                    id = 3,
                    categoryID = 1,
                    name = "B",
                    durationMinutes = 40
                ),
                questions = emptyArray(),
            )
        ),
        questions = listOf(
            QuestionQueried(
                testSetDao = null,
                base = Question(
                    com.provigz.avtotest.model.Question(
                        id = 565436,
                        text = "Водачът на ППС е задължен да подаде сигнал към останалите участници в движението:",
                        thumbnailID = null,
                        pictureID = null,
                        videoID = null,
                        points = 1,
                        correctAnswers = 1,
                        answers = emptyArray()
                    )
                ),
                state = QuestionState(
                    testSetID = 216782909,
                    questionID = 565436,
                    answerIDs = emptyList()
                ),
                answers = listOf(
                    Answer(
                        id = 4325,
                        questionID = 565436,
                        text = "Lorem Ipsum is simply dummy text of the printing and typesetting industry. Lorem Ipsum has been the industry's standard dummy text ever since the 1500s, when an unknown printer took a galley of type and scrambled it to",
                        pictureID = null
                    ),
                    Answer(
                        id = 4326,
                        questionID = 565436,
                        text = "Lorem Ipsum is simply dummy text of the printing and typesetting industry. Lorem Ipsum has been the industry's standard dummy text ever since the 1500s, when an unknown printer took a galley of type and scrambled it to",
                        pictureID = null
                    ),
                    Answer(
                        id = 4327,
                        questionID = 565436,
                        text = "Lorem Ipsum is simply dummy text of the printing and typesetting industry. Lorem Ipsum has been the industry's standard dummy text ever since the 1500s, when an unknown printer took a galley of type and scrambled it to",
                        pictureID = null
                    ),
                    Answer(
                        id = 4328,
                        questionID = 565436,
                        text = "Lorem Ipsum is simply dummy text of the printing and typesetting industry. Lorem Ipsum has been the industry's standard dummy text ever since the 1500s, when an unknown printer took a galley of type and scrambled it to",
                        pictureID = null
                    )
                )
            ),
            QuestionQueried(
                testSetDao = null,
                base = Question(
                    com.provigz.avtotest.model.Question(
                        id = 565436,
                        text = "Водачът на ППС е задължен да подаде сигнал към останалите участници в движението:",
                        thumbnailID = null,
                        pictureID = null,
                        videoID = null,
                        points = 1,
                        correctAnswers = 1,
                        answers = emptyArray()
                    )
                ),
                state = QuestionState(
                    testSetID = 216782909,
                    questionID = 565436,
                    answerIDs = emptyList()
                ),
                answers = listOf(
                    Answer(
                        id = 4325,
                        questionID = 565436,
                        text = "Lorem Ipsum is simply dummy text of the printing and typesetting industry. Lorem Ipsum has been the industry's standard dummy text ever since the 1500s, when an unknown printer took a galley of type and scrambled it to",
                        pictureID = null
                    ),
                    Answer(
                        id = 4326,
                        questionID = 565436,
                        text = "Lorem Ipsum is simply dummy text of the printing and typesetting industry. Lorem Ipsum has been the industry's standard dummy text ever since the 1500s, when an unknown printer took a galley of type and scrambled it to",
                        pictureID = null
                    ),
                    Answer(
                        id = 4327,
                        questionID = 565436,
                        text = "Lorem Ipsum is simply dummy text of the printing and typesetting industry. Lorem Ipsum has been the industry's standard dummy text ever since the 1500s, when an unknown printer took a galley of type and scrambled it to",
                        pictureID = null
                    ),
                    Answer(
                        id = 4328,
                        questionID = 565436,
                        text = "Lorem Ipsum is simply dummy text of the printing and typesetting industry. Lorem Ipsum has been the industry's standard dummy text ever since the 1500s, when an unknown printer took a galley of type and scrambled it to",
                        pictureID = null
                    )
                )
            )
        )
    )

    // For previewing results screen
    sampleTestSet.base.stateSecondsPassed = 2100
    sampleTestSet.base.stateTimedOut = true
    sampleTestSet.base.stateCurrentQuestionIndex = -1
    sampleTestSet.base.stateAssessed = true
    sampleTestSet.base.stateReceivedPoints = 87
    sampleTestSet.base.stateTotalPoints = 97
    sampleTestSet.base.stateCorrectQuestionsCount = 42
    sampleTestSet.base.stateIncorrectQuestionsCount = 3
    sampleTestSet.base.statePassed = true

    AvtoTestTheme {
        ComposeQuizActivity(
            testSet = sampleTestSet
        )
    }
}

@Composable
fun ComposeQuizActivity(
    testSet: TestSetQueried
) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

    ComposeQuizNavigationDrawer(
        drawerState,
        coroutineScope,
        testSet
    ) {
        ComposeQuizScaffold(
            drawerState,
            coroutineScope,
            testSet
        ) { innerPadding ->
            if (testSet.questions.isNotEmpty()) {
                if (testSet.base.stateCurrentQuestionIndex < 0) {
                    ComposeQuizResults(
                        testSet,
                        innerPadding
                    )
                } else {
                    val question = testSet.questions[testSet.base.stateCurrentQuestionIndex]

                    val questionID = question.base.id
                    val questionUpdateCount by question.updateCount.collectAsState() // Used to trigger recompositions
                    Log.i(
                        "QuizActivity",
                        "Question $questionID was updated. Individual updates: $questionUpdateCount"
                    )

                    ComposeQuizQuestion(
                        innerPadding,
                        question,
                        interactive = testSet.base.stateTimeFinished == null,
                        showResults = testSet.base.stateAssessed
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComposeQuizScaffold(
    drawerState: DrawerState,
    coroutineScope: CoroutineScope,
    testSet: TestSetQueried,
    content: @Composable (PaddingValues) -> Unit = {}
) {
    var showSubmitWarningDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            Column {
                TopAppBar(
                    colors = topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.primary,
                    ),
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                coroutineScope.launch {
                                    drawerState.open()
                                }
                            }
                        ) {
                            Icon(Icons.Default.Menu, contentDescription = "Въпроси")
                        }
                    },
                    title = {
                        val testSetID = testSet.base.id
                        if (testSet.base.stateCurrentQuestionIndex < 0) {
                            Text(
                                text = "Резултат на #$testSetID",
                                fontFamily = FontFamily.Serif,
                                fontWeight = FontWeight.Bold
                            )
                        } else {
                            val questionCount = testSet.questions.size
                            val questionNumber = testSet.base.stateCurrentQuestionIndex + 1
                            Text(
                                text = "Въпрос $questionNumber/$questionCount (#$testSetID)",
                                fontFamily = FontFamily.Serif,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    },
                    actions = {
                        if (testSet.base.stateTimeFinished == null) {
                            IconButton(onClick = {
                                showSubmitWarningDialog = true
                            }) {
                                Icon(Icons.Default.Done, contentDescription = "Предай")
                            }
                        }
                    }
                )
                if (testSet.base.stateCurrentQuestionIndex >= 0) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(70.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            val durationSeconds = testSet.base.durationMinutes * 60
                            val secondsPassed by testSet.secondsPassed.collectAsState()
                            val secondsRemaining = max(a = durationSeconds - secondsPassed, b = 0)
                            LinearProgressIndicator(
                                modifier = Modifier
                                    .fillMaxWidth(fraction = 0.95f)
                                    .height(10.dp),
                                trackColor = MaterialTheme.colorScheme.primaryContainer,
                                gapSize = 0.dp,
                                drawStopIndicator = {},
                                progress = {
                                    secondsPassed / durationSeconds.toFloat()
                                }
                            )
                            Spacer(
                                modifier = Modifier.height(20.dp)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = String.format(
                                        locale = Locale.US,
                                        format = "%02d:%02d",
                                        secondsRemaining / 60, secondsRemaining % 60
                                    ),
                                    fontSize = 20.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 40.dp)
                                )
                                val question = testSet.questions[testSet.base.stateCurrentQuestionIndex]
                                val points = question.base.points
                                Text(
                                    text = "Точки: $points",
                                    fontSize = 20.sp,
                                    modifier = Modifier.padding(horizontal = 20.dp)
                                )
                                val correctAnswers = question.base.correctAnswers
                                Text(
                                    text = "Верни: $correctAnswers",
                                    fontSize = 20.sp,
                                    modifier = Modifier.padding(horizontal = 20.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            if (testSet.base.stateCurrentQuestionIndex >= 0) {
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
                                if (testSet.base.stateCurrentQuestionIndex > 0) {
                                    --testSet.base.stateCurrentQuestionIndex
                                    CoroutineScope(Dispatchers.IO).launch {
                                        testSet.save()
                                    }
                                }
                            },
                            containerColor = if (testSet.base.stateCurrentQuestionIndex > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.padding(start = 32.dp, end = 16.dp)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Предишен"
                            )
                        }
                        FloatingActionButton(
                            onClick = {
                                if (testSet.base.stateCurrentQuestionIndex < testSet.questions.size - 1) {
                                    ++testSet.base.stateCurrentQuestionIndex
                                    CoroutineScope(Dispatchers.IO).launch {
                                        testSet.save()
                                    }
                                }
                            },
                            containerColor = if (testSet.base.stateCurrentQuestionIndex < testSet.questions.size - 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
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
        }
    ) { innerPadding ->
        content(innerPadding)

        if (showSubmitWarningDialog) {
            var allQuestionsAnswered = true
            run questionsForeach@{
                testSet.questions.forEach { questionIt ->
                    if (questionIt.state.stateSelectedAnswerIndexes.isEmpty()) {
                        allQuestionsAnswered = false
                        return@questionsForeach
                    }
                }
            }

            if (allQuestionsAnswered) {
                AlertDialog(
                    onDismissRequest = {
                        showSubmitWarningDialog = false
                    },
                    title = {
                        Text(
                            text = "Информация"
                        )
                    },
                    text = {
                        Text(
                            text = "Имате още време до края на изпита. Как желаете да продължите?"
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                showSubmitWarningDialog = false
                                CoroutineScope(Dispatchers.IO).launch {
                                    testSet.requestSubmit()
                                }
                            }
                        ) {
                            Text(text = "Предавам")
                        }
                    },
                    dismissButton = {
                        Button(
                            onClick = {
                                showSubmitWarningDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary
                            )
                        ) {
                            Text(text = "Отказ")
                        }
                    }
                )
            } else {
                AlertDialog(
                    onDismissRequest = {
                        showSubmitWarningDialog = false
                    },
                    title = {
                        Text(
                            text = "Внимание"
                        )
                    },
                    text = {
                        Text(
                            text = "Имате неотговорени въпроси. Моля, отговорете на всички въпроси."
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                showSubmitWarningDialog = false
                            }
                        ) {
                            Text(text = "OK")
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun ComposeQuizResults(
    testSet: TestSetQueried,
    innerPadding: PaddingValues
) {
    val activity = LocalActivity.current

    Column(
        modifier = Modifier
            .padding(innerPadding)
            .padding(top = 20.dp)
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            val secondsPassed = min(a = testSet.base.stateSecondsPassed, b = testSet.base.durationMinutes * 60)
            Text(
                text = String.format(
                    locale = Locale.US,
                    format = "Време: %02d:%02d",
                    secondsPassed / 60, secondsPassed % 60
                ),
                textAlign = TextAlign.Center,
                fontSize = 15.sp,
                modifier = Modifier.padding(horizontal = 15.dp)
            )
            if (testSet.base.stateTimedOut) {
                Text(
                    text = "Времето изтече!",
                    textAlign = TextAlign.Center,
                    fontSize = 15.sp,
                    color = Color.Red
                )
            }
        }
        Canvas(
            modifier = Modifier.size(100.dp)
        ) {
            if (testSet.base.statePassed) {
                drawCircle(color = Color.Green)
                drawLine(
                    color = Color.White,
                    start = center.copy(x = center.x - 35.dp.toPx(), y = center.y),
                    end = center.copy(x = center.x - 10.dp.toPx(), y = center.y + 25.dp.toPx()),
                    strokeWidth = 20f
                )
                drawLine(
                    color = Color.White,
                    start = center.copy(x = center.x - 16.dp.toPx(), y = center.y + 25.dp.toPx()),
                    end = center.copy(x = center.x + 30.dp.toPx(), y = center.y - 25.dp.toPx()),
                    strokeWidth = 20f
                )
            } else {
                drawCircle(color = Color.Red)
                drawLine(
                    color = Color.White,
                    start = center.copy(x = center.x + 25.dp.toPx(), y = center.y + 25.dp.toPx()),
                    end = center.copy(x = center.x - 25.dp.toPx(), y = center.y - 25.dp.toPx()),
                    strokeWidth = 20f
                )
                drawLine(
                    color = Color.White,
                    start = center.copy(x = center.x - 25.dp.toPx(), y = center.y + 25.dp.toPx()),
                    end = center.copy(x = center.x + 25.dp.toPx(), y = center.y - 25.dp.toPx()),
                    strokeWidth = 20f
                )
            }
        }
        Text(
            text = if (testSet.base.statePassed) "ДА" else "НЕ",
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold,
            fontSize = 25.sp,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = "${testSet.base.stateReceivedPoints} от ${testSet.base.stateTotalPoints} точки",
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold,
            fontSize = 25.sp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 30.dp)
        )
        Text(
            text = "${testSet.base.stateCorrectQuestionsCount} верни въпроса",
            textAlign = TextAlign.Center,
            fontSize = 20.sp,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = "${testSet.base.stateIncorrectQuestionsCount} грешни въпроса",
            textAlign = TextAlign.Center,
            fontSize = 20.sp,
            modifier = Modifier.fillMaxWidth()
        )
        Card(
            modifier = Modifier
                .padding(horizontal = 12.dp)
                .fillMaxWidth()
                .fillMaxHeight(fraction = 0.6f)
                .padding(top = 10.dp),
            onClick = {
                testSet.base.stateCurrentQuestionIndex = 0 // Go to first question
                CoroutineScope(Dispatchers.IO).launch {
                    testSet.save()
                }
            },
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Преглед",
                    textAlign = TextAlign.Center,
                    fontSize = 16.sp,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        Card(
            modifier = Modifier
                .padding(horizontal = 12.dp)
                .fillMaxWidth()
                .fillMaxHeight(fraction = 0.8f),
            onClick = {
                activity?.finish()
            },
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Към начало",
                    textAlign = TextAlign.Center,
                    fontSize = 16.sp,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun ComposeQuizQuestion(
    innerPadding: PaddingValues,
    question: QuestionQueried,
    interactive: Boolean,
    showResults: Boolean
) {
    var showVideoEndWarningDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .padding(innerPadding)
            .fillMaxWidth()
            .fillMaxHeight(fraction = 0.88f)
    ) {
        val videoMode = !showResults && question.base.videoID != null && !question.state.stateVideoWatched
        val questionHeightFraction = if (question.base.videoID == null && question.base.pictureID.isNullOrEmpty()) 0.3f else if (videoMode) 0.6f else 0.5f
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(fraction = questionHeightFraction)
                .align(Alignment.TopCenter),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (videoMode) "Моля, натиснете старт на видеото, за да се запознаете с въпроса." else question.base.text,
                textAlign = TextAlign.Center,
                lineHeight = 28.sp,
                fontSize = 18.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp)
                    .padding(end = 10.dp)
            )
            if (question.base.videoID != null) {
                val videoID = question.base.videoID
                if (showResults || !question.state.stateVideoWatched) {
                    AsyncVideoPlayer(
                        videoUrl = "https://avtoizpit.com/api/videos/video$videoID.mp4",
                        thumbnailUrl = "https://avtoizpit.com/api/videos/$videoID/2.png?quality=4",
                        allowPlay = question.state.stateVideoTimesWatched < 3,
                        onPlay = {
                            if (!question.state.stateVideoWatched) {
                                ++question.state.stateVideoTimesWatched
                                CoroutineScope(Dispatchers.IO).launch {
                                    question.save()
                                }
                            }
                        },
                        modifier = Modifier.fillMaxSize(fraction = 0.9f)
                    )
                } else {
                    AsyncImage(
                        model = "https://avtoizpit.com/api/videos/$videoID/2.png?quality=4",
                        contentDescription = "Изображение",
                        modifier = Modifier.fillMaxSize(fraction = 0.9f)
                    )
                }
            } else if (!question.base.pictureID.isNullOrEmpty()) {
                val pictureID = question.base.pictureID
                AsyncImage(
                    model = "https://avtoizpit.com/api/pictures/$pictureID.png?quality=4",
                    contentDescription = "Изображение",
                    modifier = Modifier.fillMaxSize(fraction = 0.9f)
                )
            }
        }
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(fraction = 1.0f - questionHeightFraction)
                .align(Alignment.BottomCenter),
            contentPadding = PaddingValues(8.dp),
            verticalArrangement = Arrangement.spacedBy(
                15.dp,
                if (videoMode) Alignment.CenterVertically else Alignment.CenterVertically
            )
        ) {
            if (videoMode) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(fraction = 0.5f),
                        onClick = {
                            if (question.state.stateVideoTimesWatched < 3) {
                                showVideoEndWarningDialog = true
                            } else {
                                question.state.stateVideoWatched = true
                                CoroutineScope(Dispatchers.IO).launch {
                                    question.save()
                                }
                            }
                        },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        elevation = CardDefaults.cardElevation(4.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 50.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Решавай въпроса",
                                textAlign = TextAlign.Center,
                                fontSize = 16.sp,
                                overflow = TextOverflow.Ellipsis,
                                maxLines = 3,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
                item {
                    val remainingPlays = 3 - question.state.stateVideoTimesWatched
                    Text(
                        text = if (remainingPlays > 0) "Може да гледате видеото още $remainingPlays " + (if (remainingPlays == 1) "път" else "пъти") + "." else "Не можете да гледате повече видеото!",
                        textAlign = TextAlign.Center,
                        fontSize = 16.sp,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            } else {
                items(count = min(a = question.answers.size, b = 4)) { index ->
                    ComposeQuestionAnswerCard(
                        answer = question.answers[index],
                        index,
                        question,
                        interactive,
                        showResults
                    )
                }
            }
        }

        if (showVideoEndWarningDialog) {
            AlertDialog(
                onDismissRequest = {
                    showVideoEndWarningDialog = false
                },
                title = {
                    Text(
                        text = "Внимание"
                    )
                },
                text = {
                    Text(
                        text = "Ако изберете решаване на въпроса, няма да можете да гледате повече видеото.\n\nСигурни ли сте, че искате да продължите?"
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showVideoEndWarningDialog = false
                            question.state.stateVideoWatched = true
                            CoroutineScope(Dispatchers.IO).launch {
                                question.save()
                            }
                        }
                    ) {
                        Text(text = "Да")
                    }
                },
                dismissButton = {
                    Button(
                        onClick = {
                            showVideoEndWarningDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary
                        )
                    ) {
                        Text(text = "Не")
                    }
                }
            )
        }
    }
}

@Composable
fun ComposeQuizNavigationDrawer(
    drawerState: DrawerState,
    coroutineScope: CoroutineScope,
    testSet: TestSetQueried,
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
                    val questionCount = testSet.questions.size

                    item {
                        val testSetID = testSet.base.id
                        Text(
                            text = "Листовка #$testSetID",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                    }
                    item {
                        var answeredQuestionCount = 0
                        testSet.questions.forEach { question ->
                            if (question.state.stateSelectedAnswerIndexes.isNotEmpty()) {
                                ++answeredQuestionCount
                            }
                        }

                        Text(
                            text = "$answeredQuestionCount/$questionCount отговорени",
                            fontSize = 20.sp,
                            modifier = Modifier.padding(vertical = 15.dp)
                        )
                    }
                    if (testSet.base.stateAssessed) {
                        item {
                            ComposeQuizNavigationQuestionCard(
                                drawerState,
                                coroutineScope,
                                index = -1,
                                testSet,
                                question = null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(60.dp)
                            )
                            Spacer(
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                    items(count = questionCount / 2 + (if (questionCount % 2 == 0) 0 else 1)) { index ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            ComposeQuizNavigationQuestionCard(
                                drawerState,
                                coroutineScope,
                                index = index * 2,
                                testSet,
                                question = testSet.questions[index * 2]
                            )
                            Spacer(
                                modifier = Modifier.width(32.dp)
                            )
                            if (index * 2 + 1 < questionCount) {
                                ComposeQuizNavigationQuestionCard(
                                    drawerState,
                                    coroutineScope,
                                    index = index * 2 + 1,
                                    testSet,
                                    question = testSet.questions[index * 2 + 1]
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
    drawerState: DrawerState,
    coroutineScope: CoroutineScope,
    index: Int,
    testSet: TestSetQueried,
    question: QuestionQueried?,
    modifier: Modifier = Modifier
) {
    var modifierCard = modifier.fillMaxWidth(fraction = if (index % 2 == 0) 0.36f else 0.7f)
    if (testSet.base.stateCurrentQuestionIndex == index) {
        val borderColor = MaterialTheme.colorScheme.onBackground
        modifierCard = modifierCard
            .drawWithCache {
                // Draw outer border
                val border = 5.dp.toPx()
                onDrawWithContent {
                    drawContent()
                    drawRoundRect(
                        topLeft = Offset(
                            x = -border / 2 + 1,
                            y = -border / 2
                        ),
                        size = Size(
                            width = size.width + border - 1,
                            height = size.height + border
                        ),
                        color = borderColor,
                        style = Stroke(width = border),
                        cornerRadius = CornerRadius(
                            x = border * 3F,
                            y = border * 3F
                        )
                    )
                }
            }
    }

    var cardContainerColor = MaterialTheme.colorScheme.secondary
    var cardContentColor = MaterialTheme.colorScheme.surfaceContainer
    if (question != null) {
        if (testSet.base.stateAssessed) {
            cardContainerColor = if (question.isCorrect()) Color(color = 0xFF006400 /* Dark green */) else Color(color = 0xFF8B0000 /* Dark red */)
            cardContentColor = Color.White
        }
        else if (question.state.stateSelectedAnswerIndexes.isEmpty()) {
            cardContainerColor = MaterialTheme.colorScheme.secondary
        }
        else {
            cardContainerColor = MaterialTheme.colorScheme.primary
        }
    }

    Card(
        onClick = {
            coroutineScope.launch {
                testSet.base.stateCurrentQuestionIndex = index
                drawerState.close()
                CoroutineScope(Dispatchers.IO).launch {
                    testSet.save()
                }
            }
        },
        colors = CardDefaults.cardColors(
            containerColor = cardContainerColor
        ),
        elevation = CardDefaults.cardElevation(4.dp),
        modifier = modifierCard
    ) {
        Row(
            modifier = Modifier.padding(16.dp)
        ) {
            if (index < 0) { // "Results" button
                Text(
                    text = "Резултат",
                    textAlign = TextAlign.Center,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth()
                )
            } else if (question != null) {
                val number = index + 1
                Text(
                    text = if (number < 10) " $number.  " else "$number. ",
                    color = cardContentColor,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                val thumbnailID = question.base.thumbnailID
                if (!thumbnailID.isNullOrEmpty()) {
                    AsyncImage(
                        model = "https://avtoizpit.com/api/pictures/$thumbnailID.png?thumbnail=true",
                        contentDescription = "Въпрос със изображение",
                        modifier = Modifier.size(26.dp)
                    )
                } else if (question.base.videoID != null) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Видео въпрос",
                        tint = cardContentColor,
                        modifier = Modifier.size(26.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Въпрос",
                        tint = cardContentColor,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ComposeQuestionAnswerCard(
    answer: Answer,
    index: Int,
    question: QuestionQueried,
    interactive: Boolean,
    showResults: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = {
            if (interactive) {
                if (index in question.state.stateSelectedAnswerIndexes) {
                    question.state.stateSelectedAnswerIndexes = question.state.stateSelectedAnswerIndexes.filterNot { it == index }
                } else {
                    question.state.stateSelectedAnswerIndexes += index
                }
                CoroutineScope(Dispatchers.IO).launch {
                    question.save()
                }
            }
        },
        colors = CardDefaults.cardColors(
            containerColor = if (index in question.state.stateSelectedAnswerIndexes) MaterialTheme.colorScheme.inversePrimary else MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .padding(12.dp)
                    .fillMaxWidth(fraction = if (showResults) 0.9f else 1f)
            ) {
                if (!answer.pictureID.isNullOrEmpty()) {
                    val pictureID = answer.pictureID
                    AsyncImage(
                        model = "https://avtoizpit.com/api/pictures/$pictureID.png?quality=4",
                        contentDescription = answer.text,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else if (!answer.text.isNullOrEmpty()) {
                    Text(
                        text = answer.text,
                        fontSize = 16.sp,
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 3
                    )
                }
            }
            if (showResults) {
                Canvas(
                    modifier = Modifier.size(25.dp)
                ) {
                    if (answer.correct == true) {
                        drawCircle(color = Color.Green)
                        drawLine(
                            color = Color.White,
                            start = center.copy(x = center.x - 6.dp.toPx(), y = center.y),
                            end = center.copy(x = center.x - 1.dp.toPx(), y = center.y + 5.dp.toPx()),
                            strokeWidth = 5f
                        )
                        drawLine(
                            color = Color.White,
                            start = center.copy(x = center.x - 3.dp.toPx(), y = center.y + 5.dp.toPx()),
                            end = center.copy(x = center.x + 6.dp.toPx(), y = center.y - 5.dp.toPx()),
                            strokeWidth = 5f
                        )
                    } else if (answer.correct == false) {
                        drawCircle(color = Color.Red)
                        drawLine(
                            color = Color.White,
                            start = center.copy(x = center.x - 5.dp.toPx(), y = center.y - 5.dp.toPx()),
                            end = center.copy(x = center.x + 5.dp.toPx(), y = center.y + 5.dp.toPx()),
                            strokeWidth = 5f
                        )
                        drawLine(
                            color = Color.White,
                            start = center.copy(x = center.x - 5.dp.toPx(), y = center.y + 5.dp.toPx()),
                            end = center.copy(x = center.x + 5.dp.toPx(), y = center.y - 5.dp.toPx()),
                            strokeWidth = 5f
                        )
                    }
                }
            }
        }
    }
}