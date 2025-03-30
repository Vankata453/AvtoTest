package com.provigz.avtotest

import android.content.Intent
import android.icu.text.SimpleDateFormat
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.provigz.avtotest.db.TestSetDao
import com.provigz.avtotest.db.TestSetDatabase
import com.provigz.avtotest.db.entity.TestSet
import com.provigz.avtotest.model.TestSetCategory
import com.provigz.avtotest.model.TestSetSubCategory
import com.provigz.avtotest.ui.theme.AvtoTestTheme
import com.provigz.avtotest.util.ComposeLoadingPrompt
import kotlinx.coroutines.flow.filter
import java.util.Locale

const val ITEM_LIMIT = 20

class QuizListActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val db = TestSetDatabase.getDatabase(context = this)
        val testSetDao = db.testSetDao()

        enableEdgeToEdge()
        setContent {
            AvtoTestTheme {
                ComposeQuizListScaffold(
                    testSetDao
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComposeQuizListScaffold(
    testSetDao: TestSetDao
) {
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
                        text = "Листовки",
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold
                    )
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier.padding(innerPadding)
        ) {
            var page = remember { 1 }
            var reachedEnd by remember { mutableStateOf(false) }
            var testSets by remember { mutableStateOf(emptyArray<TestSet>()) }
            val listState = rememberLazyListState()

            LaunchedEffect(listState) {
                snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull() }
                    .filter { it != null && it.index >= testSets.lastIndex }
                    .collect {
                        val moreTestSets = testSetDao.getLatestTestSets(
                            limit = ITEM_LIMIT,
                            offset = ITEM_LIMIT * page
                        )
                        if (moreTestSets.isEmpty()) {
                            reachedEnd = true
                        } else {
                            ++page
                            testSets += moreTestSets
                        }
                    }
            }

            if (testSets.isEmpty()) {
                LaunchedEffect(Unit) {
                    testSets = testSetDao.getLatestTestSets(
                        limit = ITEM_LIMIT,
                        offset = 0
                    )
                }
                ComposeLoadingPrompt(text = "Зареждане на листовки")
            } else {
                ComposeQuizList(
                    listState,
                    testSets,
                    reachedEnd
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ComposeQuizListPreview() {
    val testSet = TestSet(
        com.provigz.avtotest.model.TestSet(
            id = 216782909,
            subCategory = TestSetSubCategory(
                id = TestSetCategory.B,
                categoryID = 1,
                name = "B",
                durationMinutes = 40
            ),
            questions = emptyArray(),
        )
    )
    testSet.stateSecondsPassed = 2100
    testSet.stateTimedOut = true
    testSet.stateCurrentQuestionIndex = 0
    testSet.stateAssessed = true
    testSet.stateReceivedPoints = 87
    testSet.stateTotalPoints = 97
    testSet.stateCorrectQuestionsCount = 42
    testSet.stateIncorrectQuestionsCount = 3
    testSet.statePassed = true

    ComposeQuizList(
        listState = rememberLazyListState(),
        testSets = arrayOf(testSet),
        reachedEnd = false
    )
}

@Composable
fun ComposeQuizList(
    listState: LazyListState,
    testSets: Array<TestSet>,
    reachedEnd: Boolean
) {
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize()
    ) {
        itemsIndexed(testSets) { index, testSet ->
            ComposeQuizListEntry(
                testSet
            )
            if (!reachedEnd || index < testSets.lastIndex) {
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
fun ComposeQuizListEntry(
    testSet: TestSet
) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .padding(all = 10.dp)
            .fillMaxWidth()
            .clickable {
                val intent = Intent(context, QuizActivity::class.java)
                intent.putExtra("testSetID", testSet.id)
                context.startActivity(intent)
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(fraction = 0.1f)
        ) {
            val category = TestSetCategory.fromInt(testSet.categoryID)
            if (category != null) {
                Image(
                    painter = painterResource(id = category.getIcon()),
                    contentDescription = "Категория $category",
                    modifier = Modifier.size(40.dp)
                )
                Text(
                    text = "$category",
                    textAlign = TextAlign.Center,
                    fontSize = 15.sp,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        Column(
            modifier = Modifier
                .padding(start = 10.dp, top = 10.dp)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "#${testSet.id}",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                if (!testSet.voucherCode.isNullOrEmpty()) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.voucher),
                            contentDescription = "Ваучер",
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = testSet.voucherCode,
                            fontSize = 12.sp
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(5.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (testSet.stateAssessed) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Canvas(
                            modifier = Modifier.size(25.dp)
                        ) {
                            if (testSet.statePassed) {
                                drawLine(
                                    color = Color.Green,
                                    start = center.copy(
                                        x = center.x - 6.dp.toPx(),
                                        y = center.y
                                    ),
                                    end = center.copy(
                                        x = center.x - 1.dp.toPx(),
                                        y = center.y + 5.dp.toPx()
                                    ),
                                    strokeWidth = 5f
                                )
                                drawLine(
                                    color = Color.Green,
                                    start = center.copy(
                                        x = center.x - 3.dp.toPx(),
                                        y = center.y + 5.dp.toPx()
                                    ),
                                    end = center.copy(
                                        x = center.x + 6.dp.toPx(),
                                        y = center.y - 5.dp.toPx()
                                    ),
                                    strokeWidth = 5f
                                )
                            } else {
                                drawLine(
                                    color = Color.Red,
                                    start = center.copy(
                                        x = center.x - 5.dp.toPx(),
                                        y = center.y - 5.dp.toPx()
                                    ),
                                    end = center.copy(
                                        x = center.x + 5.dp.toPx(),
                                        y = center.y + 5.dp.toPx()
                                    ),
                                    strokeWidth = 5f
                                )
                                drawLine(
                                    color = Color.Red,
                                    start = center.copy(
                                        x = center.x - 5.dp.toPx(),
                                        y = center.y + 5.dp.toPx()
                                    ),
                                    end = center.copy(
                                        x = center.x + 5.dp.toPx(),
                                        y = center.y - 5.dp.toPx()
                                    ),
                                    strokeWidth = 5f
                                )
                            }
                        }
                        Text(
                            text = "${testSet.stateCorrectQuestionsCount}/${testSet.questionIDs.size} (${testSet.stateReceivedPoints}/${testSet.stateTotalPoints} т.)",
                            fontSize = 12.sp
                        )
                    }
                }
                if (testSet.voucherCode.isNullOrEmpty()) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.time),
                            contentDescription = "Време",
                            modifier = Modifier.size(15.dp)
                        )
                        val secondsPassed = testSet.getSecondsPassed()
                        Text(
                            text = String.format(
                                locale = Locale.US,
                                format = "%02d:%02d",
                                secondsPassed / 60,
                                secondsPassed % 60
                            ),
                            fontSize = 12.sp
                        )
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.calendar),
                        contentDescription = "Дата",
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = SimpleDateFormat(
                            "dd.MM.yyyy г.",
                            Locale.US
                        ).format(testSet.timeStarted),
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}
