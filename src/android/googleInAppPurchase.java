package org.apache.cordova.googleInAppPurchase;

import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;

import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaWebView;

import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.PurchasesResponseListener;
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


import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static java.lang.Integer.parseInt;

////////////////////////////////////////////////////
public class googleInAppPurchase extends CordovaPlugin implements BillingClientStateListener, ConsumeResponseListener {

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
    public Integer currentProductType;
    public BillingResult billingResult = new BillingResult();
    public Purchase lastPurchase;

    /////////////////////////////////////////////////////////////////////////////
    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
    }

    /////////////////////////////////////////////////////////////////////////////
    @Override//funkcja która łączy się z JS
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        if (action.equals("purchaseProduct")) {
            cordova.getThreadPool().execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        if (billingClient != null) {
                            purchaseProduct(callbackContext, args);
                        }

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            });
        } else if (action.equals("initBilling")) {
            initBilling(callbackContext, args);
        } else if (action.equals("restoreProducts")) {
            restoreProducts();
        } else if (action.equals("consumeProduct")) {
            consumeProduct(lastPurchase);
        } else if (action.equals("acknowledgePurchase")) {
            acknowledgePurchase(lastPurchase);
        }
        return false;  // Returning false results in a "MethodNotFound" error.
    }

    //^^^^^^^^^^^^^^^^^^^^^^^^ BILLING PART ^^^^^^^^^^^^^^^^^^^^^^^^\\

    private PurchasesResponseListener purchasesResponseListener = new PurchasesResponseListener() {
        @Override
        public void onQueryPurchasesResponse(BillingResult billingResult, List<Purchase> list) {
            Log.d(TAG, "onQueryPurchasesResponse list: " + list);

            for (Purchase purchase : list) {
                Log.d(TAG, "onQueryPurchasesResponse purchase: " + purchase);
                Log.d(TAG, "onQueryPurchasesResponse purchase.isAcknowledged(): " + purchase.isAcknowledged());

                webView.loadUrl("javascript:cordova.fireDocumentEvent('verifyPurchase', { 'purchase':'" + purchase + "','id':'" + purchase.getSkus() + "','token':'" + purchase.getPurchaseToken() + "','signature':'" + purchase.getSignature() + "','acknowledge':'" + purchase.isAcknowledged() + "','orderID':'" + purchase.getOrderId() + "','state':'" + purchase.getPurchaseState() + "','packageName':'" + purchase.getPackageName() + "'})");

                if(!purchase.isAcknowledged()){
                    consumeProduct(purchase);
                }

            }
        }
    };

    private PurchasesUpdatedListener purchasesUpdatedListener = new PurchasesUpdatedListener() {
        @Override
        public void onPurchasesUpdated(BillingResult billingResult, List<Purchase> purchases) {
            // To be implemented in a later section.
            Log.d(TAG, "onPurchasesUpdated : ");
            Log.d(TAG, "onPurchasesUpdated purchases : " + purchases);
            Log.d(TAG, "onPurchasesUpdated billingResult code: " + billingResult.getResponseCode());
            Log.d(TAG, "onPurchasesUpdated currentProductType: " + currentProductType);

            if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK
                    && purchases != null) {

                for (Purchase purchase : purchases) {
                    Log.d(TAG, "onPurchasesUpdated purchase.getPurchaseState() : " + purchase.getPurchaseState());

                    if(purchase.getPurchaseState() == 1){
                        lastPurchase = purchase;

                        String productId = purchase.getSkus().get(0);
                        Log.d(TAG, "onPurchasesUpdated productId: " + productId);

                        goToUrl("javascript:cordova.fireDocumentEvent('onProductUpdated', {'id': '" + productId + "'})");
                        goToUrl("javascript:cordova.fireDocumentEvent('verifyPurchase', { 'purchase':'" + purchase + "','orderID':'" + purchase.getOrderId() + "','id':'" + productId + "','token':'" + purchase.getPurchaseToken() + "','signature':'" + purchase.getSignature() + "','acknowledge':'" + purchase.isAcknowledged() + "','state':'" + purchase.getPurchaseState() + "','packageName':'" + purchase.getPackageName() + "'})");
                    }
                }

            } else if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.USER_CANCELED) {
                // Handle an error caused by a user cancelling the purchase flow.
                goToUrl("javascript:cordova.fireDocumentEvent('onBillingError',{ 'error':'" + "User cancelling the purchase flow" + "})");
                Log.d(TAG, "User cancelling the purchase flow");
            } else{

            }
        }
    };

    private void initBilling(final CallbackContext callbackContext, final JSONArray data) throws JSONException {
        billingClient = BillingClient.newBuilder(cordova.getActivity())
                .setListener(purchasesUpdatedListener)
                .enablePendingPurchases()
                .build();

        billingClient.startConnection(this);

        Log.d(TAG, "billingResult getDebugMessage: " + billingResult.getDebugMessage());
        Log.d(TAG, "billingResult getResponseCode: " + billingResult.getResponseCode());
    }

    private void purchaseProduct(final CallbackContext callbackContext, final JSONArray data) throws JSONException {
        String localID = data.getString(0);
        String type = data.getString(1);

        if (!billingClient.isReady()) {
            Log.d(TAG, "Billing not initalized, before you purchaseProduct you must to initialize billing");

            initBilling(callbackContext, data);
            goToUrl("javascript:cordova.fireDocumentEvent('onPurchaseFailed',{ 'id':'" + localID + "})");
            return;
        }

        Log.d(TAG, "purchaseProduct ID: " + localID);
        Log.d(TAG, "purchaseProduct type: " + type);

        // INFO
        // Type 2 -> SUBSCRIBE
        // Type 1 -> CONSUMABLE
        // Type 0 -> NON-CONSUMABLE

        // TODO C2 listeners handle
        if (type.equals("2")) {
            List<String> product = new ArrayList<>();
            product.add(localID);
            params = SkuDetailsParams.newBuilder().setSkusList(product).setType(BillingClient.SkuType.SUBS);

            billingClient.querySkuDetailsAsync(params.build(),     new SkuDetailsResponseListener() {
                @Override
                public void onSkuDetailsResponse(BillingResult billingResult,
                                                 List<SkuDetails> skuDetailsList) {

                    Log.d(TAG, "onBillingSetupFinished skuDetailsList: " + skuDetailsList);

                    for (Object skuDetailsObject : skuDetailsList) {

                        skuDetails = (SkuDetails) skuDetailsObject;
                        goToUrl("javascript:cordova.fireDocumentEvent('sendSkuDetails', { 'id':'" + skuDetails.getSku() + "','description':'" + skuDetails.getDescription() + "','IconUrl':'" + skuDetails.getIconUrl() + "','IntroductoryPrice':'" + skuDetails.getIntroductoryPrice() + "','FreeTrialPeriod':'" + skuDetails.getFreeTrialPeriod() + "','OriginalPrice':'" + skuDetails.getOriginalPrice() + "','Title':'" + skuDetails.getTitle() + "','PriceCurrencyCode':'" + skuDetails.getPriceCurrencyCode() + "'})");
                        BillingFlowParams billingFlowParams = BillingFlowParams.newBuilder().setSkuDetails(skuDetails).build();
                        Log.d(TAG, "launchBillingFlow: ");

                        int responseCode = billingClient.launchBillingFlow(cordova.getActivity(), billingFlowParams).getResponseCode();
                        Log.d(TAG, "onBillingSetupFinished responseCode: " + responseCode);
                    }

                }
            });

        } else if (type.equals("1") || type.equals("0")) {

            List<String> product = new ArrayList<>();
            product.add(localID);

            currentProductType = parseInt(type);

            params = SkuDetailsParams.newBuilder().setSkusList(product).setType(BillingClient.SkuType.INAPP);

            billingClient.querySkuDetailsAsync(params.build(),     new SkuDetailsResponseListener() {
                @Override
                public void onSkuDetailsResponse(BillingResult billingResult,
                                                 List<SkuDetails> skuDetailsList) {

                    Log.d(TAG, "onSkuDetailsResponse skuDetailsList: " + skuDetailsList);

                    for (Object skuDetailsObject : skuDetailsList) {

                        skuDetails = (SkuDetails) skuDetailsObject;
                        goToUrl("javascript:cordova.fireDocumentEvent('sendSkuDetails', { 'id':'" + skuDetails.getSku() + "','description':'" + skuDetails.getDescription() + "','IconUrl':'" + skuDetails.getIconUrl() + "','IntroductoryPrice':'" + skuDetails.getIntroductoryPrice() + "','FreeTrialPeriod':'" + skuDetails.getFreeTrialPeriod() + "','OriginalPrice':'" + skuDetails.getOriginalPrice() + "','Title':'" + skuDetails.getTitle() + "','PriceCurrencyCode':'" + skuDetails.getPriceCurrencyCode() + "'})");
                        BillingFlowParams billingFlowParams = BillingFlowParams.newBuilder().setSkuDetails(skuDetails).build();
                        Log.d(TAG, "launchBillingFlow: ");

                        int responseCode = billingClient.launchBillingFlow(cordova.getActivity(), billingFlowParams).getResponseCode();
                        Log.d(TAG, "onSkuDetailsResponse responseCode: " + responseCode);
                    }

                }
            });

        } else {
            Log.d(TAG, "*** FAIL -> purchaseProduct ");
            goToUrl("javascript:cordova.fireDocumentEvent('onPurchaseFailed',{ 'id':'" + localID + "})");
            return;
        }
    }


    // Method to consume consumable purchases
    void consumeProduct(Purchase purchase){
        Log.d(TAG, "consumeProduct purchase.getPurchaseToken(): " + purchase.getPurchaseToken());

        ConsumeParams consumeParams =
                ConsumeParams.newBuilder()
                        .setPurchaseToken(purchase.getPurchaseToken())
                        .build();

        ConsumeResponseListener listener = new ConsumeResponseListener() {
            @Override
            public void onConsumeResponse(BillingResult billingResult, String purchaseToken) {
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    // Handle the success of the consume operation.
                }
            }
        };

        billingClient.consumeAsync(consumeParams, listener);

    }

    //Method to acknowledge purchases non-consumable and subs

    public AcknowledgePurchaseResponseListener acknowledgePurchaseResponseListener = new AcknowledgePurchaseResponseListener() {
        @Override
        public void onAcknowledgePurchaseResponse(BillingResult billingResult) {
            Log.d(TAG, "*** acknowledgePurchase 3");
        }

    };

    void acknowledgePurchase(Purchase purchase){
        Log.d(TAG, "*** acknowledgePurchase");

        if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
            if (!purchase.isAcknowledged()) {
                AcknowledgePurchaseParams acknowledgePurchaseParams =
                        AcknowledgePurchaseParams.newBuilder()
                                .setPurchaseToken(purchase.getPurchaseToken())
                                .build();
                billingClient.acknowledgePurchase(acknowledgePurchaseParams, acknowledgePurchaseResponseListener);

                Log.d(TAG, "*** acknowledgePurchase 2");
            }
        }
    }

    private void restoreProducts() {
        if (billingClient == null) {
            goToUrl("javascript:cordova.fireDocumentEvent('onBillingError',{ 'error':'" + "billingClient is null" + "})");
            return;
        }

        Log.d(TAG, "restoreProducts: ");
        billingClient.queryPurchasesAsync(BillingClient.SkuType.INAPP, purchasesResponseListener);
    }

