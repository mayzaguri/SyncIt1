package com.example.syncit1;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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

        etDestinationSearch = findViewById(R.id.etDestinationSearch);
        tvSearchStartDate = findViewById(R.id.tvSearchStartDate);
        tvSearchEndDate = findViewById(R.id.tvSearchEndDate);
        btnSearchGroups = findViewById(R.id.btnSearchGroups);
        tvNoGroups = findViewById(R.id.tvNoGroups);
        rvGroups = findViewById(R.id.rvGroups);

        rvGroups.setLayoutManager(new LinearLayoutManager(this));

        // לחיצה על כפתור ההצטרפות בכרטיס תפעיל את joinGroup.
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
        rvGroups.setVisibility(View.GONE);

        db.collection("Groups")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    allGroups.clear();

                    for (DocumentSnapshot document :
                            querySnapshot.getDocuments()) {

                        Map<String, Object> data = document.getData();

                        if (data != null) {
                            Map<String, Object> group =
                                    new HashMap<>(data);

                            // שומרים את מזהה המסמך לצורך הצטרפות.
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
                    rvGroups.setVisibility(View.GONE);

                    Toast.makeText(
                            JoinTripActivity.this,
                            "שגיאה בטעינת הטיולים: " + e.getMessage(),
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

        // בדיקה שתאריך היציאה לא מאוחר מתאריך החזרה.
        if (hasStartDate && hasEndDate
                && startDate.compareTo(endDate) > 0) {
            Toast.makeText(
                    this,
                    "תאריך היציאה צריך להיות לפני תאריך החזרה",
                    Toast.LENGTH_LONG
            ).show();
            return;
        }

        List<Map<String, Object>> groupsToShow =
                new ArrayList<>();

        for (Map<String, Object> group : allGroups) {
            String groupDestination =
                    getGroupValue(group, "destination")
                            .toLowerCase(Locale.ROOT);

            String groupStart = getGroupValue(group, "startDate");
            String groupEnd = getGroupValue(group, "endDate");

            boolean destinationMatches =
                    hasDestination
                            && groupDestination.contains(destination);

            boolean datesOverlap = false;
            boolean exactDates = false;

            boolean hasGroupDates =
                    !groupStart.isEmpty() && !groupEnd.isEmpty();

            if (hasGroupDates) {
                if (hasStartDate && hasEndDate) {
                    datesOverlap =
                            groupEnd.compareTo(startDate) >= 0
                                    && groupStart.compareTo(endDate) <= 0;

                    exactDates =
                            startDate.equals(groupStart)
                                    && endDate.equals(groupEnd);

                } else if (hasStartDate) {
                    datesOverlap =
                            groupEnd.compareTo(startDate) >= 0;

                } else if (hasEndDate) {
                    datesOverlap =
                            groupStart.compareTo(endDate) <= 0;
                }
            }

            int score = 0;

            if (destinationMatches) {
                score += 4;
            }

            if (datesOverlap) {
                score += 2;
            }

            if (exactDates) {
                score += 2;
            }

            String matchInfo;

            if (!hasDestination && !hasStartDate && !hasEndDate) {
                matchInfo = "טיול זמין להצטרפות ✈️";
            } else if (destinationMatches && exactDates) {
                matchInfo = "התאמה מלאה לחיפוש ✨";
            } else if (destinationMatches && datesOverlap) {
                matchInfo = "היעד מתאים ויש חפיפה בתאריכים 📅";
            } else if (destinationMatches) {
                matchInfo = "היעד מתאים, התאריכים שונים 📍";
            } else if (datesOverlap) {
                matchInfo = "יש חפיפה בתאריכים, היעד שונה 📅";
            } else {
                matchInfo = "הצעה נוספת שאולי תעניין אותך ✈️";
            }

            group.put("matchScore", score);
            group.put("matchInfo", matchInfo);

            groupsToShow.add(group);
        }

        // ההתאמות הטובות ביותר יוצגו ראשונות.
        groupsToShow.sort((group1, group2) ->
                Integer.compare(
                        ((Number) group2.get("matchScore")).intValue(),
                        ((Number) group1.get("matchScore")).intValue()
                ));

        adapter.setGroups(groupsToShow);

        if (groupsToShow.isEmpty()) {
            tvNoGroups.setText(
                    "עדיין אין טיולים להצטרף אליהם 🌍");
            tvNoGroups.setVisibility(TextView.VISIBLE);
            rvGroups.setVisibility(View.GONE);
        } else {
            tvNoGroups.setVisibility(TextView.GONE);
            rvGroups.setVisibility(View.VISIBLE);
        }
    }

    private String getGroupValue(
            Map<String, Object> group,
            String key) {

        Object value = group.get(key);

        // אם השדה לא נמצא ברמה הראשית, בודקים גם בתוך answers.
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

        // מוסיפים את המשתמש לרשימת החברים.
        Map<String, Object> groupUpdates = new HashMap<>();
        groupUpdates.put("memberIds", FieldValue.arrayUnion(userId));

        db.collection("Groups")
                .document(groupId)
                .set(groupUpdates, SetOptions.merge())
                .addOnSuccessListener(unused -> {

                    // שומרים גם את מזהה הקבוצה בפרופיל המשתמש.
                    Map<String, Object> userUpdates = new HashMap<>();
                    userUpdates.put("groupId", groupId);

                    db.collection("Users")
                            .document(userId)
                            .set(userUpdates, SetOptions.merge())
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
                                            "ההצטרפות נשמרה בקבוצה, אבל עדכון המשתמש נכשל: "
                                                    + e.getMessage(),
                                            Toast.LENGTH_LONG
                                    ).show()
                            );
                })
                .addOnFailureListener(e ->
                        Toast.makeText(
                                JoinTripActivity.this,
                                "לא הצלחנו להצטרף: " + e.getMessage(),
                                Toast.LENGTH_LONG
                        ).show()
                );
    }
}
