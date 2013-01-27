/**
 * 
 */
package com.chanapps.four.service;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.chanapps.four.data.ChanBoard;
import com.chanapps.four.data.ChanFileStorage;
import com.chanapps.four.data.ChanHelper;
import com.chanapps.four.data.FileDesc;

/**
 * @author "Grzegorz Nittner" <grzegorz.nittner@gmail.com>
 *
 */
public class CleanUpService extends BaseChanService {

    protected static final String TAG = CleanUpService.class.getSimpleName();
    private static final boolean DEBUG = false;
    
    public static void startService(Context context) {
        if (DEBUG) Log.i(TAG, "Start clean up service");
        Intent intent = new Intent(context, CleanUpService.class);
        intent.putExtra(ChanHelper.PRIORITY_MESSAGE, 1);
        context.startService(intent);
    }

    public CleanUpService() {
   		super("cleanup");
   	}

    protected CleanUpService(String name) {
   		super(name);
   	}
	
	@Override
	protected void onHandleIntent(Intent intent) {
        long startTime = Calendar.getInstance().getTimeInMillis();
		try {
            Context context = getBaseContext();
            File cacheFolder = ChanFileStorage.getCacheDirectory(context);
            List<FileDesc> otherFiles = new ArrayList<FileDesc>();
            Map<String, Long> sizeByBoard = new HashMap<String, Long>();
            Map<String, List<FileDesc>> filesByBoard = new HashMap<String, List<FileDesc>>();
            
            long totalSize = 0;
            int totalFiles = 0;
            long otherSize = 0;
            File[] children = cacheFolder.listFiles();
    	    if (children != null) {
    	        for (File child : children) {
    	            if (child.isDirectory()) {
    	            	if (ChanBoard.getBoardByCode(getBaseContext(), child.getName()) != null) {
    	            		List<FileDesc> boardData = new ArrayList<FileDesc>();
    	            		long boardSize = addFiles(child, boardData);
    	            		
    	            		sizeByBoard.put(child.getName(), boardSize);
    	            		filesByBoard.put(child.getName(), boardData);
    	            		totalSize += boardSize;
    	            		totalFiles += boardData.size();
    	            	} else {
    	            		long folderSize = addFiles(child, otherFiles);
    	            		otherSize += folderSize;
    	            		totalSize += folderSize;
    	            	}
    	            } else {
    	            	FileDesc desc = new FileDesc(child);
    	            	totalSize += desc.size;
    	            	otherSize += desc.size;
    		            otherFiles.add(desc);
    	            }
    	        }
    	    }
    	    totalFiles += otherFiles.size();
            
            long endTime = Calendar.getInstance().getTimeInMillis();
            if (DEBUG) { 
            	Log.i(TAG, "Cache folder contains " + totalFiles + " files of size " + (totalSize/1024)
            		+ " kB. Calculated in " + (endTime - startTime) + "ms.");
            	for (Map.Entry<String, Long> entry : sizeByBoard.entrySet()) {
            		if (filesByBoard.get(entry.getKey()).size() == 0) {
            			continue;
            		}
            		Log.i(TAG, "Board " + entry.getKey() + " size " + (entry.getValue() / 1024) + "kB "
            				+ filesByBoard.get(entry.getKey()).size() + " files.");
            	}
            	Log.i(TAG, "Other files' size " + (otherSize / 1024) + "kB "
        				+ otherFiles.size() + " files.");
            }
            toastUI("" + totalFiles + " files of size " + (totalSize/1024)
            		+ " kB. Calculated in " + ((endTime - startTime)/1000) + "s.");
            
            /*
            // sorting by last modification date desc order
            Collections.sort(otherFiles, new Comparator<FileDesc>() {
                public int compare(FileDesc o1, FileDesc o2) {
                    return o1.lastModified > o2.lastModified ? 1
                    		: o1.lastModified < o2.lastModified ? -1 : 0;
                }
            });
            int i = 0;
            long prevDate = 0;
            for (FileDesc file : otherFiles) {
            	if (prevDate == file.lastModified) {
            		continue;
            	}
            	prevDate = file.lastModified;
            	if (++i > 10) {
            		break;
            	}
            	Log.i(TAG, "" + i + " " + file);
            }
            */
		} catch (Exception e) { 
            Log.e(TAG, "Error in clean up service", e);
		}
	}
	
	private long addFiles(File file, List<FileDesc> all) {
		long totalSize = 0;
		Log.i(TAG, "Checking folder " + file.getAbsolutePath());
	    File[] children = file.listFiles();
	    if (children != null) {
	        for (File child : children) {
	            if (child.isDirectory()) {
	            	totalSize += addFiles(child, all);
	            } else {
	            	FileDesc desc = new FileDesc(child);
	            	totalSize += desc.size;
		            all.add(desc);
	            }
	        }
	    }
	    return totalSize;
	}
}