/////////////////////////////////LISTENERY/////////////////////////////////

    @Override
    public void onConsumeResponse(BillingResult billingResult, String purchaseToken) {
        Log.d(TAG, "onConsumeResponse: ");
        if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
            // Handle the success of the consume operation.
        }
    }

    @Override
    public void onBillingSetupFinished(@NonNull BillingResult billingResult) {

        goToUrl("javascript:cordova.fireDocumentEvent('onInitCompleted');");
        Log.d(TAG, "onBillingSetupFinished: " + billingResult.getResponseCode());
        if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
            restoreProducts();
        }
    }

    @Override
    public void onBillingServiceDisconnected() {
        goToUrl("javascript:cordova.fireDocumentEvent('onBillingError',{ 'error':'" + "Billing Service Disconnected" + "});");
        Log.d(TAG, "onBillingServiceDisconnected: ");
    }

//    @Override
//    public void onSkuDetailsResponse(@NonNull BillingResult billingResult, @Nullable List<SkuDetails> list) {
//        if (list != null) {
//            for (Object skuDetailsObject : list) {
//                skuDetails = (SkuDetails) skuDetailsObject;
//                goToUrl("javascript:cordova.fireDocumentEvent('sendSkuDetails', { 'id':'" + skuDetails.getSku() + "','description':'" + skuDetails.getDescription() + "','IconUrl':'" + skuDetails.getIconUrl() + "','IntroductoryPrice':'" + skuDetails.getIntroductoryPrice() + "','FreeTrialPeriod':'" + skuDetails.getFreeTrialPeriod() + "','OriginalPrice':'" + skuDetails.getOriginalPrice() + "','Title':'" + skuDetails.getTitle() + "','PriceCurrencyCode':'" + skuDetails.getPriceCurrencyCode() + "'});");
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

    public void goToUrl(String url){
        // fragment using getActivity ()
        cordova.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                webView.loadUrl(url);
            }
        });
    }

}
