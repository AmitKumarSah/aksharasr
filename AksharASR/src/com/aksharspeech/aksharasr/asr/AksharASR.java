/**
 * 
 */
package com.aksharspeech.aksharasr.asr;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.StringTokenizer;

import android.app.Activity;
import android.content.Context;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Environment;
import android.os.Looper;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.aksharspeech.aksharasr.R;
import com.aksharspeech.aksharasr.net.FileUploader;

/**
 * @author amitkumarsah
 * 
 */
public class AksharASR {

	private static float mGain = 80;
	/**
	 * It denotes the RECORD button is in normal state
	 */
	private static final int RECORD_BUTTON_ON = 0;
	/**
	 * It denotes the Record button show Stop image
	 */
	private static final int RECORD_BUTTON_STOP = 1;
	private static int sIDRecordButton = R.drawable.ic_action_rec;
	private static int sIDStopButton = R.drawable.ic_action_rec_sel;
	/**
	 * Basic Variables
	 */
	private int bufferSize = 0;
	private Activity mAct = null;
	private String mCurrentAudioFileName = null;

	private FileUploader mFileUpload = null;
	private boolean mIsDecoding = false;
	/**
	 * Boolean Flags to controls the operations
	 * 
	 */
	private boolean mIsRecording = false;
	protected long mPauseTime = 0;
	private String mTempAudioFile = null;

	private long mTotalRead = 0;
	protected AudioRecord recorder = null;

	protected Thread recordingThread = null;

	/**
	 * Default Constructor.
	 * 
	 * @param activity
	 *            Must pass the current activity refernce.
	 */
	public AksharASR(Activity activity) {
		this.mAct = activity;
		this.mFileUpload = new FileUploader();
		createRecorder();
		mGain = Constants.GAIN / 100;
	}

	private boolean cancelWaveRecord() {
		Log.i(Constants.Tag, "cancelRecord called");
		if (null != recorder) {
			mIsRecording = false;
			recorder.stop();
			recorder = null;
			recordingThread = null;
		}
		deleteTempFile();
		return true;
	}

