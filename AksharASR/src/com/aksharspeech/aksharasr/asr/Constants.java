/**
 * 
 */
package com.aksharspeech.aksharasr.asr;

import android.media.AudioFormat;

/**
 * Its has all static constants.
 * 
 * This is work as config class
 * 
 * @author amitkumarsah
 * 
 */
public class Constants {
	public static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
	public static final int RECORDER_BPP = 16;
	public static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
	public static final int RECORDER_SAMPLERATE = 16000;
	// 2 bytes in 16bit format
	public static final int BytesPerElement = 2;
	public static final float GAIN = 80;
	public static final long Max_Pause_MillSeconds = 6000;
	public static final long UI_Reset_Timer = 30;
	public static final short Threshold_Limit = 15000;
	public static final String AUDIO_RECORDER_FOLDER = "/AksharRecorder/ASR";
	public static final String AUDIO_RECORDER_TEMP_FILE = "record_temp.raw";
	// public static final String mBaseURL = "http://msg2voice.com/ASR/";
	public static final String mBaseURL = "http://23.23.157.102/ASR/";
	public static final String mUploadURL = mBaseURL + "asrapi.php";
	public static final String mDecodeURL = mBaseURL + "decode.php";
	public static final String Tag = "Akshar_ASR";
	public static final String ITEM_NOT_FOUND="_%#NOT FOUND#%_";

}
