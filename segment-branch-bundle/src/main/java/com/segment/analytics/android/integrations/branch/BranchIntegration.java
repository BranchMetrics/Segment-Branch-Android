package com.segment.analytics.android.integrations.branch;

import android.content.Context;
import android.text.TextUtils;

import com.segment.analytics.Analytics;
import com.segment.analytics.ValueMap;
import com.segment.analytics.integrations.AliasPayload;
import com.segment.analytics.integrations.IdentifyPayload;
import com.segment.analytics.integrations.Integration;
import com.segment.analytics.integrations.Logger;
import com.segment.analytics.integrations.ScreenPayload;
import com.segment.analytics.integrations.TrackPayload;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Iterator;

import io.branch.indexing.BranchUniversalObject;
import io.branch.referral.Branch;
import io.branch.referral.Defines;
import io.branch.referral.util.BRANCH_STANDARD_EVENT;
import io.branch.referral.util.BranchEvent;
import io.branch.referral.util.CurrencyType;

/**
 * @author sahilverma
 * 2015-11-05
 */
public class BranchIntegration extends Integration<Branch> {

    private static final String BRANCH = "Branch Metrics";
    private final Logger mLogger;
    private final Branch mBranch; /* global instance */
    private final BranchUtil branchUtil = new BranchUtil();
    private static Context appContext;


