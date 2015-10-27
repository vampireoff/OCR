package com.lwd.ocrsearch.tesseract;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipException;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.SoundPool;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.StatFs;
import android.provider.MediaStore;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.googlecode.tesseract.android.TessBaseAPI;
import com.lwd.ocrsearch.utils.ImgPretreatment;
import com.lwd.ocrsearch.utils.ZipUtils;

public class MainActivity extends Activity {
	
	private static final int PHOTO_CAPTURE = 0x11;// 拍照
	private static final int PHOTO_RESULT = 0x12;// 结果
	
	private static String LANGUAGE = "eng";
	private static String IMG_PATH = getSDPath() + java.io.File.separator
			+ "tessdata";
	private static TextView tvResult;
	private static ImageView ivSelected;
	private static ImageView ivTreated;
	private static Button btnCamera;
	private static Button btncamera2;
	private static Button btnSelect;
	private static CheckBox chPreTreat;
	private static RadioGroup radioGroup;
	private static ScrollView scrollView;
	private static String textResult;
	private static Bitmap bitmapSelected;
	private static Bitmap bitmapTreated;
	private static final int SHOWRESULT = 0x101;
	private static final int SHOWTREATEDIMG = 0x102;
	private ProgressDialog progressDialog = null;
	private AlertDialog exitDialog = null;
	private static List<Bitmap> bitList = new ArrayList<Bitmap>();
	private static final int MB = 1024 * 1024;
	private SoundPool spPool;
	private int m;
	
	// 该handler用于处理修改结果的任务
	public Handler myHandler = new Handler() {
		
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case SHOWRESULT:
				if (textResult.equals("")){
					tvResult.setText(R.string.rec_fails);
					tvResult.setTextColor(Color.RED);
				}
				else{
					AlertDialog d = new AlertDialog.Builder(
							MainActivity.this)
					.setTitle(R.string.rec_result)
					.setMessage(textResult)
					.setPositiveButton(
							R.string.rec_true,
							new DialogInterface.OnClickListener() {
								
								@Override
								public void onClick(
										DialogInterface dialog,
										int which) {
									// TODO
									if(CameraActivity.isMobileNO(textResult) || CameraActivity.isPhone(textResult)){
										try {
											Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("tel:"
													+ textResult));
											startActivity(intent);
										} catch (Exception e) {
											// TODO: handle exception
											Toast.makeText(MainActivity.this, R.string.error_phone, 3000).show();
										}
										
									}else if(CameraActivity.isHomepage(textResult)){
										try {
											if(!textResult.startsWith("http://"))
												textResult = "http://" + textResult;
											Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(textResult));
											startActivity(intent);
										} catch (Exception e) {
											// TODO: handle exception
											Toast.makeText(MainActivity.this, R.string.error_add, 3000).show();
										}
									}else {
										Toast.makeText(MainActivity.this, R.string.rec_fail, 3000).show();
									}
									
								}
							}).setNegativeButton(R.string.rec_error, 
									new DialogInterface.OnClickListener() {
								
								@Override
								public void onClick(DialogInterface dialog, int which) {
									// TODO Auto-generated method stub
								}
							}).create();
					d.show();
					tvResult.setText(R.string.resul);
					tvResult.setTextColor(getResources().getColor(R.color.green));
				}
				break;
			case SHOWTREATEDIMG:
				tvResult.setText(R.string.recing);
				tvResult.setTextColor(Color.BLUE);
				showPicture(ivTreated, bitmapTreated);
				break;
			case 1:
