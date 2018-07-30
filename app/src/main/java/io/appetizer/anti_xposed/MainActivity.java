package io.appetizer.anti_xposed;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    // Used to load the 'native-lib' library on application startup.




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        antiXposed.getInstance().setActivity(this);

        String result = antiXposed.getInstance().check_java_maps() < 0 ? "Find Xposed":"No Xposed";
        System.out.println(antiXposed.getInstance().checkMaps()+"AAAAAAAAA");
        TextView tv = (TextView) findViewById(R.id.sample_text);
        tv.setText(result);
    }
}
