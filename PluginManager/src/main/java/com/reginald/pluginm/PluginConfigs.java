package com.reginald.pluginm;

/**
 * Created by lxy on 17-9-21.
 */

public class PluginConfigs {
    private static final String TAG = "PluginConfigs";

    public static final int PROCESS_TYPE_STANDALONE = 1;
    public static final int PROCESS_TYPE_DUAL = 2;

    private int mProcessType = PROCESS_TYPE_STANDALONE;

    public int getProcessType() {
        return mProcessType;
    }

    public PluginConfigs setProcessType(int processType) {
        mProcessType = processType;
        return this;
    }

}