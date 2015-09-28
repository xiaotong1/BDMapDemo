package com.example.mapdemo;


import java.util.ArrayList;
import java.util.List;




import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.location.Poi;
import com.baidu.mapapi.SDKInitializer;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.model.LatLng;

public class MainActivity extends Activity implements Runnable{
	// 定位相关
	LocationClient mLocClient;
	public MyLocationListenner myListener = new MyLocationListenner();
	MapView mMapView;
	BaiduMap mBaiduMap;
	private double latitude;
	private double longitude;
	boolean isFirstLoc = true;// 是否首次定位
	private TextView tvAddr;
	private ListView lvAddrs;
	private List<String> listPois=new ArrayList<String>();
	private Button btn;
	private boolean isCenter=false;
	private Thread centerThread=new Thread(this);
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		SDKInitializer.initialize(getApplicationContext());
		setContentView(R.layout.activity_main);
		initView();
		initMap();
	}


	private void initView() {
		tvAddr=(TextView) findViewById(R.id.tv_addr);
		btn=(Button)findViewById(R.id.btn);
		btn.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				if(isCenter){
					btn.setText("开启实时定位");
					centerThread.interrupt();
					isCenter=false;
				}else{
					centerThread.start();
					btn.setText("关闭实时定位");
					isCenter=true;
				}
			}
		});
		lvAddrs=(ListView) findViewById(R.id.list_poi);
		lvAddrs.setAdapter(new ArrayAdapter<String>(MainActivity.this, android.R.layout.simple_list_item_1,listPois));
		lvAddrs.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View view, int arg2,
					long arg3) {
				TextView tv=(TextView) view.findViewById(android.R.id.text1);
				tvAddr.setText(tv.getText().toString());
				centerInMap();
			}
		});
	}

	private void initMap() {
		// 地图初始化
		mMapView = (MapView) findViewById(R.id.bmapView);
		mBaiduMap = mMapView.getMap();
		// 开启定位图层
		mBaiduMap.setMyLocationEnabled(true);
		// 定位初始化
		mLocClient = new LocationClient(this);
		mLocClient.registerLocationListener(myListener);
		LocationClientOption option = new LocationClientOption();
		option.setOpenGps(true);// 打开gps
		option.setCoorType("bd09ll"); // 设置坐标类型
		option.setScanSpan(1000);
		option.setIsNeedAddress(true);// 可选，设置是否需要地址信息，默认不需要
		option.setIsNeedLocationPoiList(true);// 可选，默认false，设置是否需要POI结果，可以在BDLocation.getPoiList里得到
		mLocClient.setLocOption(option);
		mLocClient.start();
	}

	/**
	 * 定位SDK监听函数
	 */
	public class MyLocationListenner implements BDLocationListener {

		@Override
		public void onReceiveLocation(BDLocation location) {
			// map view 销毁后不在处理新接收的位置
			if (location == null || mMapView == null)
				return;
			MyLocationData locData = new MyLocationData.Builder()
					.accuracy(location.getRadius())
					// 此处设置开发者获取到的方向信息，顺时针0-360
					.direction(100).latitude(location.getLatitude())
					.longitude(location.getLongitude()).build();
			mBaiduMap.setMyLocationData(locData);
			if (isFirstLoc) {
				isFirstLoc = false;
				LatLng ll = new LatLng(location.getLatitude(),
						location.getLongitude());
				latitude=location.getLatitude();
				longitude=location.getLongitude();
				MapStatusUpdate u = MapStatusUpdateFactory.newLatLngZoom(ll,
						16f);
				mBaiduMap.animateMapStatus(u);
				listPois.add(location.getAddrStr());
				List<Poi> list = location.getPoiList();// POI数据
                if (list != null) {
                    for (Poi p : list) {
                     listPois.add(p.getName());
                    }
                }
			}
		}
		public void onReceivePoi(BDLocation poiLocation) {
		}
	}
	private void centerInMap(){
		LatLng ll = new LatLng(latitude,longitude);
		MapStatusUpdate u = MapStatusUpdateFactory.newLatLngZoom(ll,
				16f);
		mBaiduMap.animateMapStatus(u);
	}

	@Override
	protected void onPause() {
		mMapView.onPause();
		super.onPause();
	}

	@Override
	protected void onResume() {
		mMapView.onResume();
		super.onResume();
	}

	@Override
	protected void onDestroy() {
		// 退出时销毁定位
		mLocClient.stop();
		// 关闭定位图层
		mBaiduMap.setMyLocationEnabled(false);
		mMapView.onDestroy();
		mMapView = null;
		super.onDestroy();
	}

	@Override
	public void run() {
		while(true){
			centerInMap();
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
	}

}
