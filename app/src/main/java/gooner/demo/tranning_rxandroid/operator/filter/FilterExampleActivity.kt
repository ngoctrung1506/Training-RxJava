package gooner.demo.tranning_rxandroid.operator.filter

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

class FilterExampleActivity : AppCompatActivity() {

    private val TAG: String? =
        FilterExampleActivity::class.java.getSimpleName()
    internal var btn: Button? = null
    internal var textView: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_filter_example)
        btn = findViewById<Button?>(R.id.btn)
        textView = findViewById<TextView?>(R.id.textView)
        btn!!.setOnClickListener { doSomeWork() }
    }

    /*
     * simple example by using filter operator to emit only even value
     *
     */
    private fun doSomeWork() {
        Observable.just(1, 2, 3, 4, 5, 6)
            .filter { it % 2 == 0 }
            .subscribe(getObserver())
    }


    private fun getObserver(): Observer<Int> {
        return object : Observer<Int> {
            override fun onSubscribe(d: Disposable) {
                Log.d(TAG, " onSubscribe : " + d.isDisposed)
            }

            override fun onNext(value: Int) {
                textView?.run {
                    append(" onNext : ")
                    append(AppConstant.LINE_SEPARATOR)
                    append(" value : $value")
                    append(AppConstant.LINE_SEPARATOR)
                    Log.d(TAG, " onNext ")
                    Log.d(TAG, " value : $value")
                }
            }

            override fun onError(e: Throwable) {
                textView?.run {
                    append(" onError : " + e.message)
                    append(AppConstant.LINE_SEPARATOR)
                    Log.d(TAG, " onError : " + e.message)
                }
            }

            override fun onComplete() {
                textView?.run {
                    append(" onComplete")
                    append(AppConstant.LINE_SEPARATOR)
                    Log.d(TAG, " onComplete")
                }
            }
        }
    }
}
