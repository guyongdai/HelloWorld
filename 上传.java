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
				System.out.println("ִ��Handler");
				String sg = (String) msg.obj;
				aryListTask.add(sg);
				aryAdapter.notifyDataSetChanged();
				mListView = (ListView) findViewById(R.id.MyListView2);
				mListView.setAdapter(aryAdapter);
				break;
			case CONFIRM_RESPONSE:
				showDialog(CONFIRM);// �Է��ֻ��Ѿ�ͬ��Connect���󣬵����Ƿ����ڴ������ݵ�ѯ�ʶԻ���
				break;
			case CANCEL_RESPONSE:
				showDialog(CANCEL);// �Է��ֻ��Ѿ��ܾ�Connect���󣬵������ܾ�����ʾ�Ի���
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

	/************** ��ʾ�Ի��� ***************/
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case CONFIRM:
			return new AlertDialog.Builder(WifiUserList.this).setTitle("��ܰ��ʾ")
					.setMessage("���ȷ���������ݸ��Է��ֻ�" + OpponentMobileName + "��")
					.setIcon(android.R.drawable.ic_dialog_info)
					.setPositiveButton("ȷ��", new OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {

							Intent intent = new Intent(WifiUserList.this,
									choosecontactlocal.class);
							intent.putExtra("OtherIP", OpponentMobileIP);
							startActivity(intent);
						}
					}).create();
		case CANCEL:
			return new AlertDialog.Builder(WifiUserList.this).setTitle("��ܰ��ʾ")
					.setMessage("�Է��û��Ѿ�ȡ����������������").setIcon(
							android.R.drawable.ic_dialog_info)
					.setPositiveButton("ȷ��", new OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							// onBackPressed();
							Intent intent = new Intent(WifiUserList.this,
									NewMain.class);
							intent.setFlags(intent.FLAG_ACTIVITY_CLEAR_TOP);
							startActivity(intent);
						}
					}).create();
		case Constant.CONNECT_TIMEOUT:
			return new AlertDialog.Builder(WifiUserList.this).setTitle("��ʱ����")
					.setMessage("���ѵȴ��ϳ�ʱ�䣬�Ƿ񷵻����²�����ѡ���ǡ����أ�ѡ�񡰷񡱼����ȴ���")
					.setPositiveButton("��",
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
							}).setNeutralButton("��",
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

			// ���������������߳�
			getIP = new getIpThread(ds);
			getIP.start();
		} catch (SocketException e1) {
			System.out.println("����IP��Socket����");
			e1.printStackTrace();
		} catch (Exception e) {
			System.out.println("����IP��Socket����");
			e.printStackTrace();
		}

		// ѡ���û��б�
		mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
					long arg3) {

				String name_content = mListView.getItemAtPosition(arg2)
						.toString();
				int i;
				for (i = 0; i < name_content.length()
						&& name_content.charAt(i) != ':'; i++)
					;
				// ���ǶԷ��� ip
				String ip = name_content
						.substring(i + 1, name_content.length());
				OpponentMobileIP = ip;

				myProgressDialog = ProgressDialog.show(WifiUserList.this,
						"��ܰ��ʾ", "���ڵȴ��Է���Ӧ", true);

				// m_ResponseTimeout = new ResponseTimeoutThread(hand);
				// m_ResponseTimeout.start();
				// ��������շ�����Connect������߳�
				SendConnectThread sc = new SendConnectThread(ip, username);
				sc.start();
			}
		});

		// ������ȡ��Ӧ���ݵ��߳�
		// GetResponseThread gr = new GetResponseThread();
		// gr.start();
	}

	/***
	 * ��ȡ���շ��㲥�����ݰ����߳�
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
					// ����������ݱ�
					DatagramPacket incomming = new DatagramPacket(buffer,
							buffer.length);
					System.out.println("��ʼ�ȴ���ȡ���շ��㲥�����ݰ�");
					// �������ݱ�
					datasocket.receive(incomming);
					// �����ݱ��ж�ȡ����
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
						System.out.println("���������û���" + data);
						// ����message�����߳�
						Message msg = new Message();
						msg.obj = data;
						msg.what = UPDATE;
						hand.sendMessage(msg);
					}
				}
			} catch (Exception e) {
				System.out.println("����IP��ַ��socket����");
			} finally {
				System.out.println("�رս���IP��ַ��socket");
				// �ر�datasocket
				if (datasocket != null)
					datasocket.close();
			}
		}
	}

	/**
	 * ����շ�����connect������߳�
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
				// �����ͷ�����������
				ClientSocket = new Socket(IP, WifiHelper.TCP_CONNECTPORT);
				// ��������
				outStrm = ClientSocket.getOutputStream();
				outStrm.write(OwnMobileName.getBytes());

				// ��������
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
				System.out.println("��Ӧ����temp==" + temp);
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
	 * ��ȡ���շ���Ӧ���ݵ��߳�
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

				// accept����ֱ��������
				socket = ss.accept();
				System.out.println("ResponseThread Connected...");

				Message msg2 = new Message();
				msg2.what = PROGRESSDIALOG;
				hand.sendMessage(msg2);

				// ��������
				InputStream instrm = socket.getInputStream();
				byte flag = (byte) instrm.read();
				int temp = 0x000000FF & ((int) flag);
				System.out.println("��Ӧ����temp==" + temp);
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
				System.out.println("Connect�����̳߳���");
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
	 * ����Э�鷢���ļ����߳�
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
		byte ack[] = new byte[3];// ���ack������Ϊ��ǿ��˫�����ݴ���ͬ��

		public SendFileThread(File file, byte type, String IP) {
			filename = file;
			filetype = type;
			OpponentIP = IP;
		}

		public void run() {
			try {
				// �����ͷ�����������
				ClientSocket = new Socket(OpponentIP,
						WifiHelper.TCP_TRANSMISSIONPORT);
				// ��������
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
		menu.add(0, REFLASH, 1, "ˢ��");
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
		// �ر�datasocket
		if (datasocket != null)
			datasocket.close();
		super.onBackPressed();
	}

	@Override
	protected void onStop() {
		// �ر�datasocket
		if (datasocket != null)
			datasocket.close();
		super.onStop();
	}

}
