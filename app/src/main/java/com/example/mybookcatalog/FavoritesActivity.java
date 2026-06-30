package com.example.mybookcatalog;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class FavoritesActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private List<Book> favoriteBooks = new ArrayList<>();
    private BookCoverAdapter adapter;
    private View emptyState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorites);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        emptyState = findViewById(R.id.emptyFavoritesView);

        setupRecyclerView();
        loadFavorites();
    }

    private void setupRecyclerView() {
        RecyclerView recyclerView = findViewById(R.id.recyclerViewFavorites);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 3));
        adapter = new BookCoverAdapter(favoriteBooks, book -> {
            Intent intent = new Intent(FavoritesActivity.this, DetailActivity.class);
            intent.putExtra("book", book);
            startActivity(intent);
        });
        recyclerView.setAdapter(adapter);
    }

    private void loadFavorites() {
        if (mAuth.getCurrentUser() == null) return;

        String userId = mAuth.getCurrentUser().getUid();
        db.collection("users").document(userId)
                .collection("favorites")
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Toast.makeText(this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (value != null) {
                        favoriteBooks.clear();
                        if (value.isEmpty()) {
                            emptyState.setVisibility(View.VISIBLE);
                            adapter.notifyDataSetChanged();
                        } else {
                            emptyState.setVisibility(View.GONE);
                            for (QueryDocumentSnapshot doc : value) {
                                fetchBookDetails(doc.getId());
                            }
                        }
                    }
                });
    }

    private void fetchBookDetails(String bookId) {
        db.collection("books").document(bookId).get().addOnSuccessListener(documentSnapshot -> {
            Book book = documentSnapshot.toObject(Book.class);
            if (book != null) {
                book.setId(documentSnapshot.getId());
                favoriteBooks.add(book);
                adapter.notifyDataSetChanged();
            }
        });
    }
}
