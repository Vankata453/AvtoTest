package com.provigz.avtotest

import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.provigz.avtotest.db.TestSetDatabase
import com.provigz.avtotest.db.entity.Answer
import com.provigz.avtotest.db.entity.Question
import com.provigz.avtotest.db.entity.QuestionQueried
import com.provigz.avtotest.db.entity.QuestionQueriedDistributionData
import com.provigz.avtotest.db.entity.QuestionState
import com.provigz.avtotest.ui.theme.AvtoTestTheme
import com.provigz.avtotest.util.ComposeLoadingPrompt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class QuestionActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val db = TestSetDatabase.getDatabase(context = this)
        val testSetDao = db.testSetDao()

        enableEdgeToEdge()
        setContent {
            AvtoTestTheme {
                val questionID = intent.getIntExtra("questionID", -1)
                val questionState by produceState<Pair<Boolean, QuestionQueriedDistributionData?>>(false to null) {
                    if (questionID < 0) {
                        value = true to null
                        return@produceState
                    }
                    val question = testSetDao.getQuestionByID(questionID)
                    if (question == null) {
                        value = true to null
                        return@produceState
                    }
                    val questionQueried = question.query(testSetDao)
                    val questionQueriedDistributionData = questionQueried.getDistributionData()
                    value = true to questionQueriedDistributionData
                }
                val (hasLoadedQuestion, questionData) = questionState

                if (hasLoadedQuestion) {
                    if (questionData == null) {
                        AlertDialog(
                            onDismissRequest = {},
                            title = {
                                Text(
                                    text = "Грешка"
                                )
                            },
                            text = {
                                Text(
                                    text = "Въпросът не може да бъде зареден!"
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
                        ComposeQuestionScaffold(
                            questionData
                        )
                    }
                } else {
                    ComposeLoadingPrompt(text = "Зареждане на въпрос")
                }
            }
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
fun ComposeQuestionActivityPreview() {
    val question = QuestionQueriedDistributionData(
        question = QuestionQueried(
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
                testSetID = -1,
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
        occurrenceCount = 12
    )

    AvtoTestTheme {
        ComposeQuestionScaffold(
            question
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComposeQuestionScaffold(
    q: QuestionQueriedDistributionData
) {
    val questionUpdateCount by q.question.updateCount.collectAsState() // Used to trigger recompositions
    Log.i(
        "QuestionActivity",
        "Question ${q.question.base.id} was updated. Individual updates: $questionUpdateCount"
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            Column {
                TopAppBar(
                    colors = topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.primary,
                    ),
                    title = {
                        Text(
                            text = "Въпрос #${q.question.base.id}",
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    actions = {
                        IconButton(
                            onClick = {
                                CoroutineScope(Dispatchers.IO).launch {
                                    q.question.base.favorite = !q.question.base.favorite
                                    q.question.save()
                                }
                            }
                        ) {
                            Image(
                                painter = painterResource(id = if (q.question.base.favorite) R.drawable.heart_filled else R.drawable.heart),
                                contentDescription = "Маркирай към любими",
                                colorFilter = ColorFilter.tint(if (isSystemInDarkTheme()) Color.White else Color.Black),
                                modifier = Modifier.size(30.dp)
                            )
                        }
                    }
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(70.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 30.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.history),
                                contentDescription = "Пъти срещан",
                                colorFilter = ColorFilter.tint(if (isSystemInDarkTheme()) Color.White else Color.Black),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(7.dp))
                            Text(
                                text = "${q.occurrenceCount}",
                                fontSize = 20.sp
                            )
                        }
                        Text(
                            text = "Точки: ${q.question.base.points}",
                            fontSize = 20.sp,
                            modifier = Modifier.padding(horizontal = 20.dp)
                        )
                        Text(
                            text = "Верни: ${q.question.base.correctAnswers}",
                            fontSize = 20.sp,
                            modifier = Modifier.padding(horizontal = 20.dp)
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        ComposeQuizQuestion(
            innerPadding,
            question = q.question,
            interactive = false,
            showResults = true
        )
    }
}
