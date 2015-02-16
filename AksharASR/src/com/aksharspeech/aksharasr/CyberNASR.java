/**
 * 
 */
package com.aksharspeech.aksharasr;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;

import com.aksharspeech.aksharasr.asr.AksharASR;

/**
 * @author amitkumarsah
 * 
 */
public class CyberNASR extends Activity {
	AksharASR cnSpeechRecoz = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.ly_asr);
		cnSpeechRecoz = new AksharASR(this);

	}

	public void onAsrClick(View v) {
		cnSpeechRecoz.onRecordClick();

	}
	@Override
	protected void onPause(){
		super.onPause();
		if(cnSpeechRecoz!=null){
			cnSpeechRecoz.onPause();
		}
	}

	@Override
	public void onBackPressed() {
		super.onBackPressed();
		finish();
	}

	@Override
	public void onStart() {
		super.onStart();
		if(cnSpeechRecoz!=null)
		cnSpeechRecoz.onStart();
		// cnSpeechRecoz = new RemoteASR(this);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (cnSpeechRecoz != null)
			cnSpeechRecoz.onDestory();
		cnSpeechRecoz = null;
	}

	@Override
	protected void onStop() {
		super.onStop();
		if (cnSpeechRecoz != null)
			cnSpeechRecoz.onDestory();
		cnSpeechRecoz = null;
	}

	@Override
	public void onResume() {
		super.onResume();
	}

	@Override
	public void onRestart() {
		super.onRestart();
		if(cnSpeechRecoz==null){
			cnSpeechRecoz = new AksharASR(this);
		}
	}

}
