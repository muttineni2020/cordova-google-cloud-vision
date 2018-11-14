package com.google.vision;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;

import org.apache.cordova.LOG;
import org.json.JSONArray;
import org.json.JSONException;
import android.util.Log;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.vision.v1.Vision;
import com.google.api.services.vision.v1.VisionRequest;
import com.google.api.services.vision.v1.VisionRequestInitializer;
import com.google.api.services.vision.v1.model.AnnotateImageRequest;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesRequest;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesResponse;
import com.google.api.services.vision.v1.model.Feature;
import com.google.api.services.vision.v1.model.Image;

import java.io.IOException;
import java.util.ArrayList;



/**
 * Main class to handle Cordova, Android methods
 *
 * params: base64Image, scanKey
 * @return text from scanned Image
 **/
public class GoogleVision extends CordovaPlugin {

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {

        if (action.equals("ScanImage")) {
            String base64Image = args.getString(0);
            String scanKey = args.getString(1);
            try {
                this.callCloudVision(base64Image, scanKey, callbackContext);
            }
            catch(IOException e){
                LOG.d("Vision",e.getMessage());
            }
            return true;
        }

        return false;
    }


    private void callCloudVision(String base64, String scanKey, CallbackContext callbackContext) throws IOException {
        HttpTransport httpTransport = AndroidHttp.newCompatibleTransport();
        GsonFactory jsonFactory = GsonFactory.getDefaultInstance();

        VisionRequestInitializer requestInitializer =
                new VisionRequestInitializer(scanKey) {
                    /**
                     * We override this so we can inject important identifying fields into the HTTP
                     * headers. This enables use of a restricted cloud platform API key.
                     */
                    @Override
                    protected void initializeVisionRequest(VisionRequest<?> visionRequest)
                            throws IOException {
                        super.initializeVisionRequest(visionRequest);

                        String packageName = cordova.getContext().getPackageName();
                        visionRequest.getRequestHeaders().set("X-Android-Package", packageName);

                        String sig = com.google.vision.PackageUtility.getSignature(cordova.getContext().getPackageManager(), packageName);

                        visionRequest.getRequestHeaders().set("X-Android-Cert", sig);
                    }
                };

        Vision.Builder builder = new Vision.Builder(httpTransport, jsonFactory, null);
        builder.setVisionRequestInitializer(requestInitializer);

        Vision vision = builder.build();

        BatchAnnotateImagesRequest batchAnnotateImagesRequest =
                new BatchAnnotateImagesRequest();
        batchAnnotateImagesRequest.setRequests(new ArrayList<AnnotateImageRequest>() {{
            AnnotateImageRequest annotateImageRequest = new AnnotateImageRequest();

            // Set the image content  - Base 64
            Image img = new Image();
            img.setContent(base64);

            // add the features to annotate Image request
            annotateImageRequest.setImage(img);
            annotateImageRequest.setFeatures(new ArrayList<Feature>() {{
                Feature textDetection = new Feature();
                textDetection.setType("TEXT_DETECTION");
                add(textDetection);
            }});

            // Add the list of one thing to the request
            add(annotateImageRequest);
        }});

        Vision.Images.Annotate annotateRequest =
                vision.images().annotate(batchAnnotateImagesRequest);

        // requests to Vision API containing large images fail when GZipped.
        annotateRequest.setDisableGZipContent(true);

        Log.d("hello", "created Cloud Vision request object, sending request");
        BatchAnnotateImagesResponse response = annotateRequest.execute();

        String TextAnnotation = response.getResponses().get(0).getFullTextAnnotation().getText();
        System.out.println("Oh Boy - Got it ...!!" + TextAnnotation);
        Log.d("response - final", "Thank you");

        callbackContext.success(TextAnnotation);

    }

}