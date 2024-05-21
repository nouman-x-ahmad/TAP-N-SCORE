package com.noumanahmad.tapandscore


import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.noumanahmad.tapandscore.ui.theme.TAPandSCORETheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TAPandSCORETheme {
                AppNavigator()
            }
        }
    }
}

@Composable
fun AppNavigator() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Screen.MainScreen.route) {
        composable(Screen.MainScreen.route) {
            MainScreen(navController = navController)
        }
        composable(Screen.GameScreen.route) {
            GameScreen(navController = navController)
        }
        composable(Screen.TopScoresScreen.route) {
            TopScoresScreen(navController = navController, dbHelper = ScoreDbHelper(LocalContext.current))
        }
        composable(Screen.InstructionsScreen.route) { // Updated route to InstructionsScreen
            InstructionScreen(navController = navController)
        }

    }
}



@Composable
fun MainScreen(navController: NavController) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(onClick = { navController.navigate(Screen.GameScreen.route) }) {
            Text("Enter")
        }
        Button(onClick = { navController.navigate(Screen.TopScoresScreen.route) }) {
            Text("Top Scores")
        }
        Button(onClick = { navController.navigate(Screen.InstructionsScreen.route) }) {
            Text("Instructions")
        }

    }
}
@Composable
fun InstructionScreen(navController: NavController) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Instructions for Tap and Score:"
            ,
            modifier = Modifier.padding(16.dp)

        )
        Card(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(text = "1. Tap the rectangle to score points")
            Text(text = "2. Each tap scores 2 point")
            Text(text = "3. Tapping outside the rectangle deducts 1 point")
            Text(text = "4. You have 60 seconds to score as many points as possible")
            Text(text = "5. Your top 5 scores are saved and can be viewed by tapping 'Top Scores' button in the menu")

        }

        Button(onClick = { navController.navigateUp() }) {
            Text("Back")
        }
    }
}



@Composable
fun GameScreen(navController: NavController) {
    AndroidView(factory = { context ->
        GameSurfaceView(context, null).apply {
            // You can set any initial properties or listeners here if needed
        }
    })
}

@Composable
fun TopScoresScreen(navController: NavController, dbHelper: ScoreDbHelper) {
    val context = LocalContext.current
    val scores = remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        scores.value = getTopScores(dbHelper)
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = scores.value)
        Button(onClick = { navController.navigateUp() }) {
            Text("Back")
        }
    }
}

fun getTopScores(dbHelper: ScoreDbHelper): String {
    val db = dbHelper.readableDatabase
    val cursor = db.query(
        ScoreContract.ScoreEntry.TABLE_NAME,
        arrayOf(ScoreContract.ScoreEntry.COLUMN_NAME_PLAYER, ScoreContract.ScoreEntry.COLUMN_NAME_SCORE),
        null, null, null, null,
        "${ScoreContract.ScoreEntry.COLUMN_NAME_SCORE} DESC"
    )

    val scores = StringBuilder()
    with(cursor) {
        while (moveToNext()) {
            val player = getString(getColumnIndexOrThrow(ScoreContract.ScoreEntry.COLUMN_NAME_PLAYER))
            val score = getInt(getColumnIndexOrThrow(ScoreContract.ScoreEntry.COLUMN_NAME_SCORE))
            scores.append("$player: $score\n")
        }
    }
    cursor.close()
    return scores.toString()
}


