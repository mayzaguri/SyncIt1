
package com.example.syncit1;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

public class HomeActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_home);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(R.id.main), (v, insets) -> {
                    Insets systemBars = insets.getInsets(
                            WindowInsetsCompat.Type.systemBars());

                    v.setPadding(
                            systemBars.left,
                            systemBars.top,
                            systemBars.right,
                            systemBars.bottom
                    );

                    return insets;
                });

        // יצירת טיול
        findViewById(R.id.btnCreateTrip).setOnClickListener(v ->
                showCreateTripDialog());

        // הצטרפות לטיול
        findViewById(R.id.btnJoinTrip).setOnClickListener(v -> {
            Intent intent = new Intent(
                    HomeActivity.this,
                    JoinTripActivity.class
            );
            startActivity(intent);
        });
    }

    private void showCreateTripDialog() {

        View dialogView = getLayoutInflater().inflate(
                R.layout.dialog_create_trip, null);

        EditText etGroupName =
                dialogView.findViewById(R.id.etGroupName);

        EditText etDestination =
                dialogView.findViewById(R.id.etTripDestination);

        Button btnStartDate =
                dialogView.findViewById(R.id.btnStartDate);

        Button btnEndDate =
                dialogView.findViewById(R.id.btnEndDate);

        Button btnCreateGroup =
                dialogView.findViewById(R.id.btnCreateGroup);

        TextView btnCancel =
                dialogView.findViewById(R.id.btnCancelCreateTrip);

        final String[] startDate = {""};
        final String[] endDate = {""};

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        // בחירת תאריך התחלה
        btnStartDate.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();

            new DatePickerDialog(
                    this,
                    (picker, year, month, day) -> {
                        startDate[0] = String.format(
                                Locale.US,
                                "%04d-%02d-%02d",
                                year, month + 1, day
                        );

                        btnStartDate.setText(startDate[0]);
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
            ).show();
        });

        // בחירת תאריך סיום
        btnEndDate.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();

            new DatePickerDialog(
                    this,
                    (picker, year, month, day) -> {
                        endDate[0] = String.format(
                                Locale.US,
                                "%04d-%02d-%02d",
                                year, month + 1, day
                        );

                        btnEndDate.setText(endDate[0]);
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
            ).show();
        });

        // ביטול
        btnCancel.setOnClickListener(v -> dialog.dismiss());

        // יצירת קבוצה
        btnCreateGroup.setOnClickListener(v -> {

            String groupName =
                    etGroupName.getText().toString().trim();

            String destination =
                    etDestination.getText().toString().trim();

            if (groupName.isEmpty()
                    || destination.isEmpty()
                    || startDate[0].isEmpty()
                    || endDate[0].isEmpty()) {

                Toast.makeText(
                        this,
                        "צריך למלא את כל הפרטים ולבחור תאריכים",
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }

            if (endDate[0].compareTo(startDate[0]) < 0) {
                Toast.makeText(
                        this,
                        "תאריך הסיום לא יכול להיות לפני תאריך ההתחלה",
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }

            FirebaseUser currentUser = auth.getCurrentUser();

            if (currentUser == null) {
                Toast.makeText(
                        this,
                        "צריך להתחבר לחשבון לפני יצירת טיול",
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }

            String userId = currentUser.getUid();

            // מוצאים את המשתמש הקיים
            db.collection("Users")
                    .document(userId)
                    .get()
                    .addOnSuccessListener(userDocument -> {

                        if (!userDocument.exists()) {
                            Toast.makeText(
                                    HomeActivity.this,
                                    "לא נמצא מסמך משתמש ב-Users",
                                    Toast.LENGTH_LONG
                            ).show();
                            return;
                        }

                        String name = userDocument.getString("name");

                        if (name == null || name.trim().isEmpty()) {
                            Toast.makeText(
                                    HomeActivity.this,
                                    "לא נמצא שם במסמך המשתמש",
                                    Toast.LENGTH_LONG
                            ).show();
                            return;
                        }

                        createGroup(
                                userId,
                                name.trim(),
                                groupName,
                                destination,
                                startDate[0],
                                endDate[0],
                                dialog,
                                0
                        );
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(
                                    HomeActivity.this,
                                    "לא הצלחנו לקרוא את פרטי המשתמש",
                                    Toast.LENGTH_LONG
                            ).show()
                    );
        });

        dialog.show();
    }

    private void createGroup(
            String userId,
            String name,
            String groupName,
            String destination,
            String startDate,
            String endDate,
            AlertDialog dialog,
            int attempt
    ) {

        if (attempt >= 10) {
            Toast.makeText(
                    this,
                    "לא הצלחנו ליצור קוד הצטרפות. נסי שוב",
                    Toast.LENGTH_LONG
            ).show();
            return;
        }

        // קוד הצטרפות בן 6 ספרות
        String groupId = String.valueOf(
                new Random().nextInt(900000) + 100000
        );

        DocumentReference groupRef =
                db.collection("Groups").document(groupId);

        groupRef.get().addOnSuccessListener(groupDocument -> {

            // אם הקוד תפוס, מנסים קוד אחר
            if (groupDocument.exists()) {
                createGroup(
                        userId,
                        name,
                        groupName,
                        destination,
                        startDate,
                        endDate,
                        dialog,
                        attempt + 1
                );
                return;
            }

            // פרטי הטיול החדש
            Map<String, Object> groupData = new HashMap<>();
            groupData.put("groupName", groupName);
            groupData.put("destination", destination);
            groupData.put("startDate", startDate);
            groupData.put("endDate", endDate);
            groupData.put("answers", new HashMap<String, Object>());

            // עדכון המשתמש הקיים בלבד
            Map<String, Object> userData = new HashMap<>();
            userData.put("name", name);
            userData.put("groupId", groupId);

            DocumentReference userRef =
                    db.collection("Users").document(userId);

            // שמירת הטיול ועדכון המשתמש באותה פעולה
            WriteBatch batch = db.batch();

            batch.set(groupRef, groupData);
            batch.set(userRef, userData, SetOptions.merge());

            batch.commit()
                    .addOnSuccessListener(unused -> {
                        Toast.makeText(
                                HomeActivity.this,
                                "הטיול נוצר בהצלחה! קוד ההצטרפות: "
                                        + groupId,
                                Toast.LENGTH_LONG
                        ).show();

                        dialog.dismiss();
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(
                                    HomeActivity.this,
                                    "שמירת הטיול נכשלה. בדקי את Firebase",
                                    Toast.LENGTH_LONG
                            ).show()
                    );

        }).addOnFailureListener(e ->
                Toast.makeText(
                        HomeActivity.this,
                        "לא הצלחנו לבדוק את קוד ההצטרפות",
                        Toast.LENGTH_LONG
                ).show()
        );
    }
}
