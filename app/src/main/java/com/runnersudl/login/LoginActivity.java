package com.runnersudl.login;

import androidx.annotation.NonNull;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;
import com.runnersudl.HomeActivity;
import com.runnersudl.R;


import java.util.HashMap;
import java.util.Map;

import static android.content.ContentValues.TAG;

public class LoginActivity extends Activity {

    TextInputLayout email, password;
    private FirebaseAuth mAuth;

    /* TODO: Authentificación por GMAIL. */
    private static final String TAG = "GoogleActivity";
    private static final int RC_SIGN_IN = 9001;
    private GoogleSignInClient mGoogleSignInClient;
    private SignInButton signInButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        email = (TextInputLayout) findViewById(R.id.login_textInputLayoutEmail);
        password = (TextInputLayout) findViewById(R.id.login_textInputLayoutPassword);
        mAuth = FirebaseAuth.getInstance();

        /* TODO: Authentificación por GMAIL. */
        // [START config_signin]
        // Configure Google Sign In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        // [END config_signin]

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        signInButton= findViewById(R.id.signInButton);
        signInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signIn();
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        // Check if user is signed in (non-null) and update UI accordingly.
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser != null){
            goToApp();
        }
    }

    // Log In button
    public void logIn(View view){
        if (!thereAreErrors()){

            final ProgressDialog dialog = ProgressDialog.show(this, "Iniciando sesión", "Cargando. Por favor espera...", true);

            mAuth.signInWithEmailAndPassword(email.getEditText().getText().toString(), password.getEditText().getText().toString())
                    .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            dialog.cancel();

                            if (task.isSuccessful()) {
                                // Sign in success, update UI with the signed-in user's information
                                Log.d(TAG, "signInWithEmail:success");
                                FirebaseUser user = mAuth.getCurrentUser();

                                // Show toast login succesfully
                                Toast.makeText(LoginActivity.this, getString(R.string.success_message_login, email.getEditText().getText().toString()), Toast.LENGTH_LONG).show();

                                // Go to HomeActivity
                                goToApp();
                            } else {
                                // If sign in fails, display a message to the user.
                                Log.w(TAG, "signInWithEmail:failure", task.getException());
                                Toast.makeText(LoginActivity.this, "Authentication failed.",
                                        Toast.LENGTH_SHORT).show();

                            }
                        }
                    });
        }
    }

    public void goToApp(){
        FirebaseInstanceId.getInstance().getInstanceId()
                .addOnCompleteListener(new OnCompleteListener<InstanceIdResult>() {

                    @Override
                    public void onComplete(@NonNull Task<InstanceIdResult> task) {
                        if (!task.isSuccessful()) {
                            Log.w(TAG, "getInstanceId failed", task.getException());
                            return;
                        }

                        // Get new Instance ID token
                        String token = task.getResult().getToken();

                        // Log and toast
                        // Add a new document with a generated ID
                        FirebaseFirestore db = FirebaseFirestore.getInstance();

                        Map<String, Object> map_token = new HashMap<>();
                        Log.w("-------------------", token);
                        map_token.put("token", token);
                        db.collection("Tokens")
                                .document(FirebaseAuth.getInstance().getUid())
                                .set(map_token);
                    }
                });

        Intent intent = new Intent(getApplicationContext(), HomeActivity.class);
        startActivity(intent);
        finish();
    }

    // Go to register
    public void goToRegister(View view){
        Intent intent = new Intent(getApplicationContext(), RegisterActivity.class);
        startActivity(intent);
    }

    // Check if there are errors on the fields.
    private boolean thereAreErrors(){
        InputValidator validator = new InputValidator();

        boolean error = false;

        // Email validation
        if (validator.isNullOrEmpty(email.getEditText().getText().toString())){
            email.setErrorEnabled(true);
            email.setError(getString(R.string.email_vacio));
            error = true;
        }
        else if (!validator.isValidEmail(email.getEditText().getText().toString())){
            email.setErrorEnabled(true);
            email.setError(getString(R.string.email_no_valido));
            error = true;
        } else {
            email.setError(null);
            email.setErrorEnabled(false);
        }

        // Password validation
        if (validator.isNullOrEmpty(password.getEditText().getText().toString())){
            password.setErrorEnabled(true);
            password.setError(getString(R.string.contraseña_vacia));
            error = true;
        } else if (!validator.isValidPassword(password.getEditText().getText().toString(), false)) {
            password.setErrorEnabled(true);
            password.setError(getString(R.string.contraseña_no_valida));
            error = true;
        } else {
            password.setError(null);
            password.setErrorEnabled(false);
        }

        return error;
    }


    /* TODO: Authentificación por GMAIL. */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                // Google Sign In was successful, authenticate with Firebase
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account);
            } catch (ApiException e) {
                // Google Sign In failed, update UI appropriately
                Log.w(TAG, "Google sign in failed", e);
                Toast.makeText(this,"El inicio de sesión de Google falló",Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount acct) {
        Log.d(TAG, "firebaseAuthWithGoogle:" + acct.getId());
        // [START_EXCLUDE silent]
        //showProgressDialog();
        // [END_EXCLUDE]

        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d(TAG, "signInWithCredential:success");

                            FirebaseUser currentUser = mAuth.getCurrentUser();
                            if (currentUser != null){
                                goToApp();
                            }

                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w(TAG, "signInWithCredential:failure", task.getException());
                            Toast.makeText(getApplicationContext(),"Autenticación fallida",Toast.LENGTH_SHORT).show();
                        }

                        // [START_EXCLUDE]
                        //hideProgressDialog();
                        // [END_EXCLUDE]
                    }
                });
    }

    private void signIn() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }
}