	private void copyWaveFile(String inFilename, String outFilename) {
		Log.i(Constants.Tag, "copyWaveFile_infile=" + inFilename);
		Log.i(Constants.Tag, "copyWaveofile_outfile=" + outFilename);

		FileInputStream in = null;
		FileOutputStream out = null;
		long totalAudioLen = 0;
		long totalDataLen = totalAudioLen + 36;
		long longSampleRate = Constants.RECORDER_SAMPLERATE;
		int channels = 1; // AudioFormat.CHANNEL_IN_MONO 1 streo =2

		long byteRate = Constants.RECORDER_BPP * Constants.RECORDER_SAMPLERATE
				* channels / 8;

		byte[] data = new byte[bufferSize];

		try {
			in = new FileInputStream(inFilename);
			out = new FileOutputStream(outFilename);
			totalAudioLen = in.getChannel().size();
			totalDataLen = totalAudioLen + 36;

			Log.i(Constants.Tag, "CopyWaveFle_File size: " + totalDataLen);

			WriteWaveFileHeader(out, totalAudioLen, totalDataLen,
					longSampleRate, channels, byteRate);

			while (in.read(data) != -1) {
				out.write(data);
			}

			in.close();
			out.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Create the audio recorder. It is used here , so you no need to create
	 * recorder and dump it again. <br>
	 * It is make the asr efficent and optimized.
	 * 
	 */
	private void createRecorder() {
		initWaveRecorder();
		recorder = new AudioRecord(
				MediaRecorder.AudioSource.VOICE_COMMUNICATION,
				Constants.RECORDER_SAMPLERATE, Constants.RECORDER_CHANNELS,
				Constants.RECORDER_AUDIO_ENCODING, bufferSize
						* Constants.BytesPerElement);
	}

	/**
	 * delete the temp audio file
	 */
	private void deleteTempFile() {
		File file = new File(mTempAudioFile);
		if (file.delete())
			Log.i(Constants.Tag, "File got deleted");
		else
			Log.i(Constants.Tag, "File deletion failed");

	}

	private boolean destoryWaveRecord() {
		Log.i(Constants.Tag, "cancelRecord called");
		if (null != recorder) {
			mIsRecording = false;
			if (recorder.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING)
				recorder.stop();
			recorder.release();
			recorder = null;
			recordingThread = null;
		}
		deleteTempFile();
		return true;
	}

	/**
	 * It will change or show the message and update on UI Thread.
	 * 
	 * @param msg
	 */
	private void doOnUI(final String msg) {
		mAct.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				TextView tv = (TextView) mAct.findViewById(R.id.asrTVTextData);
				tv.setText(msg);
				Toast.makeText(mAct, msg, Toast.LENGTH_SHORT).show();

			}
		});
	}

	/**
	 * set the text data into TextView
	 * 
	 * @param msg
	 */
	private void doOnUIText(final String msg) {
		mAct.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				TextView tv = (TextView) mAct.findViewById(R.id.asrTVTextData);
				tv.setText(msg);

			}
		});
	}

	/**
	 * Its handler for Audio Decoder. It will show on the UI
	 * 
	 * @param response
	 */
	private void doPostData(String response) {
		Log.i("doPostData", response);
		// String a=response.substring(response.indexOf("#RESULT=")+8);
		// Log.i("AMIT", "a=@"+a);
		//
		// StringTokenizer tok = new StringTokenizer(response, "#");
		//
		// tok.nextToken();

		doOnUI("You asked for " + response.substring(response.indexOf("#RESULT=")+8) + ".");
		mIsDecoding = false;
		resetText();

	}

	// File Uploading section
	/**
	 * After the Error Message is over.
	 */
	private void doPostError() {
		mAct.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				mIsDecoding = false;
				TextView tv = (TextView) mAct.findViewById(R.id.asrTVTextData);
				tv.setText("Speak");

			}
		});

	}

	/**
	 * Its handler for Audio Uploaded to decode the audio file
	 * 
	 * @param response
	 *            Message received from server
	 */
	@SuppressWarnings("unused")
	private void doPostUpload(String response) {
		Log.i("doPostUpload", response);
		Thread th = new Thread(new Runnable() {
			@Override
			public void run() {
				Looper.prepare();
				String response = mFileUpload.getPostData(Constants.mDecodeURL,
						getEMINumber(mAct));
				if (response != null && response.contains("Error")) {
					Log.e("doPostUpload_error", response);
					doTost(response);
					doPostError();
					Looper.myLooper().quit();

				} else {

					doPostData(response);
					Looper.myLooper().quit();
				}
				Looper.loop();
			}
		}, "POST_GET_DATA_THREAD");

		th.start();

	}

	/**
	 * Its handler for Audio Uploaded to decode the audio file
	 * 
	 * @param response
	 *            Message received from server
	 */
	private void doPostUpload_NEW(String response) {
		Log.i("doPostUpload_new", response);
		if (response != null && response.contains("Error")) {
			Log.e("doPostUpload_error", response);
			doTost(response);
			doPostError();

		} else {

			doPostData(response);
		}

	}

	/**
	 * Display message through toast message. It will do in UI thread
	 * 
	 * @param msg
	 */
	private void doTost(final String msg) {
		mAct.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				if (msg.contains("Error")) {
					Toast.makeText(mAct, "Error(" + msg + ")",
							Toast.LENGTH_SHORT).show();
				} else
					Toast.makeText(mAct, msg, Toast.LENGTH_SHORT).show();

			}
		});

	}

	/**
	 * It gain the audio data with the factor of x/100;
	 * 
	 * @param data
	 * @param len
	 * @param gain
	 * @return
	 */
	private byte[] gainAudio(short data[], int len) {
		for (int i = 0; i < len; i++) {
			data[i] = (short) Math
					.min((int) (data[i] * mGain), Short.MAX_VALUE);
		}
		return short2byte(data);
	}

	/**
	 * It return the IMEI Number of the user mobile handset.
	 * 
	 * @param context
	 * @return IMEI no of the current handset
	 */
	public String getEMINumber(Context context) {
		TelephonyManager telephonyManager = (TelephonyManager) context
				.getSystemService(Context.TELEPHONY_SERVICE);
		return telephonyManager.getDeviceId();
	}

	/**
	 * initsilize the Wave Recorder
	 */
	private void initWaveRecorder() {

		bufferSize = AudioRecord.getMinBufferSize(
				Constants.RECORDER_SAMPLERATE, Constants.RECORDER_CHANNELS,
				Constants.RECORDER_AUDIO_ENCODING);
		Log.i(Constants.Tag + "_" + this.getClass().toString(), "BufferSize="
				+ bufferSize);
		mTempAudioFile = setAudioTempFile();
	}

	/**
	 * start recording Speech Recozinser recording
	 */
	private void invokeRecorder() {
		if (!mIsRecording) {
			doOnUIText("Listening....");
			setRecorderButton(RECORD_BUTTON_STOP);
			mIsRecording = true;
			mIsDecoding = true;
			if (recorder == null)
				createRecorder();
			if (recorder.getState() == AudioRecord.STATE_UNINITIALIZED)
				createRecorder();

			// Temp File will be created
			mTempAudioFile = setAudioTempFile();
			mPauseTime = 0;
			if (recorder.getState() == 1) {
				recorder.startRecording();
				if (recorder.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
					Log.i(Constants.Tag, "recorder is recording state");

				} else if (recorder.getRecordingState() == AudioRecord.RECORDSTATE_STOPPED) {
					Log.i(Constants.Tag, "recorder is stop state");
					recorder.startRecording();
				}
				recordingThread = new Thread(new Runnable() {
					@Override
					public void run() {
						writeAudioDataToFileWithGain();
					}
				}, "AudioRecorder Thread");
				recordingThread.start();
			} else
				Log.i(Constants.Tag, "Recorder is not initilzed");

		} else
			Log.i(Constants.Tag, "StartRecord_Player or record already on");

	}

	/**
	 * Call on destroy of the activity. It clean up all variables
	 */
	public void onDestory() {

		destoryWaveRecord();

		if (mFileUpload != null) {
			mFileUpload = null;
		}
	}

	public void onPause() {
		cancelWaveRecord();
		if (mFileUpload != null) {
			mFileUpload = null;
		}
	}

	/**
	 * It will record the audio. It onClick function for Record button.
	 */
	public void onRecordClick() {
		if (!mIsRecording) {
			mTotalRead = 0;
			invokeRecorder();
		} else {
			stopRecording();
			mIsRecording = false;

		}

	}

	public void onStart() {
		if (mFileUpload == null)
			this.mFileUpload = new FileUploader();
		mGain = Constants.GAIN / 100;
		if (recorder == null)
			createRecorder();

	}

	/**
	 * Its calculate the time between Pause time and current time.
	 * 
	 * @return
	 */
	protected boolean pauseDuration() {
		if (mPauseTime == 0) {
			Calendar c = Calendar.getInstance();
			mPauseTime = c.getTimeInMillis();
			return false;
		}
		if (mPauseTime > 0) {
			long cTime = Calendar.getInstance().getTimeInMillis();
			cTime = cTime - mPauseTime;
			if (cTime >= Constants.Max_Pause_MillSeconds)
				return true;
			else
				return false;

		} else
			return false;

	}

	/**
	 * It will reset the text The TextView UI
	 */
	private void resetText() {

		Thread th = new Thread(new Runnable() {

			@Override
			public void run() {
				try {
					Thread.sleep(Constants.UI_Reset_Timer * 1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
					return;
				}
				if (mAct != null && !mIsDecoding && !mIsRecording)
					mAct.runOnUiThread(new Runnable() {
						@Override
						public void run() {
							TextView tv = (TextView) mAct
									.findViewById(R.id.asrTVTextData);
							tv.setText("Press and Say");

						}
					});

			}
		}, "reset_thread");
		th.start();

	}

	/**
	 * Search the Threshold_Limit in audio stream of data.
	 * 
	 * @param arr
	 * @return
	 */
	private int searchThreshold(short[] arr) {
		int peakIndex;
		int arrLen = arr.length;
		for (peakIndex = 0; peakIndex < arrLen; peakIndex++) {
			if ((arr[peakIndex] >= Constants.Threshold_Limit)
					|| (arr[peakIndex] <= -Constants.Threshold_Limit)) {
				// if it exceeds the Threshold_Limit , out and back peakindex -
				// half
				// kernel.

				return peakIndex;
			}
		}
		return -1; // not found
	}

	/**
	 * set the Audio temp file name
	 * 
	 * @return
	 */
	private String setAudioTempFile() {
		String filepath = Environment.getExternalStorageDirectory().getPath();
		File file = new File(filepath, Constants.AUDIO_RECORDER_FOLDER);

		if (!file.exists()) {
			file.mkdirs();
		}
		File tempFile = new File(filepath, Constants.AUDIO_RECORDER_TEMP_FILE);

		if (tempFile.exists())
			tempFile.delete();
		return (file.getAbsolutePath() + "/" + Constants.AUDIO_RECORDER_TEMP_FILE);

	}

	/**
	 * Change the Record Button Image.
	 * 
	 * @param buttontype
	 * @see RECORD_BUTTON_ON
	 * @see RECORD_BUTTON_STOP
	 */
	private void setRecorderButton(int buttontype) {
		ImageButton btn = (ImageButton) mAct.findViewById(R.id.asrBTNRecord);

		if (buttontype == RECORD_BUTTON_ON) {

			btn.setImageResource(sIDRecordButton);

		} else if (buttontype == RECORD_BUTTON_STOP) {
			btn.setImageResource(sIDStopButton);

		}

	}

	/**
	 * convert short data to byte data
	 * 
	 * @param sData
	 *            short array
	 * @return byte array
	 */
	private byte[] short2byte(short[] sData) {
		int shortArrsize = sData.length;
		byte[] bytes = new byte[shortArrsize * 2];
		for (int i = 0; i < shortArrsize; i++) {
			bytes[i * 2] = (byte) (sData[i] & 0x00FF);
			bytes[(i * 2) + 1] = (byte) (sData[i] >> 8);
			sData[i] = 0;
		}
		return bytes;

	}

	/**
	 * stop recording Akshar
	 */
	private void stopRecord() {
		if (null != recorder && mIsRecording) {
			mIsRecording = false;
			Log.i(Constants.Tag, "stop");
			recorder.stop();
			recordingThread = null;
			copyWaveFile(mTempAudioFile, mCurrentAudioFileName);
			deleteTempFile();
			Log.i(Constants.Tag, "stop");
		}
	}

	// Stop operations
	/**
	 * It will stop recording and will call the uploader to decode the audio
	 */
	private void stopRecording() {
		if (mIsRecording) {
			String filename = Environment.getExternalStorageDirectory()
					.getAbsolutePath();
			filename = filename + Constants.AUDIO_RECORDER_FOLDER;
			new File(filename).mkdirs();
			String currentDate = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss",
					Locale.getDefault()).format(new Date());
			filename = filename + "/" + currentDate + ".wav";
			// Setting audoi filename
			mCurrentAudioFileName = filename;
			// Stop the recording
			stopRecord();
			setRecorderButton(RECORD_BUTTON_ON);
			while (mIsRecording) {
			}
			if (!mIsRecording) {
				Log.i(Constants.Tag, "Uploading will start...");
				Log.i(Constants.Tag, "TotalSkipped Data=" + mTotalRead);
				uploadFile(mCurrentAudioFileName);

			} else {
				Toast.makeText(mAct, "Kindly say is again!", Toast.LENGTH_SHORT)
						.show();
				Log.i(Constants.Tag, "Uploading will not start...");
			}
		}

	}

	/**
	 * It will upload the wave file to Akshar ASR Server
	 * 
	 * @param audiofilename
	 *            audio file path
	 */
	private void uploadFile(final String audiofilename) {

		Thread th = new Thread(new Runnable() {
			@Override
			public void run() {
				Looper.prepare();
				doOnUIText("Recognizing.... ");
				mIsDecoding = true;
				File f = new File(audiofilename);
				if (f.exists()) {
					Log.i(Constants.Tag, "Diff of Size="
							+ (f.length() - mTotalRead));
					if (f.length() <= (mTotalRead + 44)) {
						Log.i(Constants.Tag, "File Size is less");
						doPostError();
						Looper.myLooper().quit();
						return;
					}

				} else {

					Log.i(Constants.Tag, "File not present=" + audiofilename);
					doPostError();
					Looper.myLooper().quit();
					return;
				}
				String response = mFileUpload.uploadFile(Constants.mUploadURL,
						audiofilename, getEMINumber(mAct));
				if (response != null && response.contains("Error")) {
					Log.e("UPLOADER", response);
					doTost(response);
					doPostError();
					Looper.myLooper().quit();

				} else {
					doOnUIText("Recognizing....");
					doPostUpload_NEW(response);

					Looper.myLooper().quit();
				}
				Looper.loop();
			}
		}, "FileUploaderThread");
		th.start();

	}

	private void writeAudioDataToFileWithGain() {
		short data[] = new short[bufferSize];
		String filename = this.mTempAudioFile;
		Log.i(Constants.Tag, "writeAuidDataGain_temp file=" + filename);
		FileOutputStream os = null;
		try {
			os = new FileOutputStream(filename);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		int read = 0;
		if (null != os) {
			while (mIsRecording) {
				read = recorder.read(data, 0, bufferSize);
				if (AudioRecord.ERROR_INVALID_OPERATION != read) {
					int foundPeak = searchThreshold(data);
					try {
						os.write(gainAudio(data, read), 0, bufferSize
								* Constants.BytesPerElement);
					} catch (IOException e) {
						e.printStackTrace();
					}
					if (foundPeak > -1) {
						mPauseTime = 0;
					} else {

						if (pauseDuration()) {
							mAct.runOnUiThread(new Runnable() {

								@Override
								public void run() {
									Log.i(Constants.Tag,
											"Invoking stop recorder due to long pause");
									stopRecording();

								}
							});
						} else {
							mTotalRead += read;
						}
					}

				} else
					Log.i(Constants.Tag,
							"Error Invalid Operation in Recorder Reading");
			}

			try {
				os.close();
				data = null;
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else
			Log.i("Gain", "OS FILE IS NULL");
	}

	/**
	 * 
	 * @param out
	 * @param totalAudioLen
	 * @param totalDataLen
	 * @param longSampleRate
	 * @param channels
	 * @param byteRate
	 * @throws IOException
	 */
	private void WriteWaveFileHeader(FileOutputStream out, long totalAudioLen,
			long totalDataLen, long longSampleRate, int channels, long byteRate)
			throws IOException {

		byte[] header = new byte[44];

		header[0] = 'R'; // RIFF/WAVE header
		header[1] = 'I';
		header[2] = 'F';
		header[3] = 'F';
		header[4] = (byte) (totalDataLen & 0xff);
		header[5] = (byte) ((totalDataLen >> 8) & 0xff);
		header[6] = (byte) ((totalDataLen >> 16) & 0xff);
		header[7] = (byte) ((totalDataLen >> 24) & 0xff);
		header[8] = 'W';
		header[9] = 'A';
		header[10] = 'V';
		header[11] = 'E';
		header[12] = 'f'; // 'fmt ' chunk
		header[13] = 'm';
		header[14] = 't';
		header[15] = ' ';
		header[16] = 16; // 4 bytes: size of 'fmt ' chunk
		header[17] = 0;
		header[18] = 0;
		header[19] = 0;
		header[20] = 1; // format = 1
		header[21] = 0;
		header[22] = (byte) channels;
		header[23] = 0;
		header[24] = (byte) (longSampleRate & 0xff);
		header[25] = (byte) ((longSampleRate >> 8) & 0xff);
		header[26] = (byte) ((longSampleRate >> 16) & 0xff);
		header[27] = (byte) ((longSampleRate >> 24) & 0xff);
		header[28] = (byte) (byteRate & 0xff);
		header[29] = (byte) ((byteRate >> 8) & 0xff);
		header[30] = (byte) ((byteRate >> 16) & 0xff);
		header[31] = (byte) ((byteRate >> 24) & 0xff);
		header[32] = (byte) (2 * 16 / 8); // block align
		header[33] = 0;
		header[34] = (byte) Constants.RECORDER_BPP; // bits per sample
		header[35] = 0;
		header[36] = 'd';
		header[37] = 'a';
		header[38] = 't';
		header[39] = 'a';
		header[40] = (byte) (totalAudioLen & 0xff);
		header[41] = (byte) ((totalAudioLen >> 8) & 0xff);
		header[42] = (byte) ((totalAudioLen >> 16) & 0xff);
		header[43] = (byte) ((totalAudioLen >> 24) & 0xff);

		out.write(header, 0, 44);
	}

}
