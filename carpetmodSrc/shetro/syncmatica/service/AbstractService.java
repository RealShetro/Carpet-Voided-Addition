package shetro.syncmatica.service;

import shetro.syncmatica.Context;

abstract class AbstractService implements IService {
    Context context;

    @Override
    public void setContext(final Context context) {
        this.context = context;
    }

    @Override
    public Context getContext() {
        return context;
    }
}