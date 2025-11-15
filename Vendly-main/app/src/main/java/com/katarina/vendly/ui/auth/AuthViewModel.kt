package com.katarina.vendly.ui.auth

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {

    private var auth: FirebaseAuth = Firebase.auth

    //promenljiva ko amoze da se menja
    private val _authState = MutableLiveData<AuthState>(AuthState.Loading)
    //javna verzija samo za posmatranje
    val authState: LiveData<AuthState> = _authState

    private val userRepository = com.katarina.vendly.data.user.UserRepository()

    //proverava da li je korisnik prijavljen
    init {
        _authState.value = if (auth.currentUser == null) {
            AuthState.Unauthenticated
        } else {
            AuthState.Authenticated
        }
    }

    fun login(email: String, password: String) {
        if (email.isEmpty() || password.isEmpty()) {
            _authState.postValue(AuthState.Error("Email or password can't be empty"))
            return
        }

        //firebase proverava email i password, ako su tacni prijavljuje, ako nisu vraca gresku
        _authState.postValue(AuthState.Loading)
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    _authState.postValue(AuthState.Authenticated)
                } else {
                    _authState.postValue(
                        AuthState.Error(task.exception?.message ?: "Something went wrong")
                    )
                }
            }
    }

    fun signup(
        email: String,
        password: String,
        fullName: String,
        phoneNumber: String,
        profileImageUrl: String?
    ) {
        if (email.isEmpty() || password.isEmpty()) {
            _authState.postValue(AuthState.Error("Email or password can't be empty"))
            return
        }

        //kreiramo novog korisnika u firebase
        _authState.postValue(AuthState.Loading)
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                //provera da li je kreiranje naloga uspelo, ako nije vraca gresku
                if (!task.isSuccessful) {
                    _authState.postValue(
                        AuthState.Error(task.exception?.message ?: "Something went wrong")
                    )
                    return@addOnCompleteListener
                }

                //svaki korisnik ima svoj uid koji se koristi za cuvanje podataka
                val uid = auth.currentUser?.uid
                if (uid == null) {
                    _authState.postValue(AuthState.Error("No UID found"))
                    return@addOnCompleteListener
                }

                //pripremamo podatke za firestore
                val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                val userMap = hashMapOf(
                    "uid" to uid,
                    "email" to email,
                    "fullName" to fullName,
                    "phoneNumber" to phoneNumber,
                    "profileImageUrl" to profileImageUrl
                )

                //cuvamo podatke u firestore
                db.collection("users")
                    .document(uid)
                    .set(userMap)
                    .addOnSuccessListener { _authState.postValue(AuthState.Authenticated) }
                    .addOnFailureListener { e ->
                        _authState.postValue(AuthState.Error("Firestore error: ${e.message}"))
                    }
            }
    }

    fun signout() {
        viewModelScope.launch {
            val uid = auth.currentUser?.uid
            if (uid != null) {
                userRepository.clearUserLocation(uid) //brisemo lokaciju kada se odjavljujemo
            }
            auth.signOut()
            _authState.value = AuthState.Unauthenticated
        }
    }
}