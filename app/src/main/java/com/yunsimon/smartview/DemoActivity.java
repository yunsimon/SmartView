package com.yunsimon.smartview;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

import com.yunsimon.smartview.view.TagViewContainer;

public class DemoActivity extends AppCompatActivity {

    private TagViewContainer tagViewContainer;
    private ViewGroup tagViewPannel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_tag_view_demo);

        tagViewPannel = (ViewGroup) findViewById(R.id.tag_view_pannel);
        tagViewContainer = (TagViewContainer) findViewById(R.id.tag_view_container);
        tagViewContainer.setParentView(tagViewPannel);

        findViewById(R.id.add_tag_view).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                tagViewContainer.addTagView(R.drawable.tag_2004);
            }
        });

    }
}
