package com.chetbox.mousecursor;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

public class MouseAccessibilityService extends AccessibilityService {

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
                LayoutParams.FLAG_DISMISS_KEYGUARD | LayoutParams.FLAG_NOT_FOCUSABLE | LayoutParams.FLAG_NOT_TOUCHABLE,
                PixelFormat.TRANSLUCENT);
        cursorLayout.gravity = Gravity.TOP | Gravity.LEFT;
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
                        byte[] buffer = new byte[1024];
                        int len = is.read(buffer);
                        System.out.println("-->" + new String(buffer,0,len));

                        //回复连接成功
                        os.write("successed".getBytes());
                        os.flush();

                        while(true)
                        {
                            try{
                                len = is.read(buffer);
                                if (len == -1)
                                {
                                    break;
                                }
                                String msg = new String(buffer,0,len).trim();
                                System.out.println("-->" + msg);
                                os.write(" ".getBytes()); //表示收到回复
                                os.flush();
                                try{
                                    if(msg == "end"){
                                        break;
                                    }
                                    if(msg == "hide")
                                    {
                                        new Handler(getMainLooper()).post(new Runnable() {
                                            @Override
                                            public void run() {
                                                hideMouse();
                                            }
                                        });
                                    }
                                    final int event = Integer.parseInt(msg);
                                    new Handler(getMainLooper()).post(new Runnable() {
                                        @Override
                                        public void run() {
                                            onMouseMove(new MouseEvent(event));
                                        }
                                    });
                                } catch (NumberFormatException e)
                                {
                                    System.out.println("invalid input content!");
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

    public void onMouseMove(MouseEvent event) {
        switch (event.direction) {
            case MouseEvent.MOVE_LEFT:
                cursorLayout.x -= 10;
                break;
            case MouseEvent.MOVE_RIGHT:
                cursorLayout.x += 10;
                break;
            case MouseEvent.MOVE_UP:
                cursorLayout.y -= 10;
                break;
            case MouseEvent.MOVE_DOWN:
                cursorLayout.y += 10;
                break;
            case MouseEvent.LEFT_CLICK:
                click();
                break;
            default:
                break;
        }
        windowManager.updateViewLayout(cursorView, cursorLayout);
    }

    public  void hideMouse()
    {
        System.out.println("hide");
        cursorLayout.x = -999;
        cursorLayout.y = -999;
        windowManager.updateViewLayout(cursorView, cursorLayout);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        windowManager.addView(cursorView, cursorLayout);
        return START_STICKY;
    }

}
