package gooner.demo.tranning_rxandroid.operator.create

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

class SimpleExampleActivity : AppCompatActivity() {

    private val TAG: String? =
        SimpleExampleActivity::class.java.getSimpleName()
    internal var btn: Button? = null
    internal var textView: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_simple_example)

        btn = findViewById(R.id.btn)
        textView = findViewById(R.id.textView)

        btn?.setOnClickListener { doSomeWork() }
    }

    /*
     * simple example to emit two value one by one
     */
    private fun doSomeWork() {
        getObservable()
            // Run on a background thread
            .subscribeOn(Schedulers.io())
            // Be notified on the main thread
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(getObserver())
    }

    private fun getObservable(): Observable<String> {
        return Observable.just("Cricket", "Football", "Badminton", "Chess")
    }

    private fun getObserver(): Observer<String> {
        return object : Observer<String> {
            override fun onSubscribe(d: Disposable) {
                Log.d(
                    TAG,
                    " onSubscribe : " + d.isDisposed
                )
            }

            override fun onNext(value: String) {
                textView?.run {
                    append(" onNext : value : $value")
                    append(AppConstant.LINE_SEPARATOR)
                    Log.d(
                        TAG,
                        " onNext : value : $value"
                    )
                }
            }

            override fun onError(e: Throwable) {
                textView?.run {
                    append(" onError : " + e.message)
                    append(AppConstant.LINE_SEPARATOR)
                    Log.d(
                        TAG,
                        " onError : " + e.message
                    )
                }
            }

            override fun onComplete() {
                textView?.run {
                    append(" onComplete")
                    append(AppConstant.DOUBLE_LINE_SEPARATOR)
                    Log.d(
                        TAG,
                        " onComplete"
                    )
                }
            }
        }
    }
}
