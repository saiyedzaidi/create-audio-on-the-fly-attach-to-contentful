package in.zaidi.java;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import com.google.cloud.texttospeech.v1.AudioConfig;
import com.google.cloud.texttospeech.v1.AudioEncoding;
import com.google.cloud.texttospeech.v1.SynthesisInput;
import com.google.cloud.texttospeech.v1.SynthesizeSpeechResponse;
import com.google.cloud.texttospeech.v1.TextToSpeechClient;
import com.google.cloud.texttospeech.v1.VoiceSelectionParams;
import com.google.protobuf.ByteString;

public class SpeechClient {

	public SpeechClient(Properties properties) throws IOException {
		this.properties = properties;
		textToSpeechClient = TextToSpeechClient.create();

		voice = VoiceSelectionParams.newBuilder().setName(properties.getValue("VOICE_NAME"))
				.setLanguageCode(properties.getValue("LOCALE")).build();
		float speakingRate = 1.125f;
		try {
			speakingRate = Integer.parseInt(properties.getValue("SPEAKING_RATE"));
		} catch (NumberFormatException nfe) {

		}

		int sampleRate = 16000;
		try {
			sampleRate = Integer.parseInt(properties.getValue("SAMPLE_RATE"));
		} catch (NumberFormatException nfe) {

		}

		audioConfig = AudioConfig.newBuilder().setAudioEncoding(AudioEncoding.LINEAR16).setSampleRateHertz(sampleRate)
				.setSpeakingRate(speakingRate).build();
	}

	private TextToSpeechClient textToSpeechClient;
	VoiceSelectionParams voice;
	AudioConfig audioConfig;
	Properties properties;

	public InputStream voice(String text) throws Exception {
		SynthesisInput input = SynthesisInput.newBuilder().setText(text).build();
		SynthesizeSpeechResponse response = textToSpeechClient.synthesizeSpeech(input, voice, audioConfig);
		// Get the audio contents from the response
		ByteString audioContents = response.getAudioContent();
		return new ByteArrayInputStream(audioContents.toByteArray());
	}
}