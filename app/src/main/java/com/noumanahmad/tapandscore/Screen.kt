package com.noumanahmad.tapandscore


sealed class Screen(val route: String) {
    object MainScreen : Screen("mainScreen")
    object GameScreen : Screen("gameScreen")
    object InstructionsScreen : Screen("instructions_screen")
    object TopScoresScreen : Screen("top_scores_screen")

}