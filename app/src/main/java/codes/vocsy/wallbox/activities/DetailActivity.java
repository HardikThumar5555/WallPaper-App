package codes.vocsy.wallbox.activities;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.snackbar.Snackbar;
import com.nabinbhandari.android.permissions.PermissionHandler;
import com.nabinbhandari.android.permissions.Permissions;
import com.viven.imagezoom.ImageZoomHelper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import codes.vocsy.wallbox.R;
import codes.vocsy.wallbox.utils.Util;

import static codes.vocsy.wallbox.activities.MainActivity.EXTRA_CREATOR;
import static codes.vocsy.wallbox.activities.MainActivity.EXTRA_LIKES;
import static codes.vocsy.wallbox.activities.MainActivity.EXTRA_SIZE;
import static codes.vocsy.wallbox.activities.MainActivity.EXTRA_URL;
import static codes.vocsy.wallbox.activities.MainActivity.EXTRA_VIEWS;


public class DetailActivity extends AppCompatActivity {

    Context ctx = DetailActivity.this;
    private ImageView img;
    private TextView tv_likes, tv_creator, tv_views, tv_imgSize, tvNoConnection;
    private BottomSheetDialog bottomSheetDialog;
    private Button btnOpenDialog, btnSave, btnSetAsWall;
    private int likeCount;
    private int viewsCount;

    private String creatorName;
    private String imgSize;
    private String imgUrl;

    private ImageZoomHelper imageZoomHelper;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        img = findViewById(R.id.imageViewDetail);
        btnOpenDialog = findViewById(R.id.btnDialog);

        imageZoomHelper = new ImageZoomHelper(this);
        ImageZoomHelper.setViewZoomable(img);
        Intent i = getIntent();
        likeCount = i.getIntExtra(EXTRA_LIKES, 0);
        viewsCount = i.getIntExtra(EXTRA_VIEWS, 0);
        imgSize = i.getStringExtra(EXTRA_SIZE);
        creatorName = i.getStringExtra(EXTRA_CREATOR);
        imgUrl = i.getStringExtra(EXTRA_URL);

        if (Util.isNetworkAvailable(ctx)) {
            Glide.with(ctx).load(imgUrl).centerInside().into(img);
        } else {
            Snackbar.make(btnOpenDialog, "No Internet", Snackbar.LENGTH_LONG).show();

        }


        btnOpenDialog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openDialog();
            }
        });

    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        return imageZoomHelper.onDispatchTouchEvent(ev)||super.dispatchTouchEvent(ev);
    }

    public void openDialog() {
        bottomSheetDialog = new BottomSheetDialog(ctx, R.style.BottomSheetDialogTheme);
        bottomSheetDialog.setContentView(R.layout.details_dialog);
        tv_creator = bottomSheetDialog.findViewById(R.id.tv_user);
        tv_likes = bottomSheetDialog.findViewById(R.id.tv_likes);
        tv_views = bottomSheetDialog.findViewById(R.id.tv_views);
        tv_imgSize = bottomSheetDialog.findViewById(R.id.tv_imgSize);
        btnSave = bottomSheetDialog.findViewById(R.id.btn_save);
        btnSetAsWall = bottomSheetDialog.findViewById(R.id.btn_setWallpaper);

        tv_creator.setText(creatorName);
        tv_likes.setText(likeCount + " likes");
        tv_views.setText(viewsCount + " views");
        tv_imgSize.setText("Size " + imgSize);

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String rationale = "Please provide Storage permission to save Wallpapers.";
                String[] permissions = {Manifest.permission.WRITE_EXTERNAL_STORAGE};

                Permissions.Options options = new Permissions.Options()
                        .setRationaleDialogTitle("Info")
                        .setSettingsDialogTitle("Warning");

                Permissions.check(ctx, permissions, rationale, options, new PermissionHandler() {
                    @Override
                    public void onGranted() {
                        // do your task.
                        if (Util.isNetworkAvailable(ctx)) {
                            Snackbar.make(btnOpenDialog, "Saving Wallpaper", Snackbar.LENGTH_LONG).show();
                            bottomSheetDialog.dismiss();

                            Glide.with(ctx).asBitmap().load(imgUrl).into(new CustomTarget<Bitmap>() {
                                @Override
                                public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                                    new AsyncTaskRunner(resource, "save").execute();
                                }

                                @Override
                                public void onLoadCleared(@Nullable Drawable placeholder) {

                                }
                            });
                        }
                    }

                    @Override
                    public void onDenied(Context context, ArrayList<String> deniedPermissions) {
                        // permission denied, block the feature.
                    }
                });

            }
        });

        btnSetAsWall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (Util.isNetworkAvailable(ctx)) {
                    Snackbar.make(btnOpenDialog, "Setting Wallpaper", Snackbar.LENGTH_LONG).show();
                    bottomSheetDialog.dismiss();

                    Glide.with(ctx).asBitmap().load(imgUrl).into(new CustomTarget<Bitmap>() {
                        @Override
                        public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                            new AsyncTaskRunner(resource, "change").execute();
                        }

                        @Override
                        public void onLoadCleared(@Nullable Drawable placeholder) {

                        }
                    });
                }
            }
        });
        bottomSheetDialog.show();

    }

    public void SaveBitmap(Bitmap ImageBitmap) {
        String root = Environment.getExternalStorageDirectory().toString();
        File myDir = new File(root + "/AllPaper");
        myDir.mkdirs();

        String timeStamp = new SimpleDateFormat("ddss").format(new Date());
        String fname = "Wallpaper_" + timeStamp + ".png";

        File file = new File(myDir, fname);
        if (file.exists()) {
            file.delete();
        }
        try {
            FileOutputStream fileOut = new FileOutputStream(file);
            ImageBitmap.compress(Bitmap.CompressFormat.PNG, 100, fileOut);
            fileOut.flush();
            fileOut.close();
            Snackbar.make(btnOpenDialog, "Image Saved", Snackbar.LENGTH_LONG).show();

        } catch (Exception e) {
            e.printStackTrace();
            Snackbar.make(btnOpenDialog, "Unable to Save Image", Snackbar.LENGTH_LONG).show();

        }
    }


    private class AsyncTaskRunner extends AsyncTask<Void, Void, Void> {

        Bitmap bitmap;
        String method;
        ProgressDialog progressDialog;

        public AsyncTaskRunner(Bitmap bitmap, String method) {
            this.bitmap = bitmap;
            this.method = method;
        }

        @Override
        protected Void doInBackground(Void... p1) {
            // TODO: Implement this method
            if (method.equals("change")) {
                try {
                    Util.SetWallpaper(ctx, bitmap);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (method.equals("save")) {
                SaveBitmap(bitmap);
            }


            return null;
        }

        @Override
        protected void onPreExecute() {
            if (method.equals("change")) {
                progressDialog = ProgressDialog.show(ctx,
                        "Just a Sec",
                        "Changing Wallpaper");
            }
            if (method.equals("save")) {
                progressDialog = ProgressDialog.show(ctx,
                        "Just a Sec",
                        "Saving Wallpaper");
            }

        }

        @Override
        protected void onPostExecute(Void result) {
            progressDialog.dismiss();
        }


    }


}