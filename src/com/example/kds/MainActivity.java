package com.example.kds;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;

import org.json.JSONException;
import org.json.JSONObject;

import net.simonvt.menudrawer.MenuDrawer;
import net.simonvt.menudrawer.Position;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.kds.action.ExpressModel;
import com.example.kds.adapter.CompanyListAdapter;
import com.example.kds.bean.Express;
import com.google.gson.Gson;
import com.iflytek.cloud.ErrorCode;
import com.iflytek.cloud.InitListener;
import com.iflytek.cloud.RecognizerListener;
import com.iflytek.cloud.RecognizerResult;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SpeechRecognizer;
import com.iflytek.cloud.SpeechUtility;
import com.iflytek.cloud.ui.RecognizerDialog;
import com.iflytek.cloud.ui.RecognizerDialogListener;
import com.lidroid.xutils.HttpUtils;
import com.lidroid.xutils.exception.HttpException;
import com.lidroid.xutils.http.ResponseInfo;
import com.lidroid.xutils.http.callback.RequestCallBack;
import com.lidroid.xutils.http.client.HttpRequest.HttpMethod;

public class MainActivity extends BaseActivity {

	private TextView tvKd;
	CompanyListAdapter adapter;
	ArrayList<HashMap<String, Object>> companyList;
	AlertDialog cpListDialog;
	private EditText etDingdan;
	private Button btnSearch;
	private HttpUtils httpUtils;
	private String url = "http://api.ickd.cn/?id=102616&secret=16135ea51cb60246eff620f130a005bd";
	private String com = "shunfeng";
	private String nu;
	MenuDrawer bottomMenuDrawer;
	private TextView hisrory;
	private TextView about;
	private TextView exit;
	private ImageButton btnSpeach;
	protected SpeechRecognizer mIat;
	private RecognizerDialog mIatDialog;
	// ��HashMap�洢��д���
	private HashMap<String, String> mIatResults = new LinkedHashMap<String, String>();

