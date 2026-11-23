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

public class ActivityPreferences extends AppCompatActivity {

    private TextView tvQuestionNumber, tvProgressPercent, tvQuestion;
    private ProgressBar pbPreferencesProgress;
    private RadioGroup rgAnswers;
    private Button btnNextQuestion;

    private RadioButton[] radioButtons;

    private int currentQuestion = 0;

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

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top,
                    systemBars.right, systemBars.bottom);
            return insets;
        });

        tvQuestionNumber = findViewById(R.id.tvQuestionNumber);
        tvProgressPercent = findViewById(R.id.tvProgressPercent);
        tvQuestion = findViewById(R.id.tvQuestion);
        pbPreferencesProgress = findViewById(R.id.pbPreferencesProgress);
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
                Toast.makeText(this,
                        "צריך לבחור תשובה לפני שממשיכים",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            if (currentQuestion < questions.length - 1) {
                currentQuestion++;
                showQuestion();
            } else {
                Toast.makeText(this,
                        "סיימת את שאלון ההעדפות! ✈️",
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    private void showQuestion() {

        tvQuestion.setText(questions[currentQuestion]);

        tvQuestionNumber.setText(
                "שאלה " + (currentQuestion + 1) + " מתוך " + questions.length
        );

        int progress = (currentQuestion + 1) * 100 / questions.length;

        tvProgressPercent.setText(progress + "%");
        pbPreferencesProgress.setProgress(progress);

        rgAnswers.clearCheck();

        for (int i = 0; i < radioButtons.length; i++) {

            if (i < answers[currentQuestion].length) {
                radioButtons[i].setText(answers[currentQuestion][i]);
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
}
