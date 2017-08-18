package com.reginald.pluginm.hook;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.reginald.pluginm.PluginManager;
import com.reginald.pluginm.reflect.FieldUtils;
import com.reginald.pluginm.reflect.Utils;
import com.reginald.pluginm.stub.PluginStubMainService;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;

/**
 * Created by lxy on 17-8-16.
 */

public class IActivityManagerServiceHook extends ServiceHook {
    private static final String TAG = "IActivityManagerHook";
    private PluginManager mPluginManager;

    public static boolean init(Context context) {
        IActivityManagerServiceHook hook = new IActivityManagerServiceHook(context);
        return hook.install();
    }

    public IActivityManagerServiceHook(Context context) {
        mPluginManager = PluginManager.getInstance(context);
    }

    @Override
    public boolean install() {
        Log.d(TAG, "install() ");
        // hook
        try {
            Class activityManagerNativeCls = Class.forName("android.app.ActivityManagerNative");
            Class iActivityManagerCls = Class.forName("android.app.IActivityManager");
            Class singletonCls = Class.forName("android.util.Singleton");
            Object targetObj = FieldUtils.readStaticField(activityManagerNativeCls, "gDefault");

            if (targetObj != null) {
                if (iActivityManagerCls.isInstance(targetObj)) {
                    List<Class<?>> interfaces = Utils.getAllInterfaces(targetObj.getClass());
                    Class[] ifs = interfaces != null && interfaces.size() > 0 ? interfaces.toArray(new Class[interfaces.size()]) : new Class[0];
                    Object proxiedActivityManager = Proxy.newProxyInstance(targetObj.getClass().getClassLoader(), ifs, this);
                    FieldUtils.writeStaticField(activityManagerNativeCls, "gDefault", proxiedActivityManager);
                    mBase = targetObj;
                } else if (singletonCls.isInstance(targetObj)) {
                    Object iActivityManagerObj = FieldUtils.readField(targetObj, "mInstance");
                    List<Class<?>> interfaces = Utils.getAllInterfaces(iActivityManagerObj.getClass());
                    Class[] ifs = interfaces != null && interfaces.size() > 0 ? interfaces.toArray(new Class[interfaces.size()]) : new Class[0];
                    Object proxiedActivityManager = Proxy.newProxyInstance(iActivityManagerObj.getClass().getClassLoader(), ifs, this);
                    FieldUtils.writeField(targetObj, "mInstance", proxiedActivityManager);
                    mBase = iActivityManagerObj;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "install() hook error! " + e);
            e.printStackTrace();
            return false;
        }
        Log.d(TAG, "install() hook ok!");

        // register method handlers
        addMethodHandler(new getIntentSender());
        addMethodHandler(new stopServiceToken());

        Log.d(TAG, "install() install ok!");
        return true;
    }

    /**
     * public IIntentSender getIntentSender(int type,String packageName, IBinder token, String resultWho,
     * int requestCode, Intent[] intents, String[] resolvedTypes,int flags, Bundle bOptions, int userId);
     */
    private class getIntentSender extends ServiceHook.MethodHandler {
        /**
         * Type for IActivityManaqer.getIntentSender: this PendingIntent is
         * for a sendBroadcast operation.
         * @hide
         */
        public static final int INTENT_SENDER_BROADCAST = 1;

        /**
         * Type for IActivityManaqer.getIntentSender: this PendingIntent is
         * for a startActivity operation.
         * @hide
         */
        public static final int INTENT_SENDER_ACTIVITY = 2;

        /**
         * Type for IActivityManaqer.getIntentSender: this PendingIntent is
         * for an activity result operation.
         * @hide
         */
        public static final int INTENT_SENDER_ACTIVITY_RESULT = 3;

        /**
         * Type for IActivityManaqer.getIntentSender: this PendingIntent is
         * for a startService operation.
         * @hide
         */
        public static final int INTENT_SENDER_SERVICE = 4;

        public boolean onStartInvoke(Object receiver, Method method, Object[] args) {
            int type = (int) args[0];
            Intent pluginIntent = ((Intent[]) args[5])[0];
            Intent newIntent = null;
            args[1] = mPluginManager.getHostContext().getPackageName();

            Log.d(TAG, "getIntentSender() onStartInvoke : pluginIntent = " + pluginIntent);

            switch (type) {
                case INTENT_SENDER_ACTIVITY:
                    newIntent = mPluginManager.getPluginActivityIntent(pluginIntent);
                    break;
                case INTENT_SENDER_SERVICE:
                    newIntent = mPluginManager.getPluginServiceIntent(pluginIntent);
                    break;
            }

            Log.d(TAG, "getIntentSender() onStartInvoke : newIntent = " + newIntent);

            if (newIntent != null) {
                ((Intent[]) args[5])[0] = newIntent;
            }

            return true;
        }

        public Object onEndInvoke(Object receiver, Method method, Object[] args, Object invokeResult) {
            return invokeResult;
        }
    }

    /**
     * public boolean stopServiceToken(ComponentName className, IBinder token, int startId)
     */
    private class stopServiceToken extends ServiceHook.MethodHandler {

        public boolean onStartInvoke(Object receiver, Method method, Object[] args) {
            ComponentName component = (ComponentName) args[0];
            Log.d(TAG, "stopServiceToken() onStartInvoke : component = " + component);
            Intent pluginIntent = new Intent();
            pluginIntent.setComponent(component);
            Intent newIntent = mPluginManager.getPluginServiceIntent(pluginIntent);
            Log.d(TAG, "stopServiceToken() onStartInvoke : newIntent = " + newIntent);
            if (newIntent != null) {
                newIntent.putExtra(PluginStubMainService.INTENT_EXTRA_START_TYPE_KEY, PluginStubMainService.INTENT_EXTRA_START_TYPE_STOP);
                mPluginManager.getHostContext().startService(newIntent);
                return false;
            } else {
                return true;
            }
        }

        public Object onEndInvoke(Object receiver, Method method, Object[] args, Object invokeResult) {
            return invokeResult == null ? true : invokeResult;
        }
    }

}