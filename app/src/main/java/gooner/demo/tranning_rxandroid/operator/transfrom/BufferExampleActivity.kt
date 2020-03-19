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
import io.reactivex.disposables.Disposable

class BufferExampleActivity : AppCompatActivity() {

    private val TAG: String? =
        BufferExampleActivity::class.java.getSimpleName()
    internal var btn: Button? = null
    internal var textView: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_buffer_example)
        btn = findViewById<Button?>(R.id.btn)
        textView = findViewById<TextView?>(R.id.textView)
        btn!!.setOnClickListener { doSomeWork() }
    }

    /*
     * simple example using buffer operator - bundles all emitted values into a list
     */
    private fun doSomeWork() {
        val buffered: Observable<List<String>> =
            getObservable().buffer(3, 1)

        // 3 means,  it takes max of three from its start index and create list
        // 1 means, it jumps one step every time
        // so the it gives the following list
        // 1 - one, two, three
        // 2 - two, three, four
        // 3 - three, four, five
        // 4 - four, five
        // 5 - five


        buffered.subscribe(getObserver())
    }

    private fun getObservable(): Observable<String> {
        return Observable.just("one", "two", "three", "four", "five")
    }

    private fun getObserver(): Observer<List<String>> {
        return object : Observer<List<String>> {
            override fun onSubscribe(d: Disposable) {
                Log.d(TAG, " onSubscribe : " + d.isDisposed)
            }

            override fun onNext(stringList: List<String>) {
                textView?.run {
                    append(" onNext size : " + stringList.size + AppConstant.LINE_SEPARATOR)
                    Log.d(TAG, " onNext : size :" + stringList.size)
                    for (value in stringList) {
                        append(" value : $value" + AppConstant.LINE_SEPARATOR)
                        Log.d(TAG, " : value :$value")
                    }
                }
            }

            override fun onError(e: Throwable) {
                textView?.run {
                    append(" onError : " + e.message + AppConstant.DOUBLE_LINE_SEPARATOR)
                    Log.d(TAG, " onError : " + e.message)
                }
            }

            override fun onComplete() {
                textView?.run {
                    append(" onComplete" + AppConstant.DOUBLE_LINE_SEPARATOR)
                    Log.d(TAG, " onComplete")
                }
            }
        }
    }
}
