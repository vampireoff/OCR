package com.example.calendarviewdemo;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.example.calendarviewdemo.CalendarView.OnItemClickListener;

public class MainActivity extends Activity{
	private CalendarView calendar;
	private ImageButton calendarLeft;
	private TextView calendarCenter;
	private ImageButton calendarRight;
	private SimpleDateFormat format;
	private Button button;
	private Date startDate, endDate;
	private List<Date> dateList = new ArrayList<Date>();
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		format = new SimpleDateFormat("yyyy-MM-dd");
		//获取日历控件对象
		calendar = (CalendarView)findViewById(R.id.calendar);
		calendar.setSelectMore(true); //单选  
		
		calendarLeft = (ImageButton)findViewById(R.id.calendarLeft);
		calendarCenter = (TextView)findViewById(R.id.calendarCenter);
		calendarRight = (ImageButton)findViewById(R.id.calendarRight);
		button = (Button)findViewById(R.id.button);
		
		//设置日历日期
		calendar.setCalendarData(new Date());
		startDate = new Date();
		endDate = new Date();
		
		//获取日历中年月 ya[0]为年，ya[1]为月（格式大家可以自行在日历控件中改）
		String[] ya = calendar.getYearAndmonth().split("-"); 
		calendarCenter.setText(ya[0]+"年"+ya[1]+"月");
		calendarLeft.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				//点击上一月 同样返回年月 
				String leftYearAndmonth = calendar.clickLeftMonth(); 
				String[] ya = leftYearAndmonth.split("-"); 
				calendarCenter.setText(ya[0]+"年"+ya[1]+"月");
			}
		});
		
		calendarRight.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				//点击下一月
				String rightYearAndmonth = calendar.clickRightMonth();
				String[] ya = rightYearAndmonth.split("-"); 
				calendarCenter.setText(ya[0]+"年"+ya[1]+"月");
			}
		});
		
		//设置控件监听，可以监听到点击的每一天（大家也可以在控件中根据需求设定）
		calendar.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void OnItemClick(Date selectedStartDate,
					Date selectedEndDate, List<Date> dates, boolean dis) {
				// TODO Auto-generated method stub
				if(calendar.isSelectMore()){
					if (dis) {
						Toast.makeText(getApplicationContext(), format.format(selectedStartDate)+" 到 "+format.format(selectedEndDate), Toast.LENGTH_SHORT).show();
					}
					startDate = selectedStartDate;
					endDate = selectedEndDate;
				}else{
					dateList = dates;
				}
				
			}
		});
		
		button.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				String string = "";
				if (dateList != null && dateList.size() != 0) {
					for (Date string2 : dateList) {
						string += format.format(string2) + ",";
					}
					string = string.substring(0, string.length() - 1);
				}
				Toast.makeText(getApplicationContext(), format.format(startDate)+" 到 "+format.format(endDate) 
						+ "\n" + string, Toast.LENGTH_LONG).show();
			}
		});
		
	}
}
