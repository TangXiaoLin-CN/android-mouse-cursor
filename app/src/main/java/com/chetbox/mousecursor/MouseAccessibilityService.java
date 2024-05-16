package com.chetbox.mousecursor;

import android.accessibilityservice.AccessibilityService;
import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.ImageView;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.ByteBuffer;

public class MouseAccessibilityService extends Service {
    private static final String TAG = MouseAccessibilityService.class.getName();

    private View cursorView;
    private LayoutParams cursorLayout;
    private WindowManager windowManager;

    //ADD THIS
    private IBinder mBinder = new LocalBinder();
    public MouseAccessibilityService(){}

    @Override
    public void onCreate() {
        super.onCreate();

        cursorView = View.inflate(getBaseContext(), R.layout.cursor, null);
        cursorLayout = new LayoutParams(
                LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT,
                LayoutParams.TYPE_SYSTEM_ERROR,
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                        | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                        | WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS
                        | WindowManager.LayoutParams.FLAG_FULLSCREEN
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT);
        cursorLayout.gravity = Gravity.TOP | Gravity.LEFT;
        cursorLayout.x = 0;
        cursorLayout.y = 0;

        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        new Thread(new Runnable() {
            @Override
            public void run() {
                try{
                    //构造一个服务器端端口为9999的socket服务器；
                    ServerSocket serverSocket = new ServerSocket(9999);
                    while(true)
                    {
                        System.out.println("wait contact.........");
                        //等待接收一个socket客户端的连接，并得到客户端的socket对象。
                        // 此方法在没有客户端连接的时候，会阻塞。
                        Socket client = serverSocket.accept();
                        System.out.println("get contact!");

                        //获得socket客户端的输入管道
                        InputStream is = client.getInputStream();
                        //获得socket客户端的输出管道
                        OutputStream os= client.getOutputStream();

                        //接受信息
                        byte[] bufferRaw = new byte[1024];

                        while(true)
                        {
                            try{
                                int len = is.read(bufferRaw);
                                if (len == -1)
                                {
                                    break;
                                }
                                String msg = new String(bufferRaw,0,len).trim();
                                System.out.println("-->" + msg);
                                os.write(" ".getBytes()); //表示收到回复
                                os.flush();
                                if(msg.equals("end")){
                                    break;
                                }
                                else
                                {
                                    try{
                                        ByteBuffer buffer = ByteBuffer.wrap(bufferRaw);
                                        final int action = buffer.get();
                                        final int xLocal = buffer.getShort();
                                        final int yLocal = buffer.getShort();
                                        final int xSize = buffer.getShort();
                                        final int ySize = buffer.getShort();
                                        final Point screen = GetScreenInfo();
                                        new Handler(getMainLooper()).post(new Runnable() {
                                            @Override
                                            public void run() {
                                                onMouseMove(new MouseEvent(action),(int)(((float)xLocal / xSize) * screen.x), (int)(((float)yLocal / ySize) * screen.y));
                                            }
                                        });
                                    } catch (NumberFormatException e)
                                    {
                                        System.out.println("invalid input content!");
                                    }
                                }
                            }catch (SocketException e)
                            {
                                System.out.println("i2222222222222222222");
                                break;
                            }
                        }

                        is.close();
                        os.close();
                        client.close();
                    }
                } catch (IOException e) {
                    System.out.println("1111111111111111111111111111");
                    e.printStackTrace();
                }
            }
        }).start();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

//        if (windowManager != null && cursorView != null) {
//            windowManager.removeView(cursorView);
//        }
    }

    public void onMouseMove(MouseEvent event,int x,int y) {
        if (event.type == MouseEvent.SHOW)
        {
            setCursor(true);
            cursorLayout.x = x;
            cursorLayout.y = y;
        }else
            setCursor(false);
        windowManager.updateViewLayout(cursorView, cursorLayout);
    }

    public void setCursor(boolean state)
    {
        View cursor = cursorView.findViewById(R.id.cursor);
        if(state)
        {
            System.out.println("显示鼠标");
            cursor.setVisibility(View.VISIBLE);
        }
        else
        {
            System.out.println("隐藏鼠标");
            cursor.setVisibility(View.GONE);
        }
        windowManager.updateViewLayout(cursorView, cursorLayout);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        windowManager.addView(cursorView, cursorLayout);
        return START_STICKY;
    }

    public Point GetScreenInfo()
    {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        WindowManager windowManager = (WindowManager) getBaseContext().getSystemService(getBaseContext().WINDOW_SERVICE);
        windowManager.getDefaultDisplay().getMetrics(displayMetrics);

        int width = displayMetrics.widthPixels; // 获取屏幕宽度
        int height = displayMetrics.heightPixels; // 获取屏幕高度

        return new Point(width,height);
    }

    //ADD THIS
    @Override
    public IBinder onBind(Intent intent) {
        // Called when a client (MainActivity in case of this sample) comes to the foreground
        // and binds with this service. The service should cease to be a foreground service
        // when that happens.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            stopForeground(true);
        }
        return mBinder;
    }

    //ADD THIS
    @Override
    public void onRebind(Intent intent) {
        // Called when a client (MainActivity in case of this sample) returns to the foreground
        // and binds once again with this service. The service should cease to be a foreground
        // service when that happens.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            stopForeground(true);
        }
        super.onRebind(intent);
    }
    //ADD THIS
    @Override
    public boolean onUnbind(Intent intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForeground(13, new Notification());
            return true; // Ensures onRebind() is called when a client re-binds.
        }
        return false;
    }

    //ADD THIS
    public class LocalBinder extends Binder {
        public MouseAccessibilityService getServerInstance() {
            return MouseAccessibilityService.this;
        }
    }
}

