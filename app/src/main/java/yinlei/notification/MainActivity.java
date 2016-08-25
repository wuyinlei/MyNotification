package yinlei.notification;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

import java.util.Date;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final Notification notification = new Notification.Builder().setContext(this)
                .setImgRes(R.drawable.notify)
                .setContent("你有一条新的饿了么消息，请及时接受")
                .setTime(System.currentTimeMillis())
                .setTitle("新通知")
                .build();

        findViewById(R.id.btn_show_window).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                notification.show();

            }
        });

        findViewById(R.id.btn_hide_window).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                notification.dismiss();
            }
        });


    }
}
