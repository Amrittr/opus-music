/**
 * Opus: Firebase account login.
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.auth

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseOptions
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.metrolist.music.BuildConfig
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import timber.log.Timber

/** What the UI needs to know about the signed-in Opus user. */
data class OpusUser(
    val uid: String,
    val name: String,
    val email: String,
    val isEmailVerified: Boolean,
    val createdAtMs: Long?,
    val lastSignInAtMs: Long?,
)

/** Thrown with a message that is safe to show to the user. */
class OpusAuthException(message: String) : Exception(message)

object OpusAuth {
    const val MIN_PASSWORD_LENGTH = 6
    private const val TAG = "OpusAuth"

    private lateinit var auth: FirebaseAuth
    private val _user = MutableStateFlow<OpusUser?>(null)

    /** The signed-in user, or null when nobody is signed in. */
    val user: StateFlow<OpusUser?> = _user.asStateFlow()

    val isSignedIn: Boolean get() = _user.value != null

    /** False when the build was made without Firebase settings in local.properties. */
    val isConfigured: Boolean
        get() = BuildConfig.FIREBASE_API_KEY.isNotEmpty() &&
            BuildConfig.FIREBASE_PROJECT_ID.isNotEmpty() &&
            BuildConfig.FIREBASE_APP_ID.isNotEmpty()

    fun init(context: Context) {
        if (::auth.isInitialized) return
        if (!isConfigured) {
            Timber.tag(TAG).e("Firebase settings are missing; add them to local.properties and rebuild")
            return
        }
        val app = FirebaseApp.getApps(context).firstOrNull() ?: FirebaseApp.initializeApp(
            context,
            FirebaseOptions.Builder()
                .setApiKey(BuildConfig.FIREBASE_API_KEY)
                .setProjectId(BuildConfig.FIREBASE_PROJECT_ID)
                .setApplicationId(BuildConfig.FIREBASE_APP_ID)
                .setGcmSenderId(BuildConfig.FIREBASE_SENDER_ID)
                .setStorageBucket(BuildConfig.FIREBASE_STORAGE_BUCKET)
                .build(),
        )
        auth = FirebaseAuth.getInstance(app)
        if (BuildConfig.OPUS_AUTH_EMULATOR_HOST.isNotEmpty()) {
            auth.useEmulator(BuildConfig.OPUS_AUTH_EMULATOR_HOST, 9099)
            Timber.tag(TAG).w("Using Firebase Auth emulator at %s:9099", BuildConfig.OPUS_AUTH_EMULATOR_HOST)
        }
        _user.value = auth.currentUser?.toOpusUser()
        auth.addAuthStateListener { _user.value = it.currentUser?.toOpusUser() }
    }

    suspend fun signIn(email: String, password: String) = firebaseCall {
        auth.signInWithEmailAndPassword(email.trim(), password).await()
    }

    suspend fun createAccount(name: String, email: String, password: String) = firebaseCall {
        // The login screen closes as soon as the account exists, which can cancel the caller
        // even before the create call returns; finish setting up the profile regardless.
        withContext(NonCancellable) {
            val user = auth.createUserWithEmailAndPassword(email.trim(), password).await().user
                ?: throw OpusAuthException("Could not create the account. Please try again.")
            user.updateProfile(UserProfileChangeRequest.Builder().setDisplayName(name.trim()).build()).await()
            // A failed verification email should not block a new account from signing in.
            runCatching { user.sendEmailVerification().await() }
                .onFailure { Timber.tag(TAG).w(it, "Verification email not sent") }
            _user.value = user.toOpusUser()
        }
    }

    suspend fun sendPasswordReset(email: String) = firebaseCall {
        auth.sendPasswordResetEmail(email.trim()).await()
    }

    suspend fun sendVerificationEmail() = firebaseCall {
        requireUser().sendEmailVerification().await()
    }

    suspend fun updateName(name: String) = firebaseCall {
        requireUser().updateProfile(UserProfileChangeRequest.Builder().setDisplayName(name.trim()).build()).await()
        refresh()
    }

    suspend fun changePassword(currentPassword: String, newPassword: String) = firebaseCall {
        val user = reauthenticate(currentPassword)
        user.updatePassword(newPassword).await()
    }

    suspend fun deleteAccount(currentPassword: String) = firebaseCall {
        reauthenticate(currentPassword).delete().await()
    }

    /** Reloads the user from Firebase, e.g. to pick up a newly verified email. */
    suspend fun refresh() = firebaseCall {
        val user = auth.currentUser ?: return@firebaseCall
        user.reload().await()
        _user.value = auth.currentUser?.toOpusUser()
    }

    fun signOut() {
        if (::auth.isInitialized) auth.signOut()
    }

    private suspend fun reauthenticate(password: String): FirebaseUser {
        val user = requireUser()
        val email = user.email ?: throw OpusAuthException("This account has no email address.")
        try {
            user.reauthenticate(EmailAuthProvider.getCredential(email, password)).await()
        } catch (e: FirebaseAuthException) {
            if (e.errorCode in setOf("ERROR_INVALID_CREDENTIAL", "ERROR_WRONG_PASSWORD")) {
                throw OpusAuthException("Your current password is wrong.")
            }
            throw e
        }
        return user
    }

    // Kept out of firebaseCall: an inline function can't check whether a lateinit is set.
    private fun requireFirebase() {
        if (!::auth.isInitialized) {
            throw OpusAuthException("This build has no Firebase settings. Add them to local.properties and rebuild.")
        }
    }

    private fun requireUser(): FirebaseUser =
        auth.currentUser ?: throw OpusAuthException("You are signed out. Please sign in again.")

    /** Runs a Firebase call and turns its failures into [OpusAuthException] with a readable message. */
    private inline fun <T> firebaseCall(block: () -> T): T =
        try {
            requireFirebase()
            block()
        } catch (e: OpusAuthException) {
            throw e
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.tag(TAG).w(e, "Firebase auth call failed")
            throw OpusAuthException(friendlyMessage(e))
        }

    private fun friendlyMessage(e: Exception): String = when (e) {
        is FirebaseNetworkException -> "No internet connection. Check your network and try again."
        is FirebaseTooManyRequestsException -> "Too many attempts. Please wait a few minutes and try again."
        is FirebaseAuthException -> when (e.errorCode) {
            "ERROR_INVALID_EMAIL" -> "That email address doesn't look right."
            "ERROR_INVALID_CREDENTIAL", "ERROR_WRONG_PASSWORD", "ERROR_USER_NOT_FOUND" ->
                "Wrong email or password."
            "ERROR_EMAIL_ALREADY_IN_USE" -> "An account with this email already exists. Try signing in."
            "ERROR_WEAK_PASSWORD" -> "Password is too weak. Use at least $MIN_PASSWORD_LENGTH characters."
            "ERROR_USER_DISABLED" -> "This account has been disabled."
            "ERROR_REQUIRES_RECENT_LOGIN" -> "Please sign out and sign in again, then retry."
            "ERROR_OPERATION_NOT_ALLOWED" -> "Email sign-in is turned off for this app. Enable it in the Firebase console."
            else -> e.message ?: "Something went wrong. Please try again."
        }
        else -> e.message ?: "Something went wrong. Please try again."
    }

    private fun FirebaseUser.toOpusUser() = OpusUser(
        uid = uid,
        name = displayName?.takeIf { it.isNotBlank() } ?: email?.substringBefore('@').orEmpty(),
        email = email.orEmpty(),
        isEmailVerified = isEmailVerified,
        createdAtMs = metadata?.creationTimestamp,
        lastSignInAtMs = metadata?.lastSignInTimestamp,
    )
}
