
package com.example.syncit1;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.appcompat.app.AlertDialog;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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

        // כפתור ביטול
        View btnCancel =
                dialogView.findViewById(R.id.btnCancelRegister);

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        // בהמשך נחבר כאן את כפתור ההרשמה ל-Firebase
    }
}