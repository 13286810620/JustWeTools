package com.lfk.drawapictiure;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;

class PaintView extends View {
    // drawing board
    private Bitmap mBitmap;
    // if you set a picture in you will use it
    private Bitmap mBitmapBackGround;
    private Canvas mCanvas;
    private Path mPath;
    private Paint mBitmapPaint;
    private Paint mEraserPaint;
    private Paint mPaint;
    // width of screen
    private int width;
    // height of screen
    private int height;
    private Context context;
    // pass judgement on paint/eraser
    private boolean IsPaint = true;
    // drawing x,y
    private float mX, mY;
    // judge your fingers' tremble
    private static final float TOUCH_TOLERANCE = 4;
    // judge long pressed
    private static final long TOUCH_LONG_PRESSED = 500;
    private boolean IsRecordPath = true;
    //    private PathNode pathNode;
    private boolean mIsLongPressed;
	private boolean IsShowing = false;
    private long Touch_Down_Time;
    private long Touch_Up_Time;
    private OnPathListener listener;
	private static final int CHOOSEPATH = 0;
	private static final int INDIVIDE = 1;
	private boolean ReDoOrUnDoFlag = false;
	private PathNode pathNode;
	private ArrayList<PathNode.Node> ReDoNodes = new ArrayList<>();

	public PaintView(Context context,AttributeSet attrs) {
		super(context,attrs);
		this.context = context;
        mPaint = new Paint();
        mEraserPaint = new Paint();
        Init_Paint(UserInfo.PaintColor,UserInfo.PaintWidth);
        Init_Eraser(UserInfo.EraserWidth);
        WindowManager manager = (WindowManager)context.getSystemService(Context.WINDOW_SERVICE);
        width = manager.getDefaultDisplay().getWidth();
        height = manager.getDefaultDisplay().getHeight();
        mBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        mCanvas = new Canvas(mBitmap);
        mPath = new Path();
        mBitmapPaint = new Paint(Paint.DITHER_FLAG);
	}

    // init paint
    private void Init_Paint(int color ,int width){
        mPaint.setAntiAlias(true);
        mPaint.setDither(true);
        mPaint.setColor(color);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeJoin(Paint.Join.ROUND);
        mPaint.setStrokeCap(Paint.Cap.ROUND);
        mPaint.setStrokeWidth(width);
    }


