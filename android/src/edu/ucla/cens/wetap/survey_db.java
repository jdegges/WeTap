package edu.ucla.cens.wetap;

import java.util.ArrayList;


import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class survey_db {
	public static final String KEY_Q_TASTE = "q_taste";
    public static final String KEY_Q_VISIBILITY = "q_visibility";
    public static final String KEY_Q_OPERABLE = "q_operable";
    public static final String KEY_Q_FLOW = "q_flow";
    public static final String KEY_Q_STYLE = "q_style";
	public static final String KEY_LONGITUDE = "longitude";
	public static final String KEY_LATITUDE = "latitude";
	public static final String KEY_TIME = "time";
	public static final String KEY_PHOTO_FILENAME = "photo_filename";
    public static final String KEY_VERSION = "version";
	public static final String KEY_ROWID = "_id";
	private static boolean databaseOpen = false;
	private static Object dbLock = new Object();
	public static final String TAG = "survey_db";
	private DatabaseHelper dbHelper;
	private SQLiteDatabase db;
	
	private Context mCtx = null;
	
	private static final String DATABASE_NAME = "survey_db";
	private static final String DATABASE_TABLE = "survey_table";
	private static final int DATABASE_VERSION = 1;
	
	private static final String DATABASE_CREATE = "create table survey_table (_id integer primary key autoincrement, "
        + "q_taste text not null,"
        + "q_visibility text not null,"
        + "q_operable text not null,"
        + "q_flow text not null,"
        + "q_style text not null,"
		+ "longitude text not null,"
		+ "latitude text not null,"
		+ "time text not null,"
        + "version text not null,"
		+ "photo_filename text not null"
		+ ");";
	
    public class survey_db_row extends Object {
    	public long row_id;
        public String q_taste;
        public String q_visibility;
        public String q_operable;
        public String q_flow;
        public String q_style;
    	public String longitude;
    	public String latitude;
    	public String time;
        public String version;
    	public String photo_filename;
    }
	
	private static class DatabaseHelper extends SQLiteOpenHelper
	{
		DatabaseHelper(Context ctx)
		{
			super(ctx, DATABASE_NAME, null, DATABASE_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL(DATABASE_CREATE);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			db.execSQL("DROP TABLE IF EXISTS " + DATABASE_TABLE);
			onCreate(db);
		}
	}
	
	public survey_db(Context ctx)
	{
		mCtx = ctx;
	}
	
	public survey_db open() throws SQLException
    {
		synchronized(dbLock)
		{
			while (databaseOpen)
			{
				try
				{
					dbLock.wait();
				}
				catch (InterruptedException e){}
			}
			databaseOpen = true;
			dbHelper = new DatabaseHelper(mCtx);
			db = dbHelper.getWritableDatabase();

			return this;
		}
	}
	
	public void close()
	{
		synchronized(dbLock)
		{
			dbHelper.close();
			databaseOpen = false;
			dbLock.notify();
		}
	}

	public long createEntry(String q_taste, String q_visibility,
                            String q_operable, String q_flow, String q_style,
                            String longitude, String latitude, String time,
                            String version, String photo_filename)
	{
		ContentValues vals = new ContentValues();
        vals.put(KEY_Q_TASTE, q_taste);
        vals.put(KEY_Q_VISIBILITY, q_visibility);
        vals.put(KEY_Q_OPERABLE, q_operable);
        vals.put(KEY_Q_FLOW, q_flow);
        vals.put(KEY_Q_STYLE, q_style);
		vals.put(KEY_LONGITUDE, longitude);
		vals.put(KEY_LATITUDE, latitude);
		vals.put(KEY_TIME, time);
        vals.put(KEY_VERSION, version);
		vals.put(KEY_PHOTO_FILENAME, photo_filename);
		
		long rowid = db.insert(DATABASE_TABLE, null, vals);
		return rowid;
	}
	
	public boolean deleteEntry(long rowId)
	{
		int count = 0;
		count = db.delete(DATABASE_TABLE, KEY_ROWID+"="+rowId, null);
		
        if(count > 0) {
            return true;
        }
        return false;
	}

    public void refresh_db()
    {
        db.execSQL("DROP TABLE IF EXISTS " + DATABASE_TABLE);
        db.execSQL(DATABASE_CREATE);
    }

	public ArrayList <survey_db_row>  fetchAllEntries()
    {
		ArrayList<survey_db_row> ret = new ArrayList<survey_db_row>();
		
		try
		{
			Cursor c = db.query(DATABASE_TABLE, new String[] {KEY_ROWID,
                KEY_Q_TASTE, KEY_Q_VISIBILITY, KEY_Q_OPERABLE, KEY_Q_FLOW,
                KEY_Q_STYLE, KEY_LONGITUDE, KEY_LATITUDE, KEY_TIME,
                KEY_VERSION, KEY_PHOTO_FILENAME}, null, null, null, null,
                null);
			int numRows = c.getCount();
			
			c.moveToFirst();
			
			for (int i =0; i < numRows; ++i)
			{
				survey_db_row sr = new survey_db_row();

				sr.row_id = c.getLong(0);
                sr.q_taste = c.getString(1);
                sr.q_visibility = c.getString(2);
                sr.q_operable = c.getString(3);
                sr.q_flow = c.getString(4);
                sr.q_style = c.getString(5);
                sr.longitude = c.getString(6);
                sr.latitude = c.getString(7);
                sr.time = c.getString(8);
                sr.version = c.getString(9);
                sr.photo_filename = c.getString(10);
				ret.add(sr);

				c.moveToNext();
			}
			c.close();
		}
		catch (Exception e){
			Log.e(TAG, e.getMessage());
		}
		return ret;
	}

    public ArrayList <survey_db_row>  fetch_all_completed_entries()
    {
        ArrayList<survey_db_row> ret = new ArrayList<survey_db_row>();

        try
        {
            String[] columns = new String[] {KEY_ROWID,
                KEY_Q_TASTE, KEY_Q_VISIBILITY, KEY_Q_OPERABLE, KEY_Q_FLOW,
                KEY_Q_STYLE, KEY_LONGITUDE, KEY_LATITUDE, KEY_TIME,
                KEY_VERSION, KEY_PHOTO_FILENAME};
            String selection = KEY_LONGITUDE + "<>\"\"" + " AND " +
                               KEY_LATITUDE + "<>\"\"";

            Cursor c = db.query(DATABASE_TABLE, columns, selection, null, null,
                                null, null);
            int numRows = c.getCount();

            c.moveToFirst();

            for (int i =0; i < numRows; ++i)
            {
                survey_db_row sr = new survey_db_row();

                sr.row_id = c.getLong(0);
                sr.q_taste = c.getString(1);
                sr.q_visibility = c.getString(2);
                sr.q_operable = c.getString(3);
                sr.q_flow = c.getString(4);
                sr.q_style = c.getString(5);
                sr.longitude = c.getString(6);
                sr.latitude = c.getString(7);
                sr.time = c.getString(8);
                sr.version = c.getString(9);
                sr.photo_filename = c.getString(10);
                ret.add(sr);

                c.moveToNext();
            }
            c.close();
        }
        catch (Exception e){
            Log.e(TAG, e.getMessage());
        }
        return ret;
    }

	public survey_db_row fetchEntry(long rowId) throws SQLException
	{
        Cursor c = db.query(DATABASE_TABLE, new String[] {KEY_ROWID,
            KEY_Q_TASTE, KEY_Q_VISIBILITY, KEY_Q_OPERABLE, KEY_Q_FLOW,
            KEY_Q_STYLE, KEY_LONGITUDE, KEY_LATITUDE, KEY_TIME, KEY_VERSION,
            KEY_PHOTO_FILENAME}, KEY_ROWID+"="+rowId, null, null, null, null);
		survey_db_row sr = new survey_db_row();

		if (c != null) {
			c.moveToFirst();

            sr.row_id = c.getLong(0);
            sr.q_taste = c.getString(1);
            sr.q_visibility = c.getString(2);
            sr.q_operable = c.getString(3);
            sr.q_flow = c.getString(4);
            sr.q_style = c.getString(5);
            sr.longitude = c.getString(6);
            sr.latitude = c.getString(7);
            sr.time = c.getString(8);
            sr.version = c.getString(9);
            sr.photo_filename = c.getString(10);
		}
		else
		{
            sr.row_id = -1;
            sr.q_taste = sr.q_visibility = sr.q_operable = sr.q_flow =
            sr.q_style = sr.longitude = sr.latitude = sr.time =
            sr.photo_filename = null;
		}
		c.close();
		return sr;
	}

    public int update_gpsless_entries (String lon, String lat) {
        ContentValues values = new ContentValues();
        values.put (KEY_LONGITUDE, lon);
        values.put (KEY_LATITUDE, lat);

        String where_clause = KEY_LONGITUDE + "=\"\"" + " AND " + KEY_LATITUDE + "=\"\"";

        int ret = db.update (DATABASE_TABLE, values, where_clause, null);
        return ret;
    }
}
