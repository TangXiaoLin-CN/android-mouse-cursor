package com.chetbox.mousecursor;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.Rect;
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
    private View cursorImageView;  // 缓存光标ImageView，避免重复查找
    private LayoutParams cursorLayout;
    private WindowManager windowManager;
    private int width = 0;
    private int height = 0;
    private boolean isLandscape = false;
    private boolean isHasCutHead = false;
    private int cutHeadSize = 0;
    private boolean isCursorVisible = false;
    private boolean isCursorAdded = false;
    private Handler mainHandler;

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

        // 预先创建Handler，避免每次都创建
        mainHandler = new Handler(getMainLooper());

        StatusBarHelper.getInstance().setStatusBarHelperViewLayoutListener(new StatusBarHelper.StatusBarHelperViewLayoutListener() {
            @Override
            public void updateStatusBarHeightWhenGlobalLayout() {
                Point cutSize = StatusBarHelper.getInstance().cutHeadSize();
                Point size = StatusBarHelper.getInstance().getScreenSize();
                isLandscape = cutSize.x > 0 ? true : false;
                cutHeadSize = cutSize.x != 0 ? cutSize.x :cutSize.y;
                width = size.x;
                height = size.y;
                isHasCutHead = isHasCutHead();
            }
        });
        StatusBarHelper.getInstance().addStatusBarHelperView(getBaseContext());

        cursorView = View.inflate(getBaseContext(), R.layout.cursor, null);
        cursorImageView = cursorView.findViewById(R.id.cursor);  // 缓存引用
        cursorLayout = new LayoutParams(
                LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT,
                android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O
                        ? LayoutParams.TYPE_APPLICATION_OVERLAY
                        : LayoutParams.TYPE_SYSTEM_ERROR,
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                        | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                        | WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS
                        | WindowManager.LayoutParams.FLAG_FULLSCREEN
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                        | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                PixelFormat.TRANSLUCENT);
        cursorLayout.gravity = Gravity.TOP | Gravity.LEFT;
        cursorLayout.alpha = 1.0f;  // 完全不透明
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
                                os.write(" ".getBytes()); //表示收到回复
                                os.flush();
                                if(msg.equals("end")){
                                    // 收到end命令，停止服务
                                    mainHandler.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            stopSelf();
                                        }
                                    });
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
                                        final int screenX = (int)(((float)xLocal / xSize) * screen.x);
                                        final int screenY = (int)(((float)yLocal / ySize) * screen.y);

                                        // 使用预先创建的Handler，减少对象创建
                                        mainHandler.post(new Runnable() {
                                            @Override
                                            public void run() {
                                                onMouseMove(new MouseEvent(action), screenX, screenY);
                                            }
                                        });
                                    } catch (NumberFormatException e)
                                    {
                                        System.out.println("invalid input content!");
                                    }
                                }
                            }catch (SocketException e)
                            {
                                System.out.println("Socket closed");
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

        if (windowManager != null && cursorView != null && isCursorAdded) {
            try {
                windowManager.removeView(cursorView);
                isCursorAdded = false;
            } catch (Exception e) {
                // 忽略异常
            }
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
        // 更新位置（无论是否可见都更新）
        cursorLayout.x = x;
        cursorLayout.y = y;

        // 始终更新布局位置
        if (isCursorAdded) {
            try {
                windowManager.updateViewLayout(cursorView, cursorLayout);
            } catch (Exception e) {
                // 忽略异常
            }
        }

        // 只切换可见性，不更新布局（使用缓存的引用）
        boolean shouldShow = (event.type == MouseEvent.SHOW);
        if (shouldShow != isCursorVisible) {
            cursorImageView.setVisibility(shouldShow ? View.VISIBLE : View.INVISIBLE);
            isCursorVisible = shouldShow;
        }
    }

    public void setCursor(boolean state)
    {
        cursorImageView.setVisibility(state ? View.VISIBLE : View.INVISIBLE);
        isCursorVisible = state;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!isCursorAdded && windowManager != null && cursorView != null) {
            try {
                windowManager.addView(cursorView, cursorLayout);
                isCursorAdded = true;
            } catch (Exception e) {
                // 忽略异常
            }
        }
        return START_STICKY;
    }

    public Point GetScreenInfo()
    {
        return new Point(width,height);
    }

    public boolean isHasCutHead()
    {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        WindowManager windowManager = (WindowManager) getBaseContext().getSystemService(getBaseContext().WINDOW_SERVICE);
        windowManager.getDefaultDisplay().getMetrics(displayMetrics);

        int pwidth = displayMetrics.widthPixels; // 获取屏幕宽度
        int pheight = displayMetrics.heightPixels; // 获取屏幕高度

        return pwidth != width || pheight != height;
    }

}
