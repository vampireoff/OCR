package com.lwd.ocrsearch.tesseract;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.googlecode.tesseract.android.TessBaseAPI;
import com.lwd.ocrsearch.camera.CameraManager;
import com.lwd.ocrsearch.tesseract.R;
import com.lwd.ocrsearch.utils.ImgPretreatment;
import com.lwd.ocrsearch.utils.PlanarYUVLuminanceSource;

public class CameraActivity extends Activity {
	public static final boolean debug = true;
	private static final String TESSBASE_PATH = MainActivity.getSDPath();
	private static final String DEFAULT_LANGUAGE = "eng";
	private TessBaseAPI baseApi;
	private SurfaceView mainSurface;
	private TextView textView;
	private SurfaceHolder msurfaceHolder;
	private CameraManager cameraManager;
	private OcrFinderView orcFindView;
	private static Bitmap image = null;
	private boolean hasSurface = false;
	private String v = "";
	private String s = "";
	private boolean show = false;
	private static String TAG = CameraActivity.class.getSimpleName();
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
		Window window = getWindow();
		window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		window.requestFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.capture);
		baseApi = new TessBaseAPI();
		
		baseApi.init(TESSBASE_PATH, DEFAULT_LANGUAGE);
		Bitmap mp = BitmapFactory.decodeResource(getResources(),
				R.drawable.ic_launcher);
		
		mp = mp.copy(Bitmap.Config.ARGB_8888, false);
		baseApi.setImage(mp);
		
		String value = baseApi.getUTF8Text();
