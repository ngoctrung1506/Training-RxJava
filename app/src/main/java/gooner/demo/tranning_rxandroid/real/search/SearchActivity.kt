package gooner.demo.tranning_rxandroid.real.search

import android.os.Bundle
import android.widget.SearchView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import gooner.demo.tranning_rxandroid.R
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.TimeUnit


class SearchActivity : AppCompatActivity() {

    private var mDispose: Disposable? = null

    val TAG: String? =
        SearchActivity::class.java.getSimpleName()
    private var searchView: SearchView? = null
    var textViewResult: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)
        searchView = findViewById<SearchView?>(R.id.searchView)
        textViewResult = findViewById<TextView?>(R.id.textViewResult)
        setUpSearchObservable()
    }

    private fun setUpSearchObservable() {

        searchView?.let {
            mDispose = RxSearchObservable.fromView(it)
                .debounce(300, TimeUnit.MILLISECONDS)
                .filter { text ->
                    if (text.isEmpty()) {
                        textViewResult!!.text = ""
                        false
                    } else {
                        true
                    }
                }
                .distinctUntilChanged()
                .switchMap { query ->
                    dataFromNetwork(query)
                        .doOnError { throwable: Throwable -> }
                        // continue emission in case of error also
                        .onErrorReturn { throwable: Throwable -> "" }
                }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ result -> textViewResult!!.text = result })
        }
    }

    /**
     * Simulation of network data
     */
    private fun dataFromNetwork(query: String): Observable<String> =
        Observable.just(true)
            .delay(2, TimeUnit.SECONDS)
            .map { query }

    override fun onDestroy() {
        super.onDestroy()
        mDispose?.dispose()
    }

}

