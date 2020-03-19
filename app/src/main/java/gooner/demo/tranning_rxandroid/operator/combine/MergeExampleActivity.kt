package gooner.demo.tranning_rxandroid.operator.combine

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import gooner.demo.tranning_rxandroid.R
import gooner.demo.tranning_rxandroid.util.AppConstant
import io.reactivex.Observable
import io.reactivex.Observer
import io.reactivex.disposables.Disposable

class MergeExampleActivity : AppCompatActivity() {

    private val TAG: String? =
        MergeExampleActivity::class.java.getSimpleName()
    internal var btn: Button? = null
    internal var textView: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_merge_example)
        btn = findViewById<Button?>(R.id.btn)
        textView = findViewById<TextView?>(R.id.textView)
        btn!!.setOnClickListener { doSomeWork() }
    }

    /*
     * Using merge operator to combine Observable : merge does not maintain
     * the order of Observable.
     * It will emit all the 7 values may not be in order
     * Ex - "A1", "B1", "A2", "A3", "A4", "B2", "B3" - may be anything
     */
    private fun doSomeWork() {
        val aStrings = arrayOf("A1", "A2", "A3", "A4")
        val bStrings = arrayOf("B1", "B2", "B3")
        val aObservable: Observable<String> =
            Observable.fromArray(*aStrings)
        val bObservable: Observable<String> =
            Observable.fromArray(*bStrings)
        Observable.merge(aObservable, bObservable)
            .subscribe(getObserver())
    }


    private fun getObserver(): Observer<String> {
        return object : Observer<String> {
            override fun onSubscribe(d: Disposable) {
                Log.d(TAG, " onSubscribe : " + d.isDisposed)
            }

            override fun onNext(value: String) {
                textView!!.append(" onNext : value : $value")
                textView!!.append(AppConstant.LINE_SEPARATOR)
                Log.d(TAG, " onNext : value : $value")
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