//		Log.d("tag", " the value is ===> " + value);
		baseApi.clear();
		baseApi.end();
		
		mainSurface = (SurfaceView) findViewById(R.id.mainSurface);
		textView = (TextView)findViewById(R.id.text);
		msurfaceHolder = mainSurface.getHolder();
	}
	
	@Override
	protected void onResume() {
		// TODO
		super.onResume();
		cameraManager = new CameraManager(getApplicationContext(), this);
		orcFindView = (OcrFinderView) findViewById(R.id.ocrFindView);
		orcFindView.setCameraManager(cameraManager);
		
		if (hasSurface) {
			initCamera(msurfaceHolder);
		} else {
			msurfaceHolder.addCallback(surcallback);
		}
	}
	
	public static final int DECODE = 0;
	public static final int QUIT = DECODE + 1;
	public static final int DECOCE_FAIL = QUIT + 1;
	private Handler decodeHandle = new Handler() {
		public void handleMessage(android.os.Message msg) {
			switch (msg.what) {
			case DECODE :
				break;
			case QUIT :
				break;
			case DECOCE_FAIL :
				cameraManager.requestPreviewFrame(decodeHandle, DECODE,
						new PreviewCallback());
				break;
			default :
				break;
			}
		};
	};
	/**
	 * 识别图片并取值
	 * @param bitmap
	 * @return
	 */
	public String decodeBitmapValue(Bitmap bitmap) {
		if (bitmap == null) {
			return null;
		}
		baseApi.init(TESSBASE_PATH, DEFAULT_LANGUAGE);
		
		bitmap = bitmap.copy(Bitmap.Config.ARGB_8888, false);
		baseApi.setImage(bitmap); //识别图片
		
		String value = baseApi.getUTF8Text();
//		Log.d("tag", " the value is ===> " + value);
		baseApi.clear();
		baseApi.end();
		return value;
	}
	/**
	 * Decode the data within the viewfinder rectangle, and time how long it
	 * took. For efficiency, reuse the same reader objects from one decode to
	 * the next.
	 * 
	 * @param data
	 *            The YUV preview frame.
	 * @param width
	 *            The width of the preview frame.
	 * @param height
	 *            The height of the preview frame.
	 */
	class PreviewCallback implements Camera.PreviewCallback {
		
		private Handler previewHandler;
		private int previewMessage;
		
		PreviewCallback() {}
		
		void setHandler(Handler previewHandler, int previewMessage) {
			this.previewHandler = previewHandler;
			this.previewMessage = previewMessage;
		}
		
		@Override
		public void onPreviewFrame(byte[] data, Camera camera) {
			
			if (data != null) {
				Camera.Parameters parameters = camera.getParameters();
				int imageFormat = parameters.getPreviewFormat();
//				Log.i("map", "Image Format: " + imageFormat
//						+ " ImageFormat.NV21 is " + ImageFormat.NV21);
//				
//				Log.i("CameraPreviewCallback", "data length:" + data.length);
				
				if (imageFormat == ImageFormat.NV21) {
//					Log.i("map", "Image Format: " + imageFormat);
					
					int w = parameters.getPreviewSize().width;
					int h = parameters.getPreviewSize().height;
					
					Rect rect = new Rect(0, 0, w, h);
					
					YuvImage img = new YuvImage(data, ImageFormat.NV21, w, h,
							null);
					ByteArrayOutputStream baos = new ByteArrayOutputStream();
					
					if (img.compressToJpeg(rect, 100, baos)) {
						image = BitmapFactory.decodeByteArray(
								baos.toByteArray(), 0, baos.size());
						image = cutBitmap(image,
								cameraManager.getFramingRectInPreview(),
								Bitmap.Config.ARGB_8888);
						
						if (image != null) {
							if (debug) {
								ImageView vv = (ImageView) findViewById(R.id.igv);
								image = ImgPretreatment.doPretreatment(image);
								vv.setImageBitmap(image);
							}
							
							new Thread(new Runnable() {
								
								@Override
								public void run() {
									// TODO Auto-generated method stub
									v = decodeBitmapValue(image);
								}
							}).start();
							
							if ((isMobileNO(v) || isPhone(v) || isHomepage(v)) && show == false) {
								onPause();
								show = true;
								s = v;
								AlertDialog d = new AlertDialog.Builder(
										CameraActivity.this)
								.setTitle(R.string.rec_result)
								.setMessage(s)
								.setPositiveButton(
										R.string.rec_true,
										new DialogInterface.OnClickListener() {
											
											@Override
											public void onClick(
													DialogInterface dialog,
													int which) {
												// TODO
												if(isMobileNO(s) || isPhone(s)){
													try {
														Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("tel:"
																+ s));
														startActivity(intent);
													} catch (Exception e) {
														// TODO: handle exception
														Toast.makeText(CameraActivity.this, R.string.error_phone, 3000).show();
													}
												}else if(isHomepage(s)){
													try {
														if(!s.startsWith("http://"))
															s = "http://" +s;
														Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(s));
														startActivity(intent);
													} catch (Exception e) {
														// TODO: handle exception
														Toast.makeText(CameraActivity.this, R.string.error_add, 3000).show();
													}
												}else {
													Toast.makeText(CameraActivity.this, R.string.rec_fail, 3000).show();
												}
												show = false;
											}
										}).setNegativeButton(R.string.rec_error, 
												new DialogInterface.OnClickListener() {
											
											@Override
											public void onClick(DialogInterface dialog, int which) {
												// TODO Auto-generated method stub
												decodeHandle.removeMessages(DECOCE_FAIL);
												Message msg = Message.obtain(decodeHandle,
														DECOCE_FAIL);
												decodeHandle.sendMessageDelayed(msg,800);
												show = false;
											}
										}).create();
								d.show();
								onResume();
							}
						}
						
					}
//					if (!show) {
////						decodeHandle.removeMessages(DECOCE_FAIL);
////						Message msg = Message.obtain(decodeHandle, DECOCE_FAIL);
////						decodeHandle.sendMessageDelayed(msg, 800);
//					}
				}
			} else {
				Log.i("CameraPreviewCallback", "data is null :");
			}
		}
	}
	
	/**
	 * 判断是否是手机号码*/
	public static boolean isMobileNO(String mobiles){
		
		Pattern pattern = Pattern.compile("^[1][3,4,5,8][0-9]{9}$");
		Matcher matcher = pattern.matcher(mobiles); 
		return matcher.matches();
	}
	
	/** 
     * 电话号码验证 
     *  
     * @param  str 
     * @return 验证通过返回true 
     */  
    public static boolean isPhone(String str) {   
        Pattern p1 = null,p2 = null;  
        Matcher m = null;  
        boolean b = false;    
        p1 = Pattern.compile("^[0][1-9]{2,3}-[0-9]{5,10}$");  // 验证带区号的  
        p2 = Pattern.compile("^[1-9]{1}[0-9]{5,8}$");         // 验证没有区号的  
        if(str.length() >9)  
        {   m = p1.matcher(str);  
            b = m.matches();    
        }else{  
            m = p2.matcher(str);  
            b = m.matches();   
        }    
        return b;  
    }  
	
	/**
	 * 判断字符串是否全为数字
	 * @param str
	 * @return
	 */
	public static boolean isNumber(String str) {
		Pattern pattern = Pattern.compile("[0-9]+");
		Matcher matcher = pattern.matcher((CharSequence) str);
		boolean result = matcher.matches();
		if (result) {
			System.out.println("true");
		} else {
			System.out.println("false");
		}
		return result;
	}
	
	/**
	 * 网址验证
	 * @param str
	 * @return
	 */
	 public static boolean isHomepage(String str){
		 if(!str.startsWith("http://"))
			 str = "http://" +str;
		 String regex = "http://(([a-zA-z0-9]|-){1,}\\.){1,}[a-zA-z0-9]{1,}-*";
		 Pattern pattern = Pattern.compile(regex);
		 Matcher matcher = pattern.matcher(str);
		 return matcher.matches();
	    }
	
	public static Bitmap cutBitmap(Bitmap mBitmap, Rect r, Bitmap.Config config) {
		int width = r.width();
		int height = r.height();
		
		Bitmap croppedImage = Bitmap.createBitmap(width, height, config);
		
		Canvas cvs = new Canvas(croppedImage);
		Rect dr = new Rect(0, 0, width, height);
		
		cvs.drawBitmap(mBitmap, r, dr, null);
		
		return croppedImage;
	}
	/**
	 * A factory method to build the appropriate LuminanceSource object based on
	 * the format of the preview buffers, as described by Camera.Parameters.
	 * 
	 * @param data
	 *            A preview frame.
	 * @param width
	 *            The width of the image.
	 * @param height
	 *            The height of the image.
	 * @return A PlanarYUVLuminanceSource instance.
	 */
