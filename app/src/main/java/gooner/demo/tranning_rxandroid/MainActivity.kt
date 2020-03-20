package gooner.demo.tranning_rxandroid

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import gooner.demo.tranning_rxandroid.real.pagination.PaginationActivity
import gooner.demo.tranning_rxandroid.real.search.SearchActivity
import io.reactivex.*
import io.reactivex.Single.create
import io.reactivex.disposables.Disposable
import io.reactivex.functions.BiFunction
import org.reactivestreams.Subscriber
import org.reactivestreams.Subscription
import java.lang.Long
import java.util.concurrent.TimeUnit


class MainActivity : AppCompatActivity() {

    companion object {
        const val TAG: String = "MainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
//        onTranningRxJava()
    }

    fun startOperatorsActivity(view: View?) {
        startActivity(Intent(this@MainActivity, OperatorsActivity::class.java))
    }

    /* fun startNetworkingActivity(view: View?) {
         startActivity(
             Intent(
                 this@MainActivity,
                 NetworkingActivity::class.java
             )
         )
     }

     fun startCacheActivity(view: View?) {
         startActivity(
             Intent(
                 this@MainActivity,
                 CacheExampleActivity::class.java
             )
         )
     }

     fun startRxBusActivity(view: View?) {
         (application as MyApplication).sendAutoEvent()
         startActivity(Intent(this@MainActivity, RxBusActivity::class.java))
     }



     fun startComposeOperator(view: View?) {
         startActivity(
             Intent(
                 this@MainActivity,
                 ComposeOperatorExampleActivity::class.java
             )
         )
     }

*/

    fun startPaginationActivity(view: View?) {
        startActivity(
            Intent(
                this@MainActivity,
                PaginationActivity::class.java
            )
        )
    }

    fun startSearchActivity(view: View?) {
        startActivity(
            Intent(
                this@MainActivity,
                SearchActivity::class.java
            )
        )
    }

    private fun onTranningRxJava() {
        var alphabet = mutableListOf<String>("A", "B", "C", "D", "E", "F")
        var alphabet1 = mutableListOf<String>("A", "B", "C", "D", "E", "F", "4324234")

        var observable =
            Observable.just(alphabet, alphabet1)
                .subscribe(object : Observer<MutableList<String>> {
                    override fun onComplete() {
                    }

                    override fun onSubscribe(d: Disposable) {
                    }

                    override fun onNext(t: MutableList<String>) {
                        Log.d("Tag", t.toString())
                    }

                    override fun onError(e: Throwable) {
                    }
                })

//
//        var observer = object : Observer<ObservableOnSubscribe<String>> {
//
//            override fun onComplete() {
//                Log.d(TAG, "onComplete is called")
//            }
//
//            override fun onSubscribe(d: Disposable) {
//                Log.d(TAG, "onSubscribe is called")
//            }
//
//            override fun onNext(t: ObservableOnSubscribe<String>) {
//                Log.d(TAG, t)
//            }
//
//            override fun onError(e: Throwable) {
//                Log.d(TAG, "onError is called")
//
//            }
//        }
//
//        observable.subscribe(observer)


        var singleObservable = create(object : SingleOnSubscribe<String> {
            override fun subscribe(emitter: SingleEmitter<String>) {
                try {
                    emitter.onSuccess(alphabet[0])
                } catch (e: Exception) {
                    emitter.onError(e)
                }
            }
        }).subscribe(object : SingleObserver<String> {
            override fun onSuccess(t: String) {
                Log.d(TAG, "onSuccess is called with string is ${t}")
            }

            override fun onSubscribe(d: Disposable) {
                Log.d(TAG, "onSubscribe is called")
            }

            override fun onError(e: Throwable) {
                Log.d(TAG, "onError is called with exception is ${e.message}")
            }
        })


//        var instance = DeferClass()
//        var observable1 = instance.valueObservable()
//
//
//        instance.setValue(" Test 1")
//
//
//
//        observable1.subscribe { Log.d(TAG, "Defer" + it) }

//        Observable.interval(1, TimeUnit.SECONDS)
//            .subscribe(object : Observer<Long> {
//                override fun onSubscribe(d: Disposable) {
//                }
//
//                override fun onNext(value: Long) {
//                    Log.d(TAG, "onNext" + value)
//
//                }
//
//                override fun onError(e: Throwable) {
//                    Log.d(TAG, "onError")
//
//                }
//
//                override fun onComplete() {}
//            })

//        Observable.just("A", "B", "C", "D", "E").buffer(2).subscribe {
//            Log.d(TAG, "OnNExt")
//
//            it.forEach {
//                Log.d(TAG, it)
//            }
//        }
//

        /*.subscribe {
            Log.d(TAG, "OnNExt")
            Log.d(TAG, it)
        }*/
//


        Observable.merge(
            Observable.just("AX1", "BY1", "CQ1", "DE1", "EN1")
                .map { it.first().toString() },
            Observable.just("AX2", "AZ2", " BY2 ", " CQ2 ", " DE2 ", " EN2 ")
        )
            .subscribe {
                Log.d(TAG, "OnNExt")
                Log.d(TAG, it)
            }

        /*   Observable.create(object : ObservableOnSubscribe<String> {
               override fun subscribe(emitter: ObservableEmitter<String>) {
                   alphabet.forEach {
                       emitter.onNext(it)
                       Thread.sleep(3000)
                       Log.d(TAG, "3 second is ended")
                   }
               }
           }).debounce(2, TimeUnit.SECONDS)
               .subscribe {
                   Log.d(TAG, "OnNext debounce")
                   Log.d(TAG, it)
               }*/

        var o1 = Observable.interval(1, TimeUnit.SECONDS).map { id -> "A" + id }
        var o2 = Observable.interval(2, TimeUnit.SECONDS).map { id -> "B" + id }

        var zip = Observable.zip(o1, o2, object : BiFunction<String, String, String> {
            override fun apply(t1: String, t2: String): String {
                return t1 + " --------" + t2
            }
        }).subscribe {

            Log.d(TAG, "OnNExt")
            Log.d(TAG, it)

        }


//        var zip1 = Observable.merge(o1, o2 /*object : BiFunction<String, String, String> {
//            override fun apply(t1: String, t2: String): String {
//                return t1 + " --------" + t2
//            }
//        }*/).subscribe {
//            Log.d(TAG, it)
//
//        }

        Flowable.range(1, 10)
            .subscribe(object : Subscriber<Int?> {
                override fun onSubscribe(s: Subscription) {
                    s.request(Long.MAX_VALUE)
                }

                override fun onNext(t: Int?) {
                    println(t)
                }

                override fun onError(t: Throwable) {
                    t.printStackTrace()
                }

                override fun onComplete() {
                    println("Done")
                }
            })
    }


}
