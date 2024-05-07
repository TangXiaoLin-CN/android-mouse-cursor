import socket
import time
from pynput.keyboard import Key, KeyCode, Listener


UDP_IP = "127.0.0.1"
UDP_PORT = 9999


#1、创建socket通信对象
conn_socket = socket.socket()
 
while True:
    print("尝试连接到服务器")
    try:
        #2、使用正确的ip和端口去链接服务器
        conn_socket.connect((UDP_IP,UDP_PORT))

        #3、客户端与服务器端进行通信
        # 给socket服务器发送信息
        conn_socket.send(b'client connect')
        print("尝试第一次握手")

        #4、接收服务器的响应(服务器回复的消息)
        recvData = conn_socket.recv(1024).decode('utf-8')
        print(recvData,recvData == "successed")
        print(recvData.strip())

        if not recvData or recvData.strip() != "successed":
            print("连接失败，等待重连")
            time.sleep(3)
            continue
        break
    except:
        print("连接失败！")
        time.sleep(3)
        continue

print("连接成功！")

print("等待按键输入：")
def on_press(key):
    if key == Key.esc:
        conn_socket.send(b"end")
        print("断开连接")
    elif key == Key.space:
        conn_socket.send(b"4")
        print("发送点击")    
    elif isinstance(key, KeyCode):
        try:
            keyChar = key.char
        except:
            print("无效操作")
            return
        print("按键 {} 被按下".format(key.char))
        if keyChar == 'w':
            conn_socket.send(b"0")
        elif keyChar == 'a':
            conn_socket.send(b"2")
        elif keyChar == 's':
            conn_socket.send(b"1")
        elif keyChar == 'd':
            conn_socket.send(b"3")
        elif key.char == 'h':
            conn_socket.send(b"hide")
            print("隐藏鼠标")
        elif key.char == 'u':
            conn_socket.send(b"show")
            print("显示鼠标")    
        else:
            print("无效操作")

with Listener(on_press=on_press) as listener:
    listener.join()


#包名:
#com.chetbox.mousecursor
#启动应用：
#adb shell am start com.chetbox.mousecursor/.MainActivity

#关闭应用：
#adb shell am force-stop com.chetbox.mousecursor

#检查是否运行:
#adb shell 'ps | findstr com.chetbox.mousecursor'