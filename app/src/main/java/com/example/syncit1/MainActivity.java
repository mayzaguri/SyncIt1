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

                            Toast.makeText(
                                    MainActivity.this,
                                    "החשבון נוצר בהצלחה! ✈️",
                                    Toast.LENGTH_SHORT
                            ).show();

                            dialog.dismiss();

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
}