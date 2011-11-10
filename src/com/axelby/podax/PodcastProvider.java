package com.axelby.podax;

import java.util.HashMap;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;

public class PodcastProvider extends ContentProvider {
	public static String AUTHORITY = "com.axelby.podax.PodcastProvider";
	public static String BASE_PATH = "podcasts";
	public static Uri URI = Uri.parse("content://" + PodcastProvider.AUTHORITY
			+ "/" + PodcastProvider.BASE_PATH);
	public static final String ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE
	        + "/vnd.axelby.podcast";
	public static final String DIR_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE
	        + "/vnd.axelby.podcast";

	final static int PODCASTS = 1;
	final static int PODCASTS_QUEUE = 2;
	final static int PODCAST_ID = 3;
	
	public static final String COLUMN_ID = "_id";
	public static final String COLUMN_TITLE = "title";
	public static final String COLUMN_SUBSCRIPTION_TITLE = "subscriptionTitle";
	public static final String COLUMN_QUEUE_POSITION = "queuePosition";
	public static final String COLUMN_MEDIA_URL = "mediaUrl";
	public static final String COLUMN_LINK = "link";
	public static final String COLUMN_PUB_DATE = "pubDate";
	public static final String COLUMN_DESCRIPTION = "description";
	public static final String COLUMN_FILE_SIZE = "fileSize";
	public static final String COLUMN_LAST_POSITION = "lastPosition";
	public static final String COLUMN_DURATION = "duration";

	static UriMatcher uriMatcher;
	
	static {
		uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
		uriMatcher.addURI(PodcastProvider.AUTHORITY, "podcasts", PodcastProvider.PODCASTS);
		uriMatcher.addURI(PodcastProvider.AUTHORITY, "podcasts/queue", PodcastProvider.PODCASTS_QUEUE);
		uriMatcher.addURI(PodcastProvider.AUTHORITY, "podcasts/#", PodcastProvider.PODCAST_ID);
	}
	
	DBAdapter _dbAdapter;

	@Override
	public boolean onCreate() {
		_dbAdapter = new DBAdapter(getContext());
		return true;
	}

	@Override
	public String getType(Uri uri) {
		switch (uriMatcher.match(uri)) {
		case PodcastProvider.PODCASTS:
			return PodcastProvider.DIR_TYPE;
		case PodcastProvider.PODCASTS_QUEUE:
			return PodcastProvider.DIR_TYPE;
		}
		throw new IllegalArgumentException("Unknown URI");
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
		SQLiteQueryBuilder sqlBuilder = new SQLiteQueryBuilder();
		sqlBuilder.setTables("podcasts JOIN subscriptions on podcasts.subscriptionId = subscriptions._id");
		HashMap<String, String> columnMap = new HashMap<String, String>();
		columnMap.put(COLUMN_ID, "podcasts._id AS _id");
		columnMap.put(COLUMN_TITLE, "podcasts.title AS title");
		columnMap.put(COLUMN_SUBSCRIPTION_TITLE, "subscriptions.title AS subscriptionTitle");
		columnMap.put(COLUMN_QUEUE_POSITION, "queuePosition");
		columnMap.put(COLUMN_MEDIA_URL, "mediaUrl");
		columnMap.put(COLUMN_LINK, "link");
		columnMap.put(COLUMN_PUB_DATE, "pubDate"); 
		columnMap.put(COLUMN_DESCRIPTION, "description");
		columnMap.put(COLUMN_FILE_SIZE, "fileSize");
		columnMap.put(COLUMN_LAST_POSITION, "lastPosition");
		columnMap.put(COLUMN_DURATION, "duration");

		sqlBuilder.setProjectionMap(columnMap);

		switch (uriMatcher.match(uri)) {
		case PodcastProvider.PODCASTS:
			break;
		case PodcastProvider.PODCASTS_QUEUE:
			sqlBuilder.appendWhere("queuePosition IS NOT NULL");
			if (sortOrder == null || sortOrder == "")
				sortOrder = "queuePosition";
			break;
		default:
			throw new IllegalArgumentException("Unknown URI");
		}

		Cursor c = sqlBuilder.query(
				_dbAdapter.getRawDB(), 
				projection, 
				selection, 
				selectionArgs, 
				null, 
				null, 
				sortOrder);
		c.setNotificationUri(getContext().getContentResolver(), uri);
		return c;
	}

	@Override
	public int update(Uri uri, ContentValues values, String where,
			String[] whereArgs) {
		switch (uriMatcher.match(uri)) {
		case PodcastProvider.PODCASTS:
			break;
		case PodcastProvider.PODCAST_ID:
			String podcastId = uri.getPathSegments().get(1);
			String extraWhere = PodcastProvider.COLUMN_ID + " = " + podcastId;
			if (where != null)
				where = extraWhere + " AND " + where;
			else
				where = extraWhere;
			break;
		default:
			throw new IllegalArgumentException("Unknown URI");
		}
		
		// subscription title is not in the table
		values.remove(COLUMN_SUBSCRIPTION_TITLE);

		int count = _dbAdapter.getRawDB().update("podcasts", values, where, whereArgs);
		getContext().getContentResolver().notifyChange(uri, null);
		if (values.containsKey(PodcastProvider.COLUMN_QUEUE_POSITION))
			getContext().getContentResolver().notifyChange(Uri.withAppendedPath(URI, "queue"), null);
		return count;
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int delete(Uri arg0, String arg1, String[] arg2) {
		// TODO Auto-generated method stub
		return 0;
	}
}
