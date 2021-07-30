    var exec = require('cordova/exec');

    //^^^^^^^^^^^^^^^^^^^^^^^^ CONDITIONS PART ^^^^^^^^^^^^^^^^^^^^^^^^\\

    exports.onPurchaseFailed = function (callback, success, error){
        document.addEventListener('onPurchaseFailed', function(data_){callback(data_);});
    }
    exports.onProductUpdated = function (callback, success, error){
        document.addEventListener('onProductUpdated', function(data_){callback(data_);});
    }
    exports.onProductPurchased = function (callback, success, error){
        document.addEventListener('onProductPurchased', function(data_){callback(data_);});
    }
    exports.onRestoreCompleted = function (callback, success, error){
        document.addEventListener('onRestoreCompleted', function(data_){callback(data_);});
    }
    exports.onInitCompleted = function (callback, success, error){
        document.addEventListener('onInitCompleted', function(){callback();});
    }
    exports.hasProduct = function (callback, success, error){
        document.addEventListener('hasProduct', function(data_){
            callback(data_.isPurchased);});
    }
    exports.onBillingError = function (callback, success, error){//JUST ERROR
        document.addEventListener('onBillingError', function(data_){
            callback(data_.errorCode);});
    }
    exports.verifyPurchase = function (callback, success, error){
        document.addEventListener('verifyPurchase', function(data_){
            callback(data_);});
    }
    exports.sendSkuDetails = function (callback, success, error){
        document.addEventListener('sendSkuDetails', function(data_){
            callback(data_);});
    }

    //^^^^^^^^^^^^^^^^^^^^^^^^ ACTION PART ^^^^^^^^^^^^^^^^^^^^^^^^\\

    exports.Initialize = function (success, error) {
        exec(success, error, 'googleInAppPurchase','initBilling');
    };
    exports.purchaseProduct = function (ID, TYPE, success, error) {
        exec(success, error, 'googleInAppPurchase','purchaseProduct',[ID,TYPE]);
    };
    exports.consumeProduct = function (Purchase, success, error) {
        exec(success, error, 'googleInAppPurchase','consumeProduct',[Purchase]);
    };
    exports.restoreProducts = function (Purchase, success, error) {
        exec(success, error, 'googleInAppPurchase','restoreProducts',[Purchase]);
    };
    exports.acknowledgePurchase = function (Purchase , success, error) {
        exec(success, error, 'googleInAppPurchase','acknowledgePurchase',[Purchase]);
    };