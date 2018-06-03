package deeplinksample.atlassian.com.dashboard;

import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;

import static junit.framework.Assert.assertEquals;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {
    @Test
    public void addition_isCorrect() throws Exception {
        assertEquals(4, 2 + 2);
    }

    @Test
    public void switchMapTest() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);

        final Observable<Integer> action1 = Observable.fromArray(new Integer[]{1, 2, 3, 4, 5, 6})
                .doOnNext(new Consumer<Integer>() {
                    @Override
                    public void accept(Integer integer) throws Exception {
                        try {
                            Thread.sleep(5000);
                        } catch (Exception e) {

                        }
                    }
                })
                .doOnDispose(new Action() {
                    @Override
                    public void run() throws Exception {
                        System.out.println("Unsubscribing original");
                    }
                })
                .subscribeOn(Schedulers.io());

        final Observable<Integer> action2 = Observable.fromArray(new Integer[]{11, 12, 13, 14, 15, 16})
                .doOnNext(new Consumer<Integer>() {
                    @Override
                    public void accept(Integer integer) throws Exception {
                        try {
                            Thread.sleep(5000);
                        } catch (Exception e) {

                        }
                    }
                })
                .doOnDispose(new Action() {
                    @Override
                    public void run() throws Exception {
                        System.out.println("Unsubscribing switchmap");
                    }
                })
                .subscribeOn(Schedulers.io());

        Observable.fromArray("Action2", "Action3")
                .doOnNext(new Consumer<String>() {
                    @Override
                    public void accept(String integer) throws Exception {
                        try {
                            Thread.sleep(20000);
                        } catch (Exception e) {

                        }
                    }
                })
                .startWith("Action1")
                .switchMap(new Function<String, ObservableSource<Integer>>() {
                    @Override
                    public ObservableSource<Integer> apply(String s) throws Exception {
                        if (s.equals("Action1")) {
                            return action1;
                        } else if (s.equals("Action2")) {
                            return action2;
                        } else {
                            return action1;
                        }
                    }
                })
                .subscribe(new Observer<Integer>() {
                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onNext(Integer integer) {
                        System.out.println("new element: " + integer);
                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onComplete() {
                        latch.countDown();
                    }
                });
        latch.await();

    }

}