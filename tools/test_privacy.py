"""Check the production privacy mapping using host-JVM API stand-ins."""
from pathlib import Path
import os
import subprocess
import urllib.request

ROOT = Path(__file__).resolve().parents[1]
CACHE = ROOT / ".cache/privacy-tests"
STUBS = {'android/content/Context.java': 'package android.content; public class Context {}',
 'com/defold/extension/SignalRecorder.java': 'package com.defold.extension;\n'
                                             'public class SignalRecorder { public static final '
                                             'java.util.Map<String,Object> values = new '
                                             'java.util.HashMap<>();\n'
                                             'public static void put(String k,Object v) { '
                                             'values.put(k,v); } }',
 'com/mbridge/msdk/out/MBridgeSDKFactory.java': 'package com.mbridge.msdk.out;\n'
                                                'public class MBridgeSDKFactory { public static '
                                                'MBridgeSDKFactory getMBridgeSDK() { return new '
                                                'MBridgeSDKFactory(); }\n'
                                                'public void setDoNotTrackStatus(boolean v) { '
                                                'com.defold.extension.SignalRecorder.put("mintegral",v); '
                                                '} }',
 'com/my/target/common/MyTargetPrivacy.java': 'package com.my.target.common;\n'
                                              'public class MyTargetPrivacy { public static void '
                                              'setCcpaUserConsent(boolean v) { '
                                              'com.defold.extension.SignalRecorder.put("mytarget",v); '
                                              '} }',
 'com/unity3d/ads/metadata/MetaData.java': 'package com.unity3d.ads.metadata;\n'
                                           'public class MetaData { public '
                                           'MetaData(android.content.Context c) {} public void '
                                           'set(String k,Object v) { '
                                           'com.defold.extension.SignalRecorder.put(k,v); } public '
                                           'void commit() {} }',
 'com/unity3d/mediation/LevelPlay.java': 'package com.unity3d.mediation;\n'
                                         'public class LevelPlay { public static void '
                                         'setMetaData(String k,String v) { '
                                         'com.defold.extension.SignalRecorder.put(k,v); } }',
 'com/vungle/ads/VunglePrivacySettings.java': 'package com.vungle.ads;\n'
                                              'public class VunglePrivacySettings { public static '
                                              'void setCCPAStatus(boolean v) { '
                                              'com.defold.extension.SignalRecorder.put("vungle",v); '
                                              '} }',
 'com/yandex/mobile/ads/common/YandexAds.java': 'package com.yandex.mobile.ads.common;\n'
                                                'public class YandexAds { public static void '
                                                'setUserConsent(boolean v) { '
                                                'com.defold.extension.SignalRecorder.put("yandex",v); '
                                                '} }',
 'sg/bigo/ads/BigoAdSdk.java': 'package sg.bigo.ads;\n'
                               'public class BigoAdSdk { public static void '
                               'setUserConsent(android.content.Context c,ConsentOptions o,boolean '
                               'v) { com.defold.extension.SignalRecorder.put("bigo",v); } }',
 'sg/bigo/ads/ConsentOptions.java': 'package sg.bigo.ads; public enum ConsentOptions { CCPA }'}


def main():
    CACHE.mkdir(parents=True, exist_ok=True)
    jar = CACHE / "android-json.jar"
    if not jar.exists():
        urllib.request.urlretrieve("https://repo.maven.apache.org/maven2/com/vaadin/external/google/android-json/0.0.20131108.vaadin1/android-json-0.0.20131108.vaadin1.jar", jar)
    sources = []
    for name, source in STUBS.items():
        path = CACHE / "src" / name
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(source, encoding="utf-8")
        sources.append(path)
    sources += [ROOT / "extension-yandex-ads/src/java/com/defold/extension/PrivacySignals.java",
                ROOT / "tests/PrivacySignalsTest.java"]
    classes = CACHE / "classes"
    classes.mkdir(exist_ok=True)
    subprocess.run(["javac", "--release", "8", "-encoding", "UTF-8", "-cp", str(jar), "-d", str(classes), *map(str, sources)], check=True)
    subprocess.run(["java", "-ea", "-cp", str(classes) + os.pathsep + str(jar), "com.defold.extension.PrivacySignalsTest"], check=True)


if __name__ == "__main__":
    main()
