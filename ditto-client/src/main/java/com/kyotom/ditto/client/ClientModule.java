package com.kyotom.ditto.client;

import com.google.inject.Binder;
import com.google.inject.Module;

import static io.airlift.configuration.ConfigBinder.configBinder;

public class ClientModule implements Module
{
    @Override
    public void configure(Binder binder)
    {
        configBinder(binder).bindConfig(ParserConfig.class);   // NEW LINE
    }
}