    // 03/09/18 PRS :This methods is called asynchronously. Segment call this method after completing few network tasks.
    public static final Factory FACTORY = new Factory() {
        @Override
        public Integration<?> create(ValueMap settings, Analytics analytics) {
            appContext = analytics.getApplication().getApplicationContext();
            Logger logger = analytics.logger(BRANCH);
            Branch branch = Branch.getAutoInstance(analytics.getApplication());
            // 03/09/18 PRS : Since this method is called async way, Branch will not be able to hook on the life cycle
            // events for the initial activity. Initialising Branch here without depending on lifecycle method
            try {
                Method method = branch.getClass().getDeclaredMethod("registerAppReInit");
                method.setAccessible(true);
                method.invoke(branch);
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
            return new BranchIntegration(branch, logger); // still have reference to Branch Key.
        }

        @Override
        public String key() {
            return BRANCH;
        }
    };

    //region BranchIntegration creator
    private BranchIntegration(Branch branch, Logger logger) {
        this.mLogger = logger;
        this.mBranch = branch;
    }
    //endregion

    //region Analytics Maps
    @Override
    public void identify(IdentifyPayload identifyPayload) {
        super.identify(identifyPayload);
        String identity = identifyPayload.userId();
        mLogger.verbose("BranchSDK#setIdentity(%s)", identity);
        if(identity != null) {
            mBranch.setIdentity(identity);
        }
    }


    @Override
    public void track(TrackPayload track) {
        super.track(track);
        createBranchEventFromSegmentEvent(track.event(), track.properties().toJsonObject()).logEvent(appContext);
        mLogger.verbose("BranchSDK#BranchEventLogged(%s, %s)", track.event(), track.properties().toJsonObject());
    }

    @Override
    public void screen(ScreenPayload screen) {
        super.screen(screen);
        BranchEvent branchEvent = new BranchEvent(BRANCH_STANDARD_EVENT.VIEW_ITEM);
        updateBranchEventWithData(branchEvent, false, screen.properties().toJsonObject());
        branchEvent.logEvent(appContext);
        mLogger.verbose("BranchSDK#BranchEventLogged(%s, %s)", screen.event(), screen.properties().toJsonObject());
    }

    @Override
    public void alias(AliasPayload aliasPayload) {
        super.alias(aliasPayload);
        String newIdentity = aliasPayload.userId();
        mLogger.verbose("BranchSDK#setNewIdentity(%s)", newIdentity);
        mBranch.setIdentity(newIdentity);
    }

    @Override
    public void reset() {
        super.reset();
        mLogger.verbose("BranchSDK#Logout()");
        mBranch.logout();
    }
    //endregion

    //region Singleton
    @Override
    public Branch getUnderlyingInstance() {
        return mBranch;
    }
    //endregion


    private BranchEvent createBranchEventFromSegmentEvent(String eventName, JSONObject payload) {
        BranchEvent branchEvent;
        if (branchUtil.getBranchStandardEvent(eventName) != null) {
            branchEvent = new BranchEvent(branchUtil.getBranchStandardEvent(eventName));
            updateBranchEventWithData(branchEvent, false, payload);
        } else {
            branchEvent = new BranchEvent(eventName);
            updateBranchEventWithData(branchEvent, true, payload);
        }
        return branchEvent;
    }

    private void updateBranchEventWithData(BranchEvent branchEvent, boolean isCustomEvent, JSONObject payload) {
        io.branch.referral.BranchUtil.JsonReader reader = new io.branch.referral.BranchUtil.JsonReader(payload);
         /* BranchEvent fields:
            transactionID;
            currency;
            revenue;
            shipping;
            tax;
            coupon;
            affiliation;
            eventDescription;
            searchQuery;
            */
        
            /* Segment event fields:
        
            "order_id": "50314b8e9bcf000000000000",
            "affiliation": "Google Store",
            "value": 30,
            "revenue": 25,
            "shipping": 3,
            "tax": 2,
            "discount": 2.5,
            "coupon": "hasbros",
            "currency": "USD"
            "query"
            "products" // An array of product objects
         */

        // First Read all known params and add to the event
        branchEvent.setAffiliation(reader.readOutString("affiliation"));
        branchEvent.setCoupon(reader.readOutString("coupon"));
        branchEvent.setRevenue(reader.readOutDouble("revenue"));
        branchEvent.setSearchQuery(reader.readOutString("query"));
        branchEvent.setShipping(reader.readOutDouble("shipping"));
        branchEvent.setTax(reader.readOutDouble("tax"));
        branchEvent.setTransactionID(reader.readOutString("order_id"));
        String currencyStr = reader.readOutString("currency");
        if (!TextUtils.isEmpty(currencyStr)) {
            branchEvent.setCurrency(CurrencyType.getValue(currencyStr));
        }

        JSONArray prodArray = reader.readOutJsonArray("products");
        JSONObject customParams = reader.getJsonObject();
        convertToBranchKeys(customParams);
        if (prodArray != null) {
            for (int i = 0; i < prodArray.length(); i++) {
                JSONObject prod = prodArray.optJSONObject(i);
                if (prod != null) {
                    convertToBranchKeys(prod);
                    // Merge all custom data to the event to the product
                    for (Iterator<String> it = customParams.keys(); it.hasNext(); ) {
                        String key = it.next();
                        try {
                            // Segment event may have fields which are common for events and the product associated with the events(price, sku etc).
                            // If there is product specific value use that for BUO
                            if (!prod.has(key)) {
                                prod.putOpt(key, customParams.optString(key));
                            }
                        } catch (JSONException ignore) {
                        }
                    }
                    BranchUniversalObject buo = BranchUniversalObject.createInstance(prod);
                    if (buo.getContentMetadata().productCategory == null && !TextUtils.isEmpty(prod.optString(Defines.Jsonkey.ProductCategory.getKey()))) {
                        buo.getContentMetadata().addCustomMetadata("category", prod.optString(Defines.Jsonkey.ProductCategory.getKey()));
                    }
                    if (buo.getContentMetadata().condition == null && !TextUtils.isEmpty(prod.optString(Defines.Jsonkey.Condition.getKey()))) {
                        buo.getContentMetadata().addCustomMetadata("condition", prod.optString(Defines.Jsonkey.Condition.getKey()));
                    }
                    if (isCustomEvent) {
                        branchEvent.addCustomDataProperty(prod.optString("id"), buo.convertToJson().toString());
                    } else {
                        branchEvent.addContentItems(buo);
                    }
                }
            }
        } else {
            for (Iterator<String> it = customParams.keys(); it.hasNext(); ) {
                String key = it.next();
                branchEvent.addCustomDataProperty(key, customParams.optString(key));
            }
        }
    }

    private void convertToBranchKeys(JSONObject object) {
        replaceKey(object, "sku", Defines.Jsonkey.SKU.getKey());
        replaceKey(object, "price", Defines.Jsonkey.Price.getKey());
        replaceKey(object, "name", Defines.Jsonkey.ProductName.getKey());
        replaceKey(object, "category", Defines.Jsonkey.ProductCategory.getKey());
        replaceKey(object, "title", Defines.Jsonkey.ContentTitle.getKey());
        replaceKey(object, "brand", Defines.Jsonkey.ProductBrand.getKey());
        replaceKey(object, "rating", Defines.Jsonkey.Rating.getKey());
        replaceKey(object, "condition", Defines.Jsonkey.Condition.getKey());
        replaceKey(object, "id", Defines.Jsonkey.CanonicalIdentifier.getKey());
        replaceKey(object, "url", Defines.Jsonkey.CanonicalUrl.getKey());
        replaceKey(object, "image_url", Defines.Jsonkey.ContentImgUrl.getKey());
    }


    private void replaceKey(JSONObject object, String originalKey, String branchKey) {
        try {
            String val = object.optString(originalKey);
            if (!TextUtils.isEmpty(val)) {
                object.remove(originalKey);
                object.put(branchKey, val);
            }
        } catch (JSONException ignore) {
        }
    }
}
