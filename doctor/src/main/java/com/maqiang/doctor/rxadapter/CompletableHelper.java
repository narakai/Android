package com.maqiang.doctor.rxadapter;


import com.jakewharton.retrofit2.adapter.rxjava2.HttpException;

import java.lang.reflect.Type;

import retrofit2.Call;
import retrofit2.CallAdapter;
import retrofit2.Response;
import rx.Completable;
import rx.Completable.CompletableOnSubscribe;
import rx.Subscription;
import rx.exceptions.Exceptions;
import rx.functions.Action0;
import rx.subscriptions.Subscriptions;

final class CompletableHelper {
  static CallAdapter<Completable> createCallAdapter() {
    return new CompletableCallAdapter();
  }

  private static final class CompletableCallOnSubscribe implements  CompletableOnSubscribe {
    private final Call originalCall;

    CompletableCallOnSubscribe(Call originalCall) {
      this.originalCall = originalCall;
    }

    @Override public void call(Completable.CompletableSubscriber subscriber) {
      // Since Call is a one-shot type, clone it for each new subscriber.
      final Call call = originalCall.clone();

      // Attempt to cancel the call if it is still in-flight on unsubscription.
      Subscription subscription = Subscriptions.create(new Action0() {
        @Override public void call() {
          call.cancel();
        }
      });
      subscriber.onSubscribe(subscription);

      try {
        Response response = call.execute();
        if (!subscription.isUnsubscribed()) {
          if (response.isSuccessful()) {
            subscriber.onCompleted();
          } else {
            subscriber.onError(new HttpException(response));
          }
        }
      } catch (Throwable t) {
        Exceptions.throwIfFatal(t);
        if (!subscription.isUnsubscribed()) {
          subscriber.onError(t);
        }
      }
    }
  }

  private static class CompletableCallAdapter implements CallAdapter<Completable> {

    @Override public Type responseType() {
      return Void.class;
    }

    @Override public Completable adapt(Call call) {
      return Completable.create(new CompletableCallOnSubscribe(call));
    }
  }
}
