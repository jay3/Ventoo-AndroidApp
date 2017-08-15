package com.njackson.utils.gpx;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import fr.jayps.android.AdvancedLocation;

import static android.support.v4.content.FileProvider.getUriForFile;

public class GpxExport {

    private static final String TAG = "PB-GpxExport";

    public static void export(Context context, boolean extended_gpx, String email_to) {
        Toast.makeText(context, "Please wait while generating the file", Toast.LENGTH_LONG).show();
        final Context _context = context;
        final boolean _extended_gpx = extended_gpx;
        final String _email_to = email_to;
        new Thread(new Runnable() {
            public void run() {
                AdvancedLocation advancedLocation = new AdvancedLocation(_context);
                String gpx = advancedLocation.getGPX(_extended_gpx);

                try {
                    File newFile = new File(_context.getCacheDir(), "track.gpx");
                    FileWriter fileWriter = new FileWriter(newFile);
                    fileWriter.write(gpx);
                    fileWriter.close();
                    Uri contentUri = getUriForFile(_context, "com.njackson.fileprovider", newFile);

                    //Log.d(TAG, contentUri.toString());

                    final Intent sendIntent = new Intent();
                    sendIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    sendIntent.setAction(Intent.ACTION_SEND);
                    sendIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
                    sendIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    sendIntent.setType("application/gpx+xml");
                    //sendIntent.putExtra(Intent.EXTRA_SUBJECT, "[Subject]");
                    if (_email_to != "") {
                        sendIntent.putExtra(Intent.EXTRA_EMAIL, new String[] {_email_to});
                    }
                    sendIntent.putExtra(Intent.EXTRA_TEXT, "GPS track generated by JayPS, http://www.pebblebike.com");

                    _context.startActivity(sendIntent);
                } catch (IOException e) {
                    Log.d(TAG, "Error while creating file");
                }
            }
        }).start();
    }
}
