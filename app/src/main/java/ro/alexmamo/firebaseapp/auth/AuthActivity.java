package ro.alexmamo.firebaseapp.auth;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.GoogleAuthProvider;

import javax.inject.Inject;

import dagger.android.support.DaggerAppCompatActivity;
import ro.alexmamo.firebaseapp.R;
import ro.alexmamo.firebaseapp.main.MainActivity;

import static ro.alexmamo.firebaseapp.utils.Constants.RC_SIGN_IN;
import static ro.alexmamo.firebaseapp.utils.Constants.TAG;

public class AuthActivity extends DaggerAppCompatActivity {
    @Inject GoogleSignInClient googleSignInClient;
    @Inject AuthViewModel authViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);
        initSignInButton();
    }

    private void initSignInButton() {
        SignInButton googleSignInButton = findViewById(R.id.google_sign_in_button);
        googleSignInButton.setOnClickListener(view -> signIn());
    }

    private void signIn() {
        Intent signInIntent = googleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount googleSignInAccount = task.getResult(ApiException.class);
                if (googleSignInAccount != null) {
                    getGoogleAuthCredential(googleSignInAccount);
                }
            } catch (ApiException e) {
                Log.d(TAG, e.getMessage());
            }
        }
    }

    private void getGoogleAuthCredential(GoogleSignInAccount googleSignInAccount) {
        String googleTokenId = googleSignInAccount.getIdToken();
        AuthCredential googleAuthCredential = GoogleAuthProvider.getCredential(googleTokenId, null);
        signInWithGoogleAuthCredential(googleAuthCredential);
    }

    private void signInWithGoogleAuthCredential(AuthCredential googleAuthCredential) {
        authViewModel.signInWithGoogle(googleAuthCredential);
        authViewModel.getAuthenticatedUserLiveData().observe(this, authenticatedUser -> {
            boolean authenticatedUserIsNew = authenticatedUser.isNew;
            if (authenticatedUserIsNew) {
                createNewUser(authenticatedUser);
            } else {
                goToMainActivity(authenticatedUser.uid, authenticatedUser.name);
            }
        });
    }

    private void createNewUser(User authenticatedUser) {
        authViewModel.createUser(authenticatedUser);
        authViewModel.getCreatedUserLiveData().observe(this, user -> {
            if (user.isCreated) {
                displayWelcomeMessage(user.name);
            }
            goToMainActivity(user.uid, user.name);
        });
    }

    private void displayWelcomeMessage(String name) {
        String welcomeMessage = getWelcomeMessage(name);
        Toast.makeText(this, welcomeMessage, Toast.LENGTH_LONG).show();
    }

    private String getWelcomeMessage(String name) {
        return "Welcome " + name + "! Your account was successfully created!";
    }

    private void goToMainActivity(String uid, String name) {
        Intent intent = new Intent(AuthActivity.this, MainActivity.class);
        intent.putExtra("uid", uid);
        intent.putExtra("name", name);
        startActivity(intent);
        finish();
    }
}