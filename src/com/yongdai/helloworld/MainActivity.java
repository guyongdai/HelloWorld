package com.yongdai.helloworld;

import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.view.View.OnClickListener; 
public class MainActivity extends ActionBarActivity
{

	@Override
	public void onCreate(Bundle savedInstanceState)
	{   
		//setTheme(android.R.style.Theme_Black);
		super.onCreate(savedInstanceState);    //设置使用main。xMl文件定义的界面布局
		setContentView(R.layout.activity_main);
		Button bn=(Button)findViewById(R.id.ok);  //获取UI界面中id为R。id。OK的按钮
		bn.setOnClickListener(new OnClickListener()
		{
			
			@Override
			public void onClick(View v)
			{
				final TextView show =(TextView)findViewById(R.id.show);
				show.setText("Hello android---"+new java.util.Date());
				
			}
		});
	}


}
