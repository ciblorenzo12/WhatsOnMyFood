package com.ciblorenzo.whatsonmyfood;

import android.content.Context;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

@RunWith(AndroidJUnit4.class)
public class AdditiveDatabaseValidationTest {
    private AppDatabase database;
    private AdditiveDao dao;

    @Before
    public void createDatabase() {
        Context context = ApplicationProvider.getApplicationContext();
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase.class)
                .allowMainThreadQueries()
                .build();
        dao = database.additiveDao();
    }

    @After
    public void closeDatabase() {
        database.close();
    }

    @Test
    public void jsonNullDoesNotBecomeDisplayText() throws Exception {
        JSONObject response = new JSONObject("{\"name\":null,\"category\":\"null\"}");

        assertEquals("", AdditiveDatabaseActivity.jsonText(response, "name"));
        assertEquals("", AdditiveDatabaseActivity.jsonText(response, "category"));
    }

    @Test
    public void removesPreviouslyStoredNullPlaceholderRows() {
        dao.insertAdditive(new AdditiveEntry(
                "null", "null", "", "null", "null", "", "", ""
        ));

        dao.deleteInvalidEntries();

        assertNull(dao.getAdditiveByName("null"));
    }
}
