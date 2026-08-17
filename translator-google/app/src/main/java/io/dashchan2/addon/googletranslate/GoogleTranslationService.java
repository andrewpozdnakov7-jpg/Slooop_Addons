package io.dashchan2.addon.googletranslate;

import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.TrafficStats;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Process;
import android.os.RemoteException;
import android.util.Log;
import com.google.mlkit.common.MlKit;
import com.google.mlkit.common.model.DownloadConditions;
import com.google.mlkit.common.model.RemoteModelManager;
import com.google.mlkit.nl.translate.TranslateLanguage;
import com.google.mlkit.nl.translate.TranslateRemoteModel;
import com.google.mlkit.nl.translate.Translation;
import com.google.mlkit.nl.translate.Translator;
import com.google.mlkit.nl.translate.TranslatorOptions;
import io.dashchan2.addon.translation.IGoogleTranslationCallback;
import io.dashchan2.addon.translation.IGoogleTranslationService;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public final class GoogleTranslationService extends Service {
	private static final String TAG = "SlooopGoogleTranslate";
	private static final String CLIENT_PACKAGE = "io.dashchan2";
	private static final String PREFERENCES_NAME = "translation_models";
	private static final String PREFERENCE_RUSSIAN_MODEL_READY = "russian_model_ready";
	private static final int PROTOCOL_VERSION = 1;
	private static final int STATE_NOT_INSTALLED = 0;
	private static final int STATE_CHECKING = 1;
	private static final int STATE_DOWNLOADING = 2;
	private static final int STATE_INSTALLED = 3;
	private static final int STATE_ERROR = 4;
	private static final long APPROXIMATE_MODEL_SIZE = 30L * 1024L * 1024L;
	private static final long PROGRESS_INTERVAL_MS = 1000L;
	private static final TranslateRemoteModel RUSSIAN_MODEL = new TranslateRemoteModel.Builder(
			TranslateLanguage.RUSSIAN).build();

	private final Handler handler = new Handler(Looper.getMainLooper());
	private IGoogleTranslationCallback downloadCallback;
	private long downloadStartBytes = TrafficStats.UNSUPPORTED;
	private final Runnable progressRunnable = new Runnable() {
		@Override
		public void run() {
			if (downloadCallback == null) {
				return;
			}
			long current = TrafficStats.getUidRxBytes(Process.myUid());
			long downloaded = downloadStartBytes != TrafficStats.UNSUPPORTED &&
					current != TrafficStats.UNSUPPORTED && current >= downloadStartBytes
					? current - downloadStartBytes : 0L;
			reportStatus(downloadCallback, STATE_DOWNLOADING, downloaded, APPROXIMATE_MODEL_SIZE, null);
			handler.postDelayed(this, PROGRESS_INTERVAL_MS);
		}
	};

	private final IGoogleTranslationService.Stub binder = new IGoogleTranslationService.Stub() {
		@Override
		public int getProtocolVersion() {
			enforceTrustedCaller();
			return PROTOCOL_VERSION;
		}

		@Override
		public void getModelStatus(String sourceLanguage, String targetLanguage,
				IGoogleTranslationCallback callback) {
			enforceTrustedCaller();
			if (!validate(sourceLanguage, targetLanguage, callback)) {
				return;
			}
			reportStatus(callback, isRussianModelReady() ? STATE_INSTALLED : STATE_NOT_INSTALLED,
					0L, APPROXIMATE_MODEL_SIZE, null);
		}

		@Override
		public void downloadModel(String sourceLanguage, String targetLanguage,
				IGoogleTranslationCallback callback) {
			enforceTrustedCaller();
			if (!validate(sourceLanguage, targetLanguage, callback)) {
				return;
			}
			handler.post(() -> startModelDownload(sourceLanguage, targetLanguage, callback));
		}

		@Override
		public void deleteModel(String sourceLanguage, String targetLanguage,
				IGoogleTranslationCallback callback) {
			enforceTrustedCaller();
			if (!validate(sourceLanguage, targetLanguage, callback)) {
				return;
			}
			try {
				manager().deleteDownloadedModel(RUSSIAN_MODEL)
						.addOnSuccessListener(ignored -> {
							setRussianModelReady(false);
							reportStatus(callback, STATE_NOT_INSTALLED, 0L, APPROXIMATE_MODEL_SIZE, null);
						})
						.addOnFailureListener(error -> reportErrorStatus(callback, error));
			} catch (RuntimeException | LinkageError error) {
				reportErrorStatus(callback, error);
			}
		}

		@Override
		public void translate(String sourceLanguage, String targetLanguage, List<String> texts,
				IGoogleTranslationCallback callback) {
			enforceTrustedCaller();
			if (!validate(sourceLanguage, targetLanguage, callback) || texts == null) {
				return;
			}
			ArrayList<String> safeTexts = new ArrayList<>(texts.size());
			for (String text : texts) {
				if (text == null) {
					reportError(callback, "Translation text is missing");
					return;
				}
				safeTexts.add(text);
			}
			if (isRussianModelReady()) {
				translateTexts(sourceLanguage, targetLanguage, safeTexts, callback);
			} else {
				reportError(callback, "Google language package is not installed");
			}
		}
	};

	@Override
	public IBinder onBind(Intent intent) {
		return binder;
	}

	@Override
	public void onDestroy() {
		stopProgress();
		super.onDestroy();
	}

	private void enforceTrustedCaller() {
		int uid = Binder.getCallingUid();
		String[] packages = getPackageManager().getPackagesForUid(uid);
		if (packages != null && Arrays.asList(packages).contains(CLIENT_PACKAGE) &&
				getPackageManager().checkSignatures(getPackageName(), CLIENT_PACKAGE) ==
						PackageManager.SIGNATURE_MATCH) {
			return;
		}
		throw new SecurityException("Caller is not a trusted Slooop installation");
	}

	private static boolean validate(String sourceLanguage, String targetLanguage,
			IGoogleTranslationCallback callback) {
		if (callback == null) {
			return false;
		}
		boolean supported = "en".equals(sourceLanguage) && "ru".equals(targetLanguage) ||
				"ru".equals(sourceLanguage) && "en".equals(targetLanguage);
		if (!supported) {
			reportError(callback, "Unsupported translation direction");
		}
		return supported;
	}

	private void startModelDownload(String sourceLanguage, String targetLanguage,
			IGoogleTranslationCallback callback) {
		stopProgress();
		downloadCallback = callback;
		downloadStartBytes = TrafficStats.getUidRxBytes(Process.myUid());
		reportStatus(callback, STATE_DOWNLOADING, 0L, APPROXIMATE_MODEL_SIZE, null);
		handler.postDelayed(progressRunnable, PROGRESS_INTERVAL_MS);
		Translator translator = null;
		try {
			translator = createTranslator(sourceLanguage, targetLanguage);
			Translator downloadTranslator = translator;
			downloadTranslator.downloadModelIfNeeded(new DownloadConditions.Builder().build())
					.addOnSuccessListener(ignored -> {
						downloadTranslator.close();
						stopProgress();
						setRussianModelReady(true);
						reportStatus(callback, STATE_INSTALLED, APPROXIMATE_MODEL_SIZE,
								APPROXIMATE_MODEL_SIZE, null);
					})
					.addOnFailureListener(error -> {
						downloadTranslator.close();
						stopProgress();
						reportErrorStatus(callback, error);
					});
		} catch (RuntimeException | LinkageError error) {
			if (translator != null) {
				translator.close();
			}
			stopProgress();
			reportErrorStatus(callback, error);
		}
	}

	private void stopProgress() {
		handler.removeCallbacks(progressRunnable);
		downloadCallback = null;
		downloadStartBytes = TrafficStats.UNSUPPORTED;
	}

	private static void translateTexts(String sourceLanguage, String targetLanguage, List<String> texts,
			IGoogleTranslationCallback callback) {
		Translator translator;
		try {
			translator = createTranslator(sourceLanguage, targetLanguage);
		} catch (RuntimeException | LinkageError error) {
			reportTranslationError(callback, error);
			return;
		}
		if (texts.isEmpty()) {
			translator.close();
			reportTranslations(callback, new ArrayList<>());
			return;
		}
		String[] results = new String[texts.size()];
		AtomicInteger remaining = new AtomicInteger(texts.size());
		AtomicBoolean completed = new AtomicBoolean();
		for (int i = 0; i < texts.size(); i++) {
			int index = i;
			translator.translate(texts.get(i)).addOnSuccessListener(result -> {
				results[index] = result;
				if (remaining.decrementAndGet() == 0 && completed.compareAndSet(false, true)) {
					translator.close();
					reportTranslations(callback, Arrays.asList(results));
				}
			}).addOnFailureListener(error -> {
				if (completed.compareAndSet(false, true)) {
					translator.close();
					reportTranslationError(callback, error);
				}
			});
		}
	}

	private static Translator createTranslator(String sourceLanguage, String targetLanguage) {
		return Translation.getClient(new TranslatorOptions.Builder()
				.setSourceLanguage("en".equals(sourceLanguage)
						? TranslateLanguage.ENGLISH : TranslateLanguage.RUSSIAN)
				.setTargetLanguage("en".equals(targetLanguage)
						? TranslateLanguage.ENGLISH : TranslateLanguage.RUSSIAN)
				.build());
	}

	private boolean isRussianModelReady() {
		return getSharedPreferences(PREFERENCES_NAME, MODE_PRIVATE)
				.getBoolean(PREFERENCE_RUSSIAN_MODEL_READY, false);
	}

	private void setRussianModelReady(boolean ready) {
		getSharedPreferences(PREFERENCES_NAME, MODE_PRIVATE).edit()
				.putBoolean(PREFERENCE_RUSSIAN_MODEL_READY, ready).apply();
	}

	private RemoteModelManager manager() {
		try {
			return RemoteModelManager.getInstance();
		} catch (IllegalStateException error) {
			MlKit.initialize(getApplicationContext());
			return RemoteModelManager.getInstance();
		}
	}

	private static void reportErrorStatus(IGoogleTranslationCallback callback, Throwable error) {
		Log.e(TAG, "ML Kit model operation failed", error);
		reportStatus(callback, STATE_ERROR, 0L, APPROXIMATE_MODEL_SIZE, message(error));
	}

	private static void reportTranslationError(IGoogleTranslationCallback callback, Throwable error) {
		Log.e(TAG, "ML Kit translation failed", error);
		reportError(callback, message(error));
	}

	private static void reportStatus(IGoogleTranslationCallback callback, int state, long downloadedBytes,
			long totalBytes, String error) {
		try {
			callback.onStatus(state, downloadedBytes, totalBytes, error);
		} catch (RemoteException ignored) {}
	}

	private static void reportTranslations(IGoogleTranslationCallback callback, List<String> translations) {
		try {
			callback.onTranslation(translations);
		} catch (RemoteException ignored) {}
	}

	private static void reportError(IGoogleTranslationCallback callback, String message) {
		try {
			callback.onError(message);
		} catch (RemoteException ignored) {}
	}

	private static String message(Throwable error) {
		String message = error.getMessage();
		return message == null || message.trim().isEmpty() ? error.getClass().getSimpleName() : message;
	}
}
