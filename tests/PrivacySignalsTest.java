package com.defold.extension;
import org.json.JSONObject;
public class PrivacySignalsTest {
    public static void main(String[] args) throws Exception {
        JSONObject input = new JSONObject("{\"cmp\":{\"gdpr_applies\":false,\"us_regulation_applies\":true},\"consent\":{\"us_privacy\":{\"known\":true,\"opt_out\":true}}}");
        for (boolean optOut : new boolean[]{true, false}) {
            SignalRecorder.values.clear();
            input.getJSONObject("consent").getJSONObject("us_privacy").put("opt_out", optOut);
            PrivacySignals.apply(new android.content.Context(), input);
            assert SignalRecorder.values.size() == 7;
            for (String key : new String[]{"privacy.consent", "mytarget", "vungle", "bigo", "yandex"})
                assert SignalRecorder.values.get(key).equals(!optOut) : key;
            assert SignalRecorder.values.get("mintegral").equals(optOut);
            assert SignalRecorder.values.get("do_not_sell").equals(Boolean.toString(optOut));
        }
        SignalRecorder.values.clear();
        input.getJSONObject("consent").getJSONObject("us_privacy").put("known", false);
        try { PrivacySignals.apply(new android.content.Context(), input); throw new AssertionError("Unknown US allowed"); }
        catch (IllegalArgumentException expected) {}
        assert SignalRecorder.values.isEmpty();
        input.getJSONObject("cmp").put("us_regulation_applies", false).put("gdpr_applies", true);
        input.getJSONObject("consent").put("storage", new JSONObject("{\"IABTCF_TCString\":\"partial-choice\"}"));
        PrivacySignals.apply(new android.content.Context(), input);
        assert SignalRecorder.values.isEmpty() : "TCF must not be replaced by a global consent grant";
        // Simultaneous GDPR and US restrictions must not become a blanket grant.
        input.getJSONObject("cmp").put("us_regulation_applies", true);
        input.getJSONObject("consent").getJSONObject("us_privacy").put("known", true).put("opt_out", false);
        SignalRecorder.values.clear();
        PrivacySignals.apply(new android.content.Context(), input);
        assert !SignalRecorder.values.containsKey("yandex");
        input.getJSONObject("consent").getJSONObject("us_privacy").put("opt_out", true);
        PrivacySignals.apply(new android.content.Context(), input);
        assert SignalRecorder.values.get("yandex").equals(false);
        input.getJSONObject("cmp").put("gdpr_applies", "unknown");
        SignalRecorder.values.clear();
        try { PrivacySignals.apply(new android.content.Context(), input); throw new AssertionError("Unknown applicability allowed"); }
        catch (IllegalArgumentException expected) {}
        assert SignalRecorder.values.isEmpty();
        input.getJSONObject("cmp").put("gdpr_applies", true);
        input.getJSONObject("consent").getJSONObject("storage").put("IABTCF_TCString", "");
        try { PrivacySignals.apply(new android.content.Context(), input); throw new AssertionError("Empty TCF allowed"); }
        catch (IllegalArgumentException expected) {}
        assert SignalRecorder.values.isEmpty();
        System.out.println("Mediation privacy signal mapping tests passed");
    }
}
