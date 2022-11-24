package com.example.imad;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Base64;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;


public class CameraActivity extends AppCompatActivity {
    List<Bitmap> l = new ArrayList<Bitmap>();
    Map<String,JSONObject> result;
    RequestQueue q;
    int i;
    int j;

    AlertDialog.Builder builder;
    JSONObject fn;
    ProgressDialog progressBar;
    private int progressBarStatus = 0;
    private Handler progressBarHandler = new Handler();
    AlertDialog diag;
    ImageView Remove;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        if (ContextCompat.checkSelfPermission(CameraActivity.this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(CameraActivity.this, new String[]{
                    Manifest.permission.CAMERA
            }, 100);

        }
        result = new HashMap<String,JSONObject>();
        builder = new AlertDialog.Builder(CameraActivity.this);


        builder.setTitle("Remove Image");


        builder.setMessage("Would you like to remove clicked Image?");


        //Button One : Yes
        builder.setPositiveButton("Remove", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                LinearLayout layout = (LinearLayout) findViewById(R.id.linear);

                layout.removeView(Remove);
                Toast.makeText(CameraActivity.this, String.valueOf((int)Remove.getId()-1), Toast.LENGTH_SHORT).show();
                l.remove((int)Remove.getId()-1);
                i--;
            }
        });


        //Button Two : No
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Remove=null;
                dialog.cancel();
            }
        });
        diag = builder.create();
        fn = new JSONObject();
        q = Volley.newRequestQueue(CameraActivity.this);
        i = 0;
        j=0;
        Button btn = (Button) findViewById(R.id.add);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                startActivityForResult(intent, 100);
            }
        });
        btn.performClick();
        Button btn2 = (Button) findViewById(R.id.send);
        btn2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                RequestQueue q = Volley.newRequestQueue(CameraActivity.this);
                TextView tv = (TextView) findViewById(R.id.textView);

                for (int j=0;j<l.size();j++) {
                    processBitmap(l.get(j),j);
                }
                progressBar = new ProgressDialog(CameraActivity.this);
                progressBar.setCancelable(true);
                progressBar.setMessage("Requests Processing...");
                progressBar.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                progressBar.setProgress(0);
                progressBar.setMax(l.size());
                progressBar.show();
                progressBarStatus = i;


                new Thread(new Runnable() {
                    public void run() {
                        while (progressBarStatus < l.size()) {
                            // performing operation
                            progressBarStatus = i;
                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            // Updating the progress bar
                            progressBarHandler.post(new Runnable() {
                                public void run() {
                                    progressBar.setProgress(progressBarStatus);
                                }
                            });
                        }
                        // performing operation if file is downloaded,
                        if (progressBarStatus >= l.size()) {
                            // sleeping for 1 second after operation completed

                            //Intent i = new Intent(getApplicationContext(),CartActivity.class);
//                                startActivity(i);
                            new Handler(Looper.getMainLooper()).post(new Runnable() {
                                @Override
                                public void run() {
                                    Toast toast = Toast.makeText(CameraActivity.this, "Task Completed", Toast.LENGTH_SHORT);
                                    toast.show();
                                    displayResults();
                                }
                            });

                            // close the progress bar dialog
                            progressBar.dismiss();
                        }
                    }
                }).start();
            }
        });

    }

    public void processBitmap(Bitmap bitmap,int ind) {
        String url = "https://api.ximilar.com/detection/v2/detect/";
        Toast.makeText(CameraActivity.this, "trying api call", Toast.LENGTH_LONG).show();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        byte[] imageBytes = baos.toByteArray();
        String imageString = Base64.encodeToString(imageBytes, Base64.DEFAULT);
        JSONObject jsonParams = new JSONObject();

        try {
            //Add string params

            jsonParams.put("task", "bfc83007-4aed-4f8a-922f-197ce94f9baa");
            jsonParams.put("version", 1);
            jsonParams.put("keep_prob",0.15 );

        } catch (JSONException e) {
            e.printStackTrace();
        }
        //Create json array for filter
        JSONArray array = new JSONArray();

        //Create json objects for two filter Ids
        JSONObject jsonParam1 = new JSONObject();


        try {

            jsonParam1.put("_base64", imageString);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        //Add the filter Id object to array
        array.put(jsonParam1);


        //Add array to main json object
        try {
            jsonParams.put("records", array);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, url, jsonParams, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                i++;
                result.put(String.valueOf(ind),response);
//                tv.setText(response.toString());
                Toast.makeText(CameraActivity.this, response.toString(), Toast.LENGTH_LONG).show();
                Toast.makeText(CameraActivity.this, String.valueOf(i) + String.valueOf(l.size()), Toast.LENGTH_LONG).show();


            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(CameraActivity.this, error.toString(), Toast.LENGTH_LONG).show();
//                tv.setText(error.toString());
                j++;
                if (j<12) {
                    processBitmap(bitmap,ind);
                }
            }
        }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                HashMap headers = new HashMap();
                headers.put("Content-Type", "application/json");
                headers.put("Authorization", "Token 6094fab5d0e260d1d39b7e9f0a77b8b3648f4826");
                return headers;
            }
        };
        q.add(jsonObjectRequest);

    }
    public void addImgToFolder(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && false == Environment.isExternalStorageManager()) {
            Uri uri = Uri.parse("package:" + BuildConfig.APPLICATION_ID);
            startActivity(new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, uri));
        }
        File direct = new File(Environment.getExternalStorageDirectory() + "/LaundryImages");
        if (!direct.exists()) {
            File wallpaperDirectory = new File(Environment.getExternalStorageDirectory() + "/LaundryImages");
            wallpaperDirectory.mkdir();
        }

        String name = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault()).format(new Date());
        TextView tv = (TextView)findViewById(R.id.textView);

        File direct2 = new File(Environment.getExternalStorageDirectory() + "/LaundryImages/"+name);
        direct2.mkdir();
        int i=0;
        for (Bitmap bm : l) {
            i++;
//            tv.setText(String.valueOf(i));
            File file = new File(direct2,String.valueOf(i)+".jpg");

            try {
                FileOutputStream out = new FileOutputStream(file);
                if(file == null)tv.setText("null");
                bm.compress(Bitmap.CompressFormat.JPEG, 100, out);
                out.flush();
                out.close();

            } catch (Exception e) {
                e.printStackTrace();
            }


        }


    }
    public void displayResults() {
        l.get(0).getWidth();

    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // Match the request 'pic id with requestCode
        if (requestCode == 100) {
            // BitMap is data structure of image file which store the image in memory

            Bitmap photo = (Bitmap) data.getExtras().get("data");
            // Set the image in imageview for display
            l.add(photo);
//            ImageView iv = (ImageView) findViewById(R.id.imageView);
//            iv.setImageBitmap(photo);
            LinearLayout layout = (LinearLayout) findViewById(R.id.linear);
            ImageView imageView = new ImageView(this);
            imageView.setId(l.size());
            imageView.setPadding(2, 2, 2, 2);
            imageView.setImageBitmap(photo);
            imageView.setScaleType(ImageView.ScaleType.FIT_XY);
            ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(300, ViewGroup.LayoutParams.MATCH_PARENT);
            imageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    diag.show();
                    Remove =imageView;

                }
            });
            imageView.setLayoutParams(params);
            layout.addView(imageView);

        }
    }



}




