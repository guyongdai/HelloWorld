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
		super.onCreate(savedInstanceState);    //����ʹ��main��xMl�ļ�����Ľ��沼��
		setContentView(R.layout.activity_main);
		Button bn=(Button)findViewById(R.id.ok);  //��ȡUI������idΪR��id��OK�İ�ť
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
