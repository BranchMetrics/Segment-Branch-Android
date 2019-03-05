package com.segment.analytics.android.integrations.branch;

import java.util.HashMap;

import io.branch.referral.util.BRANCH_STANDARD_EVENT;

/**
 * Created by sojanpr on 3/9/18.
 */

public class BranchUtil {
    
    private final HashMap<String, BRANCH_STANDARD_EVENT> BranchSegmentEventNames;
    
    public BranchUtil() {
        BranchSegmentEventNames = new HashMap<>();
        // Mapping Segment event names to corresponding Brach events
        BranchSegmentEventNames.put("Products Searched", BRANCH_STANDARD_EVENT.SEARCH);
        BranchSegmentEventNames.put("Product Viewed", BRANCH_STANDARD_EVENT.VIEW_ITEM);
        BranchSegmentEventNames.put("Product Added to Wishlist", BRANCH_STANDARD_EVENT.ADD_TO_WISHLIST);
        BranchSegmentEventNames.put("Payment Info Entered", BRANCH_STANDARD_EVENT.ADD_PAYMENT_INFO);
        BranchSegmentEventNames.put("Product Reviewed", BRANCH_STANDARD_EVENT.RATE);
        BranchSegmentEventNames.put("Product List Viewed", BRANCH_STANDARD_EVENT.VIEW_ITEMS);
        BranchSegmentEventNames.put("Order Completed", BRANCH_STANDARD_EVENT.PURCHASE);
        BranchSegmentEventNames.put("Cart Shared", BRANCH_STANDARD_EVENT.SHARE);
        BranchSegmentEventNames.put("Cart Viewed", BRANCH_STANDARD_EVENT.VIEW_CART);
        BranchSegmentEventNames.put("Product Shared", BRANCH_STANDARD_EVENT.SHARE);
        BranchSegmentEventNames.put("Product Added", BRANCH_STANDARD_EVENT.ADD_TO_CART);
        BranchSegmentEventNames.put("Coupon Applied", BRANCH_STANDARD_EVENT.SPEND_CREDITS);
        BranchSegmentEventNames.put("Checkout Started", BRANCH_STANDARD_EVENT.INITIATE_PURCHASE);
        BranchSegmentEventNames.put("Product Clicked", BRANCH_STANDARD_EVENT.VIEW_ITEM);
    }
    
    /**
     * Get a matching {@link BRANCH_STANDARD_EVENT} for the segment event name provided
     *
     * @param segmentEventName {@link String} segment event name
     * @return {@link BRANCH_STANDARD_EVENT} if there a matching event for the given segment event
     */
    public BRANCH_STANDARD_EVENT getBranchStandardEvent(String segmentEventName) {
        return BranchSegmentEventNames.get(segmentEventName);
    }
    
}
