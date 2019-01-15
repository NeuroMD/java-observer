/*
 * Copyright 2016 - 2017 Neurotech MRC. http://neuromd.com/
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.neuromd.javaobserver;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Create instance of this class to use notification mechanism for your subscribers
 * Usage:
 * public SubscribersNotifier myEvent = new SubscribersNotifier();
 * ...
 * myEvent.sendNotification(param);
 * ...
 * <p>
 * To subscribe do the following:
 * private INotificationCallback _myScanCallback = new INotificationCallback()
 * {
 *
 * public void onNotify(myParamType nParam)
 * {
 * //Handle notification
 * }
 * };
 * ...
 * _myObjectWithNotifications.myEvent.subscribe(_myScanCallback);
 * ...
 */
public class SubscribersNotifier<T> {
    /**
     * List of subscribers of your class
     * Enumerate it and call notification methods
     */
    private final List<INotificationCallback<T>> mSubscribers = new ArrayList<>();
    private final ReentrantLock mLock = new ReentrantLock();

    /**
     * Appends notifications subscriber
     *
     * @param callback subscriber callback class
     */
    public void subscribe(INotificationCallback<T> callback) {
        if (mLock.isHeldByCurrentThread()){
            throw new NotificationDeadlockException("Attempt to modify notification list from notification thread");
        }
        mLock.lock();
        try {
            if (!mSubscribers.contains(callback)) {
                mSubscribers.add(callback);
            }
        } finally {
            mLock.unlock();
        }
    }

    /**
     * Removes subscriber from notification list
     *
     * @param callback callbact object for receiving notifications
     */
    public void unsubscribe(INotificationCallback<T> callback) {
        if (mLock.isHeldByCurrentThread()){
            throw new NotificationDeadlockException("Attempt to modify notification list from notification thread");
        }
        mLock.lock();
        try {
            mSubscribers.remove(callback);
        } finally {
            mLock.unlock();
        }
    }

    public void unsubscribe() {
        if (mLock.isHeldByCurrentThread()){
            throw new NotificationDeadlockException("Attempt to modify notification list from notification thread");
        }
        mLock.lock();
        try {
            mSubscribers.clear();
        } finally {
            mLock.unlock();
        }
    }

    /**
     * Sends notification to all subscribers
     *
     * @param sender object wich sent notification
     * @param param notification data
     */
    public void sendNotification(Object sender, T param) {
        mLock.lock();
        try {
            for (INotificationCallback<T> subscriber : mSubscribers) {
                if (subscriber == null) continue;
                subscriber.onNotify(sender, param);
            }
        } finally {
            mLock.unlock();
        }
    }

    /**
     * Sends notification to all subscribers in new thread
     *
     * @param sender object wich sent notification
     * @param param notification data
     */
    public void sendNotificationAsync(final Object sender, final T param) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    sendNotification(sender, param);
                } catch (Throwable thr) {
                    //do nothing
                }
            }
        }).start();
    }
}
