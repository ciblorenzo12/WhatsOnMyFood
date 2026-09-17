package com.ciblorenzo.whatsonmyfood;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.room.InvalidationTracker;
import com.ciblorenzo.whatsonmyfood.recall.PantryRecallScheduler;
import com.ciblorenzo.whatsonmyfood.recall.RecallNotifications;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import java.util.Set;

public class PantryApplication extends Application {
    private String scheduledUser;
    @Override public void onCreate() {
        super.onCreate();
        RecallNotifications.createChannel(this);
        FirebaseAuth.getInstance().addAuthStateListener(auth -> {
            FirebaseUser user = auth.getCurrentUser();
            String id = user == null ? null : user.getUid();
            if (scheduledUser != null && !scheduledUser.equals(id)) {
                PantryRecallScheduler.cancel(this, scheduledUser);
                RecallNotifications.clear(this);
            }
            scheduledUser = id;
            if (id != null) PantryRecallScheduler.start(this, id);
        });
        // All save/refresh paths use these Room tables, including OCR and manual products.
        AppDatabase.getDatabase(this).getInvalidationTracker().addObserver(
                new InvalidationTracker.Observer("pantry", "products") {
                    @Override public void onInvalidated(@NonNull Set<String> tables) {
                        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                        if (user != null) PantryRecallScheduler.sweep(PantryApplication.this, user.getUid());
                    }
                });
    }
}
