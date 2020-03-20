package gooner.demo.tranning_rxandroid.real.pagination

import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.OnScrollListener
import gooner.demo.tranning_rxandroid.R
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.processors.PublishProcessor
import io.reactivex.schedulers.Schedulers
import java.util.*
import java.util.concurrent.TimeUnit

class PaginationActivity : AppCompatActivity() {

    val TAG: String? =
        PaginationActivity::class.java.getSimpleName()
    private val compositeDisposable =
        CompositeDisposable()
    private val paginator: PublishProcessor<Int> =
        PublishProcessor.create()
    private var paginationAdapter: PaginationAdapter? = null
    private var recyclerView: RecyclerView? = null
    private var progressBar: ProgressBar? = null
    private var loading = false
    private var pageNumber = 1
    private val VISIBLE_THRESHOLD = 1
    private var lastVisibleItem = 0
    private var totalItemCount = 0
    private var layoutManager: LinearLayoutManager? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pagination)
        recyclerView = findViewById<RecyclerView?>(R.id.recyclerView)
        progressBar = findViewById<ProgressBar?>(R.id.progressBar)
        layoutManager = LinearLayoutManager(this)
        layoutManager!!.orientation = RecyclerView.VERTICAL
        recyclerView!!.layoutManager = layoutManager
        paginationAdapter = PaginationAdapter()
        recyclerView!!.adapter = paginationAdapter
        setUpLoadMoreListener()
        subscribeForData()
    }

    override fun onDestroy() {
        super.onDestroy()
        compositeDisposable.clear()
    }

    /**
     * setting listener to get callback for load more
     */
    private fun setUpLoadMoreListener() {
        recyclerView!!.addOnScrollListener(object :
            OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView!!, dx, dy)
                totalItemCount = layoutManager!!.itemCount
                lastVisibleItem = layoutManager!!
                    .findLastVisibleItemPosition()
                if (!loading
                    && totalItemCount <= lastVisibleItem + VISIBLE_THRESHOLD
                ) {
                    pageNumber++
                    paginator.onNext(pageNumber)
                    loading = true
                }
            }
        })
    }

    /**
     * subscribing for data
     */
    private fun subscribeForData() {
        val disposable: Disposable? = paginator
            .onBackpressureDrop()
            .doOnNext { page: Int ->
                loading = true
                progressBar!!.visibility = View.VISIBLE
            }
            .concatMapSingle<MutableList<String>> { page: Int ->
                dataFromNetwork(page)
                    .subscribeOn(Schedulers.io())
                    .doOnError { throwable: Throwable -> }
                    // continue emission in case of error also
                    .onErrorReturn { throwable: Throwable -> ArrayList<String>() }
            }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { items: MutableList<String> ->
                paginationAdapter!!.addItems(items)
                paginationAdapter!!.notifyDataSetChanged()
                loading = false
                progressBar!!.visibility = View.INVISIBLE
            }
        compositeDisposable.add(disposable!!)
        paginator.onNext(pageNumber)
    }

    /**
     * Simulation of network data
     */
    private fun dataFromNetwork(page: Int): Single<MutableList<String>> {
        return Single.just(true)
            .delay(2, TimeUnit.SECONDS)
            .map { value: Boolean ->
                val items: MutableList<String> = mutableListOf()
                for (i in 1..10) {
                    items.add("Item " + (page * 10 + i))
                }
                items
            }
    }
}
