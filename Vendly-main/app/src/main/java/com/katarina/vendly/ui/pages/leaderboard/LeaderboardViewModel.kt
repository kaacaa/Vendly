package com.katarina.vendly.ui.pages.leaderboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.katarina.vendly.data.user.LeaderboardUser
import com.katarina.vendly.data.user.UserRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class LeaderboardViewModel(
    private val userRepo: UserRepository = UserRepository(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : ViewModel() {

    private val _ui = MutableStateFlow(
        LeaderboardUiState(meUid = auth.currentUser?.uid)
    )
    val ui: StateFlow<LeaderboardUiState> = _ui

    init {
        viewModelScope.launch {
            val meUid = auth.currentUser?.uid

            val top10Flow = userRepo.observeLeaderboard(limit = 10)
            val myRankFlow: Flow<Pair<Int, LeaderboardUser?>?> =
                if (meUid.isNullOrBlank()) flowOf(null)
                else userRepo.observeMyRank(meUid).map { it }

            //reaguje na promene u poenima
            combine(top10Flow, myRankFlow) { top10, myRankPair ->
                val meRank = myRankPair?.first
                val meUser = myRankPair?.second

                //ako je u top 10 ne treba ispod da se prikazuje rang
                val isMeInTop = meUser?.uid?.let { uid -> top10.any { it.uid == uid } } ?: false
                val meOutsideTop = if (!isMeInTop && meUser != null) meUser else null

                _ui.update {
                    it.copy(
                        loading = false,
                        error = null,
                        users = top10,
                        meRank = meRank,
                        meOutsideTop = meOutsideTop
                    )
                }
            }.catch { e ->
                _ui.update { it.copy(loading = false, error = e.message ?: "Error") }
            }.collect()
        }
    }
}