package com.example.test64bit;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;

public class MainActivity extends Activity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        final EditText editText = new EditText(this);
        editText.setText("/data/data/" + getPackageName() + "/");
        layout.addView(editText);
        Button btn = new Button(this);
        btn.setText("test");
        layout.addView(btn);
        final TextView textView = new TextView(this);
        layout.addView(textView);
        setContentView(layout);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final StringBuffer sb = new StringBuffer();
                String text = editText.getText().toString();
                final File file = new File(text);
                sb.append("[Main]\nExist: ").append(file.exists()).append("\n")
                        .append("Read: ").append(file.canRead()).append("\n")
                        .append("Write: ").append(file.canWrite()).append("\n");
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        sb.append("[Thread]\nExist: ").append(file.exists()).append("\n")
                                .append("Read: ").append(file.canRead()).append("\n")
                                .append("Write: ").append(file.canWrite()).append("\n");
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                textView.setText(sb.toString());
                            }
                        });
                    }
                }).start();
            }
        });
    }


}