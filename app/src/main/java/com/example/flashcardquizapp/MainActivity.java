package com.example.flashcardquizapp;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private static class Card {
        String question, answer;

        Card(String question, String answer) {
            this.question = question;
            this.answer = answer;
        }
    }

    private final ArrayList<Card> cards = new ArrayList<>();
    private int index = 0;
    private boolean showingAnswer = false;

    private TextView tvCounter, tvLabel, tvCardText;
    private MaterialButton btnShowAnswer, btnPrevious, btnNext, btnAdd, btnEdit, btnDelete;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvCounter = findViewById(R.id.tvCounter);
        tvLabel = findViewById(R.id.tvLabel);
        tvCardText = findViewById(R.id.tvCardText);
        btnShowAnswer = findViewById(R.id.btnShowAnswer);
        btnPrevious = findViewById(R.id.btnPrevious);
        btnNext = findViewById(R.id.btnNext);
        btnAdd = findViewById(R.id.btnAdd);
        btnEdit = findViewById(R.id.btnEdit);
        btnDelete = findViewById(R.id.btnDelete);

        prefs = getSharedPreferences("flashcards", MODE_PRIVATE);
        loadCards();

        btnShowAnswer.setOnClickListener(v -> {
            showingAnswer = !showingAnswer;
            updateUI();
        });

        btnNext.setOnClickListener(v -> {
            if (cards.isEmpty()) return;
            index = (index + 1) % cards.size();
            showingAnswer = false;
            updateUI();
        });

        btnPrevious.setOnClickListener(v -> {
            if (cards.isEmpty()) return;
            index = (index - 1 + cards.size()) % cards.size();
            showingAnswer = false;
            updateUI();
        });

        btnAdd.setOnClickListener(v -> showCardDialog(null));

        btnEdit.setOnClickListener(v -> {
            if (!cards.isEmpty()) showCardDialog(cards.get(index));
        });

        btnDelete.setOnClickListener(v -> {
            if (cards.isEmpty()) return;
            new AlertDialog.Builder(this)
                    .setTitle("Delete flashcard?")
                    .setMessage("This cannot be undone.")
                    .setPositiveButton("Delete", (d, w) -> {
                        cards.remove(index);
                        if (index >= cards.size()) index = Math.max(0, cards.size() - 1);
                        showingAnswer = false;
                        saveCards();
                        updateUI();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        updateUI();
    }

    private void showCardDialog(Card existing) {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_card, null);
        EditText etQuestion = view.findViewById(R.id.etQuestion);
        EditText etAnswer = view.findViewById(R.id.etAnswer);

        if (existing != null) {
            etQuestion.setText(existing.question);
            etAnswer.setText(existing.answer);
        }

        new AlertDialog.Builder(this)
                .setTitle(existing == null ? "Add Flashcard" : "Edit Flashcard")
                .setView(view)
                .setPositiveButton("Save", (d, w) -> {
                    String q = etQuestion.getText().toString().trim();
                    String a = etAnswer.getText().toString().trim();
                    if (q.isEmpty() || a.isEmpty()) {
                        Toast.makeText(this, "Please fill both fields", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (existing == null) {
                        cards.add(new Card(q, a));
                        index = cards.size() - 1;
                    } else {
                        existing.question = q;
                        existing.answer = a;
                    }
                    showingAnswer = false;
                    saveCards();
                    updateUI();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void updateUI() {
        boolean empty = cards.isEmpty();

        if (empty) {
            tvCounter.setText("No cards");
            tvLabel.setText("");
            tvCardText.setText("No flashcards yet.\nTap Add to create one.");
        } else {
            Card c = cards.get(index);
            tvCounter.setText("Card " + (index + 1) + " of " + cards.size());
            tvLabel.setText(showingAnswer ? "ANSWER" : "QUESTION");
            tvCardText.setText(showingAnswer ? c.answer : c.question);
        }

        btnShowAnswer.setText(showingAnswer ? "Hide Answer" : "Show Answer");
        btnShowAnswer.setEnabled(!empty);
        btnPrevious.setEnabled(!empty);
        btnNext.setEnabled(!empty);
        btnEdit.setEnabled(!empty);
        btnDelete.setEnabled(!empty);
    }

    private void saveCards() {
        JSONArray arr = new JSONArray();
        try {
            for (Card c : cards) {
                JSONObject o = new JSONObject();
                o.put("q", c.question);
                o.put("a", c.answer);
                arr.put(o);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        prefs.edit().putString("cards", arr.toString()).apply();
    }

    private void loadCards() {
        String json = prefs.getString("cards", null);
        if (json == null) {
            cards.add(new Card("What does JVM stand for?", "Java Virtual Machine"));
            cards.add(new Card("Which keyword is used to inherit a class in Java?", "extends"));
            cards.add(new Card("What is the entry point method of a Java program?",
                    "public static void main(String[] args)"));
            saveCards();
            return;
        }
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                cards.add(new Card(o.getString("q"), o.getString("a")));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}