    // init eraser
    private void Init_Eraser(int width){
        mEraserPaint.setAntiAlias(true);
        mEraserPaint.setDither(true);
        mEraserPaint.setColor(0xFF000000);
        mEraserPaint.setStrokeWidth(width);
        mEraserPaint.setStyle(Paint.Style.STROKE);
        mEraserPaint.setStrokeJoin(Paint.Join.ROUND);
        mEraserPaint.setStrokeCap(Paint.Cap.SQUARE);
        // The most important
        mEraserPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_OUT));
    }

    // while size is changed
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
		if(IsPaint)
        	Init_Paint(UserInfo.PaintColor, UserInfo.PaintWidth);
		else
			Init_Eraser(UserInfo.EraserWidth);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawColor(Color.WHITE);
        canvas.drawBitmap(mBitmap, 0, 0, mBitmapPaint);
        if(IsPaint)
            canvas.drawPath(mPath, mPaint);
        else
            canvas.drawPath(mPath, mEraserPaint);
    }

	private void Touch_Down(float x, float y) {
		mPath.reset();
		mPath.moveTo(x, y);
		mX = x;
		mY = y;
		 if(IsRecordPath) {
			 listener.AddNodeToPath(x, y, MotionEvent.ACTION_DOWN, IsPaint);
		 }
	}


	private void Touch_Move(float x, float y) {
		float dx = Math.abs(x - mX);
		float dy = Math.abs(y - mY);
		if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {
			mPath.quadTo(mX, mY, (x + mX) / 2, (y + mY) / 2);
			mX = x;
			mY = y;
			if(IsRecordPath) {
				listener.AddNodeToPath(x, y, MotionEvent.ACTION_MOVE, IsPaint);
			}
		}
	}
	private void Touch_Up(Paint paint){
		mPath.lineTo(mX, mY);
		mCanvas.drawPath(mPath, paint);
		mPath.reset();
        if(IsRecordPath) {
			listener.AddNodeToPath(mX, mY, MotionEvent.ACTION_UP, IsPaint);
		}
	}


	public void setColor(int color) {
		showCustomToast("已选择颜色" + colorToHexString(color));
		mPaint.setColor(color);
	}


	public void setPenWidth(int width) {
		showCustomToast("设定笔粗为：" + width);
		mPaint.setStrokeWidth(width);
	}

	public void setIsPaint(boolean isPaint) {
		IsPaint = isPaint;
	}

	public void setOnPathListener(OnPathListener listener) {
		this.listener = listener;
	}

	public void setmEraserPaint(int width){
		showCustomToast("设定橡皮粗为："+width);
		mEraserPaint.setStrokeWidth(width);
	}

	public void setIsRecordPath(boolean isRecordPath,PathNode pathNode) {
		this.pathNode = pathNode;
		IsRecordPath = isRecordPath;
	}

	public void setIsRecordPath(boolean isRecordPath) {
		IsRecordPath = isRecordPath;
	}
	public boolean isShowing() {
		return IsShowing;
	}


	private static String colorToHexString(int color) {
		return String.format("#%06X", 0xFFFFFFFF & color);
	}

	// switch eraser/paint
	public void Eraser(){
		showCustomToast("切换为橡皮");
		IsPaint = false;
		Init_Eraser(UserInfo.EraserWidth);
	}

	public void Paint(){
		showCustomToast("切换为铅笔");
		IsPaint = true;
		Init_Paint(UserInfo.PaintColor, UserInfo.PaintWidth);
	}

	public Paint getmEraserPaint() {
		return mEraserPaint;
	}

	public Paint getmPaint() {
		return mPaint;
	}

	public void clean() {
		mBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
		mCanvas.setBitmap(mBitmap);
		try {
			Message msg = new Message();
			msg.obj = PaintView.this;
			msg.what = INDIVIDE;
			handler.sendMessage(msg);
			Thread.sleep(0);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	/**
	 *  @author lfk_dsk@hotmail.com
	 *  @param uri get the uri of a picture
	 * */
	public void setmBitmap(Uri uri){
		Log.e("图片路径", String.valueOf(uri));
		ContentResolver cr = context.getContentResolver();
		try {
			mBitmapBackGround = BitmapFactory.decodeStream(cr.openInputStream(uri));
//			RectF rectF = new RectF(0,0,width,height);
			mCanvas.drawBitmap(mBitmapBackGround, 0, 0, mBitmapPaint);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		invalidate();
	}

	/**
	 *  @author lfk_dsk@hotmail.com
	 *  @param file Pictures' file
	 * */
	public void BitmapToPicture(File file){
		FileOutputStream fileOutputStream = null;
		try {
			SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
			Date now = new Date();
			File tempfile = new File(file+"/"+formatter.format(now)+".jpg");
			fileOutputStream = new FileOutputStream(tempfile);
			mBitmap.compress(Bitmap.CompressFormat.JPEG, 100, fileOutputStream);
			showCustomToast(tempfile.getName() + "已保存");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	public void PathNodeToJson(PathNode pathNode,File file){
		ArrayList<PathNode.Node> arrayList = pathNode.getPathList();
		String json = "[";
		for(int i = 0;i < arrayList.size();i++){
			PathNode.Node node = arrayList.get(i);
			json += "{"+"\""+"x"+"\""+":"+px2dip(node.x)+"," +
					"\""+"y"+"\""+":"+px2dip(node.y)+","+
					"\""+"PenColor"+"\""+":"+node.PenColor+","+
					"\""+"PenWidth"+"\""+":"+node.PenWidth+","+
					"\""+"EraserWidth"+"\""+":"+node.EraserWidth+","+
					"\""+"TouchEvent"+"\""+":"+node.TouchEvent+","+
					"\""+"IsPaint"+"\""+":"+"\""+node.IsPaint+"\""+","+
					"\""+"time"+"\""+":"+node.time+
					"},";
		}
		json = json.substring(0,json.length()-1);
		json += "]";
		try {
			json = enCrypto(json, "lfk_dsk@hotmail.com");
		} catch (InvalidKeySpecException e) {
			e.printStackTrace();
		} catch (InvalidKeyException e) {
			e.printStackTrace();
		} catch (NoSuchPaddingException e) {
			e.printStackTrace();
		} catch (IllegalBlockSizeException e) {
			e.printStackTrace();
		} catch (BadPaddingException e) {
			e.printStackTrace();
		}
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
		Date now = new Date();
		File tempfile = new File(file+"/"+formatter.format(now)+".lfk");
		try {
			FileOutputStream fileOutputStream = new FileOutputStream(tempfile);
			byte[] bytes = json.getBytes();
			fileOutputStream.write(bytes);
			fileOutputStream.close();
			showCustomToast(tempfile.getName() + "已保存");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void clearReUnList(){
		ReDoNodes.clear();
	}

	public void JsonToPathNodeToHandle(Uri uri){
		Message message = new Message();
		message.obj = uri.getPath();
		message.what = CHOOSEPATH;
		handler.sendMessage(message);
	}

	private void JsonToPathNode(String file){
		String res = "";
		ArrayList<PathNode.Node> arrayList = new ArrayList<>();
		try {
			Log.e("绝对路径1",file);
			FileInputStream in = new FileInputStream(file);
			ByteArrayOutputStream bufferOut = new ByteArrayOutputStream();
			byte[] buffer = new byte[1024];
			for(int i = in.read(buffer, 0, buffer.length); i > 0 ; i = in.read(buffer, 0, buffer.length)) {
				bufferOut.write(buffer, 0, i);
			}
			res = new String(bufferOut.toByteArray(), Charset.forName("utf-8"));
			Log.e("字符串文件",res);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			res = deCrypto(res, "lfk_dsk@hotmail.com");
		} catch (InvalidKeyException e) {
			e.printStackTrace();
		} catch (InvalidKeySpecException e) {
			e.printStackTrace();
		} catch (NoSuchPaddingException e) {
			e.printStackTrace();
		} catch (IllegalBlockSizeException e) {
			e.printStackTrace();
		} catch (BadPaddingException e) {
			e.printStackTrace();
		}
		try {
			JSONArray jsonArray = new JSONArray(res);
			for(int i = 0;i < jsonArray.length();i++){
				JSONObject jsonObject = new JSONObject(jsonArray.getString(i));
				PathNode.Node node = new PathNode().NewAnode();
				node.x = dip2px(jsonObject.getInt("x"));
				node.y = dip2px(jsonObject.getInt("y"));
				node.TouchEvent = jsonObject.getInt("TouchEvent");
				node.PenWidth = jsonObject.getInt("PenWidth");
				node.PenColor = jsonObject.getInt("PenColor");
				node.EraserWidth = jsonObject.getInt("EraserWidth");
				node.IsPaint = jsonObject.getBoolean("IsPaint");
				node.time = jsonObject.getLong("time");
				arrayList.add(node);
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
		pathNode.setPathList(arrayList);
	}

	public int px2dip(float pxValue) {
		final float scale = this.getResources().getDisplayMetrics().density;
		return (int) (pxValue / scale + 0.5f);
	}


	public int dip2px(float dpValue) {
		final float scale = this.getResources().getDisplayMetrics().density;
		return (int) (dpValue * scale + 0.5f);
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		float x = event.getX();
		float y = event.getY();
		switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
				Touch_Down(x, y);
				invalidate();
				break;

			case MotionEvent.ACTION_MOVE:
				Touch_Move(x, y);
				invalidate();
				break;

			case MotionEvent.ACTION_UP:
				if(IsPaint){
					Touch_Up(mPaint);
				}else {
					Touch_Up(mEraserPaint);
				}
				invalidate();
				break;
		}
		return true;
	}

	public void preview(ArrayList<PathNode.Node> arrayList) {
		IsRecordPath = false;
		PreviewThread previewThread = new PreviewThread(this, arrayList);
		Thread thread = new Thread(previewThread);
		thread.start();
	}

	private Handler handler=new Handler(){

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what){
				case INDIVIDE:
					((View) msg.obj).invalidate();
					break;
				case CHOOSEPATH:
					JsonToPathNode(msg.obj.toString());
					break;
			}
			super.handleMessage(msg);
		}
		
	};

	public void showCustomToast(String toast) {
		LayoutInflater inflater = LayoutInflater.from(context);
		View view = inflater.inflate(R.layout.toast_item, (ViewGroup)findViewById(R.id.toast_item));
		TextView text = (TextView) view.findViewById(R.id.toast_text);
		text.setText(toast);
		Toast tempToast = new Toast(context);
		tempToast.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.CENTER, 0, 0);
		tempToast.setDuration(Toast.LENGTH_SHORT);
		tempToast.setView(view);
		tempToast.show();
	}

	class PreviewThread implements Runnable{
		private long time;
		private ArrayList<PathNode.Node> nodes;
		private View view;
		public PreviewThread(View view, ArrayList<PathNode.Node> arrayList) {
			this.view = view;
			this.nodes = arrayList;
		}
		public void run() {
			time = 0;
			IsShowing = true;
			clean();
			for(int i = 0 ;i < nodes.size();i++) {
                PathNode.Node node=nodes.get(i);
				Log.e(node.PenColor+":"+node.PenWidth+":"+node.EraserWidth,node.IsPaint+"");
				float x = node.x;
				float y = node.y;
				if(i<nodes.size()-1) {
					time=nodes.get(i+1).time-node.time;
				}
				IsPaint = node.IsPaint;
				if(node.IsPaint){
					UserInfo.PaintColor = node.PenColor;
					UserInfo.PaintWidth = node.PenWidth;
					Init_Paint(node.PenColor,node.PenWidth);
				}else {
					UserInfo.EraserWidth = node.EraserWidth;
					Init_Eraser(node.EraserWidth);
				}
				switch (node.TouchEvent) {
					case MotionEvent.ACTION_DOWN:
						Touch_Down(x,y);
						break;
					case MotionEvent.ACTION_MOVE:
					    Touch_Move(x,y);
						break;
					case MotionEvent.ACTION_UP:
						if(node.IsPaint){
							Touch_Up(mPaint);
						}else {
							Touch_Up(mEraserPaint);
						}
						break;
				}
					Message msg=new Message();
					msg.obj = view;
					msg.what = INDIVIDE;
					handler.sendMessage(msg);
				if(!ReDoOrUnDoFlag) {
					try {
						Thread.sleep(time);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
			ReDoOrUnDoFlag = false;
			IsShowing = false;
			IsRecordPath = true;
		}
	}

		/**
		 * 加密（使用DES算法）
		 *
		 * @param txt
		 *            需要加密的文本
		 * @param key
		 *            密钥
		 * @return 成功加密的文本
		 * @throws InvalidKeySpecException
		 * @throws InvalidKeyException
		 * @throws NoSuchPaddingException
		 * @throws IllegalBlockSizeException
		 * @throws BadPaddingException
		 */
	private static String enCrypto(String txt, String key)
				throws InvalidKeySpecException, InvalidKeyException,
				NoSuchPaddingException, IllegalBlockSizeException,
				BadPaddingException {
		StringBuffer sb = new StringBuffer();
		DESKeySpec desKeySpec = new DESKeySpec(key.getBytes());
		SecretKeyFactory skeyFactory = null;
		Cipher cipher = null;
		try {
			skeyFactory = SecretKeyFactory.getInstance("DES");
			cipher = Cipher.getInstance("DES");
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		SecretKey deskey = skeyFactory != null ? skeyFactory.generateSecret(desKeySpec) : null;
		if (cipher != null) {
			cipher.init(Cipher.ENCRYPT_MODE, deskey);
		}
		byte[] cipherText = cipher != null ? cipher.doFinal(txt.getBytes()) : new byte[0];
		for (int n = 0; n < cipherText.length; n++) {
			String stmp = (java.lang.Integer.toHexString(cipherText[n] & 0XFF));

			if (stmp.length() == 1) {
				sb.append("0" + stmp);
			} else {
				sb.append(stmp);
			}
		}
		return sb.toString().toUpperCase();
	}

		/**
		 * 解密（使用DES算法）
		 *
		 * @param txt
		 *            需要解密的文本
		 * @param key
		 *            密钥
		 * @return 成功解密的文本
		 * @throws InvalidKeyException
		 * @throws InvalidKeySpecException
		 * @throws NoSuchPaddingException
		 * @throws IllegalBlockSizeException
		 * @throws BadPaddingException
		 */
	private static String deCrypto(String txt, String key)
				throws InvalidKeyException, InvalidKeySpecException,
				NoSuchPaddingException, IllegalBlockSizeException,
				BadPaddingException {
		DESKeySpec desKeySpec = new DESKeySpec(key.getBytes());
		SecretKeyFactory skeyFactory = null;
		Cipher cipher = null;
		try {
			skeyFactory = SecretKeyFactory.getInstance("DES");
			cipher = Cipher.getInstance("DES");
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		SecretKey deskey = skeyFactory != null ? skeyFactory.generateSecret(desKeySpec) : null;
		if (cipher != null) {
			cipher.init(Cipher.DECRYPT_MODE, deskey);
		}
		byte[] btxts = new byte[txt.length() / 2];
		for (int i = 0, count = txt.length(); i < count; i += 2) {
			btxts[i / 2] = (byte) Integer.parseInt(txt.substring(i, i + 2), 16);
		}
		return (new String(cipher.doFinal(btxts)));
	}

	public void ReDoORUndo(boolean flag){
		if(!IsShowing) {
			ReDoOrUnDoFlag = true;
			try {
				if (flag) {
					ReDoNodes.add(pathNode.getTheLastNote());
					pathNode.deleteTheLastNote();
					preview(pathNode.getPathList());
				} else {
					pathNode.AddNode(ReDoNodes.get(ReDoNodes.size() - 1));
					ReDoNodes.remove(ReDoNodes.size() - 1);
					preview(pathNode.getPathList());
				}

			} catch (ArrayIndexOutOfBoundsException e) {
				e.printStackTrace();
				showCustomToast("无法操作＝－＝");
			}
		}
	}
}
