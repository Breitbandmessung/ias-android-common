/*
 *     Copyright (C) 2016-2025 zafaco GmbH
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License version 3
 *     as published by the Free Software Foundation.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Affero General Public License for more details.
 *
 *     You should have received a copy of the GNU Affero General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.zafaco.moduleCommon;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;


public class Database extends SQLiteOpenHelper
{


    private final Context ctx;


    private final Tool           mTool;
    private       SQLiteDatabase mDatabase;

    private boolean attachFlag = false;

    private static final int DATABASE_VERSION = 1;

    private String DATABASE_NAME = "";
    private String TABLE_NAME    = "";

    private static final String TAG = "Database";


    public Database(Context context, String database_name, String table_name)
    {
        super(context, database_name, null, DATABASE_VERSION);

        ctx = context;

        mTool = new Tool();

        DATABASE_NAME = database_name;
        TABLE_NAME    = table_name;

        LinkedHashMap<String, String> keys = new LinkedHashMap<>();
        keys.put("timestamp", "");

        createDB(keys);
    }

    @Override
    public void onCreate(SQLiteDatabase db)
    {

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
    {
        try
        {
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
            onCreate(db);
        } catch (Exception ex)
        {
            Log.error(TAG, "### Exception in onUpgrade() ###", ex);
        }
    }

    private void attachDB(String attDB, String attTable)
    {
        try
        {
            mDatabase = getWritableDatabase();
            mDatabase.execSQL("ATTACH DATABASE '" + ctx.getDatabasePath(attDB).toString() + "' AS " + attDB);

            Log.debug(TAG, "attachDB: ATTACH DATABASE '" + ctx.getDatabasePath(attDB).toString() + "' AS " + attDB);
        } catch (Exception ex)
        {
            HashMap<String, String> extras = new HashMap<>();
            extras.put("attDB", attDB);
            extras.put("attTable", attTable);
            extras.put("TABLE_NAME", TABLE_NAME);
            Log.warning(TAG, "attachDB: failed", extras, ex);
        }

        TABLE_NAME = TABLE_NAME + "," + attDB + "." + attTable;

        attachFlag = true;
    }

    private void detachDB(String attDB, String attTable)
    {

        if (!attachFlag)
            return;

        try
        {
            mDatabase = getWritableDatabase();
            mDatabase.execSQL("DETACH DATABASE " + attDB);

            Log.debug(TAG, "detachDB: DETACH DATABASE " + attDB);
        } catch (Exception ex)
        {
            HashMap<String, String> extras = new HashMap<>();
            extras.put("attDB", attDB);
            extras.put("attTable", attTable);
            extras.put("TABLE_NAME", TABLE_NAME);
            Log.warning(TAG, "detachDB: failed", extras, ex);
        }

        TABLE_NAME = attTable;
    }

    public void copyDB(String fromDB, String fromTable, String toDB, String toTable)
    {
        attachDB(fromDB, fromTable);

        mDatabase.execSQL("INSERT INTO " + toTable + " SELECT * FROM " + fromDB + "." + fromTable + ";");

        detachDB(fromDB, fromTable);
    }

    public boolean validateIfTableHasData()
    {
        try
        {
            mDatabase = getReadableDatabase();

            Cursor  c       = mDatabase.rawQuery("SELECT * FROM " + TABLE_NAME, null);
            boolean hasData = c.moveToFirst();

            c.close();
            mDatabase.close();
            return hasData;
        } catch (SQLiteException ex)
        {
            Log.warning(TAG, "validatedIfTableHasData: failed", ex);
            return false;
        }
    }

    public void dropTable()
    {
        mDatabase = getWritableDatabase();
        mDatabase.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
    }

    public void createDB(LinkedHashMap<String, String> keys)
    {
        try
        {
            mDatabase = getWritableDatabase();
            String createTable = "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (";

            createTable += "id INTEGER PRIMARY KEY AUTOINCREMENT, ";

            for (Map.Entry<String, String> entry : keys.entrySet())
            {
                createTable += entry.getKey() + " TEXT,";
            }

            createTable = createTable.substring(0, createTable.length() - 1);

            createTable += ")";


            Log.debug(TAG, "createDB: " + createTable);

            mDatabase.execSQL(createTable);
            checkColumns(keys);

            mDatabase.close();
        } catch (Exception ex)
        {
            Log.warning(TAG, "createDB: failed", keys, ex);
        }


    }

    public void createDB(JSONObject keys)
    {

        try
        {
            mDatabase = getWritableDatabase();
            String createTable = "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (";

            createTable += "id INTEGER PRIMARY KEY AUTOINCREMENT, ";

            for (Iterator<String> iter = keys.keys(); iter.hasNext(); )
            {
                createTable += iter.next() + " TEXT,";
            }

            createTable = createTable.substring(0, createTable.length() - 1);

            createTable += ")";


            mDatabase.execSQL(createTable);
            checkColumns(keys);

            mDatabase.close();
        } catch (Exception ex)
        {
            Log.warning(TAG, "createDB: failed", keys, ex);
        }


    }

    private void checkColumns(LinkedHashMap<String, String> keys)
    {

        try
        {
            mDatabase = getWritableDatabase();
            ArrayList<String> tmp = new ArrayList<>();

            String pragmaTable = "PRAGMA table_info(" + TABLE_NAME + ")";

            Cursor result = mDatabase.rawQuery(pragmaTable, null);

            while (result.moveToNext())
            {
                tmp.add(result.getString(1));
            }

            result.close();

            for (Map.Entry<String, String> entry : keys.entrySet())
            {

                if (!tmp.contains(entry.getKey()))
                {
                    String alterTable = "ALTER TABLE " + TABLE_NAME + " ADD COLUMN " + entry.getKey() + " TEXT ";


                    mDatabase.execSQL(alterTable);
                }
            }
            mDatabase.close();
        } catch (Exception ex)
        {
            keys.put("TABLE_NAME", TABLE_NAME);
            Log.warning(TAG, "checkColumns: failed", keys, ex);
        }

    }

    private void checkColumns(JSONObject keys)
    {

        try
        {
            mDatabase = getWritableDatabase();
            ArrayList<String> tmp = new ArrayList<>();

            String pragmaTable = "PRAGMA table_info(" + TABLE_NAME + ")";

            Cursor result = mDatabase.rawQuery(pragmaTable, null);

            while (result.moveToNext())
            {
                tmp.add(result.getString(1));
            }

            result.close();

            for (Iterator<String> iter = keys.keys(); iter.hasNext(); )
            {
                String key = iter.next();


                if (!tmp.contains(key))
                {
                    String alterTable = "ALTER TABLE " + TABLE_NAME + " ADD COLUMN " + key + " TEXT ";


                    mDatabase.execSQL(alterTable);
                }
            }
            mDatabase.close();
        } catch (Exception ex)
        {
            Log.warning(TAG, "checkColumns: failed", keys, ex);
        }

    }

    public void insert(LinkedHashMap<String, String> aData)
    {
        String sql    = "";
        String keys   = "";
        String values = "";

        sql += "INSERT INTO " + TABLE_NAME + " ";


        for (Map.Entry<String, String> entry : aData.entrySet())
        {
            try
            {
                keys += entry.getKey() + ",";
                values += "?,";
            } catch (Exception ex)
            {
                Log.warning(TAG, "insert LinkedHashMap: string concat", ex);
            }
        }

        keys   = keys.substring(0, keys.length() - 1);
        values = values.substring(0, values.length() - 1);
        sql += "(" + keys + ") VALUES (" + values + ");";


        try
        {

            mDatabase = getWritableDatabase();
            mDatabase.beginTransaction();
            SQLiteStatement stmt = mDatabase.compileStatement(sql);

            int i = 1;
            for (Map.Entry<String, String> entry : aData.entrySet())
            {
                try
                {


                    String tmp = entry.getValue();

                    tmp = tmp.trim();


                    stmt.bindString(i++, tmp);
                } catch (Exception ex)
                {
                    Log.warning(TAG, "insert LinkedHashMap: statement binding", ex);
                }
            }

            stmt.executeInsert();
            stmt.clearBindings();

            mDatabase.setTransactionSuccessful();
            mDatabase.endTransaction();

            mDatabase.close();
        } catch (SQLiteException ex)
        {
            HashMap<String, String> extras = new HashMap<>();
            extras.put("query", sql);
            Log.warning(TAG, "insert LinkedHashMap: failed", extras, ex);
        }
    }

    public void insert(JSONObject aData)
    {
        String sql    = "";
        String keys   = "";
        String values = "";

        sql += "INSERT INTO " + TABLE_NAME + " ";

        for (Iterator<String> iter = aData.keys(); iter.hasNext(); )
        {
            keys += iter.next() + ",";
            values += "?,";
        }

        keys   = keys.substring(0, keys.length() - 1);
        values = values.substring(0, values.length() - 1);
        sql += "(" + keys + ") VALUES (" + values + ");";


        try
        {
            mDatabase = getWritableDatabase();
            mDatabase.beginTransaction();
            SQLiteStatement stmt = mDatabase.compileStatement(sql);

            int i = 1;
            for (Iterator<String> iter = aData.keys(); iter.hasNext(); )
            {
                try
                {
                    String key = iter.next();


                    String tmp = aData.getString(key);

                    tmp = tmp.trim();


                    stmt.bindString(i++, tmp);
                } catch (Exception ex)
                {
                    Log.warning(TAG, "insert JSONObject: statement binding", ex);
                }
            }

            stmt.executeInsert();
            stmt.clearBindings();

            mDatabase.setTransactionSuccessful();
            mDatabase.endTransaction();

            mDatabase.close();
        } catch (SQLiteException ex)
        {
            HashMap<String, String> extras = new HashMap<>();
            extras.put("query", sql);
            Log.warning(TAG, "insert JSONObject: failed", extras, ex);
        }
    }

    public int update(ContentValues cValues, int nId)
    {
        int retCode = 0;

        try
        {
            mDatabase = this.getWritableDatabase();

            if (mDatabase != null)
            {
                retCode = mDatabase.update(TABLE_NAME, cValues, "id=" + nId, null);

                mDatabase.close();
            }
        } catch (SQLiteException ex)
        {
            HashMap<String, String> extras = new HashMap<>();
            extras.put("tableName", TABLE_NAME);
            extras.put("values", cValues.toString());
            extras.put("where", "id=" + nId);
            com.zafaco.moduleCommon.Log.warning(TAG, "update: failed", extras, ex);
        }

        return retCode;
    }

    public int update(ContentValues cValues, String sWhere)
    {
        int retCode = 0;

        try
        {
            mDatabase = this.getWritableDatabase();

            if (mDatabase != null)
            {
                retCode = mDatabase.update(TABLE_NAME, cValues, sWhere, null);

                mDatabase.close();
            }
        } catch (SQLiteException ex)
        {
            HashMap<String, String> extras = new HashMap<>();
            extras.put("tableName", TABLE_NAME);
            extras.put("values", cValues.toString());
            extras.put("where", sWhere);
            com.zafaco.moduleCommon.Log.warning(TAG, "update: failed", extras, ex);
        }

        return retCode;
    }

    public int deleteID(String column, String value)
    {
        int retCode = 0;

        try
        {
            mDatabase = this.getWritableDatabase();

            if (mDatabase != null && !value.equals("0") && !value.equals(""))
            {
                String[] args = {value};
                retCode = mDatabase.delete(TABLE_NAME, column + "=?", args);

                mDatabase.close();
            }
        } catch (SQLiteException ex)
        {
            Log.warning(TAG, "deleteID: failed", ex);
        }

        return retCode;
    }


    public LinkedHashMap<String, String> selectColumns()
    {
        LinkedHashMap<String, String> columns = new LinkedHashMap<>();
        String                        query   = "SELECT * FROM " + TABLE_NAME + " LIMIT 1";
        try
        {
            mDatabase = getWritableDatabase();

            Cursor dbCursor = mDatabase.rawQuery(query, null);

            while (dbCursor.moveToNext())
            {
                for (int i = 0; i < dbCursor.getColumnCount(); i++)
                {
                    columns.put(dbCursor.getColumnName(i), null);
                }
            }
            dbCursor.close();

            mDatabase.close();
        } catch (SQLiteException ex)
        {
            HashMap<String, String> extras = new HashMap<>();
            extras.put("query", query);
            com.zafaco.moduleCommon.Log.warning(TAG, "selectColumns: failed", extras, ex);
        }

        return columns;
    }


    public ArrayList<LinkedHashMap<String, String>> select(String where, String order, int asc)
    {
        ArrayList<LinkedHashMap<String, String>> rows  = new ArrayList<>();
        String                                   query = "SELECT * FROM " + TABLE_NAME + " WHERE " + where + " ORDER BY " + order + " " + ((asc == 0) ? "ASC" : "DESC");

        try
        {


            mDatabase = this.getReadableDatabase();

            Cursor result = mDatabase.rawQuery(query, null);

            while (result.moveToNext())
            {
                LinkedHashMap<String, String> columns = new LinkedHashMap<>();

                for (int i = 0; i < result.getColumnCount(); i++)
                {

                    columns.put(result.getColumnName(i), result.getString(i));
                }
                rows.add(columns);
            }

            result.close();

            mDatabase.close();
        } catch (Exception ex)
        {
            HashMap<String, String> extras = new HashMap<>();
            extras.put("query", query);
            Log.warning(TAG, "select: failed", extras, ex);
        }

        return rows;
    }

    public ArrayList<LinkedHashMap<String, String>> selectAll(String order, int asc)
    {
        return selectAll("1", order, asc, 0);
    }


    public ArrayList<LinkedHashMap<String, String>> selectAll(String where, String order, int asc, int limit)
    {
        ArrayList<LinkedHashMap<String, String>> rows = new ArrayList<>();

        String query = "SELECT * FROM " + TABLE_NAME + " WHERE " + where + " ORDER BY " + order + " " + ((asc == 0) ? "ASC" : "DESC") + " LIMIT " + ((limit == 0) ? "10000" : limit);
        try
        {


            mDatabase = this.getReadableDatabase();

            Cursor result = mDatabase.rawQuery(query, null);

            while (result.moveToNext())
            {
                LinkedHashMap<String, String> columns = new LinkedHashMap<>();

                for (int i = 0; i < result.getColumnCount(); i++)
                {

                    columns.put(result.getColumnName(i), result.getString(i));
                }
                rows.add(columns);
            }

            result.close();

            mDatabase.close();
        } catch (Exception ex)
        {
            HashMap<String, String> extras = new HashMap<>();
            extras.put("query", query);
            Log.warning(TAG, "selectAll: failed", extras, ex);
        }

        return rows;
    }


    public Cursor selectAllCursor(String order, int asc)
    {
        String query = "SELECT *,id as _id FROM " + TABLE_NAME + " WHERE 1 ORDER BY " + order + " " + ((asc == 0) ? "ASC" : "DESC");


        mDatabase = this.getWritableDatabase();

        Cursor cDatabaseCursor = mDatabase.rawQuery(query, null);

        return cDatabaseCursor;
    }


    public Cursor selectAllCursor(String where, String order, int asc)
    {
        String query = "SELECT id as _id,* FROM " + TABLE_NAME + " WHERE " + where + " ORDER BY " + order + " " + ((asc == 0) ? "ASC" : "DESC");


        try
        {
            mDatabase = this.getWritableDatabase();

            Cursor cDatabaseCursor = mDatabase.rawQuery(query, null);

            return cDatabaseCursor;
        } catch (SQLiteException ex)
        {
            Log.warning(TAG, "selectAllCursor: failed", ex);
        }
        return null;
    }

    public int getLastID()
    {
        int    nLastID = 0;
        String query   = "SELECT MAX(id) as id FROM " + TABLE_NAME;
        try
        {


            mDatabase = getWritableDatabase();

            Cursor result = mDatabase.rawQuery(query, null);

            result.moveToFirst();

            nLastID = result.getInt(0);

            result.close();

            mDatabase.close();
        } catch (Exception ex)
        {
            HashMap<String, String> extras = new HashMap<>();
            extras.put("query", query);
            extras.put("lastId", Integer.toString(nLastID));
            Log.warning(TAG, "getLastID: failed", extras, ex);
        }

        return nLastID;
    }

    public int getCount(String ftable)
    {
        int nCount = 0;

        String query = "SELECT COUNT(1) FROM (SELECT  * FROM " + TABLE_NAME + " WHERE ftable='" + ftable + "' GROUP BY fkey ) as tbl";
        try
        {


            mDatabase = getWritableDatabase();

            Cursor result = mDatabase.rawQuery(query, null);

            result.moveToFirst();

            nCount = result.getInt(0);

            result.close();

            mDatabase.close();
        } catch (Exception ex)
        {
            HashMap<String, String> extras = new HashMap<>();
            extras.put("query", query);
            extras.put("count", Integer.toString(nCount));
            Log.warning(TAG, "getCount: failed", extras, ex);
        }

        return nCount;
    }


    public ArrayList<LinkedHashMap<String, String>> convertCursor(Cursor cCursor)
    {
        ArrayList<LinkedHashMap<String, String>> rows = new ArrayList<>();

        try
        {
            while (cCursor.moveToNext())
            {
                LinkedHashMap<String, String> columns = new LinkedHashMap<>();

                for (int i = 0; i < cCursor.getColumnCount(); i++)
                {

                    columns.put(cCursor.getColumnName(i), cCursor.getString(i));
                }
                rows.add(columns);
            }

            cCursor.close();
        } catch (Exception ex)
        {
            Log.warning(TAG, "convertCursor: failed with cursor == null", ex);
        }
        return rows;
    }


    public Cursor getSent()
    {
        Cursor cDatabaseCursor = null;

        String query = "SELECT " + "fkey as fkey," + "CASE WHEN totalSent.track_id ISNULL THEN 'true' ELSE 'false' END as updateSent " +

                "FROM meta " + "LEFT JOIN (SELECT COUNT(1) as cnt, track_id FROM coverage WHERE sent='false' GROUP BY track_id)  as totalSent  on totalSent.track_id = fkey " + "WHERE meta.sent='false' AND meta.deleted='false' AND updateSent='true'";


        try
        {
            mDatabase       = this.getWritableDatabase();
            cDatabaseCursor = mDatabase.rawQuery(query, null);
        } catch (Exception ex)
        {
            HashMap<String, String> extras = new HashMap<>();
            extras.put("query", query);
            com.zafaco.moduleCommon.Log.warning(TAG, "getSent: failed", extras, ex);
        }

        return cDatabaseCursor;
    }

    public void close()
    {
        mDatabase.close();
    }
}
