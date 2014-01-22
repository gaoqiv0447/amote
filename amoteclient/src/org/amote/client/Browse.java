/**
 * Copyright (C) 2009 Aisino Corporation Inc.
 *
 * No.18A, Xingshikou street, Haidian District,Beijing
 * All rights reserved.
 * 
 * This software is the confidential and proprietary information of 
 * Aisino Corporation Inc. ("Confidential Information"). You shall not
 * disclose such Confidential Information and shall use it only in 
 * accordance with the terms of the license agreement you entered into
 * with Aisino.
 */

package org.amote.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.gmote.common.FileInfo;
import org.gmote.common.FileInfo.FileSource;
import org.gmote.common.FileInfo.FileType;
import org.gmote.common.Protocol.Command;
import org.gmote.common.packet.AbstractPacket;
import org.gmote.common.packet.ListReplyPacket;
import org.gmote.common.packet.ListReqPacket;
import org.gmote.common.packet.SimplePacket;
import org.gmote.common.packet.PluginPacket;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ImageButton;

/**
 * The Browse activity. Displays a list of files in the current directory and
 * handles onclick events to launch files.
 */
public class Browse extends ListActivity implements BaseActivity,OnClickListener
		 {
	static final String DEBUG_TAG = "Amote_Browse";
	static final String BROWSE_USER_GUIDE = "browse_user_guide";
	static final int DIALOG_BROWSE_GUIDE = 0;
	static final int DIALOG_BREOSE_INSTALL_RESULT_S = 1; // install success
	static final int DIALOG_BREOSE_INSTALL_RESULT_F = 2;

	AbstractPacket reply;
	FileInfo fileInfo = null;
	ActivityUtil mUtil = null;
	private static boolean inGmoteStreamMode = false;
	private static boolean lastReqWasBaseList = false;
	private static String filePath = "";
	private boolean isUserGuide = true;
	private SharedPreferences prefs;
	private SharedPreferences.Editor editor;

	private ImageButton mHomeButton = null;
	private ImageButton mBackButton = null;
	private static int mDirCount = 1;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		mUtil = new ActivityUtil();
		mUtil.onCreate(icicle, this);

//		// init sharedpreferences
		// prefs = getSharedPreferences(GmoteClient.PREFS,MODE_WORLD_READABLE);
		// editor = getSharedPreferences(GmoteClient.PREFS, MODE_WORLD_WRITEABLE).edit();
		
		// isUserGuide = prefs.getBoolean(BROWSE_USER_GUIDE,true);
		// if(isUserGuide) {
			// showDialog(DIALOG_BROWSE_GUIDE);
//		}else {
//			LoadFileList();
//		}
		LoadFileList();
	}
	
	private void LoadFileList() {
		Intent intent = getIntent();
		fileInfo = (FileInfo) intent
				.getSerializableExtra(getString(R.string.current_path));
		Log.d(DEBUG_TAG, "Gmote Stream Mode = " + inGmoteStreamMode);

		if (fileInfo == null) {
			mUtil.send(new SimplePacket(Command.BASE_LIST_REQ));
			filePath = "";
			lastReqWasBaseList = true;
			Log.d(DEBUG_TAG, "===file info == null");
		} else {
			mUtil.send(new ListReqPacket(fileInfo));
			filePath = fileInfo.getAbsolutePath();
			lastReqWasBaseList = false;
		}

		mUtil.showProgressDialog(getString(R.string.fetching_list_of_files));
		Log.d(DEBUG_TAG, "Browse onCreate");
	}

	private void writeTitle() {
		String title = "";
		if (filePath.length() == 0) {
			title += "Browse";
		} else {
			title += filePath;
		}

		TextView txtTitle = (TextView) findViewById(R.id.file_list_title);
		txtTitle.setText(title);
	}

	@Override
	public void onStart() {
		super.onStart();
		mUtil.onStart(this);
	}

	@Override
	public void onStop() {
		super.onStop();
		mUtil.onStop();
	}

	@Override
	public void onResume() {
		super.onResume();
		mUtil.onResume();
	}

	@Override
	public void onPause() {
		super.onPause();
		mUtil.onPause();
	}

	@Override
	protected Dialog onCreateDialog(final int id) {
		if (id == ActivityUtil.DIALOG_BROWSE_VIEW_SETTINGS) {
			final String menuOptions[] = { "Show playable files only",
					"Show all files" };
			return new AlertDialog.Builder(Browse.this)
					.setTitle("View Settings")
					.setItems(menuOptions,
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int which) {
									Command command;
									if (which == 0) {
										command = Command.SHOW_PLAYABLE_FILES_ONLY_REQ;
									} else {
										command = Command.SHOW_ALL_FILES_REQ;
									}
									mUtil.send(new SimplePacket(command));

									Intent intent = new Intent(Browse.this,
											Browse.class);
									intent.putExtra(
											getString(R.string.current_path),
											fileInfo);
									startActivity(intent);
								}
							}).create();

		}else if (id == DIALOG_BROWSE_GUIDE) {
			return new AlertDialog.Builder(Browse.this)
					.setTitle(R.string.user_guide_tilte)
					.setMessage(R.string.user_guide_msg)
					.setPositiveButton(R.string.user_guide_ok, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int whichButton) {
							BrowsePluginInstall();
						}
					})
					.setNegativeButton(R.string.user_guide_cancel, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int whichButton) {
							BrowsePluginInstallCancel();
						}
					})
					.create();
		}else if(id == DIALOG_BREOSE_INSTALL_RESULT_F) {
			return new AlertDialog.Builder(Browse.this)
					.setTitle(R.string.user_guide_tilte)
					.setMessage(R.string.plugin_install_error_msg)
					.setPositiveButton(R.string.plugin_reinstall, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int whichButton) {
							BrowsePluginInstall();
						}
					})
					.setNegativeButton(R.string.user_guide_cancel, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int whichButton) {
							BrowsePluginInstallCancel();
						}
					})
					.create();
		}else if(id == DIALOG_BREOSE_INSTALL_RESULT_S) {
			return new AlertDialog.Builder(Browse.this)
					.setTitle(R.string.user_guide_tilte)
					.setMessage(R.string.plugin_install_sucess_msg)
					.setPositiveButton(R.string.plugin_ok, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int whichButton) {
							LoadFileList();
						}
					})
					.create();
		} else {
			return mUtil.onCreateDialog(id);
		}
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		FileInfo file = ((ListReplyPacket) reply).getFiles()[position];
		if (file.isDirectory()) {
			++mDirCount;
			Intent intent = new Intent(this, Browse.class);
			intent.putExtra(getString(R.string.current_path), file);
			startActivity(intent);
		} else if (file.isControllable()) {

			// modifyed by zhangdawei,when click one file,goto Touchpad activity
			Intent intent = new Intent(this, Touchpad.class);
			// end
			intent.putExtra(getString(R.string.file_type), file);
			intent.putExtra(getString(R.string.gmote_stream_mode),
					inGmoteStreamMode);
			startActivity(intent);
		} else {
			Toast.makeText(Browse.this, getString(R.string.unknow_file),
					Toast.LENGTH_SHORT).show();
		}
	}


	public void handleReceivedPacket(AbstractPacket tempReply) {
		if (tempReply.getCommand() == Command.PLAY_DVD) {
			Log.d(DEBUG_TAG, "Browse# play dvd ");

			Intent intent = new Intent(this, Touchpad.class);
			startActivity(intent);
			finish();
		} else if (tempReply.getCommand() == Command.LIST_REPLY) {
			Log.d(DEBUG_TAG, "Browse# got list ");
			reply = tempReply;
			displayFiles();
			setContentView(R.layout.file_list);
			Log.d(DEBUG_TAG, "Browse# setup list ");
			getListView().setTextFilterEnabled(true);
			getListView().requestFocus();

			mHomeButton = (ImageButton) findViewById(R.id.new_browse_home);
			mHomeButton.setOnClickListener(this);
			mBackButton = (ImageButton) findViewById(R.id.new_browse_back);
			mBackButton.setOnClickListener(this);

			writeTitle();
		} else if (tempReply.getCommand() == Command.MEDIA_INFO) {
			// Simply ignore this packet.
			return;
		} else if(tempReply.getCommand() == Command.BROWSE_PLUGIN_INSTALL_RESULT) {
			PluginPacket pluginpacket = (PluginPacket) tempReply;
			boolean re = pluginpacket.getPluginResult();
			if(re) {
				showDialog(DIALOG_BREOSE_INSTALL_RESULT_S);
			}else {
				showDialog(DIALOG_BREOSE_INSTALL_RESULT_F);
			}
		} else {
			Log.e(DEBUG_TAG,
					"Unexpected packet in browse: " + tempReply.getCommand());
			return;
		}
		reply = tempReply;

	}

	class FileAdapterView extends LinearLayout {
		public FileAdapterView(Context context, String fileName,
				ImageView imageIcon, LinearLayout.LayoutParams imageParams) {
			super(context);
			imageParams = new LinearLayout.LayoutParams(48,
					LayoutParams.WRAP_CONTENT);

			this.setOrientation(HORIZONTAL);
			this.setHorizontalGravity(Gravity.FILL_HORIZONTAL);
			this.setGravity(Gravity.CENTER_VERTICAL);

			addView(imageIcon, imageParams);

			// Add the file name
			LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(
					LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
			nameParams.setMargins(5, 1, 1, 1);

			TextView nameControl = new TextView(context);
			nameControl.setText(fileName);
			nameControl.setTextSize(14f);
			nameControl.setTextColor(Color.WHITE);
			addView(nameControl, nameParams);
		}
	}

	class FileAdapter extends BaseAdapter implements Filterable {
		private List<FileInfo> originalFiles;
		private List<FileInfo> filteredFiles;
		private LayoutInflater mInflater;
		private Map<FileType, Bitmap> imageCache = null;
		private Bitmap folderImage;
		private Bitmap unknownFileImage;

		public FileAdapter(Context context, List<FileInfo> files) {
			this.originalFiles = files;
			this.filteredFiles = files;

			mInflater = LayoutInflater.from(context);
			if (imageCache == null) {

				imageCache = new HashMap<FileType, Bitmap>();
				imageCache.put(FileType.MUSIC, BitmapFactory.decodeResource(
						context.getResources(), R.drawable.audio));
				imageCache.put(FileType.VIDEO, BitmapFactory.decodeResource(
						context.getResources(), R.drawable.video));
				imageCache
						.put(FileType.DVD_DRIVE,
								BitmapFactory.decodeResource(
										context.getResources(), R.drawable.dvd));
				imageCache.put(FileType.PLAYLIST, BitmapFactory.decodeResource(
						context.getResources(), R.drawable.audio));
				imageCache.put(FileType.IMAGE, BitmapFactory.decodeResource(
						context.getResources(), R.drawable.image_viewer));

				folderImage = BitmapFactory.decodeResource(
						context.getResources(), R.drawable.folder);
				unknownFileImage = BitmapFactory.decodeResource(
						context.getResources(), R.drawable.file);
			}
		}

		public int getCount() {
			return filteredFiles.size();
		}

		public Object getItem(int position) {
			return filteredFiles.get(position);
		}

		public long getItemId(int position) {
			return position;
		}

		public View getView(int position, View convertView, ViewGroup parent) {
			FileInfo file = filteredFiles.get(position);

			Bitmap selectedImage = null;

			if (file.getFileType() != null) {
				if (imageCache.containsKey(file.getFileType())) {
					selectedImage = imageCache.get(file.getFileType());
				} else {
					selectedImage = unknownFileImage;
				}
			} else if (file.isDirectory()) {
				selectedImage = folderImage;
			} else {
				selectedImage = unknownFileImage;
			}

			// A ViewHolder keeps references to children views to avoid
			// unneccessary
			// calls
			// to findViewById() on each row.
			ViewHolder holder;

			// When convertView is not null, we can reuse it directly, there is
			// no
			// need
			// to reinflate it. We only inflate a new View when the convertView
			// supplied
			// by ListView is null.
			if (convertView == null) {
				convertView = mInflater.inflate(R.layout.file_list_item, null);

				// Creates a ViewHolder and store references to the two children
				// views
				// we want to bind data to.
				holder = new ViewHolder();
				holder.text = (TextView) convertView.findViewById(R.id.text);
				holder.icon = (ImageView) convertView.findViewById(R.id.icon);

				convertView.setTag(holder);
			} else {
				// Get the ViewHolder back to get fast access to the TextView
				// and the ImageView.
				holder = (ViewHolder) convertView.getTag();
			}

			// Bind the data efficiently with the holder.
			holder.text.setText(file.getFileName());
			holder.icon.setImageBitmap(selectedImage);

			return convertView;
		}

		@Override
		public Filter getFilter() {
			return new Filter() {

				@Override
				protected FilterResults performFiltering(CharSequence constraint) {
					Log.d(DEBUG_TAG, "Perform Filter: " + constraint);
					FilterResults filterResults = new FilterResults();
					List<FileInfo> results = new ArrayList<FileInfo>();
					String constraintLower = constraint.toString()
							.toLowerCase();
					for (FileInfo fileInfo : originalFiles) {
						if (fileInfo.getFileName().toLowerCase()
								.startsWith(constraintLower)) {
							results.add(fileInfo);
						}
					}
					filterResults.values = results;
					filterResults.count = results.size();
					return filterResults;
				}

				// FilterResults is not using generics.
				@SuppressWarnings("unchecked")
				@Override
				protected void publishResults(CharSequence constraint,
						FilterResults results) {

					filteredFiles = (List<FileInfo>) results.values;
					if (results.count > 0) {
						notifyDataSetChanged();
					} else {
						notifyDataSetInvalidated();
					}
				}
			};
		}
	}

	static class ViewHolder {
		TextView text;
		ImageView icon;
	}

	@Override
	public void onClick(View v) {
		int id = v.getId();
		if(id == R.id.new_browse_home) {
			finish();
		}else if(id == R.id.new_browse_back) {
			if(mDirCount > 1) {
				mDirCount--;
				finish();
			}
		}
	}


	private void displayFiles() {

		if (reply != null) {
			List<FileInfo> filesToDisplay;
			if (inGmoteStreamMode) {
				FileInfo[] allFiles = ((ListReplyPacket) reply).getFiles();

				// We can only stream songs that are on the file system.
				filesToDisplay = new ArrayList<FileInfo>();
				for (FileInfo file : allFiles) {
					if (file.getFileSource() == FileSource.FILE_SYSTEM) {
						filesToDisplay.add(file);
					}
				}
			} else {
				filesToDisplay = Arrays.asList(((ListReplyPacket) reply)
						.getFiles());
			}
			FileAdapter fileAdapter = new FileAdapter(Browse.this,
					filesToDisplay);
			setListAdapter(fileAdapter);
		}
	}
	
	private void BrowsePluginInstall() {
		//send socket
		mUtil.send(new SimplePacket(Command.BROWSE_PLUGIN_INSTALL));
		mUtil.showProgressDialog(getString(R.string.plugin_install_progress));
		isUserGuide = false;
		// save
		editor.putBoolean(BROWSE_USER_GUIDE, isUserGuide);
		editor.commit();
	}
	
	private void BrowsePluginInstallCancel() {
		isUserGuide = true;
		editor.putBoolean(BROWSE_USER_GUIDE, isUserGuide);
		editor.commit();
		finish();
	}
}
