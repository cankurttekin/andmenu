package com.cankurttekin.andmenu.applicationMain;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import com.cankurttekin.andmenu.R;
import com.cankurttekin.andmenu.applicationMain.lib.ConfigManager;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class ConfigEditorActivity extends BaseWindowActivity {
    private EditText editor;
    private final ActivityResultLauncher<String> createDocumentLauncher = registerForActivityResult(
            new ActivityResultContracts.CreateDocument("application/json"),
            uri -> {
                if (uri != null) {
                    saveToFile(uri);
                }
            }
    );

    private final ActivityResultLauncher<String[]> openDocumentLauncher = registerForActivityResult(
            new ActivityResultContracts.OpenDocument(),
            uri -> {
                if (uri != null) {
                    loadFromFile(uri);
                }
            }
    );

    public ConfigEditorActivity() {
        super(R.layout.config_editor_layout, false);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setHeaderFooterTexts("Config Editor", null);
        this.setWindowBoundarySize(ROOT_WINDOW_FULL_WIDTH_IN_MOBILE, 1);
        this.setWindowLocationGravity(Gravity.CENTER);

        editor = findViewById(R.id.config_editor_edittext);
        editor.setText(ConfigManager.exportConfig(this));

        Button saveButton = findViewById(R.id.config_editor_save_button);
        saveButton.setOnClickListener(v -> {
            if (ConfigManager.importConfig(this, editor.getText().toString())) {
                Toast.makeText(this, "Config updated. Restart recommended.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Error: Invalid JSON", Toast.LENGTH_LONG).show();
            }
        });

        Button exportFileButton = findViewById(R.id.config_editor_export_file_button);
        exportFileButton.setOnClickListener(v -> createDocumentLauncher.launch("andmenu_config.json"));

        Button importFileButton = findViewById(R.id.config_editor_import_file_button);
        importFileButton.setOnClickListener(v -> openDocumentLauncher.launch(new String[]{"application/json", "text/*", "*/*"}));

        Button shareButton = findViewById(R.id.config_editor_share_button);
        shareButton.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_TEXT, editor.getText().toString());
            startActivity(Intent.createChooser(intent, "Export Config"));
        });

        Button closeButton = findViewById(R.id.config_editor_close_button);
        closeButton.setOnClickListener(v -> finish());
    }

    private void saveToFile(Uri uri) {
        try (OutputStream outputStream = getContentResolver().openOutputStream(uri)) {
            if (outputStream != null) {
                outputStream.write(editor.getText().toString().getBytes(StandardCharsets.UTF_8));
                Toast.makeText(this, "File saved", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Error saving file: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void loadFromFile(Uri uri) {
        try (InputStream inputStream = getContentResolver().openInputStream(uri);
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            StringBuilder stringBuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line).append('\n');
            }
            editor.setText(stringBuilder.toString());
            Toast.makeText(this, "File loaded", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Error loading file: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}
