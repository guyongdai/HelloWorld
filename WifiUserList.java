package pip.Wifi;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import wyf.wpf.Constant;
import wyf.wpf.NewMain;
import wyf.wpf.R;
import wyf.wpf.choosecontactlocal;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class WifiUserList extends Activity {

	private static final int UPDATE = 100;
	private static final int CONFIRM_RESPONSE = 101;
	private static final int CANCEL_RESPONSE = 102;
	private static final int CONFIRM = 103;
	private static final int CANCEL = 104;
	private static final int REFLASH = 105;
	private static final int PROGRESSDIALOG = 106;

	private ListView mListView;
	private ArrayAdapter<String> aryAdapter;
	private ArrayList<String> aryListTask = new ArrayList<String>();
	private DatagramSocket datasocket = null;
	private getIpThread getIP = null;
	private String username = null;
	private String OpponentMobileName = null;
	private String OpponentMobileIP = null;
	private ProgressDialog myProgressDialog = null;

	private ResponseTimeoutThread m_ResponseTimeout = null;

	Handler hand = new Handler() {
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case UPDATE:
				System.out.println("执行Handler");
				String sg = (String) msg.obj;
				aryListTask.add(sg);
				aryAdapter.notifyDataSetChanged();
				mListView = (ListView) findViewById(R.id.MyListView2);
				mListView.setAdapter(aryAdapter);
				break;
			case CONFIRM_RESPONSE:
				showDialog(CONFIRM);// 对方手机已经同意Connect请求，弹出是否现在传递数据的询问对话框
				break;
			case CANCEL_RESPONSE:
				showDialog(CANCEL);// 对方手机已经拒绝Connect请求，弹出被拒绝的提示对话框
				break;
			case PROGRESSDIALOG:
				if (myProgressDialog.isShowing()) {
					myProgressDialog.dismiss();
					myProgressDialog = null;
				}
				break;
			case Constant.CONNECT_TIMEOUT:
				showDialog(Constant.CONNECT_TIMEOUT);
				break;
			}
		}
	};

	/************** 提示对话框 ***************/
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case CONFIRM:
			return new AlertDialog.Builder(WifiUserList.this).setTitle("温馨提示")
					.setMessage("点击确定传递数据给对方手机" + OpponentMobileName + "！")
					.setIcon(android.R.drawable.ic_dialog_info)
					.setPositiveButton("确认", new OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {

							Intent intent = new Intent(WifiUserList.this,
									choosecontactlocal.class);
							intent.putExtra("OtherIP", OpponentMobileIP);
							startActivity(intent);
						}
					}).create();
		case CANCEL:
			return new AlertDialog.Builder(WifiUserList.this).setTitle("温馨提示")
					.setMessage("对方用户已经取消了您的链接请求").setIcon(
							android.R.drawable.ic_dialog_info)
					.setPositiveButton("确认", new OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							// onBackPressed();
							Intent intent = new Intent(WifiUserList.this,
									NewMain.class);
							intent.setFlags(intent.FLAG_ACTIVITY_CLEAR_TOP);
							startActivity(intent);
						}
					}).create();
		case Constant.CONNECT_TIMEOUT:
			return new AlertDialog.Builder(WifiUserList.this).setTitle("超时操作")
					.setMessage("您已等待较长时间，是否返回重新操作？选择“是”返回，选择“否”继续等待。")
					.setPositiveButton("是",
							new DialogInterface.OnClickListener() {

								public void onClick(DialogInterface dialog,
										int which) {
									if (myProgressDialog.isShowing()) {
										myProgressDialog.dismiss();
										myProgressDialog = null;
									}
									dialog.dismiss();
									onBackPressed();
								}
							}).setNeutralButton("否",
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int witch) {
									dialog.dismiss();
									m_ResponseTimeout = new ResponseTimeoutThread(
											hand);
									m_ResponseTimeout.start();
								}
							}).create();
		default:
			return null;
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.wifiuserlist);

		mListView = (ListView) findViewById(R.id.MyListView2);

		aryAdapter = new ArrayAdapter<String>(WifiUserList.this,
				android.R.layout.simple_list_item_1, aryListTask);
		mListView.setAdapter(aryAdapter);

		Intent intent = getIntent();
		username = intent.getStringExtra("username");

		DatagramSocket ds;
		try {
			ds = new DatagramSocket(WifiHelper.UDP_BOADCASTPORT);
			ds.setBroadcast(true);

			// 创建并启动接收线程
			getIP = new getIpThread(ds);
			getIP.start();
		} catch (SocketException e1) {
			System.out.println("接收IP的Socket错了");
			e1.printStackTrace();
		} catch (Exception e) {
			System.out.println("接收IP的Socket错了");
			e.printStackTrace();
		}

		// 选择用户列表
		mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
					long arg3) {

				String name_content = mListView.getItemAtPosition(arg2)
						.toString();
				int i;
				for (i = 0; i < name_content.length()
						&& name_content.charAt(i) != ':'; i++)
					;
				// 这是对方的 ip
				String ip = name_content
						.substring(i + 1, name_content.length());
				OpponentMobileIP = ip;

				myProgressDialog = ProgressDialog.show(WifiUserList.this,
						"温馨提示", "正在等待对方响应", true);

				// m_ResponseTimeout = new ResponseTimeoutThread(hand);
				// m_ResponseTimeout.start();
				// 启动向接收方发送Connect请求的线程
				SendConnectThread sc = new SendConnectThread(ip, username);
				sc.start();
			}
		});

		// 启动获取响应数据的线程
		// GetResponseThread gr = new GetResponseThread();
		// gr.start();
	}

	/***
	 * 获取接收方广播的数据包的线程
	 * 
	 * @author XijieChen
	 * 
	 */
	class getIpThread extends Thread {

		public getIpThread(DatagramSocket dsocket) throws Exception {
			datasocket = dsocket;
		}

		public void run() {

			byte[] buffer = new byte[WifiHelper.NAME_BUFFER_LENGTH];
			try {
				while (true) {
					Thread.sleep(2000);
					// 构造接收数据报
					DatagramPacket incomming = new DatagramPacket(buffer,
							buffer.length);
					System.out.println("开始等待获取接收方广播的数据包");
					// 接收数据报
					datasocket.receive(incomming);
					// 从数据报中读取数据
					String ss = incomming.getAddress().toString();
					int ii = ss.length();
					String sa = ss.substring(1, ii);

					String data = new String(incomming.getData(), 0, incomming
							.getLength());
					OpponentMobileName = data;
					data = data + ":" + sa;

					int flag = 0;
					for (int i = 0; i < aryListTask.size(); i++) {
						if (aryListTask.get(i).equals(data)) {
							flag = 1;
							break;
						}
					}
					if (flag == 0) {
						System.out.println("搜索到的用户：" + data);
						// 发送message给主线程
						Message msg = new Message();
						msg.obj = data;
						msg.what = UPDATE;
						hand.sendMessage(msg);
					}
				}
			} catch (Exception e) {
				System.out.println("接收IP地址的socket出错");
			} finally {
				System.out.println("关闭接收IP地址的socket");
				// 关闭datasocket
				if (datasocket != null)
					datasocket.close();
			}
		}
	}

	/**
	 * 向接收方发送connect请求的线程
	 * 
	 * @author XijieChen
	 * 
	 */
	class SendConnectThread extends Thread {

		private String IP = null;
		private OutputStream outStrm = null;
		private Socket ClientSocket = null;
		private String OwnMobileName = null;
		private InputStream instrm = null;

		public SendConnectThread(String ip, String name) {
			IP = ip;
			OwnMobileName = name;
		}

		public void run() {
			try {
				// 建立和服务器的连接
				ClientSocket = new Socket(IP, WifiHelper.TCP_CONNECTPORT);
				// 阻塞运行
				outStrm = ClientSocket.getOutputStream();
				outStrm.write(OwnMobileName.getBytes());

				// 阻塞运行
				instrm = ClientSocket.getInputStream();
				byte flag = (byte) instrm.read();

				Message msg2 = new Message();
				msg2.what = PROGRESSDIALOG;
				hand.sendMessage(msg2);

				try {
					sleep(500);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				int temp = 0x000000FF & ((int) flag);
				System.out.println("响应数据temp==" + temp);
				if (temp == 0) {
					Message msg = new Message();
					msg.what = CONFIRM_RESPONSE;
					hand.sendMessage(msg);
				} else if (temp == 1) {
					Message msg = new Message();
					msg.what = CANCEL_RESPONSE;
					hand.sendMessage(msg);
				}
				ClientSocket.close();
				outStrm.close();
				instrm.close();

			} catch (IOException e) {
				try {
					outStrm.close();
				} catch (Exception e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}

	/***
	 * 获取接收方响应数据的线程
	 * 
	 * @author XijieChen
	 * 
	 */
	class GetResponseThread extends Thread {

		ServerSocket ss = null;
		Socket socket = null;

		public void run() {

			try {
				ss = new ServerSocket(WifiHelper.TCP_CONNECTPORT);
				System.out.println("ResponseListening...");

				// accept阻塞直到被连接
				socket = ss.accept();
				System.out.println("ResponseThread Connected...");

				Message msg2 = new Message();
				msg2.what = PROGRESSDIALOG;
				hand.sendMessage(msg2);

				// 阻塞运行
				InputStream instrm = socket.getInputStream();
				byte flag = (byte) instrm.read();
				int temp = 0x000000FF & ((int) flag);
				System.out.println("响应数据temp==" + temp);
				if (temp == 0) {
					Message msg = new Message();
					msg.what = CONFIRM_RESPONSE;
					hand.sendMessage(msg);
				} else if (temp == 1) {
					Message msg = new Message();
					msg.what = CANCEL_RESPONSE;
					hand.sendMessage(msg);
				}

				socket.close();
				ss.close();

			} catch (Exception e) {
				System.out.println("Connect请求线程出错");
				e.printStackTrace();
			}
		}
	}

	private class ResponseTimeoutThread extends Thread {
		Handler mhandler = null;

		ResponseTimeoutThread(Handler mhandler) {
			this.mhandler = mhandler;
		}

		public void run() {
			try {
				Thread.sleep(WifiHelper.timecheck);
				mhandler.obtainMessage(Constant.CONNECT_TIMEOUT).sendToTarget();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
			}
		}
	}

	/**
	 * 按照协议发送文件的线程
	 * 
	 * @author XijieChen
	 */
	public static class SendFileThread extends Thread {

		private Socket ClientSocket = null;
		private OutputStream outStrm = null;
		private FileInputStream fis = null;
		private InputStream instream = null;

		File filename = null;
		byte filetype = 0;
		String OpponentIP = null;
		byte ack[] = new byte[3];// 这个ack数组是为了强制双方数据传递同步

		public SendFileThread(File file, byte type, String IP) {
			filename = file;
			filetype = type;
			OpponentIP = IP;
		}

		public void run() {
			try {
				// 建立和服务器的连接
				ClientSocket = new Socket(OpponentIP,
						WifiHelper.TCP_TRANSMISSIONPORT);
				// 阻塞运行
				outStrm = ClientSocket.getOutputStream();
				fis = new FileInputStream(filename);

				outStrm.write(filetype & 0x00ff);
				outStrm.flush();

				instream = ClientSocket.getInputStream();
				instream.read(ack, 0, 3);
				int fileLength = (int) filename.length();
				byte highhigh = (byte) (fileLength >> 24);
				byte highlow = (byte) (fileLength >> 16);
				byte lowhigh = (byte) (fileLength >> 8);
				byte lowlow = (byte) fileLength;
				byte[] lengthbyte = new byte[4];
				lengthbyte[3] = highhigh;
				lengthbyte[2] = highlow;
				lengthbyte[1] = lowhigh;
				lengthbyte[0] = lowlow;
				// outStrm.write(highhigh & 0x000000ff);
				// outStrm.write(highlow & 0x000000ff);
				// outStrm.write(lowhigh & 0x000000ff);
				// outStrm.write(lowlow & 0x000000ff);
				outStrm.write(lengthbyte);
				instream.read(ack, 0, 3);
				outStrm.flush();
				System.out.println(fileLength + " " + highhigh + " " + highlow
						+ " " + lowhigh + " " + lowlow);
				int left = fileLength;
				int sendonce = 0;
				byte[] Buffer = new byte[WifiHelper.WIFI_BUFFER_LENGTH];
				while (left > 0) {
					sendonce = fis.read(Buffer);
					System.out.println("left" + left);
					outStrm.write(Buffer, 0, sendonce);
					outStrm.flush();
					instream.read(ack, 0, 3);
					left = left - sendonce;
				}
				ClientSocket.close();
				outStrm.close();
				instream.close();
				fis.close();
			} catch (IOException e) {
				try {
					ClientSocket.close();
					outStrm.close();
					instream.close();
					fis.close();
				} catch (Exception e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(0, REFLASH, 1, "刷新");
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// TODO Auto-generated method stub
		int id = item.getItemId();
		switch (id) {
		case REFLASH:

			break;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onBackPressed() {
		// 关闭datasocket
		if (datasocket != null)
			datasocket.close();
		super.onBackPressed();
	}

	@Override
	protected void onStop() {
		// 关闭datasocket
		if (datasocket != null)
			datasocket.close();
		super.onStop();
	}

}
