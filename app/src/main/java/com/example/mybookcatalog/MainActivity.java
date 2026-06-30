package com.example.mybookcatalog;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private FirebaseAnalytics mFirebaseAnalytics;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    
    private List<Book> featuredBooks = new ArrayList<>();
    private List<Book> popularBooks = new ArrayList<>();
    private List<Book> newArrivals = new ArrayList<>();

    private List<Book> featuredFiltered = new ArrayList<>();
    private List<Book> popularFiltered = new ArrayList<>();
    private List<Book> newArrivalsFiltered = new ArrayList<>();
    
    private BookCoverAdapter featuredAdapter;
    private BookCoverAdapter popularAdapter;
    private BookCoverAdapter newArrivalsAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        
        // Initialize Firebase
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // Track app open
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.SCREEN_NAME, "Main Screen");
        bundle.putString(FirebaseAnalytics.Param.SCREEN_CLASS, "MainActivity");
        mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundle);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        setupRecyclerViews();
        listenToBookData();
        setupBottomNavigation();
        setupSearch();
        
        // Ensure user is signed in (Anonymously for this demo)
        if (mAuth.getCurrentUser() == null) {
            mAuth.signInAnonymously().addOnSuccessListener(authResult -> {
                Toast.makeText(this, "Signed in anonymously", Toast.LENGTH_SHORT).show();
            });
        }

        // For first-time run: Populate Firestore if empty
        checkAndPopulateInitialData();
    }

    private void setupSearch() {
        com.google.android.material.chip.ChipGroup chipGroup = findViewById(R.id.categoryChipGroup);
        chipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            filterBooks();
        });
    }

    private void filterBooks() {
        com.google.android.material.chip.ChipGroup chipGroup = findViewById(R.id.categoryChipGroup);
        int checkedId = chipGroup.getCheckedChipId();
        String selectedGenre = "";
        
        if (checkedId == R.id.chipFiction) selectedGenre = "fiction";
        else if (checkedId == R.id.chipSciFi) selectedGenre = "science fiction";
        else if (checkedId == R.id.chipThriller) selectedGenre = "thriller";
        else if (checkedId == R.id.chipSelfHelp) selectedGenre = "self-help";

        filterList(featuredBooks, featuredFiltered, "", selectedGenre, featuredAdapter);
        filterList(popularBooks, popularFiltered, "", selectedGenre, popularAdapter);
        filterList(newArrivals, newArrivalsFiltered, "", selectedGenre, newArrivalsAdapter);
    }

    private void filterList(List<Book> source, List<Book> filtered, String query, String genre, BookCoverAdapter adapter) {
        filtered.clear();
        for (Book b : source) {
            boolean matchesQuery = query.isEmpty() || 
                                   b.getTitle().toLowerCase().contains(query) || 
                                   b.getAuthor().toLowerCase().contains(query);
            
            boolean matchesGenre = genre.isEmpty() || 
                                   b.getGenre().toLowerCase().contains(genre);
            
            if (matchesQuery && matchesGenre) {
                filtered.add(b);
            }
        }
        adapter.notifyDataSetChanged();
    }

    private void checkAndPopulateInitialData() {
        // We will check if the database has exactly 10 books. 
        // If it has 6 (the old ones) or 0, we will reset it to the new 10 books.
        db.collection("books").get().addOnSuccessListener(queryDocumentSnapshots -> {
            if (queryDocumentSnapshots.size() != 10) {
                // Clear old data first
                for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                    doc.getReference().delete();
                }
                populateFirestore();
            }
        });
    }

    private void populateFirestore() {
        List<Book> list = new ArrayList<>();
        
        // 1. The Last Ember (Featured)
        list.add(new Book("The Last Ember", "Daniel Roth", "Historical Thriller", 
            "A former archaeologist is pulled into a deadly conspiracy when ancient secrets buried beneath Rome resurface.", 
            "https://images.unsplash.com/photo-1589998059171-988d887df646?auto=format&fit=crop&q=80&w=400"));
        list.get(list.size()-1).setSection("featured");
        
        // 2. Quantum Mirage (Featured)
        list.add(new Book("Quantum Mirage", "Lila Chen", "Science Fiction", 
            "In a future where time travel is illegal, a rogue physicist must choose between saving the world or saving her daughter.", 
            "https://images.unsplash.com/photo-1614850523296-d8c1af93d400?auto=format&fit=crop&q=80&w=400"));
        list.get(list.size()-1).setSection("featured");

        // 3. Roots & Wings (Featured)
        list.add(new Book("Roots & Wings", "Maria Esteban", "Literary Fiction", 
            "A moving generational story of a Cuban-American family searching for identity, belonging, and redemption.", 
            "https://images.unsplash.com/photo-1544947950-fa07a98d237f?auto=format&fit=crop&q=80&w=400"));
        list.get(list.size()-1).setSection("featured");

        // 4. The Mind Sculptor (Popular)
        list.add(new Book("The Mind Sculptor", "Dr. Evan Shaw", "Psychology / Non-Fiction", 
            "A groundbreaking look at neuroplasticity and how you can rewire your brain for success and happiness.", 
            "https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c?auto=format&fit=crop&q=80&w=400"));
        list.get(list.size()-1).setSection("popular");

        // 5. Inkbound (Popular)
        list.add(new Book("Inkbound: Chronicles of the Lost Library", "J.R. Faulkner", "Fantasy / Adventure", 
            "A young librarian discovers that ancient books can open portals to other worlds—but not all stories have happy endings.", 
            "https://images.unsplash.com/photo-1512820790803-83ca734da794?auto=format&fit=crop&q=80&w=400"));
        list.get(list.size()-1).setSection("popular");

        // 6. Startup Savage (Popular)
        list.add(new Book("Startup Savage", "Nicole Vega", "Business / Entrepreneurship", 
            "A brutally honest guide to launching a tech startup in the real world, full of failures, pivots, and unexpected wins.", 
            "https://images.unsplash.com/photo-1519389950473-47ba0277781c?auto=format&fit=crop&q=80&w=400"));
        list.get(list.size()-1).setSection("popular");

        // 7. Beneath Crimson Skies (New Arrival)
        list.add(new Book("Beneath Crimson Skies", "Tomasz Novak", "Historical Fiction / WWII", 
            "The intertwined lives of resistance fighters, spies, and survivors during the Nazi occupation of Warsaw.", 
            "https://images.unsplash.com/photo-1533230832467-5501309e7c5b?auto=format&fit=crop&q=80&w=400"));
        list.get(list.size()-1).setSection("new");

        // 8. The Art of Stillness (New Arrival)
        list.add(new Book("The Art of Stillness", "Tara Bell", "Self-Help / Mindfulness", 
            "Learn how to find peace in a chaotic world by mastering the ancient wisdom of stillness.", 
            "https://images.unsplash.com/photo-1506126613408-eca07ce68773?auto=format&fit=crop&q=80&w=400"));
        list.get(list.size()-1).setSection("new");

        // 9. Neon Ghosts (New Arrival)
        list.add(new Book("Neon Ghosts", "Khalid Jones", "Urban Fantasy / Mystery", 
            "A private investigator with the ability to see spirits uncovers a supernatural conspiracy beneath the city's neon lights.", 
            "https://images.unsplash.com/photo-1550684848-fac1c5b4e853?auto=format&fit=crop&q=80&w=400"));
        list.get(list.size()-1).setSection("new");

        // 10. Eat Green, Live Clean (New Arrival)
        list.add(new Book("Eat Green, Live Clean", "Dr. Sanjay Patel", "Health & Wellness", 
            "A practical guide to plant-based nutrition and detox living, backed by science and easy recipes.", 
            "https://images.unsplash.com/photo-1490645935967-10de6ba17061?auto=format&fit=crop&q=80&w=400"));
        list.get(list.size()-1).setSection("new");

        for (Book b : list) {
            db.collection("books").add(b);
        }
    }

    private void setupRecyclerViews() {
        RecyclerView rvFeatured = findViewById(R.id.recyclerViewFeatured);
        featuredAdapter = createAdapter(featuredFiltered);
        rvFeatured.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvFeatured.setAdapter(featuredAdapter);

        RecyclerView rvPopular = findViewById(R.id.recyclerViewPopular);
        popularAdapter = createAdapter(popularFiltered);
        rvPopular.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvPopular.setAdapter(popularAdapter);

        RecyclerView rvNew = findViewById(R.id.recyclerViewNew);
        newArrivalsAdapter = createAdapter(newArrivalsFiltered);
        rvNew.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvNew.setAdapter(newArrivalsAdapter);
    }

    private BookCoverAdapter createAdapter(List<Book> books) {
        return new BookCoverAdapter(books, book -> {
            // Track book selection
            Bundle params = new Bundle();
            params.putString(FirebaseAnalytics.Param.ITEM_ID, book.getId());
            params.putString(FirebaseAnalytics.Param.ITEM_NAME, book.getTitle());
            params.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "book");
            mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, params);

            Intent intent = new Intent(MainActivity.this, DetailActivity.class);
            intent.putExtra("book", book);
            startActivity(intent);
        });
    }

    private void listenToBookData() {
        db.collection("books").addSnapshotListener((value, error) -> {
            if (error != null) {
                Toast.makeText(this, "Error loading books: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                return;
            }

            if (value != null) {
                int totalBooks = value.size();
                Toast.makeText(this, "Found " + totalBooks + " books in cloud", Toast.LENGTH_SHORT).show();

                featuredBooks.clear();
                popularBooks.clear();
                newArrivals.clear();

                for (QueryDocumentSnapshot doc : value) {
                    Book book = doc.toObject(Book.class);
                    book.setId(doc.getId());
                    
                    String section = book.getSection();
                    if ("popular".equals(section)) {
                        popularBooks.add(book);
                    } else if ("new".equals(section)) {
                        newArrivals.add(book);
                    } else {
                        // Default to featured if section is missing or "featured"
                        featuredBooks.add(book);
                    }
                }
                
                // Initial filter (show all)
                filterBooks();
            }
        });
    }

    private void setupBottomNavigation() {
        com.google.android.material.bottomnavigation.BottomNavigationView bottomNav = findViewById(R.id.bottomNavigation);
        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_catalog) {
                return true;
            } else if (itemId == R.id.nav_books) {
                if (mAuth.getCurrentUser() == null) {
                    Toast.makeText(this, "Please sign in to see your books", Toast.LENGTH_SHORT).show();
                } else {
                    startActivity(new Intent(this, FavoritesActivity.class));
                }
                return true;
            } else if (itemId == R.id.nav_reservations) {
                startActivity(new Intent(this, ReservationsActivity.class));
                return true;
            } else if (itemId == R.id.nav_settings) {
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
            }
            return false;
        });
    }
}
