package com.example.secretvault;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.OpenableColumns;
import android.provider.Settings;
import android.text.InputType;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class MainActivity extends AppCompatActivity {

    private static final int PICK_FILE_REQUEST = 101;
    private String currentCategory = "Photos";
    private final String SECRET_PIN = "1234"; // ডিফল্ট পাসওয়ার্ড 1234

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        showLoginScreen();
    }

    private void showLoginScreen() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER);
        layout.setPadding(60, 60, 60, 60);

        TextView title = new TextView(this);
        title.setText("Secret Vault (PIN: 1234)");
        title.setTextSize(22);
        title.setGravity(Gravity.CENTER);
        layout.addView(title);

        EditText pinInput = new EditText(this);
        pinInput.setHint("Enter PIN");
        pinInput.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        layout.addView(pinInput);

        Button loginBtn = new Button(this);
        loginBtn.setText("Unlock Vault");
        loginBtn.setOnClickListener(v -> {
            if (pinInput.getText().toString().equals(SECRET_PIN)) {
                checkPermissions();
                showVaultDashboard();
            } else {
                Toast.makeText(this, "Wrong PIN!", Toast.LENGTH_SHORT).show();
            }
        });
        layout.addView(loginBtn);

        setContentView(layout);
    }

    private void checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivity(intent);
            }
        }
    }

    private void showVaultDashboard() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER);
        layout.setPadding(60, 60, 60, 60);

        TextView header = new TextView(this);
        header.setText("Select File to Hide");
        header.setTextSize(20);
        header.setGravity(Gravity.CENTER);
        layout.addView(header);

        Button btnPhoto = new Button(this);
        btnPhoto.setText("Hide Photo (ছবি লুকান)");
        btnPhoto.setOnClickListener(v -> openFilePicker("image/*", "Photos"));
        layout.addView(btnPhoto);

        Button btnVideo = new Button(this);
        btnVideo.setText("Hide Video (ভিডিও লুকান)");
        btnVideo.setOnClickListener(v -> openFilePicker("video/*", "Videos"));
        layout.addView(btnVideo);

        Button btnAudio = new Button(this);
        btnAudio.setText("Hide Audio (অডিও লুকান)");
        btnAudio.setOnClickListener(v -> openFilePicker("audio/*", "Audios"));
        layout.addView(btnAudio);

        setContentView(layout);
    }

    private void openFilePicker(String mimeType, String category) {
        currentCategory = category;
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType(mimeType);
        startActivityForResult(intent, PICK_FILE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_FILE_REQUEST && resultCode == Activity.RESULT_OK && data != null && data.getData() != null) {
            hideFileFromUri(data.getData());
        }
    }

    private void hideFileFromUri(Uri uri) {
        try {
            String fileName = getFileName(uri);
            File vaultDir = new File(Environment.getExternalStorageDirectory(), ".SecretVault/" + currentCategory);
            if (!vaultDir.exists()) {
                vaultDir.mkdirs();
                new File(vaultDir, ".nomedia").createNewFile();
            }

            File destFile = new File(vaultDir, fileName + ".locked");
            InputStream in = getContentResolver().openInputStream(uri);
            FileOutputStream out = new FileOutputStream(destFile);

            byte[] buffer = new byte[4096];
            int len;
            while ((len = in.read(buffer)) > 0) {
                out.write(buffer, 0, len);
            }
            in.close();
            out.close();

            Toast.makeText(this, "Hidden in .SecretVault/" + currentCategory, Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private String getFileName(Uri uri) {
        String result = "hidden_file_" + System.currentTimeMillis();
        if ("content".equals(uri.getScheme())) {
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (index >= 0) result = cursor.getString(index);
                }
            }
        }
        return result;
    }
}
