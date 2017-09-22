package com.reginald.pluginm.comm.invoker;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.text.TextUtils;

import com.reginald.pluginm.pluginapi.IInvokeCallback;
import com.reginald.pluginm.pluginapi.IInvokeResult;
import com.reginald.pluginm.utils.ConfigUtils;
import com.reginald.pluginm.utils.Logger;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by lxy on 17-9-21.
 */

public class HostInvokerManager {
    private static final String TAG = "HostInvokerManager";
    private static volatile HostInvokerManager sInstance;

    private Context mContext;
    private final Map<String, Map<String, String>> mHostInvokerConfigMap = new HashMap<>();
    private final Map<String, IHostInvoker> mHostInvokerMap = new HashMap<>();

    public static synchronized HostInvokerManager getInstance(Context hostContext) {
        if (sInstance == null) {
            sInstance = new HostInvokerManager(hostContext);
        }

        return sInstance;
    }

    public InvokeResult invokeHost(String serviceName, String methodName, String params, InvokeCallback callback) {
        IHostInvoker hostInvoker = fetchHostInvoker(serviceName);
        if (hostInvoker != null) {
            IInvokeCallback iInvokeCallback = InvokeCallbackWrapper.build(callback);
            IInvokeResult result = hostInvoker.onInvoke(mContext, methodName, params, iInvokeCallback);
            return InvokeResult.build(result);
        }

        Logger.d(TAG, String.format("invokeHost() service = %s, method = %s, params = %s, callback = %s, NOT found!",
                serviceName, methodName, params, callback));

        return new InvokeResult(IInvokeResult.RESULT_NOT_FOUND, null);

    }


    private HostInvokerManager(Context hostContext) {
        Context appContext = hostContext.getApplicationContext();
        mContext = appContext != null ? appContext : hostContext;
        init();
    }

    private void init() {
        PackageManager pm = mContext.getPackageManager();
        try {
            PackageInfo packageInfo = pm.getPackageInfo(mContext.getPackageName(), PackageManager.GET_META_DATA);
            if (packageInfo != null && packageInfo.applicationInfo.metaData != null) {
                Map<String, Map<String, String>> map = ConfigUtils.parseInvokerConfig(packageInfo.applicationInfo.metaData);
                Logger.d(TAG, String.format("init() metaData = %s, invokerConfigMap = %s", packageInfo.applicationInfo.metaData, map));
                mHostInvokerConfigMap.putAll(map);
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
    }

    private IHostInvoker fetchHostInvoker(String serviceName) {
        String key = keyForHostInvokerMap(serviceName);
        if (key == null) {
            Logger.w(TAG, String.format("fetchHostInvoker() key is Null! for %s @ %s", serviceName, mContext.getPackageName()));
            return null;
        }

        synchronized (mHostInvokerMap) {
            IHostInvoker hostInvoker = mHostInvokerMap.get(key);
            Logger.w(TAG, String.format("fetchHostInvoker() cached hostInvoker = %s", hostInvoker));

            if (hostInvoker != null) {
                return hostInvoker;
            }

            Map<String, String> serviceConfig = mHostInvokerConfigMap.get(serviceName);

            if (serviceConfig == null) {
                Logger.e(TAG, String.format("fetchHostInvoker() service config for %s@%s not found!", serviceName, mContext.getPackageName()));
                return null;
            }

            try {
                String className = serviceConfig.get(ConfigUtils.KEY_INVOKER_CLASS);
                Class<?> invokerClazz = mContext.getClassLoader().loadClass(className);
                hostInvoker = (IHostInvoker) invokerClazz.newInstance();
                mHostInvokerMap.put(key, hostInvoker);
                return hostInvoker;
            } catch (ClassNotFoundException e) {
                Logger.e(TAG, String.format("fetchHostInvoker() pluginInvokerClass NOT found! for %s @ %s",
                        serviceName, mContext.getPackageName()), e);
            } catch (Exception e) {
                Logger.e(TAG, String.format("fetchHostInvoker() pluginInvoker init error! for %s @ %s",
                        serviceName, mContext.getPackageName()), e);
            }
        }

        return null;
    }

    private static String keyForHostInvokerMap(String serviceName) {
        return !TextUtils.isEmpty(serviceName) ? serviceName : null;
    }
}
