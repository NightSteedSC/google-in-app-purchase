package org.apache.cordova.googleInAppPurchase;

import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;

import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaWebView;

import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.AcknowledgePurchaseResponseListener;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.ConsumeParams;
import com.android.billingclient.api.ConsumeResponseListener;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.SkuDetails;
import com.android.billingclient.api.SkuDetailsParams;
import com.android.billingclient.api.SkuDetailsResponseListener;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

////////////////////////////////////////////////////
public class googleInAppPurchase extends CordovaPlugin implements PurchasesUpdatedListener, BillingClientStateListener, ConsumeResponseListener, AcknowledgePurchaseResponseListener {

    private static final String TAG = "***** : ";
    public BillingClient billingClient;
    public SkuDetails skuDetails;
    public boolean readyToPurchase = false;

    public List<String> listOfSubs = new ArrayList<>();
    public List<String> listOfProducts = new ArrayList<>();
    public List<SkuDetails> skuDetailsList;
    public final List<Purchase> mPurchases = new ArrayList<>();
    SkuDetailsParams.Builder params;
    public Purchase.PurchasesResult purchaseToRestore;

    public BillingResult billingResult;


    /////////////////////////////////////////////////////////////////////////////
    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
        billingResult = new BillingResult();
    }

    /////////////////////////////////////////////////////////////////////////////
    @Override//funkcja która łączy się z JS
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
//        initBilling(callbackContext, args);

        if (action.equals("purchaseProduct")) {
            cordova.getThreadPool().execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        purchaseProduct(callbackContext, args);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            });
        } else if (action.equals("initBilling")) {
            initBilling(callbackContext, args);
        } else if (action.equals("restoreProducts")) {
            restoreProducts();
        } else if (action.equals("consumProduct")) {
            consumProduct(callbackContext, args);
        } else if (action.equals("acknowledgePurchase")) {
            acknowledgePurchase(callbackContext, args);
        }
        return false;  // Returning false results in a "MethodNotFound" error.
    }

    //^^^^^^^^^^^^^^^^^^^^^^^^ BILLING PART ^^^^^^^^^^^^^^^^^^^^^^^^\\

    private void initBilling(final CallbackContext callbackContext, final JSONArray data) throws JSONException {
        //   Log.d(TAG, "Billing is Ready? : " + billingClient.isReady());


        billingClient = BillingClient.newBuilder(cordova.getActivity())
                .setListener(this)
                .enablePendingPurchases()
                .build();

        billingClient.startConnection(this);

        ;

        Log.d(TAG, "billingResult getDebugMessage: " + billingResult.getDebugMessage());
        Log.d(TAG, "billingResult getResponseCode: " + billingResult.getResponseCode());
//        Purchase.PurchasesResult queryPurchases = billingClient.queryPurchases(BillingClient.SkuType.INAPP);
//        Log.d(TAG, " : "+queryPurchases.getBillingResult());
//        Log.d(TAG, " : "+queryPurchases.getPurchasesList());
//        ConsumeParams consumeParams =
//                ConsumeParams.newBuilder()
//                        .setPurchaseToken("ocoejalncmieplkjddppccph.AO-J1OzHL-sSvpm32aoisUp_VQdH4DfQfqn2EsxmsGu0s8GQR5mdXlNY5oHLHiScv3XQfRdU1D1PI9b0RrS_qhB_RgoamFJjezxJSlAUIcWG5rAgADTTNho")
//                        .build();
//        billingClient.consumeAsync(consumeParams, this::onConsumeResponse);
    }

    private void purchaseProduct(final CallbackContext callbackContext, final JSONArray data) throws JSONException {
        String localID = data.getString(0);
        String type = data.getString(1);
        if (!billingClient.isReady()) {
            Log.d(TAG, "Billing not initalized, before you purchaseProduct you must to initialize billing");

            initBilling(callbackContext, data);
            webView.loadUrl("javascript:cordova.fireDocumentEvent('onPurchaseFailed',{ 'id':'" + localID + ");");
            return;
        }

        Log.d(TAG, "purchaseProduct ID: " + localID);
        Log.d(TAG, "Type of product" + type);

        // INFO
        // Type 2 -> SUBSCRIBE
        // Type 1 -> CONSUMABLE
        // Type 0 -> NON-CONSUMABLE

        // TODO C2 listeners handle
//        if (type.equals("2")) {
//            List<String> product = new ArrayList<>();
//            product.add(localID);
//            params = SkuDetailsParams.newBuilder().setSkusList(product).setType(BillingClient.SkuType.SUBS);
//            billingClient.querySkuDetailsAsync(params.build(), this);
//            return;
//        } else if (type.equals("1") || type.equals("0")) {
//
//            List<String> product = new ArrayList<>();
//            product.add(localID);
//            params = SkuDetailsParams.newBuilder().setSkusList(product).setType(BillingClient.SkuType.INAPP);
//            billingClient.querySkuDetailsAsync(params.build(), this);
//            return;
//        } else {
//            Log.d(TAG, "*** FAIL -> purchaseProduct ");
//            webView.loadUrl("javascript:cordova.fireDocumentEvent('onPurchaseFailed',{ 'id':'" + localID + ");");
//            return;
//        }
    }


    // Method to consume consumable purchases
    void consumProduct(final CallbackContext callbackContext, final JSONArray data) throws JSONException {
        String token = data.getString(0);
        ConsumeParams consumeParams =
                ConsumeParams.newBuilder()
                        .setPurchaseToken(token)
                        .build();
        billingClient.consumeAsync(consumeParams, this::onConsumeResponse);

    }

    //Method to acknowledge purchases non-consumable and subs
    void acknowledgePurchase(final CallbackContext callbackContext, final JSONArray data) throws JSONException {
        String token = data.getString(0);

        AcknowledgePurchaseParams acknowledgePurchaseParams =
                AcknowledgePurchaseParams.newBuilder()
                        .setPurchaseToken(token)
                        .build();
        billingClient.acknowledgePurchase(acknowledgePurchaseParams, this::onAcknowledgePurchaseResponse);

    }

    private void restoreProducts() {

        Log.d(TAG, "restoreProducts 1: ");

        if (billingClient == null) {
            webView.loadUrl("javascript:cordova.fireDocumentEvent('onBillingError',{ 'error':'" + "billingClient is null" + ");");
            return;
        }

        Log.d(TAG, "restoreProducts 2: ");

        List<Purchase> list = billingClient.queryPurchases(BillingClient.SkuType.INAPP).getPurchasesList();
        List<Purchase> listsub = billingClient.queryPurchases(BillingClient.SkuType.SUBS).getPurchasesList();


        Log.d(TAG, "restoreProducts 3 List: " + list);

        if(!list.isEmpty()){
            for (Purchase purchase : list) {
                webView.loadUrl("javascript:cordova.fireDocumentEvent('verifyPurchase', { 'purchase':'" + purchase + "','id':'" + purchase.getSku() + "','token':'" + purchase.getPurchaseToken() + "','signature':'" + purchase.getSignature() + "','acknowledge':'" + purchase.isAcknowledged() + "','orderID':'" + purchase.getOrderId() + "','state':'" + purchase.getPurchaseState() + "','packageName':'" + purchase.getPackageName() + "'});");
            }
        }

        if(!listsub.isEmpty()){
            for (Purchase purchase : listsub) {
                webView.loadUrl("javascript:cordova.fireDocumentEvent('verifyPurchase', { 'purchase':'" + purchase + "','id':'" + purchase.getSku() + "','token':'" + purchase.getPurchaseToken() + "','signature':'" + purchase.getSignature() + "','acknowledge':'" + purchase.isAcknowledged() + "','orderID':'" + purchase.getOrderId() + "','state':'" + purchase.getPurchaseState() + "','packageName':'" + purchase.getPackageName() + "'});");
            }
        }

        webView.loadUrl("javascript:cordova.fireDocumentEvent('onRestoreCompleted');");
    }

