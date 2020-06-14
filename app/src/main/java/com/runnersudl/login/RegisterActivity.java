package com.runnersudl.login;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.Toast;
import com.bumptech.glide.Glide;
import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;
import com.runnersudl.HomeActivity;
import com.runnersudl.R;

import java.util.HashMap;
import java.util.Map;
import static androidx.constraintlayout.widget.Constraints.TAG;

public class RegisterActivity extends Activity {

    TextInputLayout email, password, confirm_password, peso, altura;

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        email = (TextInputLayout) findViewById(R.id.register_textInputLayoutEmail);
        password = (TextInputLayout) findViewById(R.id.register_textInputLayoutPassword);
        confirm_password = (TextInputLayout) findViewById(R.id.register_textInputLayoutConfirmPassword);
        peso = (TextInputLayout) findViewById(R.id.register_textInputLayoutPeso);
        altura = (TextInputLayout) findViewById(R.id.register_textInputLayoutAltura);
    }

    // Go to login
    public void goToLogin(View view){
        Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
        startActivity(intent);
        finish();
    }

    // Register button | Store all fields on SharedPreferences
    public void register(View view){
        if (!thereAreErrors()){
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(getString(R.string.keyEmail), email.getEditText().getText().toString());
            editor.putString(getString(R.string.keyPassword), password.getEditText().getText().toString());
            editor.putString(getString(R.string.keyPeso), peso.getEditText().getText().toString());
            editor.putString(getString(R.string.keyAltura), altura.getEditText().getText().toString());
            editor.apply();

            // Get firebase Auth instance
            mAuth = FirebaseAuth.getInstance();

            final ProgressDialog dialog = ProgressDialog.show(this, "Registrando", "Cargando. Por favor espera...", true);

            // Create User using email and password
            mAuth.createUserWithEmailAndPassword(email.getEditText().getText().toString(), password.getEditText().getText().toString())
                    .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            dialog.cancel();

                            if (task.isSuccessful()) {
                                // Sign in success, update UI with the signed-in user's information
                                Log.d(TAG, "createUserWithEmail:success");
                                FirebaseUser user = mAuth.getCurrentUser();

                                // Write a message to the database
                                FirebaseDatabase database = FirebaseDatabase.getInstance();
                                DatabaseReference mRef =  database.getReference().child("Users data").child(user.getUid());

                                String name = email.getEditText().getText().toString();
                                mRef.child("Nombre").setValue(name.substring(0, name.indexOf('@')));
                                mRef.child("PhotoURL").setValue("");
                                mRef.child("Altura").setValue(altura.getEditText().getText().toString());
                                mRef.child("Peso").setValue(peso.getEditText().getText().toString());

                                mRef.push();

                                FirebaseInstanceId.getInstance().getInstanceId()
                                        .addOnCompleteListener(new OnCompleteListener<InstanceIdResult>() {

                                            @Override
                                            public void onComplete(@NonNull Task<InstanceIdResult> task) {
                                                if (!task.isSuccessful()) {
                                                    Log.w(ContentValues.TAG, "getInstanceId failed", task.getException());
                                                    return;
                                                }

                                                // Get new Instance ID token
                                                String token = task.getResult().getToken();

                                                // Log and toast
                                                // Add a new document with a generated ID
                                                FirebaseFirestore db = FirebaseFirestore.getInstance();

                                                Map<String, Object> map_token = new HashMap<>();
                                                map_token.put("token", token);
                                                db.collection("Tokens")
                                                        .document(FirebaseAuth.getInstance().getUid())
                                                        .update(map_token);
                                            }
                                        });

                                // Show toast register succesfully
                                Toast.makeText(RegisterActivity.this, getString(R.string.success_message_register, email.getEditText().getText().toString()), Toast.LENGTH_LONG).show();

                                // Go to HomeActivity
                                Intent intent = new Intent(getApplicationContext(), HomeActivity.class);
                                startActivity(intent);
                                finish();
                            } else {
                                // If sign in fails, display a message to the user.
                                Log.w(TAG, "createUserWithEmail:failure", task.getException());
                                Toast.makeText(RegisterActivity.this, "Authentication failed.", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
        }
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

        if (validator.isNullOrEmpty(confirm_password.getEditText().getText().toString())){
            confirm_password.setErrorEnabled(true);
            confirm_password.setError(getString(R.string.contraseña_vacia));
            error = true;
        } else if (!validator.isValidPassword(confirm_password.getEditText().getText().toString(), false)){
            confirm_password.setErrorEnabled(true);
            confirm_password.setError(getString(R.string.contraseña_no_valida));
            error = true;
        } else if (!password.getEditText().getText().toString().equals(confirm_password.getEditText().getText().toString())){
            confirm_password.setErrorEnabled(true);
            confirm_password.setError(getString(R.string.contraseña_no_coincide));
            error = true;
        } else  {
            confirm_password.setError(null);
            confirm_password.setErrorEnabled(false);
        }

        if (validator.isNullOrEmpty(peso.getEditText().getText().toString())){
            peso.setErrorEnabled(true);
            peso.setError(getString(R.string.peso_vacio));
            error = true;
        } else if (!validator.isNumeric(peso.getEditText().getText().toString())){
            peso.setErrorEnabled(true);
            peso.setError(getString(R.string.peso_no_valido));
            error = true;
        } else {
            peso.setError(null);
            peso.setErrorEnabled(false);
        }

        if (validator.isNullOrEmpty(altura.getEditText().getText().toString())){
            altura.setErrorEnabled(true);
            altura.setError(getString(R.string.altura_vacia));
            error = true;
        } else if (!validator.isNumeric(altura.getEditText().getText().toString())){
            altura.setErrorEnabled(true);
            altura.setError(getString(R.string.altura_no_valida));
            error = true;
        } else {
            altura.setError(null);
            altura.setErrorEnabled(false);
        }

        return error;
    }
}
