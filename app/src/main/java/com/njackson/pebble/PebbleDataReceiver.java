package com.njackson.pebble;

import com.getpebble.android.kit.util.PebbleDictionary;
import com.njackson.Constants;
import com.njackson.R;
import com.njackson.application.PebbleBikeApplication;
import com.njackson.events.GPSServiceCommand.ResetGPSState;
import com.njackson.events.PebbleServiceCommand.NewMessage;
import com.njackson.oruxmaps.IOruxMaps;
import com.njackson.state.IGPSDataStore;
import com.njackson.utils.services.IServiceStarter;
import com.squareup.otto.Bus;

import android.content.Context;
import android.util.Log;

import javax.inject.Inject;

public class PebbleDataReceiver extends com.getpebble.android.kit.PebbleKit.PebbleDataReceiver {

    private static final String TAG = "PB-PebbleDataReceiver";

    @Inject IOruxMaps _oruxMaps;
    @Inject IMessageManager _messageManager;
    @Inject Bus _bus;
    @Inject IServiceStarter _serviceStarter;
    @Inject IGPSDataStore _dataStore;

    public PebbleDataReceiver() {
        super(Constants.WATCH_UUID);
    }

    @Override
    public void receiveData(final Context context, final int transactionId, final PebbleDictionary data) {
        Log.i(TAG, "receiveData"+transactionId);
        ((PebbleBikeApplication)context.getApplicationContext()).inject(this);

        _messageManager.sendAckToPebble(transactionId);

        if (data.contains(Constants.CMD_BUTTON_PRESS)) {
            handleButtonData(context,data);
        }

        if (data.contains(Constants.MSG_VERSION_PEBBLE)) {
            handleVersion(context,data);
        }
        /*if (data.contains(Constants.MSG_LIVE_ASK_NAMES)) {
            live_max_name = data.getInteger(Constants.MSG_LIVE_ASK_NAMES).intValue();
            Log.d(TAG, "Constants.MSG_LIVE_ASK_NAMES, live_max_name: " + live_max_name);
            start = true;
        }*/

    }

    private void handleVersion(Context context, PebbleDictionary data) {
        int version = data.getInteger(Constants.MSG_VERSION_PEBBLE).intValue();
        Log.i(TAG, "handleVersion:" + version);
        if (version < Constants.LAST_VERSION_PEBBLE) {
            String message = context.getString(R.string.message_pebble_new_watchface);
            sendMessageToPebble(message);
        }
        sendSavedData();
    }

    private void handleButtonData(Context context, PebbleDictionary data) {
        int button = data.getUnsignedIntegerAsLong(Constants.CMD_BUTTON_PRESS).intValue();
        Log.i(TAG, "handleButtonData:" + button);

        if (button == Constants.ORUXMAPS_START_RECORD_CONTINUE_PRESS) {
            _oruxMaps.startRecordNewSegment();
        } else if (button == Constants.ORUXMAPS_STOP_RECORD_PRESS) {
            _oruxMaps.stopRecord();
        } else if (button == Constants.ORUXMAPS_NEW_WAYPOINT_PRESS) {
            _oruxMaps.newWaypoint();
        } else if (button == Constants.STOP_PRESS) {
            _serviceStarter.stopLocationServices();
        } else if (button == Constants.PLAY_PRESS) {
            _serviceStarter.startLocationServices();
        } else if (button == Constants.REFRESH_PRESS) {
            resetSavedData();
            _bus.post(new ResetGPSState());
        }
    }

    private void resetSavedData() {
        _dataStore.resetAllValues();
        _dataStore.commit();
    }
    private void sendSavedData() {
        // use _messageManager and not _bus to be able to send data even if GPS is not started
        // TODO(nic): refactor me!
        _messageManager.sendSavedDataToPebble(
                Constants.STATE_STOP, // TODO: send real state
                _dataStore.getMeasurementUnits(),
                _dataStore.getDistance(),
                _dataStore.getElapsedTime(),
                _dataStore.getAscent(),
                _dataStore.getMaxSpeed()
        );
    }
    private void sendMessageToPebble(String message) {
        // use _messageManager and not _bus to be able to send data even if GPS is not started
        // TODO(nic): refactor me!
        _messageManager.sendMessageToPebble(message);
    }
}