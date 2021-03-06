package xyz.sleepygamers.agallery.FullScreenImage;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.WallpaperManager;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;
import com.soundcloud.android.crop.Crop;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import xyz.sleepygamers.agallery.Edit.ImageEditActivity;
import xyz.sleepygamers.agallery.R;
import xyz.sleepygamers.agallery.utils.RealPathUtil;
import xyz.sleepygamers.agallery.utils.TouchImageView;

public class ImageViewActivity extends AppCompatActivity {

    boolean mToolbarVisibility = true;
    LinearLayout bottomBar;
    ImageButton ib_edit, ib_crop, ib_delete;
    String imageFilePath;
    Uri imageURI;
    Toolbar mToolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_view);

        mToolbar = findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);

        // Get intent, action and MIME type
        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();

        if (Intent.ACTION_VIEW.equals(action) && type != null) {
            if (type.startsWith("image/")) {
                handleSendImage(intent);
            } else {
                Toast.makeText(this, "Invalid Image", Toast.LENGTH_SHORT).show();
                finish();
            }
        } else {
            Toast.makeText(this, "No actions specified", Toast.LENGTH_SHORT).show();
            finish();
        }
        TouchImageView imgDisplay = (TouchImageView) findViewById(R.id.imgDisplay);

        Glide.with(ImageViewActivity.this)
                .load(new File(imageFilePath)).into(imgDisplay);

        imgDisplay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //       Toast.makeText(_activity, "Clicked", Toast.LENGTH_SHORT).show();
                setToolbarView();
            }
        });

        setTitle();
        setBottomBar();
    }

    void handleSendImage(Intent intent) {
        Uri imageUri = intent.getData();
        if (imageUri != null) {
            // Update UI to reflect image being shared
            //imageURI = imageUri;
            imageFilePath = RealPathUtil.getPath(this, imageUri);
            imageURI = Uri.fromFile(new File(imageFilePath));
        } else {
            Toast.makeText(this, "Invalid Image", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void setBottomBar() {
        bottomBar = findViewById(R.id.bottomBar);
        ib_edit = findViewById(R.id.ib_edit);
        ib_crop = findViewById(R.id.ib_crop);
        ib_delete = findViewById(R.id.ib_delete);
        ib_crop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cropFuntion();
            }
        });
        ib_edit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editPic();
            }
        });
        ib_delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDeleteDialog();
            }
        });
    }

    private void cropFuntion() {
        String path = imageURI.getPath();
        Uri inputUri = Uri.fromFile(new File(path));
        File photoFile = null;
        try {
            String timeStamp =
                    new SimpleDateFormat("yyyyMMdd_HHmmss",
                            Locale.getDefault()).format(new Date());
            String imageFileName = "IMG_" + timeStamp + "_";
            File storageDir =
                    //            getExternalFilesDir(Environment.DIRECTORY_PICTURES);
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);

            photoFile = File.createTempFile(
                    imageFileName,  /* prefix */
                    ".jpg",         /* suffix */
                    storageDir      /* directory */
            );
            imageFilePath = photoFile.getAbsolutePath();

        } catch (IOException ex) {
            // Error occurred while creating the File
        }
        if (photoFile != null) {
            Uri photoURI = FileProvider.getUriForFile(this, "xyz.sleepygamers.agallery.provider", photoFile);
            Crop.of(inputUri, photoURI).start(this);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent result) {
        if (requestCode == Crop.REQUEST_CROP) {
            if (resultCode == RESULT_OK) {
                galleryAddPic();
            } else if (resultCode == RESULT_CANCELED) {
                deleteFile();
            }
        }
    }

    private void deleteFile() {
        try {
            File file = new File(imageFilePath);
            boolean deleted = file.delete();
        } catch (Exception e) {
        }
    }

    private void galleryAddPic() {
        try {
            Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            File f = new File(imageFilePath);
            Uri contentUri = Uri.fromFile(f);
            mediaScanIntent.setData(contentUri);
            this.sendBroadcast(mediaScanIntent);
        } catch (Exception e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_image_view, menu);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Take appropriate action for each action item click
        switch (item.getItemId()) {
            case R.id.action_share:
                sharePicture();
                return true;
            case R.id.action_details:
                setPictureDetails();
                return true;
            case R.id.action_setpicture:
                setPictureAs();
                return true;
            case R.id.action_setwallpaper:
                wallpaperFunction();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void wallpaperFunction() {
        Dexter.withActivity(this)
                .withPermission(Manifest.permission.CAMERA)
                .withListener(new PermissionListener() {
                    @Override
                    public void onPermissionGranted(PermissionGrantedResponse response) {

                        WallpaperManager myWallpaperManager = WallpaperManager
                                .getInstance(ImageViewActivity.this);
                        String imageFilePath = imageURI.getPath();
                        Bitmap myBitmap = BitmapFactory.decodeFile(imageFilePath);
                        if (myBitmap != null) {
                            try {
                                myWallpaperManager.setBitmap(myBitmap);
                            } catch (IOException e) {
                                Toast.makeText(ImageViewActivity.this,
                                        "Failed to set Wallpaper", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(ImageViewActivity.this,
                                    "Failed to decode image", Toast.LENGTH_SHORT).show();

                        }
                    }

                    @Override
                    public void onPermissionDenied(PermissionDeniedResponse response) {
                        makeToast();
                    }

                    void makeToast() {
                        Toast.makeText(ImageViewActivity.this, "You must accept wallpaper permission", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(PermissionRequest permission, PermissionToken token) {/* ... */}
                }).check();
    }

    private void setPictureDetails() {
        String path = imageURI.getPath();
        String title = "", time = "", width = "", height = "", filesize = "";
        try {
            ExifInterface exifInterface = new ExifInterface(path);
            ImageDetailsHelper details = new ImageDetailsHelper(exifInterface, path);
            title = details.getTitle();
            time = details.getTime();
            width = details.getWidth();
            height = details.getHeight();
            filesize = details.getFilesize() + " KB";
        } catch (IOException e) {
            e.printStackTrace();
        }
        final Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.gallery_preview_details_dialog);
        dialog.setTitle("Reorder...");
        TextView tv_path, tv_title, tv_time, tv_width, tv_height, tv_filesize;
        tv_path = dialog.findViewById(R.id.path);
        tv_title = dialog.findViewById(R.id.title);
        tv_time = dialog.findViewById(R.id.time);
        tv_width = dialog.findViewById(R.id.width);
        tv_height = dialog.findViewById(R.id.height);
        tv_filesize = dialog.findViewById(R.id.filesize);
        tv_path.setText(path);
        tv_title.setText(title);
        tv_time.setText(time);
        tv_width.setText(width);
        tv_height.setText(height);
        tv_filesize.setText(filesize);

        Button dialogButton = (Button) dialog.findViewById(R.id.close);
        dialogButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        dialog.show();

    }

    private void sharePicture() {
        String path = imageURI.getPath();
        Uri uri = Uri.fromFile(new File(path));
        Intent shareIntent = new Intent(android.content.Intent.ACTION_SEND);
        shareIntent.setType("image/*");
        shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
        startActivity(Intent.createChooser(shareIntent, getString(R.string.share)));
    }

    private void setPictureAs() {
        String path = imageURI.getPath();
        Uri uri = Uri.fromFile(new File(path));
        Intent intent = new Intent(Intent.ACTION_ATTACH_DATA);
        intent.setDataAndType(uri, "image/*");
        intent.putExtra("mimeType", "image/*");
        startActivity(Intent.createChooser(intent,
                getString(R.string.setpicture)));
    }

    private void editPic() {
        Intent i = new Intent(this, ImageEditActivity.class);
        i.putExtra("path", imageURI.getPath());
        startActivity(i);

    }

    void setToolbarView() {
        if (mToolbarVisibility) {
            getSupportActionBar().hide();
            //   bottomBar.setVisibility(View.GONE);
            bottomBar.animate().translationY(bottomBar.getHeight()).setInterpolator(new DecelerateInterpolator(1));
            //    mToolbar.animate().translationY(-mToolbar.getHeight()).setInterpolator(new AccelerateInterpolator(2));
        } else {
            getSupportActionBar().show();
            //bottomBar.setVisibility(View.VISIBLE);
            bottomBar.animate().translationY(0).setInterpolator(new AccelerateInterpolator(1));

            //     mToolbar.animate().translationY(0).setInterpolator(new DecelerateInterpolator(2));
        }
        mToolbarVisibility = !mToolbarVisibility;
    }

    void setTitle() {
        String path = imageURI.getPath();
        try {
            path = path.substring(path.lastIndexOf("/") + 1);
            if (path.length() > 15) {
                path = path.substring(0, 15).concat("...");
            }
            getSupportActionBar().setTitle(path);
        } catch (Exception ex) {
            //        Toast.makeText(GalleryPreview.this, ex.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void showDeleteDialog() {
        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case DialogInterface.BUTTON_POSITIVE:
                        deleteFileFromPath();
                        break;

                    case DialogInterface.BUTTON_NEGATIVE:
                        //No button clicked
                        break;
                }
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(ImageViewActivity.this);
        builder.setMessage("Are you sure?").setPositiveButton("Yes", dialogClickListener)
                .setNegativeButton("No", dialogClickListener).show();
    }

    void deleteFileFromPath() {
        String path = imageURI.getPath();
        File file = new File(path);
        // Set up the projection (we only need the ID)
        String[] projection = {MediaStore.Images.Media._ID};

        // Match on the file path
        String selection = MediaStore.Images.Media.DATA + " = ?";
        String[] selectionArgs = new String[]{file.getAbsolutePath()};

        // Query for the ID of the media matching the file path
        Uri queryUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        ContentResolver contentResolver = getContentResolver();
        Cursor c = contentResolver.query(queryUri, projection, selection, selectionArgs, null);
        if (c.moveToFirst()) {
            // We found the ID. Deleting the item via the content provider will also remove the file
            long id = c.getLong(c.getColumnIndexOrThrow(MediaStore.Images.Media._ID));
            Uri deleteUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id);
            contentResolver.delete(deleteUri, null, null);
            c.close();
            Toast.makeText(this, "File deleted", Toast.LENGTH_SHORT).show();
            finish();
        } else {
            // File not found in media store DB
            Toast.makeText(this, "File not found", Toast.LENGTH_SHORT).show();
        }

    }

}
