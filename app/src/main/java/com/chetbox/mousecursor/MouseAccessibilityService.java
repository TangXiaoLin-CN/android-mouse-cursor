package com.chetbox.mousecursor;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Build;
import android.os.Handler;
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

public class MouseAccessibilityService extends AccessibilityService {

    static final int VMouseState_SHOW = 1;
    static final int VMouseState_HIDE = 2;

    private static final String TAG = MouseAccessibilityService.class.getName();

    private View cursorView;
    private LayoutParams cursorLayout;
    private WindowManager windowManager;

    private static void logNodeHierachy(AccessibilityNodeInfo nodeInfo, int depth) {
        Rect bounds = new Rect();
        nodeInfo.getBoundsInScreen(bounds);

        StringBuilder sb = new StringBuilder();
        if (depth > 0) {
            for (int i=0; i<depth; i++) {
                sb.append("  ");
            }
            sb.append("\u2514 ");
        }
        sb.append(nodeInfo.getClassName());
        sb.append(" (" + nodeInfo.getChildCount() +  ")");
        sb.append(" " + bounds.toString());
        if (nodeInfo.getText() != null) {
            sb.append(" - \"" + nodeInfo.getText() + "\"");
        }
        Log.v(TAG, sb.toString());

        for (int i=0; i<nodeInfo.getChildCount(); i++) {
            AccessibilityNodeInfo childNode = nodeInfo.getChild(i);
            if (childNode != null) {
                logNodeHierachy(childNode, depth + 1);
            }
        }
    }

    private static AccessibilityNodeInfo findSmallestNodeAtPoint(AccessibilityNodeInfo sourceNode, int x, int y) {
        Rect bounds = new Rect();
        sourceNode.getBoundsInScreen(bounds);

        if (!bounds.contains(x, y)) {
            return null;
        }

        for (int i=0; i<sourceNode.getChildCount(); i++) {
            AccessibilityNodeInfo nearestSmaller = findSmallestNodeAtPoint(sourceNode.getChild(i), x, y);
            if (nearestSmaller != null) {
                return nearestSmaller;
            }
        }
        return sourceNode;
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
    }

    @Override
    public void onInterrupt() {
        Log.d(TAG, "onInterrupt");
    }

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
        if (Build.VERSION.SDK_INT >= 23)
        {
            cursorLayout.type = LayoutParams.TYPE_APPLICATION_OVERLAY;
        }else
        {
            cursorLayout.type = LayoutParams.TYPE_SYSTEM_ALERT;
        }
        cursorLayout.x = 0;
        cursorLayout.y = 0;

        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
           ////UDP写法
//        try {
//            udpSocket = new DatagramSocket(9999);
//            new Thread(new Runnable() {
//                @Override
//                public void run() {
//                    byte[] buffer = new byte[1];
//                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
//                    while (true) {
//                        try {
//                            udpSocket.receive(packet);
//                            String message = new String(packet.getData()).trim();
//                            final int event = Integer.parseInt(message);
//                            new Handler(getMainLooper()).post(new Runnable() {
//                                @Override
//                                public void run() {
//                                    onMouseMove(new MouseEvent(event));
//                                }
//                            });
//                        } catch (IOException e) {}
//                    }
//                }
//            }).start();
//        } catch (SocketException e) {
//            throw new RuntimeException(e);
//        }

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
//                                else if(msg.equals("hide")) {
//                                    new Handler(getMainLooper()).post(new Runnable() {
//                                        @Override
//                                        public void run() {
//                                            setCursor(false);
//                                        }
//                                    });
//                                }
//                                else if(msg.equals("show"))
//                                {
//                                    new Handler(getMainLooper()).post(new Runnable() {
//                                        @Override
//                                        public void run() {
//                                            setCursor(true);
//                                        }
//                                    });
//                                }
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

        if (windowManager != null && cursorView != null) {
            windowManager.removeView(cursorView);
        }
    }

    private void click() {
        Log.d(TAG, String.format("Click [%d, %d]", cursorLayout.x, cursorLayout.y));
        AccessibilityNodeInfo nodeInfo = getRootInActiveWindow();
        if (nodeInfo == null) return;
        AccessibilityNodeInfo nearestNodeToMouse = findSmallestNodeAtPoint(nodeInfo, cursorLayout.x, cursorLayout.y + 50);
        if (nearestNodeToMouse != null) {
            logNodeHierachy(nearestNodeToMouse, 0);
            nearestNodeToMouse.performAction(AccessibilityNodeInfo.ACTION_CLICK);
        }
        nodeInfo.recycle();
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

}
