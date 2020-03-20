package gooner.demo.tranning_rxandroid.real.search

import android.widget.SearchView
import android.widget.SearchView.OnQueryTextListener
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

object RxSearchObservable {

    fun fromView(searchView: SearchView): Observable<String> {
        val subject: PublishSubject<String> =
            PublishSubject.create()
        searchView.setOnQueryTextListener(object : OnQueryTextListener {
            override fun onQueryTextSubmit(s: String): Boolean {
                subject.onNext(s)
                return true
            }

            override fun onQueryTextChange(text: String): Boolean {
                subject.onNext(text)
                return true
            }
        })
        return subject
    }
}