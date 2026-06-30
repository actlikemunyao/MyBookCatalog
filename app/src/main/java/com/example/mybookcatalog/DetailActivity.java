package com.example.mybookcatalog;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class DetailActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private FirebaseAnalytics mFirebaseAnalytics;
    private boolean isFavorite = false;
    private FloatingActionButton fabFavorite;
    private MaterialButton btnReserve;
    private Book book;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_detail);
        
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.detail_main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        book = (Book) getIntent().getSerializableExtra("book");
        fabFavorite = findViewById(R.id.fabFavorite);
        btnReserve = findViewById(R.id.btnReserve);

        if (book != null) {
            setupUI();
            checkIfFavorite();
            checkIfReserved();
            trackBookView();
        }

        fabFavorite.setOnClickListener(v -> toggleFavorite());
        btnReserve.setOnClickListener(v -> reserveBook());
    }

    private void setupUI() {
        ImageView imageViewCover = findViewById(R.id.detailImage);
        TextView textViewTitle = findViewById(R.id.detailTitle);
        TextView textViewAuthor = findViewById(R.id.detailAuthor);
        TextView textViewGenre = findViewById(R.id.detailGenre);
        TextView textViewDescription = findViewById(R.id.detailDescription);

        Glide.with(this)
                .load(book.getImageUrl())
                .placeholder(R.drawable.ic_menu_book)
                .into(imageViewCover);

        textViewTitle.setText(book.getTitle());
        textViewAuthor.setText("Author: " + book.getAuthor());
        textViewGenre.setText("Genre: " + book.getGenre());
        textViewDescription.setText(book.getDescription());
    }

    private void trackBookView() {
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.ITEM_ID, book.getId());
        bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, book.getTitle());
        bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "book_detail");
        mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.VIEW_ITEM, bundle);
    }

    private void checkIfFavorite() {
        if (mAuth.getCurrentUser() == null) return;

        String userId = mAuth.getCurrentUser().getUid();
        db.collection("users").document(userId)
                .collection("favorites").document(book.getId())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        isFavorite = true;
                        fabFavorite.setImageResource(android.R.drawable.btn_star_big_on);
                    }
                });
    }

    private void toggleFavorite() {
        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "Please sign in to favorite books", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = mAuth.getCurrentUser().getUid();
        DocumentReference favRef = db.collection("users").document(userId)
                .collection("favorites").document(book.getId());

        if (isFavorite) {
            favRef.delete().addOnSuccessListener(aVoid -> {
                isFavorite = false;
                fabFavorite.setImageResource(android.R.drawable.btn_star_big_off);
                Toast.makeText(this, "Removed from favorites", Toast.LENGTH_SHORT).show();
            });
        } else {
            Map<String, Object> favData = new HashMap<>();
            favData.put("title", book.getTitle());
            favData.put("timestamp", com.google.firebase.Timestamp.now());
            
            favRef.set(favData).addOnSuccessListener(aVoid -> {
                isFavorite = true;
                fabFavorite.setImageResource(android.R.drawable.btn_star_big_on);
                Toast.makeText(this, "Added to favorites", Toast.LENGTH_SHORT).show();
                
                // Track favorite event
                Bundle params = new Bundle();
                params.putString("book_id", book.getId());
                params.putString("book_title", book.getTitle());
                mFirebaseAnalytics.logEvent("add_to_favorites", params);
            });
        }
    }

    private void checkIfReserved() {
        if (mAuth.getCurrentUser() == null) return;

        String userId = mAuth.getCurrentUser().getUid();
        db.collection("users").document(userId)
                .collection("reservations").document(book.getId())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        btnReserve.setText("Reserved");
                        btnReserve.setEnabled(false);
                        btnReserve.setAlpha(0.6f);
                    }
                });
    }

    private void reserveBook() {
        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "Please sign in to reserve books", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = mAuth.getCurrentUser().getUid();
        Map<String, Object> reserveData = new HashMap<>();
        reserveData.put("title", book.getTitle());
        reserveData.put("timestamp", com.google.firebase.Timestamp.now());

        db.collection("users").document(userId)
                .collection("reservations").document(book.getId())
                .set(reserveData)
                .addOnSuccessListener(aVoid -> {
                    btnReserve.setText("Reserved");
                    btnReserve.setEnabled(false);
                    btnReserve.setAlpha(0.6f);
                    Toast.makeText(this, "Book reserved successfully!", Toast.LENGTH_SHORT).show();
                    
                    Bundle params = new Bundle();
                    params.putString("book_id", book.getId());
                    params.putString("book_title", book.getTitle());
                    mFirebaseAnalytics.logEvent("book_reserved", params);
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Reservation failed", Toast.LENGTH_SHORT).show());
    }
}
