package com.example.syncit1;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.text.SimpleDateFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
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

        TextView btnBack = findViewById(R.id.btnBack);
        btnBack.bringToFront();
        btnBack.setOnClickListener(v -> finish());

        etDestinationSearch = findViewById(R.id.etDestinationSearch);
        tvSearchStartDate = findViewById(R.id.tvSearchStartDate);
        tvSearchEndDate = findViewById(R.id.tvSearchEndDate);
        btnSearchGroups = findViewById(R.id.btnSearchGroups);
        tvNoGroups = findViewById(R.id.tvNoGroups);
        rvGroups = findViewById(R.id.rvGroups);

        rvGroups.setLayoutManager(new LinearLayoutManager(this));

        adapter = new GroupAdapter(group -> joinGroup(group));
        rvGroups.setAdapter(adapter);

        btnSearchGroups.setOnClickListener(v -> filterGroups());

        tvSearchStartDate.setOnClickListener(
                v -> showDatePicker(tvSearchStartDate));

        tvSearchEndDate.setOnClickListener(
                v -> showDatePicker(tvSearchEndDate));

        loadGroups();
    }

    // בחירת תאריך
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

    // טעינת כל הקבוצות מ-Firebase
    private void loadGroups() {

        tvNoGroups.setText("טוענים טיולים... ✈️");
        tvNoGroups.setVisibility(View.VISIBLE);
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

                            group.put("groupId", document.getId());

                            allGroups.add(group);
                        }
                    }

                    filterGroups();
                })
                .addOnFailureListener(e -> {

                    tvNoGroups.setText(
                            "לא הצלחנו לטעון את הטיולים");

                    tvNoGroups.setVisibility(View.VISIBLE);
                    rvGroups.setVisibility(View.GONE);

                    Toast.makeText(
                            JoinTripActivity.this,
                            "שגיאה בטעינת הטיולים: " + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    // התאריך הנוכחי
    private String getTodayDate() {

        Calendar calendar = Calendar.getInstance();

        return String.format(
                Locale.US,
                "%04d-%02d-%02d",
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH) + 1,
                calendar.get(Calendar.DAY_OF_MONTH)
        );
    }

    // בדיקה שהתאריך תקין בפורמט yyyy-MM-dd
    private static boolean isValidDate(String date) {

        if (date == null || date.trim().isEmpty()) {
            return false;
        }

        SimpleDateFormat format =
                new SimpleDateFormat("yyyy-MM-dd", Locale.US);

        format.setLenient(false);

        try {

            Date parsedDate = format.parse(date);

            return parsedDate != null
                    && format.format(parsedDate).equals(date);

        } catch (ParseException e) {
            return false;
        }
    }

    // בדיקת תקינות תאריכי הקבוצה
    private static boolean hasValidGroupDates(
            String startDate,
            String endDate) {

        return isValidDate(startDate)
                && isValidDate(endDate)
                && startDate.compareTo(endDate) <= 0;
    }

    // בדיקת תקינות מספר המשתתפים המקסימלי
    private static int getValidMaxMembers(Object value) {

        if (!(value instanceof Number)) {
            return -1;
        }

        Number number = (Number) value;

        int maxMembers = number.intValue();

        if (maxMembers <= 0
                || number.doubleValue() != maxMembers) {
            return -1;
        }

        return maxMembers;
    }

    // ספירת חברים קיימים ללא כפילויות
    private static int countMembers(Object membersObject) {

        if (!(membersObject instanceof List<?>)) {
            return 0;
        }

        List<String> uniqueMembers = new ArrayList<>();

        for (Object member : (List<?>) membersObject) {

            if (member != null) {

                String id = member.toString();

                if (!id.trim().isEmpty()
                        && !uniqueMembers.contains(id)) {

                    uniqueMembers.add(id);
                }
            }
        }

        return uniqueMembers.size();
    }

    // סינון ודירוג הקבוצות
    private void filterGroups() {

        String destination = etDestinationSearch.getText()
                .toString()
                .trim()
                .toLowerCase(Locale.ROOT);

        String startDate = getSelectedDate(tvSearchStartDate);
        String endDate = getSelectedDate(tvSearchEndDate);

        String today = getTodayDate();

        boolean hasDestination = !destination.isEmpty();
        boolean hasStartDate = !startDate.isEmpty();
        boolean hasEndDate = !endDate.isEmpty();

        boolean hasDateSearch = hasStartDate || hasEndDate;

        if (hasStartDate && !isValidDate(startDate)
                || hasEndDate && !isValidDate(endDate)) {

            Toast.makeText(
                    this,
                    "אחד התאריכים אינו תקין",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

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

            // 1. לא מציגים קבוצות סגורות
            Object closedValue = group.get("isClosed");

            if (Boolean.TRUE.equals(closedValue)) {
                continue;
            }

            // 2. חייב להיות מספר משתתפים מקסימלי תקין
            int maxMembers =
                    getValidMaxMembers(group.get("maxMembers"));

            if (maxMembers <= 0) {
                continue;
            }

            // 3. בדיקת מספר המשתתפים
            int currentMembers =
                    countMembers(group.get("memberIds"));

            if (currentMembers >= maxMembers) {
                continue;
            }

            // 4. בדיקת תאריכי הקבוצה
            String groupStart =
                    getGroupValue(group, "startDate");

            String groupEnd =
                    getGroupValue(group, "endDate");

            if (!hasValidGroupDates(groupStart, groupEnd)) {
                continue;
            }

            // 5. לא מציגים טיולים שהסתיימו
            if (groupEnd.compareTo(today) < 0) {
                continue;
            }

            // 6. בדיקת התאמה ליעד
            String groupDestination =
                    getGroupValue(group, "destination")
                            .toLowerCase(Locale.ROOT);

            boolean destinationMatches =
                    hasDestination
                            && !groupDestination.isEmpty()
                            && groupDestination.contains(destination);

            // 7. בדיקת התאמה לתאריכים
            boolean exactDates = false;
            boolean datesOverlap = false;

            if (hasStartDate && hasEndDate) {

                exactDates =
                        groupStart.equals(startDate)
                                && groupEnd.equals(endDate);

                datesOverlap =
                        groupEnd.compareTo(startDate) >= 0
                                && groupStart.compareTo(endDate) <= 0;

            } else if (hasStartDate) {

                datesOverlap =
                        groupEnd.compareTo(startDate) >= 0;

            } else if (hasEndDate) {

                datesOverlap =
                        groupStart.compareTo(endDate) <= 0;
            }

            // 8. דירוג לפי קטגוריות במקום ניקוד
            int matchCategory;
            String matchInfo;

            if (!hasDestination && !hasDateSearch) {

                matchCategory = 0;
                matchInfo = "טיול זמין להצטרפות ✈️";

            } else if (hasDestination && hasDateSearch) {

                // חיפוש לפי יעד ותאריכים

                if (destinationMatches && exactDates) {

                    matchCategory = 0;
                    matchInfo = "התאמה מלאה לחיפוש ✨";

                } else if (exactDates) {

                    matchCategory = 1;
                    matchInfo = "התאריכים מתאימים, יעד שונה 📅";

                } else if (destinationMatches) {

                    matchCategory = 2;
                    matchInfo = "היעד מתאים, התאריכים שונים 📍";

                } else {

                    matchCategory = 3;
                    matchInfo = "אפשרות נוספת שאולי תעניין אותך ✈️";
                }

            } else if (hasDestination) {

                // חיפוש לפי יעד בלבד

                if (destinationMatches) {

                    matchCategory = 0;
                    matchInfo = "היעד מתאים לחיפוש 📍";

                } else {

                    matchCategory = 1;
                    matchInfo = "טיול נוסף שאולי יעניין אותך ✈️";
                }

            } else {

                // חיפוש לפי תאריכים בלבד

                if (exactDates) {

                    matchCategory = 0;
                    matchInfo = "התאריכים מתאימים בדיוק ✨";

                } else if (datesOverlap) {

                    matchCategory = 1;
                    matchInfo = "יש חפיפה בתאריכים 📅";

                } else {

                    matchCategory = 2;
                    matchInfo = "טיול נוסף שאולי יעניין אותך ✈️";
                }
            }

            group.put("matchCategory", matchCategory);
            group.put("matchInfo", matchInfo);

            groupsToShow.add(group);
        }

        // מיון לפי קטגוריות ההתאמה
        // קטגוריה 0 תמיד ראשונה, אחריה 1 וכן הלאה
        // קבוצות באותה קטגוריה שומרות על סדר הטעינה

        groupsToShow.sort((group1, group2) -> {

            int category1 =
                    ((Number) group1.get("matchCategory")).intValue();

            int category2 =
                    ((Number) group2.get("matchCategory")).intValue();

            return Integer.compare(category1, category2);
        });

        adapter.setGroups(groupsToShow);

        if (groupsToShow.isEmpty()) {

            tvNoGroups.setText(
                    "עדיין אין טיולים פתוחים להצטרפות 🌍");

            tvNoGroups.setVisibility(View.VISIBLE);
            rvGroups.setVisibility(View.GONE);

        } else {

            tvNoGroups.setVisibility(View.GONE);
            rvGroups.setVisibility(View.VISIBLE);
        }
    }

    // קבלת ערך מהמסמך או מתוך answers
    private String getGroupValue(
            Map<String, Object> group,
            String key) {

        Object value = group.get(key);

        if (value == null) {

            Object answersObject = group.get("answers");

            if (answersObject instanceof Map<?, ?>) {

                value = ((Map<?, ?>) answersObject).get(key);
            }
        }

        return value == null
                ? ""
                : value.toString().trim();
    }

    // קבלת תאריך שנבחר
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

    // הצטרפות לקבוצה
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

        DocumentReference groupRef =
                db.collection("Groups").document(groupId);

        DocumentReference userRef =
                db.collection("Users").document(userId);

        // בדיקת זמינות ועדכון הנתונים באותה Transaction
        db.runTransaction(transaction -> {

            // קריאת שני המסמכים לפני ביצוע שינויים
            DocumentSnapshot groupDocument =
                    transaction.get(groupRef);

            DocumentSnapshot userDocument =
                    transaction.get(userRef);

            if (!groupDocument.exists()) {
                return "not_found";
            }

            if (!userDocument.exists()) {
                return "user_not_found";
            }

            // בדיקת סגירת הקבוצה
            Boolean isClosed =
                    groupDocument.getBoolean("isClosed");

            if (Boolean.TRUE.equals(isClosed)) {
                return "closed";
            }

            // בדיקת תקינות מספר המשתתפים
            int maxMembers =
                    getValidMaxMembers(
                            groupDocument.get("maxMembers"));

            if (maxMembers <= 0) {
                return "invalid_group";
            }

            // בדיקת תקינות תאריכים
            String groupStart =
                    groupDocument.getString("startDate");

            String groupEnd =
                    groupDocument.getString("endDate");

            if (!hasValidGroupDates(groupStart, groupEnd)
                    || groupEnd.compareTo(getTodayDate()) < 0) {

                return "invalid_group";
            }

            // קריאת חברי הקבוצה
            List<String> memberIds = new ArrayList<>();

            Object existingMembers =
                    groupDocument.get("memberIds");

            if (existingMembers instanceof List<?>) {

                for (Object id : (List<?>) existingMembers) {

                    if (id != null) {

                        String memberId = id.toString();

                        if (!memberId.trim().isEmpty()
                                && !memberIds.contains(memberId)) {

                            memberIds.add(memberId);
                        }
                    }
                }
            }

            // בדיקה אם המשתמש כבר חבר בקבוצה
            if (memberIds.contains(userId)) {
                return "already_member";
            }

            // בדיקת מכסה
            if (memberIds.size() >= maxMembers) {

                transaction.update(
                        groupRef,
                        "isClosed",
                        true
                );

                return "full";
            }

            // הוספת המשתמש לקבוצה
            memberIds.add(userId);

            boolean shouldClose =
                    memberIds.size() >= maxMembers;

            transaction.update(
                    groupRef,
                    "memberIds",
                    memberIds
            );

            transaction.update(
                    groupRef,
                    "isClosed",
                    shouldClose
            );

            // קריאת כל הקבוצות הקיימות של המשתמש
            List<String> groupIds = new ArrayList<>();

            Object existingGroupIds =
                    userDocument.get("groupIds");

            if (existingGroupIds instanceof List<?>) {

                for (Object id : (List<?>) existingGroupIds) {

                    if (id != null) {

                        String existingId = id.toString();

                        if (!existingId.trim().isEmpty()
                                && !groupIds.contains(existingId)) {

                            groupIds.add(existingId);
                        }
                    }
                }
            }

            // שמירה על groupId הישן, אם קיים
            String oldGroupId =
                    userDocument.getString("groupId");

            if (oldGroupId != null
                    && !oldGroupId.trim().isEmpty()
                    && !groupIds.contains(oldGroupId)) {

                groupIds.add(oldGroupId);
            }

            // הוספת הקבוצה החדשה בלי למחוק קבוצות קודמות
            if (!groupIds.contains(groupId)) {
                groupIds.add(groupId);
            }

            Map<String, Object> userData = new HashMap<>();

            userData.put("groupIds", groupIds);

            // Merge שומר על שאר שדות המשתמש
            transaction.set(
                    userRef,
                    userData,
                    SetOptions.merge()
            );

            return shouldClose
                    ? "joined_and_closed"
                    : "joined";

        }).addOnSuccessListener(result -> {

            switch (result) {

                case "joined":

                    Toast.makeText(
                            JoinTripActivity.this,
                            "הצטרפת לטיול בהצלחה! ✈️",
                            Toast.LENGTH_LONG
                    ).show();

                    openPreferences(groupId);
                    break;

                case "joined_and_closed":

                    Toast.makeText(
                            JoinTripActivity.this,
                            "הצטרפת בהצלחה! הקבוצה התמלאה ונסגרה להצטרפות 🎉",
                            Toast.LENGTH_LONG
                    ).show();

                    openPreferences(groupId);
                    break;

                case "already_member":

                    Toast.makeText(
                            JoinTripActivity.this,
                            "את כבר חברה בקבוצה הזאת",
                            Toast.LENGTH_LONG
                    ).show();

                    break;

                case "closed":
                case "full":

                    Toast.makeText(
                            JoinTripActivity.this,
                            "הקבוצה כבר מלאה או סגורה להצטרפות",
                            Toast.LENGTH_LONG
                    ).show();

                    loadGroups();
                    break;

                case "not_found":

                    Toast.makeText(
                            JoinTripActivity.this,
                            "הטיול כבר לא קיים",
                            Toast.LENGTH_LONG
                    ).show();

                    loadGroups();
                    break;

                case "user_not_found":

                    Toast.makeText(
                            JoinTripActivity.this,
                            "לא נמצא מסמך משתמש",
                            Toast.LENGTH_LONG
                    ).show();

                    break;

                case "invalid_group":

                    Toast.makeText(
                            JoinTripActivity.this,
                            "פרטי הקבוצה אינם תקינים או שהטיול הסתיים",
                            Toast.LENGTH_LONG
                    ).show();

                    loadGroups();
                    break;
            }

        }).addOnFailureListener(e ->

                Toast.makeText(
                        JoinTripActivity.this,
                        "לא הצלחנו להצטרף לטיול: " + e.getMessage(),
                        Toast.LENGTH_LONG
                ).show()
        );
    }

    // מעבר למסך שאלון ההעדפות
    private void openPreferences(String groupId) {

        Intent intent = new Intent(
                JoinTripActivity.this,
                ActivityPreferences.class
        );

        // העברת מזהה הקבוצה למסך ההעדפות
        intent.putExtra("groupId", groupId);

        startActivity(intent);

        finish();
    }
}
