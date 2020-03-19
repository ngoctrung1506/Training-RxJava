package gooner.demo.tranning_rxandroid.operator.transfrom

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import gooner.demo.tranning_rxandroid.R
import gooner.demo.tranning_rxandroid.util.AppConstant
import io.reactivex.Observable
import io.reactivex.Observer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import java.util.*
import java.util.concurrent.TimeUnit

class SwitchMapExampleActivity : AppCompatActivity() {

    private val TAG: String? =
        SwitchMapExampleActivity::class.java.getSimpleName()
    internal var btn: Button? = null
    internal var textView: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_switch_map_example)
        btn = findViewById<Button?>(R.id.btn)
        textView = findViewById<TextView?>(R.id.textView)
        btn!!.setOnClickListener { doSomeWork() }
    }

    /* whenever a new item is emitted by the source Observable, it will unsubscribe to and stop
     * mirroring the Observable that was generated from the previously-emitted item,
     * and begin only mirroring the current one.
     *
     * Result: 5x
     */
    private fun doSomeWork() {
        getObservable()
            .switchMap { integer ->
                val delay = Random().nextInt(2)
                Observable.just(integer.toString() + "x")
                    .delay(
                        delay.toLong(),
                        TimeUnit.SECONDS,
                        Schedulers.io()
                    )
            }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(getObserver())
    }

    private fun getObservable(): Observable<Int> {
        return Observable.just(1, 2, 3, 4, 5)
    }

    private fun getObserver(): Observer<String> {
        return object : Observer<String> {
            override fun onSubscribe(d: Disposable) {
                Log.d(TAG, " onSubscribe : " + d.isDisposed)
            }

            override fun onNext(value: String) {
                textView!!.append(" onNext : value : $value")
                textView!!.append(AppConstant.LINE_SEPARATOR)
                Log.d(TAG, " onNext value : $value")
            }

            override fun onError(e: Throwable) {
                textView!!.append(" onError : " + e.message)
                textView!!.append(AppConstant.LINE_SEPARATOR)
                Log.d(TAG, " onError : " + e.message)
            }

            override fun onComplete() {
                textView!!.append(" onComplete")
                textView!!.append(AppConstant.LINE_SEPARATOR)
                Log.d(TAG, " onComplete")
            }
        }
    }
}
