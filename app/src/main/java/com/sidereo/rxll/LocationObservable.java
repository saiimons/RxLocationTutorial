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

import android.location.Location;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import hugo.weaving.DebugLog;
import rx.Observable;
import rx.Subscriber;
import rx.functions.Action0;
import rx.subscriptions.Subscriptions;

public class LocationObservable implements LocationListener, Action0, Observable.OnSubscribe<Location> {
    private final LocationRequest locationRequest;
    private final GoogleApiClient googleApiClient;
    private Subscriber<? super Location> subscriber;

    @DebugLog
    public LocationObservable(GoogleApiClient googleApiClient, LocationRequest locationRequest) {
        this.googleApiClient = googleApiClient;
        this.locationRequest = locationRequest;
    }

    @DebugLog
    @Override
    public void call(Subscriber<? super Location> subscriber) {
        this.subscriber = subscriber;
        this.subscriber.add(Subscriptions.create(this));
        this.subscriber.onNext(LocationServices.FusedLocationApi.getLastLocation(googleApiClient));
        LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, this);
    }


    @DebugLog
    @Override
    public void onLocationChanged(android.location.Location location) {
        if (!subscriber.isUnsubscribed()) {
            subscriber.onNext(location);
        }
    }

    @DebugLog
    @Override
    public void call() {
        LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient, this);
    }
}