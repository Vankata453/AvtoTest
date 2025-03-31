package com.provigz.avtotest

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.provigz.avtotest.db.TestSetDao
import com.provigz.avtotest.db.TestSetDatabase
import com.provigz.avtotest.db.entity.Question
import com.provigz.avtotest.db.entity.QuestionWithOccurrenceCount
import com.provigz.avtotest.ui.theme.AvtoTestTheme
import com.provigz.avtotest.util.ComposeDropdownMenu
import com.provigz.avtotest.util.ComposeLoadingPrompt
import kotlinx.coroutines.flow.filter

const val QUESTION_ITEM_LIMIT = 20

enum class QuestionSortOptions(val sql: String) {
    AtoZ(sql = "REPLACE(text,'\"','') ASC"), // TODO: Figure out how to properly ignore leading " in order
    ZtoA(sql = "REPLACE(text,'\"','') DESC"), // TODO: Figure out how to properly ignore leading " in order
    MostCommon(sql = "occurrenceCount DESC"),
    LeastCommon(sql = "occurrenceCount ASC")
}
enum class QuestionFilterOptions(val sql: String) {
    All(sql = ""),
    Image(sql = "q.pictureID IS NOT NULL"),
    Video(sql = "q.videoID IS NOT NULL"),
    Favorite(sql = "q.favorite = 1")
}

class QuestionListActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val db = TestSetDatabase.getDatabase(context = this)
        val testSetDao = db.testSetDao()

        enableEdgeToEdge()
        setContent {
            AvtoTestTheme {
                ComposeQuestionListScaffold(
                    testSetDao
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComposeQuestionListScaffold(
    testSetDao: TestSetDao
) {
    val sortCriteria = remember { mutableIntStateOf(0) }
    val filterCriteria = remember { mutableIntStateOf(0) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                colors = topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                ),
                title = {
                    Text(
                        text = "Въпроси",
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold
                    )
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier.padding(innerPadding)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ComposeDropdownMenu(
                    options = arrayOf(
                        "А-Я",
                        "Я-А",
                        "Най-често срещани",
                        "Най-рядко срещани"
                    ),
                    selectedIndex = sortCriteria,
                    modifier = Modifier.fillMaxWidth(fraction = 0.5f)
                )
                ComposeDropdownMenu(
                    options = arrayOf(
                        "Всички",
                        "Изображение",
                        "Видео",
                        "Любими"
                    ),
                    selectedIndex = filterCriteria,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            var page = remember { 1 }
            var reachedEnd by remember { mutableStateOf(false) }
            var questions by remember { mutableStateOf(emptyList<QuestionWithOccurrenceCount>()) }
            val listState = rememberLazyListState()

            LaunchedEffect(Pair(sortCriteria.intValue, filterCriteria.intValue)) {
                page = 1
                reachedEnd = false
                questions = testSetDao.getQuestionsAndOccurenceCountSortFilter(
                    limit = QUESTION_ITEM_LIMIT,
                    offset = 0,
                    sortQuery = QuestionSortOptions.entries[sortCriteria.intValue].sql,
                    filterQuery = QuestionFilterOptions.entries[filterCriteria.intValue].sql
                )
            }
            LaunchedEffect(listState) {
                snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull() }
                    .filter { it != null && it.index >= questions.lastIndex }
                    .collect {
                        val moreQuestions = testSetDao.getQuestionsAndOccurenceCountSortFilter(
                            limit = QUESTION_ITEM_LIMIT,
                            offset = QUESTION_ITEM_LIMIT * page,
                            sortQuery = QuestionSortOptions.entries[sortCriteria.intValue].sql,
                            filterQuery = QuestionFilterOptions.entries[filterCriteria.intValue].sql
                        )
                        if (moreQuestions.isEmpty()) {
                            reachedEnd = true
                        } else {
                            ++page
                            questions += moreQuestions
                        }
                    }
            }

            if (questions.isEmpty()) {
                ComposeLoadingPrompt(text = "Зареждане на въпроси")
            } else {
                ComposeQuestionList(
                    listState,
                    questions,
                    reachedEnd
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ComposeQuestionListPreview() {
    val q = QuestionWithOccurrenceCount(
        question = Question(
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
        occurrenceCount = 12
    )
    q.question.favorite = true

    ComposeQuestionList(
        listState = rememberLazyListState(),
        questions = listOf(q),
        reachedEnd = false
    )
}

@Composable
fun ComposeQuestionList(
    listState: LazyListState,
    questions: List<QuestionWithOccurrenceCount>,
    reachedEnd: Boolean
) {
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize()
    ) {
        itemsIndexed(questions) { index, question ->
            ComposeQuestionListEntry(
                question
            )
            if (!reachedEnd || index < questions.lastIndex) {
                HorizontalDivider(
                    thickness = 1.dp,
                    color = Color.Gray
                )
            }
        }
        if (!reachedEnd) {
            item {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 5.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(30.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ComposeQuestionListEntry(
    q: QuestionWithOccurrenceCount
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .padding(all = 10.dp)
            .fillMaxWidth()
            .clickable {
                val intent = Intent(context, QuestionActivity::class.java)
                intent.putExtra("questionID", q.question.id)
                context.startActivity(intent)
            }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!q.question.thumbnailID.isNullOrEmpty()) {
                AsyncImage(
                    model = "https://avtoizpit.com/api/pictures/${q.question.thumbnailID}.png?thumbnail=true",
                    contentDescription = "Изображение",
                    modifier = Modifier.size(20.dp)
                )
            }
            Text(
                text = q.question.text,
                fontSize = 15.sp,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
                modifier = Modifier.fillMaxWidth(fraction = if (q.question.favorite) 0.92f else 1f)
            )
            if (q.question.favorite) {
                Image(
                    painter = painterResource(id = R.drawable.red_heart_filled),
                    contentDescription = "Любим",
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(5.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(id = R.drawable.history),
                    contentDescription = "Пъти срещан",
                    colorFilter = ColorFilter.tint(if (isSystemInDarkTheme()) Color.White else Color.Black),
                    modifier = Modifier.size(15.dp)
                )
                Text(
                    text = "${q.occurrenceCount}",
                    fontSize = 12.sp
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(id = R.drawable.point),
                    contentDescription = "Точки",
                    colorFilter = ColorFilter.tint(if (isSystemInDarkTheme()) Color.White else Color.Black),
                    modifier = Modifier.size(15.dp)
                )
                Text(
                    text = "${q.question.points}",
                    fontSize = 12.sp
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(id = R.drawable.checkmarks),
                    contentDescription = "Верни отговори",
                    colorFilter = ColorFilter.tint(if (isSystemInDarkTheme()) Color.White else Color.Black),
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "${q.question.correctAnswers}",
                    fontSize = 12.sp
                )
            }
        }
    }
}
