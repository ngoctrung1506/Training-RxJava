package gooner.demo.tranning_rxandroid

import io.reactivex.Observable

class DeferClass {

    private var value: String? = null

    fun setValue(value: String) {
        this.value = value
    }

    /*
 * DeferClass instance = new DeferClass();
 * Observable<String> value = instance.valueObservable();
 * instance.setValue("Test Value");
 * value.subscribe(System.out::println); //null will be printed
 *
 * Suppose we call the below class and create an Observable,
 * when we call print, it will print null, since value had yet
 * to be initialized when Observable.just() is called.
 */
    fun valueObservable(): Observable<String> {
        return Observable.defer<String> { Observable.just<String>(value)}
    }
}
