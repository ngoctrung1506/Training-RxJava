package gooner.demo.tranning_rxandroid.operator.transfrom

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import gooner.demo.tranning_rxandroid.R
import gooner.demo.tranning_rxandroid.model.ApiUser
import gooner.demo.tranning_rxandroid.model.User
import gooner.demo.tranning_rxandroid.util.AppConstant
import gooner.demo.tranning_rxandroid.util.Utils
import io.reactivex.Observable
import io.reactivex.ObservableEmitter
import io.reactivex.ObservableOnSubscribe
import io.reactivex.Observer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers

class MapExampleActivity : AppCompatActivity() {

    private val TAG: String? =
        MapExampleActivity::class.java.getSimpleName()
    internal var btn: Button? = null
    internal var textView: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map_example)
        btn = findViewById<Button?>(R.id.btn)
        textView = findViewById<TextView?>(R.id.textView)
        btn!!.setOnClickListener { doSomeWork() }
    }


    private fun doSomeWork() {
        getObservable()
            // Run on a background thread
            .subscribeOn(Schedulers.io())
            // Be notified on the main thread
            .observeOn(AndroidSchedulers.mainThread())
            .map { userApi ->
                Utils.convertApiUserListToUserList(userApi)
            }

            .subscribe(getObserver())
    }

    private fun getObservable(): Observable<List<ApiUser>> {
        return Observable.create(object :
            ObservableOnSubscribe<List<ApiUser>> {
            override fun subscribe(e: ObservableEmitter<List<ApiUser>>) {
                if (!e.isDisposed) {
                    e.onNext(Utils.getApiUserList())
                    e.onComplete()
                }
            }
        })
    }

    private fun getObserver(): Observer<List<User>> {
        return object : Observer<List<User>> {
            override fun onSubscribe(d: Disposable) {
                Log.d(TAG, " onSubscribe : " + d.isDisposed)
            }

            override fun onNext(userList: List<User>) {
                textView?.run {
                    append(" onNext")
                    append(AppConstant.LINE_SEPARATOR)
                    for (user in userList) {
                        append(" firstname : " + user.firstname)
                        append(AppConstant.LINE_SEPARATOR)
                    }
                    Log.d(TAG, " onNext : " + userList.size)
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
                    append(AppConstant.DOUBLE_LINE_SEPARATOR)
                    Log.d(TAG, " onComplete")
                }
            }
        }
    }
}
