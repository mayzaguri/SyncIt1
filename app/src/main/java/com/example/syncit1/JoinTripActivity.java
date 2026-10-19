package com.example.syncit1;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Calendar;

public class JoinTripActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private FirebaseAuth auth;

    private EditText etDestinationSearch;
    private TextView tvSearchStartDate;
    private TextView tvSearchEndDate;
    private Button btnSearchGroups;
    private TextView tvNoGroups;
    private RecyclerView rvGroups;

    private GroupAdapter adapter;

    private final List<Map<String, Object>> allGroups =
            new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_join_trip);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        etDestinationSearch =
                findViewById(R.id.etDestinationSearch);
        tvSearchStartDate =
                findViewById(R.id.tvSearchStartDate);
        tvSearchEndDate =
                findViewById(R.id.tvSearchEndDate);
        btnSearchGroups =
                findViewById(R.id.btnSearchGroups);
        tvNoGroups =
                findViewById(R.id.tvNoGroups);
        rvGroups =
                findViewById(R.id.rvGroups);

        rvGroups.setLayoutManager(
                new LinearLayoutManager(this));

        adapter = new GroupAdapter(group -> joinGroup(group));
        rvGroups.setAdapter(adapter);

        btnSearchGroups.setOnClickListener(v -> filterGroups());

        tvSearchStartDate.setOnClickListener(
                v -> showDatePicker(tvSearchStartDate));

        tvSearchEndDate.setOnClickListener(
                v -> showDatePicker(tvSearchEndDate));

        loadGroups();
    }

    private void showDatePicker(TextView dateView) {
        Calendar calendar = Calendar.getInstance();

        new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    String date = String.format(
                            Locale.US,
                            "%04d-%02d-%02d",
                            year,
                            month + 1,
                            dayOfMonth
                    );

                    dateView.setText(date);
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        ).show();
    }

    private void loadGroups() {
        tvNoGroups.setText("טוענים טיולים... ✈️");
        tvNoGroups.setVisibility(TextView.VISIBLE);
        rvGroups.setVisibility(RecyclerView.GONE);

        db.collection("Groups")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    allGroups.clear();

                    for (DocumentSnapshot document :
                            querySnapshot.getDocuments()) {

                        Map<String, Object> data =
                                document.getData();

                        if (data != null) {
                            Map<String, Object> group =
                                    new HashMap<>(data);

                            group.put("groupId", document.getId());
                            allGroups.add(group);
                        }
                    }

                    filterGroups();
                })
                .addOnFailureListener(e -> {
                    tvNoGroups.setText(
                            "לא הצלחנו לטעון את הטיולים");
                    tvNoGroups.setVisibility(TextView.VISIBLE);
                    rvGroups.setVisibility(RecyclerView.GONE);

                    Toast.makeText(
                            JoinTripActivity.this,
                            "שגיאה בטעינת הטיולים: "
                                    + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    private void filterGroups() {
        String destination = etDestinationSearch.getText()
                .toString()
                .trim()
                .toLowerCase(Locale.ROOT);

        String startDate = getSelectedDate(tvSearchStartDate);
        String endDate = getSelectedDate(tvSearchEndDate);

        boolean hasDestination = !destination.isEmpty();
        boolean hasStartDate = !startDate.isEmpty();
        boolean hasEndDate = !endDate.isEmpty();

        int maxScore =
                (hasDestination ? 2 : 0)
                        + (hasStartDate ? 1 : 0)
                        + (hasEndDate ? 1 : 0);

        for (Map<String, Object> group : allGroups) {
            String groupDestination =
                    getGroupValue(group, "destination")
                            .toLowerCase(Locale.ROOT);

            String groupStart =
                    getGroupValue(group, "startDate");

            String groupEnd =
                    getGroupValue(group, "endDate");

            int score = 0;

            boolean destinationMatches =
                    hasDestination
                            && groupDestination.contains(destination);

            boolean startMatches =
                    hasStartDate
                            && startDate.equals(groupStart);

            boolean endMatches =
                    hasEndDate
                            && endDate.equals(groupEnd);

            if (destinationMatches) {
                score += 2;
            }

            if (startMatches) {
                score += 1;
            }

            if (endMatches) {
                score += 1;
            }

            String matchInfo;

            if (maxScore == 0) {
                matchInfo = "טיול זמין להצטרפות ✈️";
            } else if (score == maxScore) {
                matchInfo = "התאמה מלאה לחיפוש ✨";
            } else if (score > 0) {
                matchInfo = "מתאים לחלק מההעדפות שלך 🌍";
            } else {
                matchInfo = "הצעה נוספת שאולי תעניין אותך ✈️";
            }

            group.put("matchScore", score);
            group.put("matchInfo", matchInfo);
        }

        // מציגים קודם את הטיולים עם הציון הגבוה ביותר.
        allGroups.sort((group1, group2) ->
                Integer.compare(
                        (int) group2.get("matchScore"),
                        (int) group1.get("matchScore")
                ));

        // מציגים גם הצעות נוספות במקום להשאיר מסך ריק.
        adapter.setGroups(new ArrayList<>(allGroups));

        if (allGroups.isEmpty()) {
            tvNoGroups.setText(
                    "עדיין אין טיולים להצטרף אליהם 🌍");
            tvNoGroups.setVisibility(TextView.VISIBLE);
            rvGroups.setVisibility(RecyclerView.GONE);
        } else {
            tvNoGroups.setVisibility(TextView.GONE);
            rvGroups.setVisibility(RecyclerView.VISIBLE);
        }
    }

    private String getGroupValue(
            Map<String, Object> group,
            String key) {

        Object value = group.get(key);

        // אם השדה לא נמצא ברמה הראשית,
        // מחפשים אותו גם בתוך answers.
        if (value == null) {
            Object answersObject = group.get("answers");

            if (answersObject instanceof Map<?, ?>) {
                value = ((Map<?, ?>) answersObject).get(key);
            }
        }

        return value == null ? "" : value.toString().trim();
    }

    private String getSelectedDate(TextView dateView) {
        String date = dateView.getText().toString().trim();

        if (date.isEmpty()
                || date.contains("תאריך")
                || date.contains("בחרי")
                || date.contains("בחר")) {
            return "";
        }

        return date;
    }

    private void joinGroup(Map<String, Object> group) {
        if (auth.getCurrentUser() == null) {
            Toast.makeText(
                    this,
                    "צריך להתחבר כדי להצטרף לטיול",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        String userId = auth.getCurrentUser().getUid();
        String groupId = getGroupValue(group, "groupId");

        if (groupId.isEmpty()) {
            Toast.makeText(
                    this,
                    "לא נמצא קוד הטיול",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        // מוסיפים את המשתמש לרשימת החברים בקבוצה.
        db.collection("Groups")
                .document(groupId)
                .update("memberIds", FieldValue.arrayUnion(userId))
                .addOnSuccessListener(unused -> {
                    // שומרים גם את הקבוצה בפרופיל המשתמש.
                    db.collection("Users")
                            .document(userId)
                            .set(
                                    new HashMap<String, Object>() {{
                                        put("groupId", groupId);
                                    }},
                                    com.google.firebase.firestore.SetOptions.merge()
                            )
                            .addOnSuccessListener(userUnused -> {
                                Toast.makeText(
                                        JoinTripActivity.this,
                                        "הצטרפת לטיול בהצלחה! ✈️",
                                        Toast.LENGTH_LONG
                                ).show();

                                finish();
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(
                                            JoinTripActivity.this,
                                            "הצטרפת לקבוצה, אבל לא הצלחנו לעדכן את הפרופיל: "
                                                    + e.getMessage(),
                                            Toast.LENGTH_LONG
                                    ).show()
                            );
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(
                            JoinTripActivity.this,
                            "לא הצלחנו להצטרף: " + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }
}
