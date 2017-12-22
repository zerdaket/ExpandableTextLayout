package com.zerdaket.demo;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

import com.zerdaket.expandable.ExpandableTextLayout;

public class MainActivity extends AppCompatActivity {

    private ExpandableTextLayout mExpandableTextLayout;
    private TextView mTextView;
    private final String TEXT_CONTENT = "北国风光，千里冰封，万里雪飘。望长城内外，惟余莽莽；大河上下，顿失滔滔。山舞银蛇，原驰蜡象，欲与天公试比高。须晴日，看红装素裹，分外妖娆。\n" +
            "江山如此多娇，引无数英雄竞折腰。惜秦皇汉武，略输文采；唐宗宋祖，稍逊风骚。一代天骄，成吉思汗，只识弯弓射大雕。俱往矣，数风流人物，还看今朝。";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mExpandableTextLayout = findViewById(R.id.expandable_layout);
        mTextView = findViewById(R.id.content_text);
        mTextView.setText(TEXT_CONTENT);
    }

}
