package com.provigz.avtotest

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.provigz.avtotest.db.TestSetDatabase
import com.provigz.avtotest.db.entity.Property
import com.provigz.avtotest.db.entity.TestSet
import com.provigz.avtotest.model.TestSetCategory
import com.provigz.avtotest.ui.theme.AvtoTestTheme
import com.provigz.avtotest.util.ComposeLoadingPrompt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val db = TestSetDatabase.getDatabase(context = this)
        val testSetDao = db.testSetDao()

        enableEdgeToEdge()
        setContent {
            AvtoTestTheme {
                val selectedCategoryIDState by produceState<Pair<Boolean, Int?>>(false to null) {
                    val data = testSetDao.getProperty("selectedCategory")
                    value = true to data?.toInt()
                }
                var (hasLoadedSelectedCategoryID, selectedCategoryID) = selectedCategoryIDState

                if (!hasLoadedSelectedCategoryID) {
                    ComposeLoadingPrompt()
                } else {
                    if (selectedCategoryID == null || TestSetCategory.fromInt(selectedCategoryID) == null) {
                        selectedCategoryID = TestSetCategory.B.toInt()
                        LaunchedEffect(Unit) {
                            testSetDao.setProperty(
                                Property(
                                    name = "selectedCategory",
                                    value = selectedCategoryID.toString()
                                )
                            )
                        }
                    }

                    val testSetIDState by produceState<Pair<Boolean, String?>>(false to null) {
                        val data = testSetDao.getProperty("startedTestSet")
                        value = true to data
                    }
                    val (hasLoadedTestSetID, startedTestSetID) = testSetIDState

                    if (!hasLoadedTestSetID) {
                        ComposeLoadingPrompt()
                    } else {
                        val testSetState by produceState<Pair<Boolean, TestSet?>>(false to null) {
                            if (startedTestSetID.isNullOrEmpty()) {
                                value = true to null
                                return@produceState
                            }
                            val data = testSetDao.getTestSetByID(startedTestSetID.toInt())
                            value = true to data
                        }
                        val (hasLoadedTestSet, startedTestSet) = testSetState

                        if (!hasLoadedTestSet) {
                            ComposeLoadingPrompt()
                        } else {
                            ComposeMainScaffold(
                                context = this@MainActivity,
                                selectedCategoryInitial = TestSetCategory.fromInt(selectedCategoryID)!!,
                                onCategorySelect = { category ->
                                    CoroutineScope(Dispatchers.IO).launch {
                                        testSetDao.setProperty(
                                            Property(
                                                name = "selectedCategory",
                                                value = category.toInt().toString()
                                            )
                                        )
                                    }
                                },
                                startedTestSet,
                                onTestSetDelete = {
                                    CoroutineScope(Dispatchers.IO).launch {
                                        testSetDao.deleteTestSetByID(startedTestSet!!.id)
                                        runOnUiThread {
                                            recreate()
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onRestart() {
        super.onRestart()
        recreate()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
fun ComposeMainScaffold(
    context: MainActivity = MainActivity(),
    selectedCategoryInitial: TestSetCategory = TestSetCategory.B,
    onCategorySelect: (TestSetCategory) -> Unit = {},
    startedTestSet: TestSet? = null,
    onTestSetDelete: () -> Unit = {}
) {
    val active = startedTestSet == null
    
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
                        text = "AvtoTest",
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold
                    )
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            val selectedCategory = remember { mutableStateOf(selectedCategoryInitial) }

            Text(
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .fillMaxHeight(fraction = 0.3f),
                text =
                """
                    This is an example of a scaffold. It uses the Scaffold composable's parameters to create a screen with a simple top app bar, bottom app bar, and floating action button.
    
                    It also contains some basic inner content, such as this text.
                """.trimIndent(),
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(fraction = if (active) 0.85f else 0.7f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .blur(if (active) 0.dp else 10.dp)
                        .verticalScroll(
                            state = rememberScrollState(),
                            enabled = active
                        ),
                    verticalArrangement = Arrangement.spacedBy(13.dp)
                ) {
                    ComposeCategoryRow {
                        ComposeCategoryIcon(
                            category = TestSetCategory.A,
                            icon = R.drawable.car,
                            selectedCategory, onCategorySelect, active
                        )
                        ComposeCategoryIcon(
                            category = TestSetCategory.A1,
                            icon = R.drawable.car,
                            selectedCategory, onCategorySelect, active
                        )
                        ComposeCategoryIcon(
                            category = TestSetCategory.A2,
                            icon = R.drawable.car,
                            selectedCategory, onCategorySelect, active
                        )
                        ComposeCategoryIcon(
                            category = TestSetCategory.AM,
                            icon = R.drawable.car,
                            selectedCategory, onCategorySelect, active
                        )
                    }
                    ComposeCategoryRow {
                        ComposeCategoryIcon(
                            category = TestSetCategory.B,
                            icon = R.drawable.car,
                            selectedCategory, onCategorySelect, active
                        )
                        ComposeCategoryIcon(
                            category = TestSetCategory.B1,
                            icon = R.drawable.car,
                            selectedCategory, onCategorySelect, active
                        )
                    }
                    ComposeCategoryRow {
                        ComposeCategoryIcon(
                            category = TestSetCategory.C,
                            icon = R.drawable.car,
                            selectedCategory, onCategorySelect, active
                        )
                        ComposeCategoryIcon(
                            category = TestSetCategory.C1,
                            icon = R.drawable.car,
                            selectedCategory, onCategorySelect, active
                        )
                        ComposeCategoryIcon(
                            category = TestSetCategory.CE,
                            icon = R.drawable.car,
                            selectedCategory, onCategorySelect, active
                        )
                    }
                    ComposeCategoryRow {
                        ComposeCategoryIcon(
                            category = TestSetCategory.D,
                            icon = R.drawable.car,
                            selectedCategory, onCategorySelect, active
                        )
                        ComposeCategoryIcon(
                            category = TestSetCategory.D1,
                            icon = R.drawable.car,
                            selectedCategory, onCategorySelect, active
                        )
                        ComposeCategoryIcon(
                            category = TestSetCategory.DE,
                            icon = R.drawable.car,
                            selectedCategory, onCategorySelect, active
                        )
                    }
                    ComposeCategoryRow {
                        ComposeCategoryIcon(
                            category = TestSetCategory.TKT,
                            icon = R.drawable.car,
                            selectedCategory, onCategorySelect, active
                        )
                        ComposeCategoryIcon(
                            category = TestSetCategory.TTB,
                            icon = R.drawable.car,
                            selectedCategory, onCategorySelect, active
                        )
                        ComposeCategoryIcon(
                            category = TestSetCategory.TTM,
                            icon = R.drawable.car,
                            selectedCategory, onCategorySelect, active
                        )
                    }
                    ComposeCategoryRow {
                        ComposeCategoryIcon(
                            category = TestSetCategory.BTA,
                            icon = R.drawable.car,
                            selectedCategory, onCategorySelect, active
                        )
                    }
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "ADR придобиване",
                            textAlign = TextAlign.Center,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.fillMaxWidth()
                        )
                        ComposeCategoryRow {
                            ComposeCategoryIcon(
                                category = TestSetCategory.OGain,
                                icon = R.drawable.car,
                                selectedCategory, onCategorySelect, active
                            )
                            ComposeCategoryIcon(
                                category = TestSetCategory.CGain,
                                icon = R.drawable.car,
                                selectedCategory, onCategorySelect, active
                            )
                            ComposeCategoryIcon(
                                category = TestSetCategory.ADRGain1,
                                icon = R.drawable.car,
                                selectedCategory, onCategorySelect, active
                            )
                            ComposeCategoryIcon(
                                category = TestSetCategory.ADRGain7,
                                icon = R.drawable.car,
                                selectedCategory, onCategorySelect, active
                            )
                        }
                    }
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "ADR удължаване",
                            textAlign = TextAlign.Center,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.fillMaxWidth()
                        )
                        ComposeCategoryRow {
                            ComposeCategoryIcon(
                                category = TestSetCategory.OExtend,
                                icon = R.drawable.car,
                                selectedCategory, onCategorySelect, active
                            )
                            ComposeCategoryIcon(
                                category = TestSetCategory.CExtend,
                                icon = R.drawable.car,
                                selectedCategory, onCategorySelect, active
                            )
                            ComposeCategoryIcon(
                                category = TestSetCategory.ADRExtend1,
                                icon = R.drawable.car,
                                selectedCategory, onCategorySelect, active
                            )
                            ComposeCategoryIcon(
                                category = TestSetCategory.ADRExtend7,
                                icon = R.drawable.car,
                                selectedCategory, onCategorySelect, active
                            )
                        }
                    }
                }
                if (startedTestSet != null) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(fraction = 0.8f)
                            .wrapContentHeight(),
                        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically)
                    ) {
                        Text(
                            text = "Имате вече започната листовка:",
                            textAlign = TextAlign.Center,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            text = """
                                #${startedTestSet.id}
                                Категория: ${TestSetCategory.fromInt(startedTestSet.categoryID)}
                                Изминато време: ${String.format(
                                    locale = Locale.US,
                                    format = "%02d:%02d",
                                    startedTestSet.stateSecondsPassed / 60, startedTestSet.stateSecondsPassed % 60
                                )}
                            """.trimIndent(),
                            textAlign = TextAlign.Center,
                            fontSize = 20.sp,
                            lineHeight = 30.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 15.dp)
                        )
                        Text(
                            text = "Можете да я продължите.",
                            textAlign = TextAlign.Center,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
            if (startedTestSet != null) {
                var showCancelWarningDialog by remember { mutableStateOf(false) }

                FloatingActionButton(
                    onClick = {
                        showCancelWarningDialog = true
                    },
                    containerColor = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp)
                        .padding(top = 20.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(7.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Clear, contentDescription = "Отказ")
                        Text(
                            text = "Отказ",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (showCancelWarningDialog) {
                    AlertDialog(
                        onDismissRequest = {
                            showCancelWarningDialog = false
                        },
                        title = {
                            Text(
                                text = "Внимание"
                            )
                        },
                        text = {
                            Text(
                                text = "Това ще изтрие текущата листовка и вашия напредък.\n\nСигурни ли сте?"
                            )
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    showCancelWarningDialog = false
                                    onTestSetDelete()
                                }
                            ) {
                                Text(text = "Да")
                            }
                        },
                        dismissButton = {
                            Button(
                                onClick = {
                                    showCancelWarningDialog = false
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
            FloatingActionButton(
                onClick = {
                    val intent = Intent(context, QuizActivity::class.java)
                    if (active) {
                        intent.putExtra("categoryID", selectedCategory.value.toInt())
                    }
                    context.startActivity(intent)
                },
                containerColor = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp)
                    .padding(top = 20.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(7.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Решавай")
                    Text(
                        text = "Решавай",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun ComposeCategoryRow(content: @Composable () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(15.dp, Alignment.CenterHorizontally),
        modifier = Modifier.fillMaxWidth()
    ) {
        content()
    }
}

@Composable
fun ComposeCategoryIcon(
    category: TestSetCategory,
    icon: Int,
    selectedCategory: MutableState<TestSetCategory>,
    onCategorySelect: (TestSetCategory) -> Unit,
    active: Boolean
) {
    val selected = selectedCategory.value == category
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(
            enabled = active
        ) {
            if (!selected) {
                selectedCategory.value = category
                onCategorySelect(category)
            }
        }
    ) {
        Image(
            painter = painterResource(id = icon),
            contentDescription = "Категория $category",
            modifier = Modifier
                .width(60.dp)
                .height(30.dp),
            colorFilter = ColorFilter.tint(if (selected) Color(color = 0xFFFFA500) else MaterialTheme.colorScheme.onBackground)
        )
        Text(
            text = category.toString(),
            textAlign = TextAlign.Center,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
}