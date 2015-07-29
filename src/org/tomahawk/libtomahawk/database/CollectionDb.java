/* == This file is part of Tomahawk Player - <http://tomahawk-player.org> ===
 *
 *   Copyright 2015, Enno Gottschalk <mrmaffen@googlemail.com>
 *
 *   Tomahawk is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   Tomahawk is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with Tomahawk. If not, see <http://www.gnu.org/licenses/>.
 */
package org.tomahawk.libtomahawk.database;

import org.tomahawk.libtomahawk.collection.Artist;
import org.tomahawk.libtomahawk.resolver.models.ScriptResolverTrack;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CollectionDb extends SQLiteOpenHelper {

    public static final String TAG = CollectionDb.class.getSimpleName();

    public static final String ID = "_id";

    public static final String TABLE_ARTISTS = "artists";

    public static final String ARTISTS_ARTIST = "artist";

    public static final String ARTISTS_ARTISTDISAMBIGUATION = "artistDisambiguation";

    public static final String TABLE_ALBUMARTISTS = "albumArtists";

    public static final String ALBUMARTISTS_ALBUMARTIST = "albumArtist";

    public static final String ALBUMARTISTS_ALBUMARTISTDISAMBIGUATION = "albumArtistDisambiguation";

    public static final String TABLE_ALBUMS = "albums";

    public static final String ALBUMS_ALBUM = "album";

    public static final String ALBUMS_IMAGEPATH = "imagePath";

    public static final String ALBUMS_ALBUMARTISTID = "albumArtistId";

    public static final String TABLE_ARTISTALBUMS = "artistAlbums";

    public static final String ARTISTALBUMS_ALBUMID = "albumId";

    public static final String ARTISTALBUMS_ARTISTID = "artistId";

    public static final String TABLE_TRACKS = "tracks";

    public static final String TRACKS_TRACK = "track";

    public static final String TRACKS_ARTISTID = "artistId";

    public static final String TRACKS_ALBUMID = "albumId";

    public static final String TRACKS_URL = "url";

    public static final String TRACKS_DURATION = "duration";

    public static final String TRACKS_ALBUMPOS = "albumPos";

    public static final String TRACKS_LINKURL = "linkUrl";

    private static final String CREATE_TABLE_ARTISTS = "CREATE TABLE IF NOT EXISTS "
            + TABLE_ARTISTS + " ("
            + ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
            + ARTISTS_ARTIST + " TEXT,"
            + ARTISTS_ARTISTDISAMBIGUATION + " TEXT,"
            + "UNIQUE (" + ARTISTS_ARTIST + ", " + ARTISTS_ARTISTDISAMBIGUATION
            + ") ON CONFLICT IGNORE);";

    private static final String CREATE_TABLE_ALBUMARTISTS = "CREATE TABLE IF NOT EXISTS "
            + TABLE_ALBUMARTISTS + " ("
            + ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
            + ALBUMARTISTS_ALBUMARTIST + " TEXT,"
            + ALBUMARTISTS_ALBUMARTISTDISAMBIGUATION + " TEXT,"
            + "UNIQUE (" + ALBUMARTISTS_ALBUMARTIST + ", " + ALBUMARTISTS_ALBUMARTISTDISAMBIGUATION
            + ") ON CONFLICT IGNORE);";

    private static final String CREATE_TABLE_ALBUMS = "CREATE TABLE IF NOT EXISTS "
            + TABLE_ALBUMS + " ("
            + ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
            + ALBUMS_ALBUM + " TEXT,"
            + ALBUMS_ALBUMARTISTID + " INTEGER,"
            + ALBUMS_IMAGEPATH + " TEXT,"
            + "UNIQUE (" + ALBUMS_ALBUM + ", " + ALBUMS_ALBUMARTISTID + ") ON CONFLICT IGNORE,"
            + "FOREIGN KEY(" + ALBUMS_ALBUMARTISTID + ") REFERENCES "
            + TABLE_ALBUMARTISTS + "(" + ID + "));";

    private static final String CREATE_TABLE_ARTISTALBUMS = "CREATE TABLE IF NOT EXISTS "
            + TABLE_ARTISTALBUMS + " ("
            + ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
            + ARTISTALBUMS_ALBUMID + " INTEGER,"
            + ARTISTALBUMS_ARTISTID + " INTEGER,"
            + "UNIQUE (" + ARTISTALBUMS_ALBUMID + ", " + ARTISTALBUMS_ARTISTID
            + ") ON CONFLICT IGNORE,"
            + "FOREIGN KEY(" + ARTISTALBUMS_ALBUMID + ") REFERENCES "
            + TABLE_ALBUMS + "(" + ID + "),"
            + "FOREIGN KEY(" + ARTISTALBUMS_ARTISTID + ") REFERENCES "
            + TABLE_ARTISTS + "(" + ID + "));";

    private static final String CREATE_TABLE_TRACKS = "CREATE TABLE IF NOT EXISTS "
            + TABLE_TRACKS + " ("
            + ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
            + TRACKS_TRACK + " TEXT,"
            + TRACKS_ARTISTID + " INTEGER,"
            + TRACKS_ALBUMID + " INTEGER,"
            + TRACKS_URL + " TEXT,"
            + TRACKS_DURATION + " INTEGER,"
            + TRACKS_ALBUMPOS + " INTEGER,"
            + TRACKS_LINKURL + " TEXT,"
            + "UNIQUE (" + TRACKS_TRACK + ", " + TRACKS_ARTISTID + ", " + TRACKS_ALBUMID
            + ") ON CONFLICT IGNORE,"
            + "FOREIGN KEY(" + TRACKS_ARTISTID + ") REFERENCES "
            + TABLE_ARTISTS + "(" + ID + "),"
            + "FOREIGN KEY(" + TRACKS_ALBUMID + ") REFERENCES "
            + TABLE_ALBUMS + "(" + ID + "));";

    private static final int DB_VERSION = 2;

    private static final String DB_FILE_SUFFIX = "_collection.db";

    private final SQLiteDatabase mDb;

    private static class JoinInfo {

        String table;

        Map<String, String> conditions = new HashMap<>();

    }

    public CollectionDb(Context context, String collectionId) {
        super(context, collectionId + DB_FILE_SUFFIX, null, DB_VERSION);

        close();
        mDb = getWritableDatabase();
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_ARTISTS);
        db.execSQL(CREATE_TABLE_ALBUMARTISTS);
        db.execSQL(CREATE_TABLE_ALBUMS);
        db.execSQL(CREATE_TABLE_ARTISTALBUMS);
        db.execSQL(CREATE_TABLE_TRACKS);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.d(TAG, "Upgrading database from version " + oldVersion + " to " + newVersion
                + ", which might destroy all old data");
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE `" + TABLE_ALBUMS + "` ADD COLUMN `"
                    + ALBUMS_IMAGEPATH + "` TEXT");
        }
    }

    public synchronized void addTracks(ScriptResolverTrack[] tracks) {
        long time = System.currentTimeMillis();
        mDb.beginTransaction();
        ContentValues values = new ContentValues();
        values.put(ARTISTS_ARTIST, Artist.COMPILATION_ARTIST.getName());
        values.put(ARTISTS_ARTISTDISAMBIGUATION, "");
        mDb.insert(TABLE_ARTISTS, null, values);

        // First we insert all artists and albumArtists
        for (ScriptResolverTrack track : tracks) {
            if (track.artist == null) {
                track.artist = "";
            }
            if (track.artistDisambiguation == null) {
                track.artistDisambiguation = "";
            }
            if (track.album == null) {
                track.album = "";
            }
            if (track.albumArtist == null) {
                track.albumArtist = "";
            }
            if (track.albumArtistDisambiguation == null) {
                track.albumArtistDisambiguation = "";
            }
            if (track.track == null) {
                track.track = "";
            }
            values = new ContentValues();
            values.put(ARTISTS_ARTIST, track.artist);
            values.put(ARTISTS_ARTISTDISAMBIGUATION, track.artistDisambiguation);
            mDb.insert(TABLE_ARTISTS, null, values);
            values = new ContentValues();
            values.put(ALBUMARTISTS_ALBUMARTIST, track.albumArtist);
            values.put(ALBUMARTISTS_ALBUMARTISTDISAMBIGUATION, track.albumArtistDisambiguation);
            mDb.insert(TABLE_ALBUMARTISTS, null, values);
        }

        Map<String, Integer> cachedArtists = new HashMap<>();
        Map<Integer, String> cachedArtistIds = new HashMap<>();
        Cursor cursor = mDb.query(TABLE_ARTISTS,
                new String[]{ID, ARTISTS_ARTIST, ARTISTS_ARTISTDISAMBIGUATION},
                null, null, null, null, null);
        try {
            cursor.moveToFirst();
            if (!cursor.isAfterLast()) {
                do {
                    cachedArtists.put(concatKeys(cursor.getString(1), cursor.getString(2)),
                            cursor.getInt(0));
                    cachedArtistIds.put(cursor.getInt(0), cursor.getString(1));
                } while (cursor.moveToNext());
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        // Check if we want to store the album as a compilation album (with artist "Various Artists")
        Map<String, Set<String>> albumArtists = new HashMap<>();
        for (ScriptResolverTrack track : tracks) {
            Set<String> artists = albumArtists.get(track.album);
            if (artists == null) {
                artists = new HashSet<>();
                albumArtists.put(track.album, artists);
            }
            if (artists.size() < 2) {
                artists.add(track.artist);
            }
        }
        for (ScriptResolverTrack track : tracks) {
            values = new ContentValues();
            values.put(ALBUMS_ALBUM, track.album);
            int albumArtistId;
            if (albumArtists.get(track.album).size() == 1) {
                albumArtistId = cachedArtists.get(
                        concatKeys(track.artist, track.artistDisambiguation));
            } else {
                albumArtistId = cachedArtists.get(
                        concatKeys(Artist.COMPILATION_ARTIST.getName(), ""));
            }
            values.put(ALBUMS_ALBUMARTISTID, albumArtistId);
            values.put(ALBUMS_IMAGEPATH, track.imagePath);
            mDb.insert(TABLE_ALBUMS, null, values);
        }

        Map<String, Integer> cachedAlbums = new HashMap<>();
        Map<Integer, String> cachedAlbumIds = new HashMap<>();
        cursor = mDb.query(TABLE_ALBUMS,
                new String[]{ID, ALBUMS_ALBUM, ALBUMS_ALBUMARTISTID},
                null, null, null, null, null);
        try {
            cursor.moveToFirst();
            if (!cursor.isAfterLast()) {
                do {
                    cachedAlbums.put(concatKeys(cursor.getString(1), cursor.getString(2)),
                            cursor.getInt(0));
                    cachedAlbumIds.put(cursor.getInt(0), cursor.getString(1));
                } while (cursor.moveToNext());
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        for (ScriptResolverTrack track : tracks) {
            values = new ContentValues();
            int albumArtistId;
            if (albumArtists.get(track.album).size() == 1) {
                albumArtistId = cachedArtists.get(
                        concatKeys(track.artist, track.artistDisambiguation));
            } else {
                albumArtistId = cachedArtists.get(
                        concatKeys(Artist.COMPILATION_ARTIST.getName(), ""));
            }
            int artistId = cachedArtists.get(concatKeys(track.artist, track.artistDisambiguation));
            int albumId = cachedAlbums.get(concatKeys(track.album, albumArtistId));
            values.put(ARTISTALBUMS_ARTISTID, artistId);
            values.put(ARTISTALBUMS_ALBUMID, albumId);
            mDb.insert(TABLE_ARTISTALBUMS, null, values);
            values = new ContentValues();
            values.put(TRACKS_TRACK, track.track);
            values.put(TRACKS_ARTISTID, artistId);
            values.put(TRACKS_ALBUMID, albumId);
            values.put(TRACKS_URL, track.url);
            values.put(TRACKS_DURATION, (int) track.duration);
            values.put(TRACKS_LINKURL, track.linkUrl);
            values.put(TRACKS_ALBUMPOS, track.albumPos);
            mDb.insert(TABLE_TRACKS, null, values);
        }

        mDb.setTransactionSuccessful();
        mDb.endTransaction();
        Log.d(TAG, "Added " + tracks.length + " tracks in " + (System.currentTimeMillis() - time)
                + "ms");
    }

    public synchronized void wipe() {
        mDb.execSQL("DROP TABLE IF EXISTS `" + TABLE_ARTISTS + "`;");
        mDb.execSQL(CREATE_TABLE_ARTISTS);
        mDb.execSQL("DROP TABLE IF EXISTS `" + TABLE_ALBUMARTISTS + "`;");
        mDb.execSQL(CREATE_TABLE_ALBUMARTISTS);
        mDb.execSQL("DROP TABLE IF EXISTS `" + TABLE_ALBUMS + "`;");
        mDb.execSQL(CREATE_TABLE_ALBUMS);
        mDb.execSQL("DROP TABLE IF EXISTS `" + TABLE_ARTISTALBUMS + "`;");
        mDb.execSQL(CREATE_TABLE_ARTISTALBUMS);
        mDb.execSQL("DROP TABLE IF EXISTS `" + TABLE_TRACKS + "`;");
        mDb.execSQL(CREATE_TABLE_TRACKS);
    }

    public synchronized Cursor tracks(Map<String, String> where, String[] orderBy) {
        String[] fields = new String[]{ARTISTS_ARTIST, ARTISTS_ARTISTDISAMBIGUATION, ALBUMS_ALBUM,
                TRACKS_TRACK, TRACKS_DURATION, TRACKS_URL, TRACKS_LINKURL, TRACKS_ALBUMPOS};
        List<JoinInfo> joinInfos = new ArrayList<>();
        JoinInfo joinInfo = new JoinInfo();
        joinInfo.table = TABLE_ARTISTS;
        joinInfo.conditions.put(TABLE_TRACKS + "." + TRACKS_ARTISTID, TABLE_ARTISTS + "." + ID);
        joinInfos.add(joinInfo);
        joinInfo = new JoinInfo();
        joinInfo.table = TABLE_ALBUMS;
        joinInfo.conditions.put(TABLE_TRACKS + "." + TRACKS_ALBUMID, TABLE_ALBUMS + "." + ID);
        joinInfos.add(joinInfo);
        return sqlSelect(TABLE_TRACKS, fields, where, joinInfos, orderBy);
    }

    public synchronized Cursor albums(String[] orderBy) {
        String[] fields = new String[]{ALBUMS_ALBUM, ARTISTS_ARTIST, ARTISTS_ARTISTDISAMBIGUATION,
                ALBUMS_IMAGEPATH};
        List<JoinInfo> joinInfos = new ArrayList<>();
        JoinInfo joinInfo = new JoinInfo();
        joinInfo.table = TABLE_ARTISTS;
        joinInfo.conditions.put(
                TABLE_ALBUMS + "." + ALBUMS_ALBUMARTISTID, TABLE_ARTISTS + "." + ID);
        joinInfos.add(joinInfo);
        return sqlSelect(TABLE_ALBUMS, fields, null, joinInfos, orderBy);
    }

    public synchronized Cursor artists(String[] orderBy) {
        String[] fields = new String[]{ARTISTS_ARTIST, ARTISTS_ARTISTDISAMBIGUATION};
        return sqlSelect(TABLE_ARTISTS, fields, null, null, orderBy);
    }

    public synchronized Cursor albumArtists(String[] orderBy) {
        String[] fields = new String[]{ALBUMARTISTS_ALBUMARTIST,
                ALBUMARTISTS_ALBUMARTISTDISAMBIGUATION};
        return sqlSelect(TABLE_ALBUMARTISTS, fields, null, null, orderBy);
    }

    public synchronized Cursor artistAlbums(String artist, String artistDisambiguation) {
        String[] fields = new String[]{ID};
        Map<String, String> where = new HashMap<>();
        where.put(ARTISTS_ARTIST, artist);
        where.put(ARTISTS_ARTISTDISAMBIGUATION, artistDisambiguation);
        int artistId;
        Cursor cursor = null;
        try {
            cursor = sqlSelect(TABLE_ARTISTS, fields, where, null, null);
            if (cursor.moveToFirst()) {
                artistId = cursor.getInt(0);
            } else {
                Log.e(TAG, "artistAlbums - Couldn't find artist with given name!");
                return null;
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        fields = new String[]{ALBUMS_ALBUM, ARTISTS_ARTIST, ARTISTS_ARTISTDISAMBIGUATION};
        where = new HashMap<>();
        where.put(ARTISTALBUMS_ARTISTID, String.valueOf(artistId));
        List<JoinInfo> joinInfos = new ArrayList<>();
        JoinInfo joinInfo = new JoinInfo();
        joinInfo.table = TABLE_ALBUMS;
        joinInfo.conditions.put(
                TABLE_ARTISTALBUMS + "." + ARTISTALBUMS_ALBUMID, TABLE_ALBUMS + "." + ID);
        joinInfos.add(joinInfo);
        joinInfo = new JoinInfo();
        joinInfo.table = TABLE_ARTISTS;
        joinInfo.conditions.put(
                TABLE_ALBUMS + "." + ALBUMS_ALBUMARTISTID, TABLE_ARTISTS + "." + ID);
        joinInfos.add(joinInfo);
        return sqlSelect(TABLE_ARTISTALBUMS, fields, where, joinInfos, new String[]{ALBUMS_ALBUM});
    }

    public synchronized Cursor albumTracks(String album, String albumArtist,
            String albumArtistDisambiguation) {
        String[] fields = new String[]{ID};
        Map<String, String> where = new HashMap<>();
        where.put(ARTISTS_ARTIST, albumArtist);
        where.put(ARTISTS_ARTISTDISAMBIGUATION, albumArtistDisambiguation);
        int artistId;
        Cursor cursor = null;
        try {
            cursor = sqlSelect(TABLE_ARTISTS, fields, where, null, null);
            if (cursor.moveToFirst()) {
                artistId = cursor.getInt(0);
            } else {
                Log.e(TAG, "albumTracks - Couldn't find artist with given name!");
                return null;
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        fields = new String[]{ID};
        where = new HashMap<>();
        where.put(ALBUMS_ALBUM, album);
        where.put(ALBUMS_ALBUMARTISTID, String.valueOf(artistId));
        int albumId;
        cursor = null;
        try {
            cursor = sqlSelect(TABLE_ALBUMS, fields, where, null, null);
            if (cursor.moveToFirst()) {
                albumId = cursor.getInt(0);
            } else {
                Log.e(TAG, "albumTracks - Couldn't find album with given name!");
                return null;
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        where = new HashMap<>();
        where.put(TRACKS_ALBUMID, String.valueOf(albumId));
        return tracks(where, new String[]{TRACKS_ALBUMPOS});
    }

    private Cursor sqlSelect(String table, String[] fields, Map<String, String> where,
            List<JoinInfo> joinInfos, String[] orderBy) {
        String whereString = "";
        String[] whereValues = null;
        if (where != null) {
            whereString = " WHERE ";
            whereValues = where.values().toArray(new String[where.size()]);
            boolean notFirst = false;
            for (String whereKey : where.keySet()) {
                if (notFirst) {
                    whereString += " AND ";
                }
                notFirst = true;
                whereString += table + "." + whereKey + " = ?";
            }
        }

        String joinString = "";
        if (joinInfos != null) {
            for (JoinInfo joinInfo : joinInfos) {
                joinString += " INNER JOIN " + joinInfo.table + " ON ";
                boolean notFirst = false;
                for (String joinKey : joinInfo.conditions.keySet()) {
                    if (notFirst) {
                        joinString += " AND ";
                    }
                    notFirst = true;
                    joinString += joinKey + " = " + joinInfo.conditions.get(joinKey);
                }
            }
        }

        String orderString = "";
        if (orderBy != null) {
            orderString = " ORDER BY ";
            boolean notFirst = false;
            for (String orderingTerm : orderBy) {
                if (notFirst) {
                    joinString += " , ";
                }
                notFirst = true;
                orderString += orderingTerm;
            }
        }

        String fieldsString = "*";
        if (fields != null) {
            fieldsString = "";
            boolean notFirst = false;
            for (String field : fields) {
                if (notFirst) {
                    fieldsString += ", ";
                }
                notFirst = true;
                fieldsString += field;
            }
        }
        String statement = "SELECT " + fieldsString + " FROM " + table + joinString + whereString
                + orderString;
        return mDb.rawQuery(statement, whereValues);
    }

    private static String concatKeys(Object... keys) {
        String result = "";
        for (int i = 0; i < keys.length; i++) {
            if (i > 0) {
                result += "♣";
            }
            result += keys[i];
        }
        return result;
    }

}