	Handler mHandler = new Handler() {

		public void handleMessage(android.os.Message msg) {
			if (msg.what == 1) {
				dialog.dismiss();
				Toast.makeText(context,
						getResources().getString(R.string.no_new_verson_tips),
						Toast.LENGTH_LONG).show();
			}

		};
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		SysApplication.getInstance().addActivity(this);

		/**
		 * �ײ��˵�
		 */

		bottomMenuDrawer = MenuDrawer.attach(this, Position.BOTTOM);// MenuDrawer.Type.OVERLAY,
		bottomMenuDrawer.setTouchMode(MenuDrawer.TOUCH_MODE_FULLSCREEN);
		bottomMenuDrawer.setMenuView(R.layout.bottom_menu);
		bottomMenuDrawer.setContentView(R.layout.kuaidi_main);
		bottomMenuDrawer.setMenuSize(100);

		hisrory = (TextView) findViewById(R.id.history);
		about = (TextView) findViewById(R.id.about);
		exit = (TextView) findViewById(R.id.exit);
		etDingdan = (EditText) findViewById(R.id.etDingdan);
		tvKd = (TextView) findViewById(R.id.tvKD);
		btnSearch = (Button) findViewById(R.id.btnSearch);
		btnSpeach = (ImageButton) findViewById(R.id.btnSpeach);
		btnSpeach.setOnClickListener(speachOnclick);
		httpUtils = new HttpUtils();
		/**
		 * ��ʼ���������������ö���ֻ�г�ʼ����ſ���ʹ��MSC�ĸ������
		 */
		SpeechUtility
				.createUtility(context, SpeechConstant.APPID + "=562ed2a1");
		mIat = SpeechRecognizer.createRecognizer(context, mInitListener);
		mIatDialog = new RecognizerDialog(context, mInitListener);

		/**
		 * companyList��ʼ��
		 */

		companyList = new ArrayList<HashMap<String, Object>>();

		for (int i = 0; i < getResources().getStringArray(R.array.company_logo).length; i++) {
			HashMap<String, Object> map = new HashMap<String, Object>();
			int resId = getResources().getIdentifier(
					getResources().getStringArray(R.array.company_logo)[i],
					"drawable", context.getPackageName());
			map.put("logo", resId);
			map.put("name",
					getResources().getStringArray(R.array.company_name)[i]);

			companyList.add(map);
		}

		/**
		 * ���ѡ�����¼�
		 */

		tvKd.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {

				showCompanyList();

			}
		});

		/**
		 * ��ѯ��ť����¼�
		 */
		btnSearch.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {

				showProgressDialog(getResources().getString(
						R.string.msg_load_ing));
				searchDetails();
			}
		});

		/**
		 * ������ť
		 */

		hisrory.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {

				showSuccess(v);
			}
		});
		about.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {

				showSuccess(v);
			}
		});
		exit.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {

				showSuccess(v);
			}
		});

	}

	/**
	 * ������ť
	 */
	private OnClickListener speachOnclick = new OnClickListener() {

		@Override
		public void onClick(View arg0) {
			mIatResults.clear();
			setParam();
			mIatDialog.setListener(mRecognizerDialogListener);
			mIatDialog.show();
		}
	};
	/**
	 * ��ʼ����������
	 */
	private InitListener mInitListener = new InitListener() {

		@Override
		public void onInit(int code) {
			if (code != ErrorCode.SUCCESS) {
				showToast("��ʼ��ʧ�ܣ������룺" + code);
			}
		}
	};

	private void printResult(RecognizerResult results) {
		String text = JsonParser.parseIatResult(results.getResultString());

		String sn = null;
		// ��ȡjson����е�sn�ֶ�
		try {
			JSONObject resultJson = new JSONObject(results.getResultString());
			sn = resultJson.optString("sn");
		} catch (JSONException e) {
			e.printStackTrace();
		}

		mIatResults.put(sn, text);

		StringBuffer resultBuffer = new StringBuffer();
		for (String key : mIatResults.keySet()) {
			resultBuffer.append(mIatResults.get(key));
		}

		etDingdan.setText(resultBuffer.toString());
		etDingdan.setSelection(resultBuffer.length());
	}

	/**
	 * ��дUI������
	 */
	private RecognizerDialogListener mRecognizerDialogListener = new RecognizerDialogListener() {
		public void onResult(RecognizerResult results, boolean isLast) {
			printResult(results);
		}

		/**
		 * ʶ��ص�����.
		 */
		public void onError(SpeechError error) {
			showToast("�޷�ʶ��");
		}

	};

	public void setParam() {
		// ��ղ���
		mIat.setParameter(SpeechConstant.PARAMS, null);

		// ������д����
		mIat.setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_CLOUD);
		// ���÷��ؽ����ʽ
		mIat.setParameter(SpeechConstant.RESULT_TYPE, "json");

		// ��������
		mIat.setParameter(SpeechConstant.LANGUAGE, "zh_cn");

		// ��������ǰ�˵�:������ʱʱ�䣬���û��೤ʱ�䲻˵��������ʱ����
		mIat.setParameter(SpeechConstant.VAD_BOS, "4000");

		// ����������˵�:��˵㾲�����ʱ�䣬���û�ֹͣ˵���೤ʱ���ڼ���Ϊ�������룬 �Զ�ֹͣ¼��
		mIat.setParameter(SpeechConstant.VAD_EOS, "1000");

		// ���ñ�����,����Ϊ"0"���ؽ���ޱ��,����Ϊ"1"���ؽ���б��
		mIat.setParameter(SpeechConstant.ASR_PTT, "0");

		// ������Ƶ����·����������Ƶ��ʽ֧��pcm��wav������·��Ϊsd����ע��WRITE_EXTERNAL_STORAGEȨ��
		// ע��AUDIO_FORMAT���������Ҫ���°汾������Ч
		mIat.setParameter(SpeechConstant.AUDIO_FORMAT, "wav");
		mIat.setParameter(SpeechConstant.ASR_AUDIO_PATH,
				Environment.getExternalStorageDirectory() + "/msc/iat.wav");
	}

	/**
	 * �˵���ť�ĵ���¼�
	 * 
	 * @param v
	 *            ����İ�ť
	 */

	public void showSuccess(View v) {

		bottomMenuDrawer.setActiveView(v);
		switch (v.getId()) {
		case R.id.history:
			startActivity(new Intent(MainActivity.this, HistoryActivity.class));
			break;

		case R.id.exit:
			SysApplication.getInstance().exit();
			break;

		case R.id.about:
			showProgressDialog(getResources().getString(
					R.string.check_update_tips));

			Thread t = new Thread(new Runnable() {

				@Override
				public void run() {

					try {

						Thread.sleep(3000);// ������ʾ3���ȡ��ProgressDialog

						mHandler.sendEmptyMessage(1);
					} catch (InterruptedException e) {
						e.printStackTrace();

					}

				}

			});

			t.start();
			break;

		default:
			break;
		}
		// bottomMenuDrawer.toggleMenu();
		bottomMenuDrawer.closeMenu(true);

	}

	/**
	 * ��ѯ��ť�ĵ���¼�
	 */

	protected void searchDetails() {

		nu = etDingdan.getText().toString().trim();
		url = url + "&com=" + com + "&nu=" + nu;

		httpUtils.send(HttpMethod.GET, url, new RequestCallBack<String>() {

			@Override
			public void onFailure(HttpException arg0, String arg1) {
				Log.i("Gson", arg1);
				dialog.dismiss();
			}

			@Override
			public void onSuccess(ResponseInfo<String> arg0) {

				String str = arg0.result;
				Express express = new Gson().fromJson(str, Express.class);
				dialog.dismiss();
				if (express.status == 0) {

					Toast.makeText(MainActivity.this, express.message,
							Toast.LENGTH_LONG).show();
					etDingdan.setText("");
				} else {
					Intent intent = new Intent(MainActivity.this,
							ResultActivity.class);
					intent.putExtra("json", str);
					startActivity(intent);
					new ExpressModel(context).insert(express, str);
				}

			}
		});
	}

	/**
	 * AlerDialog
	 */

	protected void showCompanyList() {

		Builder builder = new Builder(context);
		cpListDialog = builder.create();
		View view = LayoutInflater.from(context).inflate(R.layout.company_list,
				null);
		ListView list = (ListView) view.findViewById(R.id.list);
		adapter = new CompanyListAdapter(companyList, context);
		list.setAdapter(adapter);

		cpListDialog.setView(view);
		cpListDialog.setCancelable(true);
		cpListDialog.show();

		list.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {

				tvKd.setText((CharSequence) ((HashMap<String, Object>) adapter
						.getItem(position)).get("name"));
				com = company_code[position];
				cpListDialog.dismiss();
			}
		});
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		mIat.cancel();
		mIat.destroy();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
}
