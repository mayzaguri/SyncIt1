package com.example.syncit1;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.FieldValue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ActivityPreferences extends AppCompatActivity {

    private TextView tvQuestionNumber, tvProgressPercent, tvQuestion;
    private ProgressBar pbPreferencesProgress;
    private RadioGroup rgAnswers;
    private Button btnNextQuestion;

    private RadioButton[] radioButtons;

    private int currentQuestion = 0;

    private String groupId;
    private String userId;

    // שמירת התשובות שנבחרו במהלך השאלון
    private final String[] selectedAnswers = new String[9];

    private final String[] questions = {
            "איזה סוג טיול הכי מעניין אותך?",
            "מה הקצב המועדף עליך בטיול?",
            "איזה סוג לינה אתה מעדיף?",
            "מה הכי חשוב לך בטיול?",
            "כמה זמן היית רוצה להקדיש לאטרקציות?",
            "איזה אוכל אתה מעדיף בטיול?",
            "מה התקציב המועדף עליך?",
            "באיזו שעה אתה מעדיף להתחיל את היום?",
            "מה הכי חשוב לך בחוויית הטיול?"
    };

    private final String[][] answers = {
            {"🏖️ בטן-גב ומנוחה", "🏔️ טבע ונופים", "🏙️ עיר וקניות", "🎢 אטרקציות והרפתקאות", "🎭 תרבות והיסטוריה"},
            {"🐢 רגוע ונינוח", "🚶 מאוזן", "🏃 עמוס ומלא פעילויות"},
            {"🏨 מלון", "🏡 דירה או Airbnb", "🏕️ קמפינג", "🛏️ הוסטל"},
            {"💰 מחיר משתלם", "✨ חוויות מיוחדות", "🛋️ נוחות ופינוק", "📍 מיקום מרכזי"},
            {"⏱️ מעט אטרקציות", "⚖️ כמות בינונית", "🎯 כמה שיותר"},
            {"🍕 אוכל מקומי", "🍔 אוכל מוכר", "🥗 אוכל מגוון", "🍽️ מסעדות מיוחדות"},
            {"💵 חסכוני", "💳 בינוני", "💎 מפנק"},
            {"🌅 מוקדם בבוקר", "☀️ בבוקר", "🌤️ בצהריים"},
            {"📸 נופים ותמונות", "🎉 בילויים וכיף", "🧘 מנוחה ורוגע", "🗺️ גילוי מקומות חדשים"}
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_preferences);

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

        // קבלת מזהה הקבוצה שהועבר מהמסך הקודם
        groupId = getIntent().getStringExtra("groupId");

        // קבלת מזהה המשתמש המחובר
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            userId = FirebaseAuth.getInstance()
                    .getCurrentUser()
                    .getUid();
        }

        tvQuestionNumber = findViewById(R.id.tvQuestionNumber);
        tvProgressPercent = findViewById(R.id.tvProgressPercent);
        tvQuestion = findViewById(R.id.tvQuestion);

        pbPreferencesProgress =
                findViewById(R.id.pbPreferencesProgress);

        rgAnswers = findViewById(R.id.rgAnswers);

        btnNextQuestion = findViewById(R.id.btnNextQuestion);

        radioButtons = new RadioButton[]{
                findViewById(R.id.rbAnswer1),
                findViewById(R.id.rbAnswer2),
                findViewById(R.id.rbAnswer3),
                findViewById(R.id.rbAnswer4),
                findViewById(R.id.rbAnswer5)
        };

        showQuestion();

        btnNextQuestion.setOnClickListener(v -> {

            int selectedId = rgAnswers.getCheckedRadioButtonId();

            if (selectedId == -1) {

                Toast.makeText(
                        this,
                        "צריך לבחור תשובה לפני שממשיכים",
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }

            // שמירת התשובה של השאלה הנוכחית
            RadioButton selectedButton =
                    findViewById(selectedId);

            selectedAnswers[currentQuestion] =
                    selectedButton.getText().toString();

            if (currentQuestion < questions.length - 1) {

                currentQuestion++;
                showQuestion();

            } else {

                // השאלון הסתיים - שומרים את התשובות
                savePreferences();
            }
        });
    }

    // הצגת השאלה הנוכחית
    private void showQuestion() {

        tvQuestion.setText(questions[currentQuestion]);

        tvQuestionNumber.setText(
                "שאלה " + (currentQuestion + 1)
                        + " מתוך " + questions.length
        );

        int progress =
                (currentQuestion + 1) * 100 / questions.length;

        tvProgressPercent.setText(progress + "%");

        pbPreferencesProgress.setProgress(progress);

        rgAnswers.clearCheck();

        for (int i = 0; i < radioButtons.length; i++) {

            if (i < answers[currentQuestion].length) {

                radioButtons[i].setText(
                        answers[currentQuestion][i]);

                radioButtons[i].setVisibility(View.VISIBLE);

            } else {

                radioButtons[i].setVisibility(View.GONE);
            }
        }

        if (currentQuestion == questions.length - 1) {

            btnNextQuestion.setText("סיום ✓");

        } else {

            btnNextQuestion.setText("המשך ←");
        }
    }

    // שמירת שאלון ההעדפות ב-Firestore
    private void savePreferences() {

        if (groupId == null || groupId.trim().isEmpty()) {

            Toast.makeText(
                    this,
                    "לא נמצא מזהה קבוצה",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        if (userId == null || userId.trim().isEmpty()) {

            Toast.makeText(
                    this,
                    "צריך להתחבר כדי לשמור את השאלון",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        btnNextQuestion.setEnabled(false);
        btnNextQuestion.setText("שומר תשובות...");

        // יצירת רשימת התשובות
        List<Map<String, Object>> answerList =
                new ArrayList<>();

        for (int i = 0; i < questions.length; i++) {

            Map<String, Object> answerData =
                    new HashMap<>();

            answerData.put("questionId", "q" + (i + 1));
            answerData.put("question", questions[i]);
            answerData.put("answer", selectedAnswers[i]);

            answerList.add(answerData);
        }

        // יצירת מסמך ההעדפות של המשתמש
        Map<String, Object> preferencesData =
                new HashMap<>();

        preferencesData.put("userId", userId);
        preferencesData.put("answers", answerList);
        preferencesData.put("completed", true);
        preferencesData.put("questionnaireVersion", 1);
        preferencesData.put("updatedAt",
                FieldValue.serverTimestamp());

        // שמירה בתוך הקבוצה, במסמך אישי לכל משתמש
        FirebaseFirestore.getInstance()
                .collection("Groups")
                .document(groupId)
                .collection("Preferences")
                .document(userId)
                .set(preferencesData, SetOptions.merge())

                .addOnSuccessListener(unused -> {

                    Toast.makeText(
                            ActivityPreferences.this,
                            "שאלון ההעדפות נשמר בהצלחה! ✈️",
                            Toast.LENGTH_LONG
                    ).show();

                    btnNextQuestion.setText("נשמר בהצלחה ✓");

                })

                .addOnFailureListener(e -> {

                    btnNextQuestion.setEnabled(true);
                    btnNextQuestion.setText("סיום ✓");

                    Toast.makeText(
                            ActivityPreferences.this,
                            "שמירת השאלון נכשלה: "
                                    + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }
}
