package com.provigz.avtotest

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.provigz.avtotest.db.TestSetDao
import com.provigz.avtotest.db.TestSetDatabase
import com.provigz.avtotest.db.entity.Answer
import com.provigz.avtotest.db.entity.Property
import com.provigz.avtotest.db.entity.QuestionQueried
import com.provigz.avtotest.db.entity.TestSet
import com.provigz.avtotest.db.entity.TestSetQueried
import com.provigz.avtotest.model.TestSetRequest
import com.provigz.avtotest.model.TestSetSubCategory
import com.provigz.avtotest.ui.theme.AvtoTestTheme
import com.provigz.avtotest.viewmodel.NetworkRequestViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
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
fun ComposeLoadingPrompt() {
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
            text = "Зареждане на листовка...",
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
        val testSetUpdateCount by testSetQueried!!.updateCount.collectAsState() // Used to trigger recompositions
        Log.i("QuizActivity", "testSet was updated. Individual updates: $testSetUpdateCount")

        ComposeQuizActivity(
            testSetQueried!!
        )
    } else {
        ComposeLoadingPrompt()
    }
}

@Preview(showBackground = true)
@Composable
fun ComposeQuizActivity(
    //context: QuizActivity = QuizActivity(),
    testSet: TestSetQueried = TestSetQueried(
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
                questions = arrayOf(
                    com.provigz.avtotest.model.Question(
                        id = 565436,
                        text = "Водачът на ППС е задължен да подаде сигнал към останалите участници в движението:",
                        thumbnailID = null,
                        pictureID = null,
                        videoID = null,
                        points = 1,
                        correctAnswers = 1,
                        answers = arrayOf(
                            com.provigz.avtotest.model.Answer(
                                id = 4325,
                                text = "Lorem Ipsum is simply dummy text of the printing and typesetting industry. Lorem Ipsum has been the industry's standard dummy text ever since the 1500s, when an unknown printer took a galley of type and scrambled it to",
                                pictureID = ""
                            ),
                            com.provigz.avtotest.model.Answer(
                                id = 4326,
                                text = "Lorem Ipsum is simply dummy text of the printing and typesetting industry. Lorem Ipsum has been the industry's standard dummy text ever since the 1500s, when an unknown printer took a galley of type and scrambled it to",
                                pictureID = ""
                            ),
                            com.provigz.avtotest.model.Answer(
                                id = 4327,
                                text = "Lorem Ipsum is simply dummy text of the printing and typesetting industry. Lorem Ipsum has been the industry's standard dummy text ever since the 1500s, when an unknown printer took a galley of type and scrambled it to",
                                pictureID = ""
                            ),
                            com.provigz.avtotest.model.Answer(
                                id = 4328,
                                text = "Lorem Ipsum is simply dummy text of the printing and typesetting industry. Lorem Ipsum has been the industry's standard dummy text ever since the 1500s, when an unknown printer took a galley of type and scrambled it to",
                                pictureID = ""
                            )
                        )
                    )
                )
            )
        )
    )
) {
    if (testSet.questions.isEmpty()) {
        return
    }
    val question = testSet.questions[testSet.base.stateCurrentQuestionIndex]

    val questionID = question.base.id
    val questionUpdateCount by question.updateCount.collectAsState() // Used to trigger recompositions
    Log.i("QuizActivity", "Question $questionID was updated. Individual updates: $questionUpdateCount")

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
            ComposeQuizQuestion(
                question,
                innerPadding
            )
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
    val question = testSet.questions[testSet.base.stateCurrentQuestionIndex]

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
                    val questionCount = testSet.questions.size
                    val questionNumber = testSet.base.stateCurrentQuestionIndex + 1
                    Text(
                        text = "Въпрос $questionNumber/$questionCount (#$testSetID)",
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
                    val points = question.base.points
                    val correctAnswers = question.base.correctAnswers
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
    ) { innerPadding ->
        content(innerPadding)
    }
}

@Composable
fun ComposeQuizQuestion(
    question: QuestionQueried,
    innerPadding: PaddingValues
) {
    Box(
        modifier = Modifier
            .padding(innerPadding)
            .fillMaxWidth()
            .fillMaxHeight(fraction = 0.88f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(fraction = if (question.base.pictureID.isNullOrEmpty() && question.base.videoID.isNullOrEmpty()) 0.3f else 0.5f)
                .align(Alignment.TopCenter),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = question.base.text,
                textAlign = TextAlign.Center,
                lineHeight = 28.sp,
                fontSize = 18.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp)
                    .padding(end = 10.dp)
            )
            if (!question.base.videoID.isNullOrEmpty()) {
                // TODO: Video playback
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
                .fillMaxHeight(fraction = if (question.base.pictureID.isNullOrEmpty() && question.base.videoID.isNullOrEmpty()) 0.7f else 0.5f)
                .align(Alignment.BottomCenter),
            contentPadding = PaddingValues(8.dp),
            verticalArrangement = Arrangement.spacedBy(15.dp)
        ) {
            items(count = min(a = question.answers.size, b = 4)) { index ->
                ComposeQuestionAnswerCard(
                    answer = question.answers[index],
                    index,
                    question
                )
            }
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
                                thumbnailID = testSet.questions[index * 2].base.thumbnailID
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
                                    thumbnailID = testSet.questions[index * 2 + 1].base.thumbnailID
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
    thumbnailID: String? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(fraction = if (index % 2 == 0) 0.36f else 0.7f),
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
            containerColor = if (testSet.base.stateCurrentQuestionIndex == index) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
        ),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp)
        ) {
            val number = index + 1
            Text(
                text = if (number < 10) " $number.  " else "$number. ",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            if (!thumbnailID.isNullOrEmpty()) {
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
    answer: Answer,
    index: Int,
    question: QuestionQueried
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = {
            if (index in question.state.stateSelectedAnswerIndexes) {
                question.state.stateSelectedAnswerIndexes = question.state.stateSelectedAnswerIndexes.filterNot { it == index }
            } else {
                question.state.stateSelectedAnswerIndexes += index
            }
            CoroutineScope(Dispatchers.IO).launch {
                question.save()
            }
        },
        colors = CardDefaults.cardColors(
            containerColor = if (index in question.state.stateSelectedAnswerIndexes) MaterialTheme.colorScheme.inversePrimary else MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
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
    }
}