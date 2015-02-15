/**
 * 
 */
package com.aksharspeech.aksharasr;

import com.aksharspeech.aksharasr.asr.SpeechRecoziser;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;

/**
 * @author amitkumarsah
 * 
 */
public class CyberNASR extends Activity {
	SpeechRecoziser cnSpeechRecoz = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.ly_asr);
		cnSpeechRecoz = new SpeechRecoziser(this);

	}

	public void onAsrClick(View v) {
		cnSpeechRecoz.onRecordClick();

	}

	@Override
	public void onBackPressed() {
		super.onBackPressed();
		finish();
	}

	@Override
	public void onStart() {
		super.onStart();
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
	}

	@Override
	public void onResume() {
		super.onResume();
	}

	@Override
	public void onRestart() {
		super.onRestart();
	}

}
