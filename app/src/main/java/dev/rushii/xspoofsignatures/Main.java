package dev.rushii.xspoofsignatures;

import android.content.pm.*;
import android.os.Build;
import android.util.Log;

import de.robv.android.xposed.*;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

@SuppressWarnings("deprecation")
public class Main implements IXposedHookLoadPackage {
    private static final String TAG = "XSpoofSignatures";

    @Override
    public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable {
        if ("android".equals(lpparam.packageName)) return;

        XC_MethodHook hook = new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    PackageManager.PackageInfoFlags flags = (PackageManager.PackageInfoFlags) param.args[1];
                    long val = flags.getValue();
                    param.args[1] = PackageManager.PackageInfoFlags.of(val | PackageManager.GET_META_DATA);
                } else {
                    param.args[1] = (int) param.args[1] | PackageManager.GET_META_DATA;
                }
            }

            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                PackageInfo pi = (PackageInfo) param.getResult();
                if (pi == null || pi.applicationInfo == null || pi.applicationInfo.metaData == null) return;
                String fakeSig = pi.applicationInfo.metaData.getString("fake-signature");
                if (fakeSig == null) return;
                spoofPackageInfo(pi, fakeSig);
            }
        };
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            XposedHelpers.findAndHookMethod("android.app.ApplicationPackageManager", lpparam.classLoader, 
                "getPackageInfo", String.class, PackageManager.PackageInfoFlags.class, hook);
        } else {
            XposedHelpers.findAndHookMethod("android.app.ApplicationPackageManager", lpparam.classLoader, 
                "getPackageInfo", String.class, int.class, hook);
        }
    }

    private void spoofPackageInfo(PackageInfo pi, String fakeSigStr) {
		try {
			Signature spoofedSig = new Signature(fakeSigStr);
			Signature[] sigArray = new Signature[]{spoofedSig};
			Log.d(TAG, "Spoofing signatures for " + pi.packageName);
			pi.signatures = sigArray;
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && pi.signingInfo != null) {
				Object signingDetails = XposedHelpers.getObjectField(pi.signingInfo, "mSigningDetails");
				if (signingDetails != null) {
					String fieldName = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) ? "mSignatures" : "signatures";
					XposedHelpers.setObjectField(signingDetails, fieldName, sigArray);
					XposedHelpers.setObjectField(signingDetails, "pastSigningCertificates", null);
					XposedHelpers.setIntField(signingDetails, "signatureSchemeVersion", 3);
				}
			}
		} catch (Exception e) {
			Log.e(TAG, "Spoofing failed for " + pi.packageName, e);
		}
	}

    private Signature[] copySignatures(Signature[] orig, Signature extra) {
        Signature[] signatures = new Signature[orig.length + 1];
        signatures[0] = extra;
        System.arraycopy(orig, 0, signatures, 1, orig.length);
        return signatures;
    }
}
