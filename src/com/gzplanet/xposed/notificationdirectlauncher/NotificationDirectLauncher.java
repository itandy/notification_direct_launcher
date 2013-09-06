package com.gzplanet.xposed.notificationdirectlauncher;

import java.lang.reflect.Method;

import android.app.ActivityManagerNative;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Build;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;

import com.android.internal.statusbar.IStatusBarService;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class NotificationDirectLauncher implements IXposedHookLoadPackage {
	final static String TAG = "NotificationDirectLauncher";
	final static String PKGNAME_SETTINGS = "com.android.systemui";
	final static String CLASS_PHONESTATUSBAR = "com.android.systemui.statusbar.phone.PhoneStatusBar";
	final static String CLASS_BASESTATUSBAR = "com.android.systemui.statusbar.BaseStatusBar";
	final static String CLASS_NOTIFICATIONCLICKER = "com.android.systemui.statusbar.BaseStatusBar.NotificationClicker";
	final static int FLAG_EXCLUDE_NONE = 0;

	Context mContext;
	IStatusBarService mBarService;
	Object mPhoneStatusBar;
	Method mVisibilityChanged;

	@Override
	public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable {
		if (!lpparam.packageName.equals(PKGNAME_SETTINGS))
			return;

		mVisibilityChanged = XposedHelpers.findMethodExact(CLASS_BASESTATUSBAR, lpparam.classLoader,
				"visibilityChanged", boolean.class);

		XposedHelpers.findAndHookMethod(CLASS_PHONESTATUSBAR, lpparam.classLoader, "start", new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				mContext = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");
				mBarService = (IStatusBarService) XposedHelpers.getObjectField(param.thisObject, "mBarService");
				mPhoneStatusBar = param.thisObject;

//				Log.d(TAG, "After PhoneStatusBar.start(): " + mPhoneStatusBar.hashCode());
			}
		});

		XposedHelpers.findAndHookMethod(CLASS_NOTIFICATIONCLICKER, lpparam.classLoader, "onClick", View.class,
				new XC_MethodReplacement() {

					/*
					 * Custom implementation of NotificationClicker.onClick
					 */
					@Override
					protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
//						Log.d(TAG, "onClick - new");

						// get member variables
						PendingIntent intent = (PendingIntent) XposedHelpers
								.getObjectField(param.thisObject, "mIntent");
						String pkg = (String) XposedHelpers.getObjectField(param.thisObject, "mPkg");
						String tag = (String) XposedHelpers.getObjectField(param.thisObject, "mTag");
						int id = XposedHelpers.getIntField(param.thisObject, "mId");

						// get parameter
						View v = (View) param.args[0];

						try {
							ActivityManagerNative.getDefault().resumeAppSwitches();
							ActivityManagerNative.getDefault().dismissKeyguardOnNextActivity();
						} catch (RemoteException e) {
						}

						if (intent != null) {
							int[] pos = new int[2];
							v.getLocationOnScreen(pos);
							Intent overlay = new Intent();
							overlay.setSourceBounds(new Rect(pos[0], pos[1], pos[0] + v.getWidth(), pos[1]
									+ v.getHeight()));
							try {
								intent.send(mContext, 0, overlay);
							} catch (PendingIntent.CanceledException e) {
								// the stack trace isn't very helpful here. Just log the exception message.
								XposedBridge.log("Sending contentIntent failed: " + e);
							}

							// Avoid the display of keyguard
							/*
							 * KeyguardManager kgm = (KeyguardManager)
							 * mContext.getSystemService(Context.KEYGUARD_SERVICE); if (kgm != null)
							 * kgm.exitKeyguardSecurely(null);
							 */}

						try {
							mBarService.onNotificationClick(pkg, tag, id);
						} catch (RemoteException ex) {
							// system process is dead if we're here.
						}

						// close the shade if it was open
						/*
						 * Original codes
						 * animateCollapsePanels(CommandQueue.FLAG_EXCLUDE_NONE);
						 * visibilityChanged(false);
						 */
						final String funcName = Build.VERSION.SDK_INT == Build.VERSION_CODES.JELLY_BEAN ? "animateCollapse"
								: "animateCollapsePanels";
						XposedHelpers.callMethod(mPhoneStatusBar, funcName, FLAG_EXCLUDE_NONE);
						try {
							if (mVisibilityChanged != null) {
								mVisibilityChanged.invoke(mPhoneStatusBar, false);
								Log.d(TAG, "Invoked visibilityChanged");
							}
						} catch (Exception e) {
							XposedBridge.log(e.getMessage());
						}

						return null;
					}
				});
	}
}
