package com.levelzeros.utippy.utility;

import android.app.AlertDialog;
import android.app.DownloadManager;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.widget.Toast;

import com.levelzeros.utippy.R;

import java.io.File;
import java.net.HttpURLConnection;
import java.net.URL;


/**
 * Created by Poon on 15/2/2017.
 */

/**
 * Utility to download files
 */
public class DownloadHelper extends AsyncTask<URL, Void, Integer> {
    private static final String TAG = "DownloadHelper";

    //Code to identify different outcomes
    private static final int DOWNLOAD_FILE = 100;
    private static final int FILE_EXISTED = 101;
    private static final int CONNECTION_TIMEOUT = 102;

    //Variables
    private final Context mContext;
    private final String mCourseName;
    private File mDownloadFile;
    private URL mFileUrl;
    private String mFileName;
    private static ProgressDialog mProgressDialog;
    private static Toast mToast;

    //Callback to handle retry request
    private OnTaskExecuted mCallback;

    //Interface to handle retry request
    public interface OnTaskExecuted {
        void onRetry(boolean status, String fileUrl);
    }

    public DownloadHelper(Context mContext, String mCourseName, OnTaskExecuted mCallback) {
        this.mContext = mContext;
        this.mCourseName = mCourseName;
        this.mCallback = mCallback;
    }

    /**
     * Setting up download task, check existing file and connection
     */
    @Override
    protected void onPreExecute() {
        mProgressDialog = new ProgressDialog(mContext);
        mProgressDialog.setCancelable(false);
        mProgressDialog.setMessage(mContext.getString(R.string.check_file_progress));
        mProgressDialog.show();
    }

    @Override
    protected Integer doInBackground(URL... params) {
        String str;
        int response = 0;
        if (params.length <= 0) {
            return null;
        }
        mFileUrl = params[0];

        try {
            HttpURLConnection connection = (HttpURLConnection) mFileUrl.openConnection();
            connection.setReadTimeout(10000);
            connection.setConnectTimeout(25000);
            connection.setRequestMethod("HEAD");
            connection.connect();

            int responseCode = connection.getResponseCode();

            switch (responseCode) {
                case HttpURLConnection.HTTP_OK:
                    File rootFileDir;
                    File fileDir;

                    str = connection.getURL().toString();
                    mFileName = str.substring(str.lastIndexOf("/") + 1)
                            .split("\\?")[0]
                            .replace("%20", " ")
                            .replace("%21", "!")
                            .replace("%22", "\"")
                            .replace("%23", "#")
                            .replace("%24", "$")
                            .replace("%25", "%")
                            .replace("%26", "&")
                            .replace("%27", "'")
                            .replace("%28", "(")
                            .replace("%29", ")");

                    rootFileDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), mContext.getString(R.string.app_name));
                    rootFileDir.mkdirs();

                    fileDir = new File(rootFileDir, mCourseName);
                    fileDir.mkdirs();

                    mDownloadFile = new File(fileDir, mFileName);
                    if (!mDownloadFile.exists()) {
                        response = DOWNLOAD_FILE;
                    } else {
                        response = FILE_EXISTED;
                    }
            }

        } catch (Exception e) {
            e.printStackTrace();
            response = CONNECTION_TIMEOUT;
        }
        return response;
    }

    @Override
    protected void onPostExecute(Integer response) {
        mProgressDialog.dismiss();
        switch (response) {
            case DOWNLOAD_FILE:
                DownloadFile();
                return;

            case FILE_EXISTED:
                AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
                builder.setTitle(mContext.getString(R.string.title_file_existed))
                        .setMessage(mContext.getString(R.string.message_file_existed))
                        .setPositiveButton(mContext.getString(R.string.option_yes), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                mDownloadFile.delete();
                                DownloadFile();
                            }
                        })
                        .setNegativeButton(mContext.getString(R.string.option_no), null);
                AlertDialog dialog = builder.create();
                dialog.setCanceledOnTouchOutside(true);
                dialog.show();
                return;

            case CONNECTION_TIMEOUT:
                mCallback.onRetry(false, mFileUrl.toString());
                return;

            default:
                mCallback.onRetry(false, mFileUrl.toString());
        }
    }

    /**
     * Method to download files
     * @return
     */
    private boolean DownloadFile() {
        //Check if user disabled Download Manager
        int state = mContext.getPackageManager().getApplicationEnabledSetting("com.android.providers.downloads");
        if (state == PackageManager.COMPONENT_ENABLED_STATE_DISABLED ||
                state == PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER ||
                state == PackageManager.COMPONENT_ENABLED_STATE_DISABLED_UNTIL_USED) {

            // Prompt user to enable Android Download Manager
            AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
            builder.setMessage(mContext.getString(R.string.title_request_download_manager))
                    .setTitle(mContext.getString(R.string.message_request_download_manager))
                    .setPositiveButton(mContext.getString(R.string.option_enable_download_manager), new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            try {
                                //Open the specific App Info page:
                                Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                intent.setData(Uri.parse("package:com.android.providers.downloads"));
                                mContext.startActivity(intent);
                            } catch (ActivityNotFoundException e) {
                                //Open the generic Apps page:
                                Intent intent = new Intent(android.provider.Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS);
                                mContext.startActivity(intent);
                            }
                        }
                    })
                    .setNegativeButton(mContext.getString(R.string.option_cancel), null);
            AlertDialog dialog = builder.create();
            dialog.setCanceledOnTouchOutside(true);
            dialog.show();

            return false;
        } else {
            mToast = Toast.makeText(mContext, mContext.getString(R.string.toast_downloading), Toast.LENGTH_LONG);
            mToast.show();

            DownloadManager manager = (DownloadManager) mContext.getSystemService(Context.DOWNLOAD_SERVICE);
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(mFileUrl.toString()));

            request.setTitle(mFileName);
            request.setDescription(mContext.getString(R.string.message_downloading));
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationUri(Uri.fromFile(mDownloadFile));
            manager.enqueue(request);

            return true;
        }
    }
}

