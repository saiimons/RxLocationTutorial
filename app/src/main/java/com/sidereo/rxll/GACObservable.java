/*
        Copyright 2015 Sidereo Solutions Inc.

        Licensed under the Apache License, Version 2.0 (the "License");
        you may not use this file except in compliance with the License.
        You may obtain a copy of the License at

        http://www.apache.org/licenses/LICENSE-2.0

        Unless required by applicable law or agreed to in writing, software
        distributed under the License is distributed on an "AS IS" BASIS,
        WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
        See the License for the specific language governing permissions and
        limitations under the License.
*/

package com.sidereo.rxll;

import android.content.Context;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;

import hugo.weaving.DebugLog;
import rx.Observable;
import rx.Subscriber;

public class GACObservable implements GoogleApiClient.OnConnectionFailedListener, Observable.OnSubscribe<GoogleApiClient> {
    private final GoogleApiClient googleApiClient;
    private Subscriber<? super GoogleApiClient> subscriber;

    public GACObservable(Context context) {
        googleApiClient = new GoogleApiClient.Builder(context)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

    @DebugLog
    @Override
    public void call(Subscriber<? super GoogleApiClient> subscriber) {
        this.subscriber = subscriber;
        if (!googleApiClient.isConnected() || !googleApiClient.isConnecting()) {
            googleApiClient.blockingConnect();
        }
        if (!subscriber.isUnsubscribed()) {
            subscriber.onNext(googleApiClient);
            subscriber.onCompleted();
        }
    }

    @DebugLog
    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        if (!subscriber.isUnsubscribed()) {
            subscriber.onError(new RuntimeException(connectionResult.toString()));
        }
    }
}