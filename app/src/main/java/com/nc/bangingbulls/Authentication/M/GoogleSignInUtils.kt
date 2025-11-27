package com.nc.bangingbulls.Authentication.M

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.credentials.CredentialManager
import androidx.credentials.CredentialOption
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.Firebase
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class GoogleSignInUtils {

    companion object {
        fun doGoogleSignIn(
            context: Context,
            scope: CoroutineScope,
            launcher: ActivityResultLauncher<Intent>? = null
        ) {

            val credentialManager = CredentialManager.Companion.create(context)
            val request = GetCredentialRequest.Builder()
                .addCredentialOption(getCredentialOptions(context))
                .build()
            scope.launch(Dispatchers.IO) {// this is the coroutine launch for network based requests
                try {
                    val result = credentialManager.getCredential(context, request)
                    Log.d("CredentialResult", "Credential type: ${result.credential.type}")
                    Log.d("CredentialResult", "Credential: ${result.credential}")

                    when (result.credential) {
                        is CustomCredential -> {
                            if (result.credential.type == GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                                val googleIdTokenCredential = GoogleIdTokenCredential.Companion.createFrom(result.credential.data)
                                val googleTokenId = googleIdTokenCredential.idToken
                                val authCredential = GoogleAuthProvider.getCredential(googleTokenId, null)
                                val authResult = Firebase.auth.signInWithCredential(authCredential).await()
                                val user = authResult.user

                                user?.let {
                                    if (it.isAnonymous.not()) {
                                        Log.d("Auth", "Sign-in successful for user: ${it.uid}")
                                    } else {
                                        Log.e("Auth", "User is anonymous, login failed!")
                                    }
                                } ?: Log.e("Auth", "User is null after sign-in!")

                            }

                        }

                        else -> {

                        }
                    }
               /* } catch (e: NoCredentialException) {
                    scope.launch(Dispatchers.Main) {
                        Toast.makeText(
                            context,
                            "No Google Account Found. Please add one.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    launcher?.launch(getIntent())
                } catch (e: GetCredentialException) {
                    e.printStackTrace()
                }*/
                } catch (e: NoCredentialException) {
                    // Don’t open Settings. Log and show a hint; then fallback to One Tap client or retry with filter false.
                    scope.launch(Dispatchers.Main) {
                        Toast.makeText(context, "No credentials available. Check client ID/SHAs.", Toast.LENGTH_LONG).show()
                    }
                    // Optional fallback: One Tap via Identity (beginSignIn)
                } catch (e: GetCredentialException) {
                    e.printStackTrace()
                }

            }
        }

        private fun getIntent(): Intent {
            return Intent(Settings.ACTION_ADD_ACCOUNT).apply {
                putExtra(Settings.EXTRA_ACCOUNT_TYPES, arrayOf("com.google"))
            }
        }
        private fun getCredentialOptions(context: Context): CredentialOption {
            return GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setAutoSelectEnabled(false)
                .setServerClientId("")
                .build()
        }


    }
}
