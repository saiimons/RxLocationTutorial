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
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.jakewharton.rxbinding.view.RxView;
import com.jakewharton.rxbinding.view.ViewClickEvent;
import com.trello.rxlifecycle.ActivityEvent;
import com.trello.rxlifecycle.components.support.RxAppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.Date;

import butterknife.Bind;
import butterknife.ButterKnife;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.functions.Func2;
import rx.schedulers.Schedulers;

public class MainActivity extends RxAppCompatActivity implements Action1<Location> {
    @Bind(R.id.location)
    TextView location;
    @Bind(R.id.start_stop)
    Button startStop;
    @Bind(R.id.run)
    Button run;
    @Bind(R.id.bike)
    Button bike;

    private Subscription subscription;

    private Observable<Location> locationObservable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        /*
         Let's create LocationRequests out of clicks !
          */
        final Observable<LocationRequest> locationRequests = Observable
                // Merge all sources of clicks
                .merge(
                        RxView.clickEvents(run),
                        RxView.clickEvents(bike)
                )
                .subscribeOn(AndroidSchedulers.mainThread())
                        // Then map all clicks to Location requests
                .map(new ClicksToLocationRequests());

        /*
         Let's get access to the Google Api Client
          */
        Observable<GoogleApiClient> apiClients = Observable.create(new GACObservable(this));

        /*
         Then let's combine all that into an Observable<Location>
          */
        Observable<Observable<Location>> trackers = Observable
                // First, let's combine the stuff, we need the latest Api Client and the latest request
                // This will get us the implementation needed to create the Location Observable
                .combineLatest(
                        apiClients,
                        locationRequests,
                        new Func2<GoogleApiClient, LocationRequest, LocationObservable>() {
                            @Override
                            public LocationObservable call(GoogleApiClient googleApiClient, LocationRequest request) {
                                return new LocationObservable(googleApiClient, request);
                            }
                        })
                        // Then we need to "FlatMap" it to a real Observable
                .map(new Func1<LocationObservable, Observable<Location>>() {
                    @Override
                    public Observable<Location> call(LocationObservable locationObservable) {
                        return Observable.create(locationObservable);
                    }
                });





        /*
         We will use the next location observable each time a new one is available
         */
        locationObservable = Observable
                .switchOnNext(trackers)
                        // Many guys will listen to it, we shall share it.
                .share();


        /*
         Let's listen to start stop of the thing
          */
        RxView
                .clickEvents(startStop)
                .subscribe(
                        new Action1<ViewClickEvent>() {
                            @Override
                            public void call(ViewClickEvent viewClickEvent) {
                                // Are we already listening ?
                                if (subscription == null || subscription.isUnsubscribed()) {
                                    subscription = locationObservable
                                            // Let's unsubscribe when activity is paused
                                            .compose(MainActivity.this.<Location>bindUntilEvent(ActivityEvent.PAUSE))
                                                    // Let the activity handle what to do with the location
                                            .subscribeOn(Schedulers.io())
                                            .observeOn(AndroidSchedulers.mainThread())
                                            .subscribe(
                                                    MainActivity.this,
                                                    new Action1<Throwable>() {
                                                        @Override
                                                        public void call(Throwable throwable) {
                                                            Log.e("ERRRR", "failed to acquire location", throwable);
                                                        }
                                                    }
                                            );
                                }
                                // Stop listening to all these locations !
                                else {
                                    subscription.unsubscribe();
                                    location.setText("");
                                }
                            }
                        }
                );
    }

    @Override
    public void call(Location location) {
        this.location.setText(location == null ? "" : SimpleDateFormat.getTimeInstance().format(new Date(location.getTime())) + "\n" + location.getLatitude() + " / " + location.getLongitude());
    }

    private static class ClicksToLocationRequests implements Func1<ViewClickEvent, LocationRequest> {
        @Override
        public LocationRequest call(ViewClickEvent viewClickEvent) {
            // Create a new LocationRequest
            LocationRequest locationRequest = new LocationRequest();
            locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
            // Set it up depending on the view ID
            switch (viewClickEvent.view().getId()) {
                case R.id.run:
                    locationRequest.setInterval(5000);
                    locationRequest.setFastestInterval(3000);
                    break;
                case R.id.bike:
                    locationRequest.setInterval(2500);
                    locationRequest.setFastestInterval(1500);
                    break;
            }
            // Send it up the chain
            return locationRequest;
        }
    }
}