//	public PlanarYUVLuminanceSource buildLuminanceSource(byte[] data,
//			int width, int height) {
//		Rect rect = cameraManager.getFramingRectInPreview();
//		if (rect == null) {
//			return null;
//		}
//		// Go ahead and assume it's YUV rather than die.
//		return new PlanarYUVLuminanceSource(data, width, height, rect.left,
//				rect.top, rect.width(), rect.height(), false);
//	}
	
	@Override
	protected void onPause() {
		cameraManager.stopPreview();
		cameraManager.closeDriver();
		if (!hasSurface) {
			SurfaceView surfaceView = (SurfaceView) findViewById(R.id.mainSurface);
			SurfaceHolder surfaceHolder = surfaceView.getHolder();
			surfaceHolder.removeCallback(surcallback);
		}
		super.onPause();
	}
	
	private SurfaceHolder.Callback surcallback = new SurfaceHolder.Callback() {
		
		@Override
		public void surfaceDestroyed(SurfaceHolder holder) {
			// TODO
			hasSurface = false;
		}
		
		@Override
		public void surfaceCreated(SurfaceHolder holder) {
			// TODO
			if (holder == null) {
//				Log.e(TAG,"*** WARNING *** surfaceCreated() gave us a null surface!");
			}
			if (!hasSurface) {
				hasSurface = true;
				initCamera(holder);
			}
		}
		
		@Override
		public void surfaceChanged(SurfaceHolder holder, int format, int width,
				int height) {
			// TODO
		}
	};
	
	private void initCamera(SurfaceHolder surfaceHolder) {
		if (surfaceHolder == null) {
			throw new IllegalStateException("No SurfaceHolder provided");
		}
		if (cameraManager.isOpen()) {
//			Log.w(TAG,"initCamera() while already open -- late SurfaceView callback?");
			return;
		}
		try {
			cameraManager.openDriver(surfaceHolder);
			cameraManager.startPreview();
			cameraManager.requestPreviewFrame(decodeHandle, DECODE,
					new PreviewCallback());
		} catch (IOException ioe) {
//			Log.w(TAG, ioe);
		} catch (RuntimeException e) {
//			Log.w(TAG, "Unexpected error initializing camera", e);
		}
	}
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
}
