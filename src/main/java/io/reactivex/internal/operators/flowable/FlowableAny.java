/**
 * Copyright 2016 Netflix, Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See
 * the License for the specific language governing permissions and limitations under the License.
 */
package io.reactivex.internal.operators.flowable;

import org.reactivestreams.*;

import io.reactivex.Flowable;
import io.reactivex.functions.Predicate;
import io.reactivex.internal.subscriptions.*;

public final class FlowableAny<T> extends Flowable<Boolean> {
    final Publisher<T> source;
    final Predicate<? super T> predicate;
    public FlowableAny(Publisher<T> source, Predicate<? super T> predicate) {
        this.source = source;
        this.predicate = predicate;
    }
    
    @Override
    protected void subscribeActual(Subscriber<? super Boolean> s) {
        source.subscribe(new AnySubscriber<T>(s, predicate));
    }
    
    static final class AnySubscriber<T> extends DeferredScalarSubscription<Boolean> implements Subscriber<T> {
        /** */
        private static final long serialVersionUID = -2311252482644620661L;
        
        final Predicate<? super T> predicate;
        
        Subscription s;
        
        boolean done;

        public AnySubscriber(Subscriber<? super Boolean> actual, Predicate<? super T> predicate) {
            super(actual);
            this.predicate = predicate;
        }
        @Override
        public void onSubscribe(Subscription s) {
            if (SubscriptionHelper.validateSubscription(this.s, s)) {
                this.s = s;
                actual.onSubscribe(this);
                s.request(Long.MAX_VALUE);
            }
        }
        
        @Override
        public void onNext(T t) {
            if (done) {
                return;
            }
            boolean b;
            try {
                b = predicate.test(t);
            } catch (Throwable e) {
                done = true;
                s.cancel();
                actual.onError(e);
                return;
            }
            if (b) {
                done = true;
                s.cancel();
                complete(true);
            }
        }
        
        @Override
        public void onError(Throwable t) {
            if (!done) {
                done = true;
                actual.onError(t);
            }
        }
        
        @Override
        public void onComplete() {
            if (!done) {
                done = true;
                complete(false);
            }
        }
        
        @Override
        public void cancel() {
            super.cancel();
            s.cancel();
        }
    }
}
