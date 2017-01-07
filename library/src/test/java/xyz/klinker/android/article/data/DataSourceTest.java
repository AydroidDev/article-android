/*
 * Copyright (C) 2017 Jacob Klinker
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package xyz.klinker.android.article.data;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.robolectric.RuntimeEnvironment;

import xyz.klinker.android.article.ArticleRobolectricSuite;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class DataSourceTest extends ArticleRobolectricSuite {

    private DataSource source;

    @Mock
    private SQLiteDatabase database;
    @Mock
    private DatabaseSQLiteHelper helper;
    @Mock
    private Cursor cursor;

    @Before
    public void setUp() {
        source = new DataSource(helper);
        source.context = RuntimeEnvironment.application;
        when(database.isOpen()).thenReturn(true);
        when(helper.getWritableDatabase()).thenReturn(database);
        source.open();
    }

    @After
    public void tearDown() {
        source.close();
        verify(helper).close();
    }

    @Test
    public void realConstructor() {
        DataSource dataSource = DataSource.getInstance(RuntimeEnvironment.application);
        dataSource.open();
        dataSource.close();
    }

    @Test
    public void getDatabase() {
        assertEquals(database, source.getDatabase());
    }

    @Test
    public void clearTables() {
        source.clearTables();

        verify(database).delete("article", null, null);
        verify(database).delete("content", null, null);
        verifyNoMoreInteractions(database);
    }

    @Test
    public void beginTransaction() {
        source.beginTransaction();
        verify(database).beginTransaction();
    }

    @Test
    public void setTransactionSuccessful() {
        source.setTransactionSuccessful();
        verify(database).setTransactionSuccessful();
    }

    @Test
    public void endTransaction() {
        source.endTransaction();
        verify(database).endTransaction();
    }

    @Test
    public void execSql() {
        source.execSql("test sql");
        verify(database).execSQL("test sql");
    }

    @Test
    public void rawQuery() {
        source.rawQuery("test sql");
        verify(database).rawQuery("test sql", null);
    }

    @Test
    public void insertArticle() {
        source.insertArticle(new Article());

        verify(database).insert(eq("article"), eq((String) null), any(ContentValues.class));
        verify(database).insert(eq("content"), eq((String) null), any(ContentValues.class));
        verifyNoMoreInteractions(database);
    }

    @Test
    public void updateSavedArticleState() {
        Article article = new Article();
        article.id = 2L;
        article.saved = false;
        ContentValues values = new ContentValues();
        values.put("saved", false);

        source.updateSavedArticleState(article);

        verify(database).update("article", values, "_id=?", new String[] {"2"});
        verifyNoMoreInteractions(database);
    }

    @Test
    public void getArticle() {
        Cursor cursor = mock(Cursor.class);
        when(database.query(
                anyString(),
                any(String[].class),
                eq("url=?"),
                eq(new String[] {"http://google.com"}),
                eq((String) null),
                eq((String) null),
                eq((String) null)))
                .thenReturn(cursor);
        when(cursor.moveToFirst()).thenReturn(true);
        assertNotNull(source.getArticle("http://google.com"));
    }

    @Test
    public void getArticle_noMatchingUrls() {
        when(database.query(
                anyString(),
                any(String[].class),
                eq("url=?"),
                eq(new String[] {"http://google.com"}),
                eq((String) null),
                eq((String) null),
                eq((String) null)))
                .thenReturn(cursor);
        when(cursor.moveToFirst()).thenReturn(false);
        assertNull(source.getArticle("http://google.com"));
    }

    @Test
    public void getAllArticles() {
        when(database.query("article", null, null, null, null, null, "inserted_at desc"))
                .thenReturn(cursor);
        assertEquals(cursor, source.getAllArticles());
    }

    @Test
    public void getSavedArticles() {
        when(database.query("article", null, "saved=1", null, null, null, "inserted_at desc"))
                .thenReturn(cursor);
        assertEquals(cursor, source.getSavedArticles());
    }
}
