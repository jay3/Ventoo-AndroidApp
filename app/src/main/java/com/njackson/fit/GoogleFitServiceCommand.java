package com.njackson.fit;

import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.DetectedActivity;
import com.njackson.application.IInjectionContainer;
import com.njackson.events.ActivityRecognitionCommand.NewActivityEvent;
import com.njackson.events.GoogleFitCommand.GoogleFitChangeState;
import com.njackson.events.GoogleFitCommand.GoogleFitStatus;
import com.njackson.events.base.BaseStatus;
import com.njackson.service.IServiceCommand;
import com.njackson.utils.googleplay.IGoogleFitSessionManager;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import java.util.Date;

import javax.inject.Inject;
import javax.inject.Named;

/**
 * Created by njackson on 05/01/15.
 */
public class GoogleFitServiceCommand implements IServiceCommand, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private static final String TAG = "GoogleFitService";
    @Inject Bus _bus;
    @Inject @Named("GoogleFit") GoogleApiClient _googleAPIClient;
    @Inject IGoogleFitSessionManager _sessionManager;

    private GoogleFitStatus.State _currentStatus;

    @Subscribe
    public void onNewActivityEvent(NewActivityEvent event) {
        if(_currentStatus.compareTo(GoogleFitStatus.State.SERVICE_STARTED) == 0) {
            switch (event.getActivityType()) {
                case DetectedActivity.ON_BICYCLE:
                case DetectedActivity.RUNNING:
                case DetectedActivity.WALKING:
                case DetectedActivity.ON_FOOT:
                    _sessionManager.addDataPoint(new Date().getTime(), event.getActivityType());
                    break;
            }
        }
    }

    @Subscribe
    public void onChangeState(GoogleFitChangeState event) {
        switch(event.getState()) {
            case START:
                start();
                break;
            case STOP:
                stop();
        }
    }

    @Override
    public void execute(IInjectionContainer container) {
        container.inject(this);
        _bus.register(this);
    }

    @Override
    public BaseStatus.Status getStatus() {
        return null;
    }

    private void start() {
        _googleAPIClient.registerConnectionCallbacks(this);
        _googleAPIClient.registerConnectionFailedListener(this);
        _googleAPIClient.connect();

        _currentStatus = GoogleFitStatus.State.SERVICE_STARTED;
        _bus.post(new GoogleFitStatus(_currentStatus));
    }

    public void stop (){
        _bus.unregister(this);

        _googleAPIClient.unregisterConnectionFailedListener(this);
        _googleAPIClient.unregisterConnectionCallbacks(this);

        stopRecordingSession();

        _currentStatus = GoogleFitStatus.State.SERVICE_STOPPED;
        _bus.post(new GoogleFitStatus(_currentStatus));
        Log.d(TAG,"Destroy GoogleFit Service");
    }

    /* GOOOGLE CLIENT DELEGATE METHODS */
    @Override
    public void onConnected(Bundle bundle) {
        Log.d(TAG,"Connected");
        startRecordingSession();
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d(TAG,"Connection Suspended");
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.d(TAG,"Connection Failed");
        _bus.post(new GoogleFitStatus(GoogleFitStatus.State.GOOGLEFIT_CONNECTION_FAILED, connectionResult));
    }
    /* END GOOGLE CLIENT DELEGATE METHODS */

    private void startRecordingSession() {
        long startTime = new Date().getTime();
        Log.d(TAG,"Start Recording Sessions");
        _sessionManager.startSession(startTime,_googleAPIClient);
    }

    private void stopRecordingSession() {
        if(_googleAPIClient.isConnected()) {
            Log.d(TAG,"Stopped Recording Sessions");
            _sessionManager.saveActiveSession(new Date().getTime());
        }
    }
}
