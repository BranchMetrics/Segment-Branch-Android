package com.segment.analytics.android.integrations.branch;

import android.content.Context;

import com.segment.analytics.Analytics;
import com.segment.analytics.ValueMap;
import com.segment.analytics.integrations.IdentifyPayload;
import com.segment.analytics.integrations.Integration;
import com.segment.analytics.integrations.Logger;
import com.segment.analytics.integrations.ScreenPayload;
import com.segment.analytics.integrations.TrackPayload;

import org.json.JSONObject;

import io.branch.referral.Branch;

/**
 * @author sahilverma
 * @date 2015-11-05
 */
public class BranchIntegration extends Integration<Branch> {

    private static final String BRANCH = "branch_key";
    private static final String VIEWED_SCREEN = "viewed_screen_";
    final Logger mLogger;
    final Branch mBranch; /* global instance */
    final String mBranch_Key;

    public static final Factory FACTORY = new Factory() {

        @Override
        public Integration<?> create(ValueMap settings, Analytics analytics) {
            Logger logger = analytics.logger(BRANCH);
            String branch_key = settings.getString("branch_key");
            Context applicationContext = analytics.getApplication().getApplicationContext();
            Branch branch = Branch.getAutoInstance(applicationContext);
            return new BranchIntegration(branch, logger, branch_key); // still have reference to Branch Key.
        }

        @Override
        public String key() {
            return BRANCH;
        }
    };

    //region BranchIntegration creator
    public BranchIntegration(Branch branch, Logger logger, String branch_key) {
        this.mLogger = logger;
        this.mBranch = branch;
        this.mBranch_Key = branch_key;
    }
    //endregion

    //region Analytics Maps
    @Override
    public void identify(IdentifyPayload identifyPayload) {
        super.identify(identifyPayload);
        String identity = identifyPayload.userId();
        mLogger.verbose("BranchSDK#setIdentity(%s)", identity);
        mBranch.setIdentity(identity);
    }

    @Override
    public void track(TrackPayload track) {
        super.track(track);
        // note, that the #event method returns the name of the event
        String eventName = track.event();
        JSONObject metaData = track.properties().toJsonObject();
        mLogger.verbose("BranchSDK#userCompletedAction(%s, %s)", eventName, metaData);
        mBranch.userCompletedAction(eventName, metaData);
    }

    @Override
    public void screen(ScreenPayload screen) {
        super.screen(screen);
        String eventName = screen.event();
        JSONObject metaData = screen.properties().toJsonObject();
        mLogger.verbose("BranchSDK#userCompletedAction(%s, %s)", eventName, metaData);
        mBranch.userCompletedAction(VIEWED_SCREEN + eventName, metaData);
    }
    //endregion

    //region Singleton
    @Override
    public Branch getUnderlyingInstance() {
        return mBranch;
    }
    //endregion
}
