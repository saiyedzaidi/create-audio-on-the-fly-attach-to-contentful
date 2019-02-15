package com.sapient.jackbot;

import java.io.IOException;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.contentful.java.cma.CMAClient;
import com.contentful.java.cma.model.CMAAsset;
import com.contentful.java.cma.model.CMAAssetFile;
import com.contentful.java.cma.model.CMAEntry;
import com.contentful.java.cma.model.CMALink;
import com.contentful.java.cma.model.CMAResource;
import com.contentful.java.cma.model.CMAType;

public class Main implements RequestHandler<Request, String> {

	private SpeechClient speechClient;
	private Properties properties;
	private int waitBetweenRetries = 500;

	public Main() throws IOException {
		this(new Properties());
	}

	public Main(Properties properties) throws IOException {
		this.properties = properties;
		String wait = properties.getValue("WAIT_BETWEEN_RETRIES");
		if (wait != null) {
			try {
				waitBetweenRetries = Integer.parseInt(wait);
			} catch (NumberFormatException e) {
			}
		}
	}

	private SpeechClient getSpeechClient() throws IOException {
		if (speechClient == null) {
			speechClient = new SpeechClient(properties);
		}
		return speechClient;
	}

	public String handleRequest(Request request, Context context) {
		context.getLogger().log("Input: " + request);
		final CMAClient client = new CMAClient.Builder().setAccessToken(properties.getValue("CMS_ACCESS_KEY"))
				.setSpaceId(request.getSpaceId()).build();
		CMAEntry c = client.entries().fetchOne(request.getEntityId());
		if (client.users().fetchMe().getId().equals(c.getSystem().getPublishedBy().getId())) {
			context.getLogger().log("Got an invocation originating due to the same user as the lambda user, should be an echo of the publish that this lambda made, ignoring");
		} else {
			String copy = request.getCopyText();
			context.getLogger().log("Copy: " + copy);
			if (copy != null && !copy.trim().isEmpty()) {
				copy = copy.trim();

				String fileName = copy.replaceAll("[^A-Za-z0-9]", "") + ".wav";
				context.getLogger().log("Filename: " + fileName);
				try {
					CMAResource cmaUpload = client.uploads().create(getSpeechClient().voice(copy));
					context.getLogger().log("Created audio resource "+cmaUpload.getId());
					CMAAssetFile file = new CMAAssetFile()
							.setUploadFrom(new CMALink(CMAType.Upload).setId(cmaUpload.getId())).setFileName(fileName)
							.setContentType("audio/wav");
					context.getLogger().log("Created asset file from audio");
					CMAAsset cma = new CMAAsset();
					CMAAsset.Fields fields = cma.getFields();
					fields.setFile("en-US", file);
					fields.setTitle("en-US", fileName);
					CMAAsset asset = client.assets().create(cma);
					context.getLogger().log("Created asset "+asset.getId());
					client.assets().process(asset, "en-US");
					asset = client.assets().fetchOne(asset.getId());
					// wait till processing is done
					String url = asset.getFields().localize("en-US").getFile().getUrl();
					int attempts = 10;
					while (attempts > 0 && (url == null || url.length() == 0)) {
						context.getLogger().log("Waiting for asset to be processed");
						asset = client.assets().fetchOne(asset.getId());
						url = asset.getFields().localize("en-US").getFile().getUrl();
						attempts--;

						try {
							Thread.sleep(waitBetweenRetries);
						} catch (Exception e) {
							e.printStackTrace();
						}
					}

					if (attempts <= 0) {
						throw new IllegalStateException("Could not finish processing for " + asset);
					}
					context.getLogger().log("Publishing asset");
					asset = client.assets().publish(asset);
					context.getLogger().log("Attaching asset to content");
					c.setField("audioFile", "en-US", asset);
					context.getLogger().log("Updating content");
					c = client.entries().update(c);
					context.getLogger().log("Publishing content");
					client.entries().publish(c);

					if (request.getOldAssetId() != null && !request.getOldAssetId().isEmpty()) {
						context.getLogger().log("Archiving old asset "+request.getOldAssetId());
						CMAAsset unpublished = client.assets()
								.unPublish(client.assets().fetchOne(request.getOldAssetId()));
						client.assets().archive(unpublished);
					}

				} catch (Exception e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}
		}

		return "done";
	}
}
