package com.defold.extension;

import android.content.Context;
import org.json.JSONObject;
import com.unity3d.ads.metadata.MetaData;
import com.unity3d.mediation.LevelPlay;
import com.mbridge.msdk.out.MBridgeSDKFactory;
import com.my.target.common.MyTargetPrivacy;
import com.vungle.ads.VunglePrivacySettings;
import sg.bigo.ads.BigoAdSdk;
import sg.bigo.ads.ConsentOptions;

/** Runs before Yandex initialization and each consent revision, on the Android UI thread. */
final class PrivacySignals {
    static void apply(Context context, JSONObject payload) throws Exception {
        JSONObject cmp = payload.getJSONObject("cmp");
        if (!(cmp.get("gdpr_applies") instanceof Boolean)
                || !(cmp.get("us_regulation_applies") instanceof Boolean)) {
            throw new IllegalArgumentException("Unknown regulation applicability");
        }
        JSONObject consent = payload.getJSONObject("consent");
        if (cmp.getBoolean("gdpr_applies")) {
            String tc = consent.getJSONObject("storage").getString("IABTCF_TCString");
            if (tc.isEmpty()) throw new IllegalArgumentException("Missing TC string");
            // Yandex and its mediation adapters read current TCF/AC preferences.
            // Do not override partial vendor/purpose choices with a global grant.
        }
        if (cmp.getBoolean("us_regulation_applies")) {
            JSONObject us = consent.getJSONObject("us_privacy");
            if (!us.getBoolean("known")) throw new IllegalArgumentException("Unknown US choices");
            boolean optOut = us.getBoolean("opt_out");
            // Restrict Yandex's own data processing as well as mediated networks.
            // Never let a US opt-in override a simultaneous GDPR choice.
            if (optOut || !cmp.getBoolean("gdpr_applies")) {
                com.yandex.mobile.ads.common.YandexAds.setUserConsent(!optOut);
            }
            MetaData unity = new MetaData(context);
            unity.set("privacy.consent", !optOut);
            unity.commit();
            LevelPlay.setMetaData("do_not_sell", Boolean.toString(optOut));
            MBridgeSDKFactory.getMBridgeSDK().setDoNotTrackStatus(optOut);
            MyTargetPrivacy.setCcpaUserConsent(!optOut);
            VunglePrivacySettings.setCCPAStatus(!optOut);
            BigoAdSdk.setUserConsent(context, ConsentOptions.CCPA, !optOut);
        }
    }
}