/////////////////////////////////LISTENERY/////////////////////////////////

    @Override
    public void onAcknowledgePurchaseResponse(@NonNull BillingResult billingResult) {
        Log.d(TAG, "onAcknowledgePurchaseResponse: ");
    }

    @Override
    public void onConsumeResponse(BillingResult billingResult, String purchaseToken) {
        Log.d(TAG, "onConsumeResponse: ");
        if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
            // Handle the success of the consume operation.
        }
    }

    @Override
    public void onPurchasesUpdated(BillingResult billingResult, List<Purchase> purchases) {

        Log.d(TAG, "billingResult.getResponseCode() 2 " + billingResult.getResponseCode());
        if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK
                && purchases != null) {
            for (Purchase purchase : purchases) {
                webView.loadUrl("javascript:cordova.fireDocumentEvent('onProductPurchased',{ 'id':'" + purchase.getSku() + "'});");
                webView.loadUrl("javascript:cordova.fireDocumentEvent('onProductUpdated',{ 'id':'" + purchase.getSku() + ");");
                webView.loadUrl("javascript:cordova.fireDocumentEvent('verifyPurchase', { 'purchase':'" + purchase + "','orderID':'" + purchase.getOrderId() + "','id':'" + purchase.getSku() + "','token':'" + purchase.getPurchaseToken() + "','signature':'" + purchase.getSignature() + "','acknowledge':'" + purchase.isAcknowledged() + "','state':'" + purchase.getPurchaseState() + "','packageName':'" + purchase.getPackageName() + "'});");


            }
        } else if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.USER_CANCELED) {
            // Handle an error caused by a user cancelling the purchase flow.
            webView.loadUrl("javascript:cordova.fireDocumentEvent('onBillingError',{ 'error':'" + "User cancelling the purchase flow" + ");");
            Log.d(TAG, "User cancelling the purchase flow");
        } else{

        }
    }

    @Override
    public void onBillingSetupFinished(@NonNull BillingResult billingResult) {
        webView.loadUrl("javascript:cordova.fireDocumentEvent('onInitCompleted');");
        Log.d(TAG, "onBillingSetupFinished: " + billingResult.getResponseCode());

        List<String> product = new ArrayList<>();
        product.add("medium_pack");
        params = SkuDetailsParams.newBuilder().setSkusList(product).setType(BillingClient.SkuType.INAPP);

        billingClient.querySkuDetailsAsync(params.build(),     new SkuDetailsResponseListener() {
            @Override
            public void onSkuDetailsResponse(BillingResult billingResult,
                                             List<SkuDetails> skuDetailsList) {
                // Process the result.

                Log.d(TAG, "onBillingSetupFinished skuDetailsList: " + skuDetailsList);

            for (Object skuDetailsObject : skuDetailsList) {

                skuDetails = (SkuDetails) skuDetailsObject;
                webView.loadUrl("javascript:cordova.fireDocumentEvent('sendSkuDetails', { 'id':'" + skuDetails.getSku() + "','description':'" + skuDetails.getDescription() + "','IconUrl':'" + skuDetails.getIconUrl() + "','IntroductoryPrice':'" + skuDetails.getIntroductoryPrice() + "','FreeTrialPeriod':'" + skuDetails.getFreeTrialPeriod() + "','OriginalPrice':'" + skuDetails.getOriginalPrice() + "','Title':'" + skuDetails.getTitle() + "','PriceCurrencyCode':'" + skuDetails.getPriceCurrencyCode() + "'});");
                BillingFlowParams billingFlowParams = BillingFlowParams.newBuilder().setSkuDetails(skuDetails).build();
                Log.d(TAG, "launchBillingFlow: ");


                int responseCode = billingClient.launchBillingFlow(cordova.getActivity(), billingFlowParams).getResponseCode();

                Log.d(TAG, "onBillingSetupFinished responseCode: " + responseCode);
            }

//                BillingFlowParams billingFlowParams = BillingFlowParams.newBuilder()
//                        .setSkuDetails(skuDetails)
//                        .build();




            }
        });



        if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
            restoreProducts();
        }
    }

    @Override
    public void onBillingServiceDisconnected() {
        webView.loadUrl("javascript:cordova.fireDocumentEvent('onBillingError',{ 'error':'" + "Billing Service Disconnected" + ");");
        Log.d(TAG, "onBillingServiceDisconnected: ");
    }

