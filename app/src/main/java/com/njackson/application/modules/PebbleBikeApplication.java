package com.njackson.application.modules;

import android.app.Application;

import java.util.Arrays;
import java.util.List;

import dagger.ObjectGraph;

/**
 * Created by server on 30/03/2014.
 */
public class PebbleBikeApplication extends Application {

    protected ObjectGraph graph;

    @Override public void onCreate() {
        super.onCreate();

        graph = ObjectGraph.create(getModules().toArray());
    }

    protected List<Object> getModules() {
        return Arrays.asList(
                new AndroidModule(this),
                new PebbleBikeModule(),
                new PebbleServiceModule()
        );
    }

    public void inject(Object object) {
        graph.inject(object);
    }

}