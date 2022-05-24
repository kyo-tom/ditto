package com.kyotom.ditto.parser;

import com.google.common.collect.ImmutableList;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.kyotom.ditto.client.ClientModule;
import com.kyotom.ditto.parser.bridging.Bridging;
import com.kyotom.ditto.parser.tree.Statement;
import io.airlift.bootstrap.Bootstrap;
import io.trino.sql.SqlFormatter;

public class Main {

    private static boolean inited;
    private static SqlParser sqlParser;
    private static Bridging bridging;

    public static void main(String[] args) {
        init();
        System.out.println(transformSingle("create table b.a(b bigint primary key,k varchar,name varchar(16),money decimal(4,8)) stored as orc location 'hdfs://abcsa' TBLPROPERTIES ('a'='1','b'='2')"));
    }

    public static String transformSingle(String sql){
        SqlParser sqlParser = getSqlParser();
        Statement statement = sqlParser.createStatement(sql);
        io.trino.sql.tree.Node node = bridging.process(statement);
        String formatSql = SqlFormatter.formatSql(node);
        return formatSql;
    }

    public static void init(){
        ImmutableList.Builder<Module> modules = ImmutableList.builder();
        modules.add(
                new ClientModule(),
                new ParserModule());

        Bootstrap app = new Bootstrap(modules.build());
        Injector injector = app.strictConfig().doNotInitializeLogging().initialize();
        sqlParser = injector.getInstance(SqlParser.class);
        bridging = injector.getInstance(Bridging.class);
        inited = true;
    }

    public static SqlParser getSqlParser(){
        if (inited) {
            return sqlParser;
        }
        else {
            throw new RuntimeException("You need to init before get sql parser");
        }
    }
}