//				setProgressBarIndeterminateVisibility(false);
				progressDialog.dismiss();
				break;
			}
			super.handleMessage(msg);
		}
		
	};
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		//标题显示进度条
//		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
//		// 全屏显示
//		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
		setContentView(R.layout.activity_main);
		
		initdata();
		
		if(freeSpaceOnSd() < 65){
			btnCamera.setClickable(false);
			btnSelect.setClickable(false);
			Toast.makeText(MainActivity.this, getString(R.string.sd_no_space), 2000).show();
		}else {
			// 若文件夹不存在 首先创建文件夹
			File path = new File(IMG_PATH);
			if (!path.exists()) {
				path.mkdirs();
			}
			if (path.listFiles().length < 2) {
//				setProgressBarIndeterminateVisibility(true);
				progressDialog.show();
				progressDialog.setCanceledOnTouchOutside(false);
				new Thread(new movefile()).start();
			}
		}
		
		// 用于设置解析语言
		radioGroup.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			
			@Override
			public void onCheckedChanged(RadioGroup group, int checkedId) {
				switch (checkedId) {
				case R.id.rb_en:
					LANGUAGE = "eng";
					break;
				case R.id.rb_ch:
					LANGUAGE = "chi_sim";
					break;
				}
			}
		});
	}
	
	private void initdata(){
		tvResult = (TextView) findViewById(R.id.tv_result);
		ivSelected = (ImageView) findViewById(R.id.iv_selected);
		ivTreated = (ImageView) findViewById(R.id.iv_treated);
		btnCamera = (Button) findViewById(R.id.btn_camera);
		btncamera2 = (Button) findViewById(R.id.btn_camera2);
		btnSelect = (Button) findViewById(R.id.btn_select);
		chPreTreat = (CheckBox) findViewById(R.id.ch_pretreat);
		radioGroup = (RadioGroup) findViewById(R.id.radiogroup);
		scrollView = (ScrollView) findViewById(R.id.scrollv);
		
		btnCamera.setOnClickListener(new cameraButtonListener());
		btnSelect.setOnClickListener(new selectButtonListener());
		btncamera2.setOnClickListener(new cameraButtonListener2());
		
		progressDialog = new ProgressDialog(this);
		progressDialog.setMessage(getString(R.string.hard_loading));
		progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		
		tvResult.setTextColor(Color.GRAY);
		tvResult.getPaint().setFakeBoldText(true);
		
		spPool = new SoundPool(10, AudioManager.STREAM_SYSTEM, 5);
		m = spPool.load(this, R.raw.ringer, 1);
	}
	
	public class movefile implements Runnable{
		@Override
		public void run() {
			// TODO Auto-generated method stub
			// 把 Assert里的字库copy到文件下
			try {
				InputStream in = getResources().getAssets()
						.open("tessdata.zip");
				File lan_zip = new File(IMG_PATH + "/traineddata.zip");
				OutputStream out = new FileOutputStream(lan_zip);
				byte[] temp = new byte[1024];
				int size = -1;
				while ((size = in.read(temp)) != -1) {
					out.write(temp, 0, size);
				}
				out.flush();
				out.close();
				in.close();
				// 解压其文件夹
				try {
					ZipUtils.upZipFile(lan_zip, IMG_PATH);
				} catch (ZipException e) {
					// TODO
					e.printStackTrace();
				} catch (IOException e) {
					// TODO
					e.printStackTrace();
				}
				// 删除压缩文件.zip
				lan_zip.delete();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			myHandler.sendEmptyMessage(1);
		}
	}
	
	/** 计算sdcard上的剩余空间 **/
	private int freeSpaceOnSd() {
		if(getSDPath() == null)
			return 0;
		else {
			StatFs stat = new StatFs(Environment.getExternalStorageDirectory()
					.getPath());
			double sdFreeMB = ((double) stat.getAvailableBlocks() * (double) stat
					.getBlockSize()) / MB;
			return (int) sdFreeMB;
		}
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		
		if (resultCode == Activity.RESULT_CANCELED)
			return;
		
		if (requestCode == PHOTO_CAPTURE) {
			startPhotoCrop(Uri.fromFile(new File(IMG_PATH, "temp.jpg")));
		}
		
		// 处理结果
		if (requestCode == PHOTO_RESULT) {
			bitmapSelected = decodeUriAsBitmap(Uri.fromFile(new File(IMG_PATH,
					"temp_cropped.jpg")));
			bitList.add(bitmapSelected);
			if (chPreTreat.isChecked()){
				tvResult.setText(R.string.dealing);
				tvResult.setTextColor(Color.BLUE);
			}
			else{
				tvResult.setText(R.string.recing);
				tvResult.setTextColor(Color.BLUE);
			}
			scrollView.setVisibility(View.VISIBLE);
			// 显示选择的图片
			showPicture(ivSelected, bitmapSelected);
			
			// 新线程来处理识别
			new Thread(new Runnable() {
				@Override
				public void run() {
					if (chPreTreat.isChecked()) {
						bitmapTreated = ImgPretreatment
								.doPretreatment(bitmapSelected);
						Message msg = new Message();
						msg.what = SHOWTREATEDIMG;
						myHandler.sendMessage(msg);
						textResult = doOcr(bitmapTreated, LANGUAGE);
					} else {
						bitmapTreated = ImgPretreatment
								.converyToGrayImg(bitmapSelected);
						Message msg = new Message();
						msg.what = SHOWTREATEDIMG;
						myHandler.sendMessage(msg);
						textResult = doOcr(bitmapTreated, LANGUAGE);
					}
					bitList.add(bitmapTreated);
					Message msg2 = new Message();
					msg2.what = SHOWRESULT;
					myHandler.sendMessage(msg2);
				}
			}).start();
		}
		super.onActivityResult(requestCode, resultCode, data);
	}
	
	// 扫描识别
	class cameraButtonListener implements OnClickListener {
		
		@Override
		public void onClick(View arg0) {
			spPool.play(m, 1, 1, 0, 0, 1);
			Intent intent = new Intent(MainActivity.this, CameraActivity.class);
			startActivity(intent);
		}
	};
	
	// 拍照识别
	class cameraButtonListener2 implements OnClickListener {
		
		@Override
		public void onClick(View arg0) {
			spPool.play(m, 1, 1, 0, 0, 1);
			Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
			intent.putExtra(MediaStore.EXTRA_OUTPUT,
					Uri.fromFile(new File(IMG_PATH, "temp.jpg")));
			startActivityForResult(intent, PHOTO_CAPTURE);
		}
	};
	
	// 从相册选取照片并裁剪
	class selectButtonListener implements OnClickListener {
		
		@Override
		public void onClick(View v) {
			spPool.play(m, 1, 1, 0, 0, 1);
			Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
			intent.addCategory(Intent.CATEGORY_OPENABLE);
			intent.setType("image/*");
			intent.putExtra("crop", "true");
			intent.putExtra("scale", true);
			intent.putExtra("return-data", false);
			intent.putExtra(MediaStore.EXTRA_OUTPUT,
					Uri.fromFile(new File(IMG_PATH, "temp_cropped.jpg")));
			intent.putExtra("outputFormat",
					Bitmap.CompressFormat.JPEG.toString());
			intent.putExtra("noFaceDetection", true); // no face detection
			startActivityForResult(intent, PHOTO_RESULT);
		}
	}
	
	// 将图片显示在view中
	public static void showPicture(ImageView iv, Bitmap bmp){
		iv.setImageBitmap(bmp);
	}
	
	/**
	 * 进行图片识别
	 * 
	 * @param bitmap
	 *            待识别图片
	 * @param language
	 *            识别语言
	 * @return 识别结果字符串
	 */
	public String doOcr(Bitmap bitmap, String language) {
		TessBaseAPI baseApi = new TessBaseAPI();
		baseApi.init(getSDPath(), language);
		// 必须加此行，tess-two要求BMP必须为此配置
		bitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
		
		baseApi.setImage(bitmap);
		
		String text = baseApi.getUTF8Text();
		
		baseApi.clear();
		baseApi.end();
		
		return text;
	}
	
	/**
	 * 获取sd卡的路径
	 * 
	 * @return 路径的字符串
	 */
	public static String getSDPath() {
		File sdDir = null;
		boolean sdCardExist = Environment.getExternalStorageState().equals(
				android.os.Environment.MEDIA_MOUNTED); // 判断sd卡是否存在
		if (sdCardExist) {
			sdDir = Environment.getExternalStorageDirectory();// 获取外存目录
		}
		return sdDir.toString();
	}
	
	/**
	 * 调用系统图片编辑进行裁剪
	 */
	public void startPhotoCrop(Uri uri) {
		Intent intent = new Intent("com.android.camera.action.CROP");
		intent.setDataAndType(uri, "image/*");
		intent.putExtra("crop", "true");
		intent.putExtra("scale", true);
		intent.putExtra(MediaStore.EXTRA_OUTPUT,
				Uri.fromFile(new File(IMG_PATH, "temp_cropped.jpg")));
		intent.putExtra("return-data", false);
		intent.putExtra("outputFormat", Bitmap.CompressFormat.JPEG.toString());
		intent.putExtra("noFaceDetection", true); // no face detection
		startActivityForResult(intent, PHOTO_RESULT);
	}
	
	/**
	 * 根据URI获取位图
	 * 
	 * @param uri
	 * @return 对应的位图
	 */
	private Bitmap decodeUriAsBitmap(Uri uri) {
		Bitmap bitmap = null;
		try {
			BitmapFactory.Options options = new BitmapFactory.Options();
			options.inSampleSize = 2;
			bitmap = BitmapFactory.decodeStream(getContentResolver()
					.openInputStream(uri),null,options);
			bitmap = compressImage(bitmap);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return null;
		}catch (OutOfMemoryError error) {
			// TODO: handle exception
			return null;
		}
		return bitmap;
	}
	
	/**
	 * 质量压缩
	 * @param image
	 * @return
	 */
	private Bitmap compressImage(Bitmap image) {  
		  
        ByteArrayOutputStream baos = new ByteArrayOutputStream();  
        image.compress(Bitmap.CompressFormat.JPEG, 100, baos);//质量压缩方法，这里100表示不压缩，把压缩后的数据存放到baos中  
        int options = 80;  
        while ( (baos.toByteArray().length / 1024)>100) {  //循环判断如果压缩后图片是否大于100kb,大于继续压缩         
            baos.reset();//重置baos即清空baos  
            options -= 10;//每次都减少10  
            image.compress(Bitmap.CompressFormat.JPEG, options, baos);//这里压缩options%，把压缩后的数据存放到baos中  
        }  
        ByteArrayInputStream isBm = new ByteArrayInputStream(baos.toByteArray());//把压缩后的数据baos存放到ByteArrayInputStream中  
        Bitmap bitmap = BitmapFactory.decodeStream(isBm, null, null);//把ByteArrayInputStream数据生成图片  
        return bitmap;  
    }  
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		// TODO Auto-generated method stub
		if (keyCode == KeyEvent.KEYCODE_BACK
				&& event.getAction() == KeyEvent.ACTION_DOWN) {
			// 如果存在
			back();
		}
		return super.onKeyDown(keyCode, event);
	}
	
	public void back() {
		if (exitDialog != null && exitDialog.isShowing()) {
			exitDialog.dismiss();
		}else {
			setExitDialog();
		}
	}
	
	private void setExitDialog() {
		exitDialog = new AlertDialog.Builder(this).setTitle("title")
				.setMessage("message").create();
		Window window = exitDialog.getWindow();
		window.setGravity(Gravity.BOTTOM);
		exitDialog.show();
		window.setContentView(R.layout.exit_dialog);
		window.setLayout(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
		Button btnCancel = (Button) window
				.findViewById(R.id.exit_dialog_cancle);
		Button btnOk = (Button) window.findViewById(R.id.exit_dialog_ok);
		btnCancel.setOnClickListener(new btnCancel());
		btnOk.setOnClickListener(new btnOK());
	}
	
	class btnCancel implements OnClickListener {
		@Override
		public void onClick(View v) {
			// TODO Auto-generated method stub
			exitDialog.dismiss();
		}
	}
	
	class btnOK implements OnClickListener {
		
		@Override
		public void onClick(View view) {
			// TODO Auto-generated method stub
			onDestroy();
		}
	}
	
	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		showPicture(ivSelected, null);
		showPicture(ivTreated, null);
		for(int i=0; i<bitList.size(); i++){
			if(bitList.get(i) != null || !bitList.get(i).isRecycled()){
				bitList.get(i).recycle();
			}
		}
		finish();
		System.gc();
		super.onDestroy();
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
}
