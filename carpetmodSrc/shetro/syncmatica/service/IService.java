package shetro.syncmatica.service;

import shetro.syncmatica.Context;

public interface IService {
    void setContext(Context context);

    Context getContext();

    void getDefaultConfiguration(IServiceConfiguration configuration);

    String getConfigKey();

    void configure(IServiceConfiguration configuration);

    void startup();

    void shutdown();
}