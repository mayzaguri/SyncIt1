package com.example.syncit1;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;
import android.content.Intent;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAuth = FirebaseAuth.getInstance();

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(R.id.main), (v, insets) -> {
                    Insets systemBars = insets.getInsets(
                            WindowInsetsCompat.Type.systemBars());

                    v.setPadding(
                            systemBars.left,
                            systemBars.top,
                            systemBars.right,
                            systemBars.bottom);

                    return insets;
                });

        // פתיחת חלונית ההרשמה בלחיצה על "לחץ כאן"
        TextView tvRegisterClick =
                findViewById(R.id.tvRegisterClick);

        tvRegisterClick.setOnClickListener(
                v -> showRegisterDialog());
        // התחברות
        EditText etEmail = findViewById(R.id.etEmail);
        EditText etPassword = findViewById(R.id.etPassword);
        View btnLogin = findViewById(R.id.btnLogin);

        btnLogin.setOnClickListener(v -> {

            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString();

            if (email.isEmpty() || password.isEmpty()) {

                Toast.makeText(
                        MainActivity.this,
                        "נא למלא אימייל וסיסמה",
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }

            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this, task -> {

                        if (task.isSuccessful()) {

                            FirebaseUser currentUser = mAuth.getCurrentUser();

                            if (currentUser == null) {
                                Toast.makeText(
                                        MainActivity.this,
                                        "לא נמצא משתמש מחובר",
                                        Toast.LENGTH_LONG
                                ).show();
                                return;
                            }

                            String userId = currentUser.getUid();

                            FirebaseFirestore db = FirebaseFirestore.getInstance();

                            db.collection("Users")
                                    .document(userId)
                                    .get()
                                    .addOnSuccessListener(document -> {

                                        if (document.exists()) {
                                            openHome();
                                            return;
                                        }

                                        Map<String, Object> userData = new HashMap<>();
                                        userData.put("email", currentUser.getEmail());
                                        userData.put("name", currentUser.getEmail());

                                        db.collection("Users")
                                                .document(userId)
                                                .set(userData)
                                                .addOnSuccessListener(unused -> openHome())
                                                .addOnFailureListener(e ->
                                                        Toast.makeText(
                                                                MainActivity.this,
                                                                "שמירת המשתמש נכשלה: "
                                                                        + e.getMessage(),
                                                                Toast.LENGTH_LONG
                                                        ).show()
                                                );

                                    })
                                    .addOnFailureListener(e ->
                                            Toast.makeText(
                                                    MainActivity.this,
                                                    "בדיקת המשתמש נכשלה: "
                                                            + e.getMessage(),
                                                    Toast.LENGTH_LONG
                                            ).show()
                                    );

                        } else {

                            Toast.makeText(
                                    MainActivity.this,
                                    "האימייל או הסיסמה שגויים",
                                    Toast.LENGTH_SHORT
                            ).show();
                        }
                    });
        });
    }


    // יצירת חלונית ההרשמה
    private void showRegisterDialog() {

        View dialogView = LayoutInflater.from(this)
                .inflate(R.layout.dialog_register, null);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        dialog.show();

        // שדות ההרשמה
        EditText etEmail =
                dialogView.findViewById(R.id.etRegEmail);

        EditText etPassword =
                dialogView.findViewById(R.id.etRegPassword);

        EditText etConfirmPassword =
                dialogView.findViewById(R.id.etRegConfirmPassword);

        // כפתור הרשמה
        View btnRegister =
                dialogView.findViewById(R.id.btnRegister);

        btnRegister.setOnClickListener(v -> {

            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString();
            String confirmPassword =
                    etConfirmPassword.getText().toString();

            // בדיקה שהשדות לא ריקים
            if (email.isEmpty() ||
                    password.isEmpty() ||
                    confirmPassword.isEmpty()) {

                Toast.makeText(
                        MainActivity.this,
                        "נא למלא את כל השדות",
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }

            // בדיקה שהסיסמאות זהות
            if (!password.equals(confirmPassword)) {

                Toast.makeText(
                        MainActivity.this,
                        "הסיסמאות אינן זהות",
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }

            // יצירת המשתמש ב-Firebase
            mAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this, task -> {

                        if (task.isSuccessful()) {

                            String userId = mAuth.getCurrentUser().getUid();

                            Map<String, Object> userData = new HashMap<>();
                            userData.put("email", email);
                            userData.put("name", email);

                            FirebaseFirestore db = FirebaseFirestore.getInstance();

                            db.collection("Users")
                                    .document(userId)
                                    .set(userData)
                                    .addOnSuccessListener(unused -> {

                                        Toast.makeText(
                                                MainActivity.this,
                                                "החשבון נוצר בהצלחה! ✈️",
                                                Toast.LENGTH_SHORT
                                        ).show();

                                        EditText loginEmail =
                                                findViewById(R.id.etEmail);

                                        loginEmail.setText(email);

                                        dialog.dismiss();
                                    })
                                    .addOnFailureListener(e -> {

                                        Toast.makeText(
                                                MainActivity.this,
                                                "שמירת פרטי המשתמש נכשלה: "
                                                        + e.getMessage(),
                                                Toast.LENGTH_LONG
                                        ).show();
                                    });

                        } else {

                            Toast.makeText(
                                    MainActivity.this,
                                    "ההרשמה נכשלה: "
                                            + task.getException().getMessage(),
                                    Toast.LENGTH_LONG
                            ).show();
                        }
                    });
        });

        // כפתור ביטול
        View btnCancel =
                dialogView.findViewById(R.id.btnCancelRegister);

        btnCancel.setOnClickListener(v -> dialog.dismiss());
    }
    private void openHome() {
        Toast.makeText(
                MainActivity.this,
                "התחברת בהצלחה! ✈️",
                Toast.LENGTH_SHORT
        ).show();

        Intent intent = new Intent(
                MainActivity.this,
                HomeActivity.class
        );

        startActivity(intent);
        finish();
    }
}