//    @Override
//    public void onSkuDetailsResponse(@NonNull BillingResult billingResult, @Nullable List<SkuDetails> list) {
//        if (list != null) {
//            for (Object skuDetailsObject : list) {
//                skuDetails = (SkuDetails) skuDetailsObject;
//                webView.loadUrl("javascript:cordova.fireDocumentEvent('sendSkuDetails', { 'id':'" + skuDetails.getSku() + "','description':'" + skuDetails.getDescription() + "','IconUrl':'" + skuDetails.getIconUrl() + "','IntroductoryPrice':'" + skuDetails.getIntroductoryPrice() + "','FreeTrialPeriod':'" + skuDetails.getFreeTrialPeriod() + "','OriginalPrice':'" + skuDetails.getOriginalPrice() + "','Title':'" + skuDetails.getTitle() + "','PriceCurrencyCode':'" + skuDetails.getPriceCurrencyCode() + "'});");
//                BillingFlowParams billingFlowParams = BillingFlowParams.newBuilder().setSkuDetails(skuDetails).build();
//                Log.d(TAG, "launchBillingFlow: ");
//
//                billingClient.launchBillingFlow(cordova.getActivity(), billingFlowParams).getResponseCode();
//            }
//        }
//    }

    //^^^^^^^^^^^^^^^^^^^^^^^^ LAST PART ^^^^^^^^^^^^^^^^^^^^^^^^\\

    @Override
    public void onStart() {
        Log.d(TAG, "onStart: ");
        super.onStart();
        if (billingClient != null) {
            // restoreProducts();
        }
    }

    @Override
    public void onResume(boolean multitasking) {
        Log.d(TAG, "onResume: ");
        super.onResume(multitasking);
        if (billingClient != null) {
            //  restoreProducts();
        }
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy: ");
        super.onDestroy();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "onActivityResult: ");
    }
}
