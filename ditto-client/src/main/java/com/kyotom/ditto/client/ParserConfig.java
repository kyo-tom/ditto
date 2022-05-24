package com.kyotom.ditto.client;

import com.google.common.base.Splitter;
import io.airlift.configuration.Config;
import io.airlift.configuration.ConfigDescription;

import javax.validation.constraints.NotNull;
import java.net.URI;
import java.util.List;
import java.util.stream.StreamSupport;

import static com.google.common.collect.ImmutableList.toImmutableList;

public class ParserConfig {

    private String kuduDefaultCatalog = "kudu";
    private String hiveDefaultCatalog = "hive";
    private static final Splitter SPLITTER = Splitter.on(',').trimResults().omitEmptyStrings();
    private List<URI> metastoreUris;

    @NotNull
    public List<URI> getMetastoreUris()
    {
        return metastoreUris;
    }

    @Config("hive.metastore.uri")
    @ConfigDescription("Hive metastore URIs (comma separated)")
    public ParserConfig setMetastoreUris(String uris)
    {
        if (uris == null) {
            this.metastoreUris = null;
            return this;
        }

        this.metastoreUris = StreamSupport.stream(SPLITTER.split(uris).spliterator(), false)
                .map(URI::create)
                .collect(toImmutableList());

        return this;
    }

    @NotNull
    public String getKuduDefaultCatalog()
    {
        return kuduDefaultCatalog;
    }

    @Config("kudu.default.catalog")
    @ConfigDescription("Kudu default catalog")
    public ParserConfig setKuduDefaultCatalog(String kuduDefaultCatalog)
    {
        this.kuduDefaultCatalog = kuduDefaultCatalog;

        return this;
    }

    @NotNull
    public String getHiveDefaultCatalog()
    {
        return hiveDefaultCatalog;
    }

    @Config("hive.default.catalog")
    @ConfigDescription("Hive default catalog")
    public ParserConfig setHiveDefaultCatalog(String hiveDefaultCatalog)
    {
        this.hiveDefaultCatalog = hiveDefaultCatalog;

        return this;
    }
}
