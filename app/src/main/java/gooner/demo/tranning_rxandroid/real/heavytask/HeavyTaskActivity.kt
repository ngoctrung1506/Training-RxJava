package gooner.demo.tranning_rxandroid.real.heavytask

import android.content.Context
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import butterknife.BindView
import butterknife.Unbinder
import gooner.demo.tranning_rxandroid.R
import io.reactivex.disposables.CompositeDisposable

class HeavyTaskActivity : AppCompatActivity() {

    @BindView(R.id.progress_operation_running)
    internal var _progress: ProgressBar? = null

    @BindView(R.id.list_threading_log)
    internal var _logsList: ListView? = null

    private val _adapter: LogAdapter? =
        null
    private val _logs: List<String?>? = null
    private val _disposables =
        CompositeDisposable()
    private val unbinder: Unbinder? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_heavy_task)
    }

    class LogAdapter(
        context: Context?,
        logs: List<String?>
    ) :
        ArrayAdapter<String?>(
            context!!,
            R.layout.item_log,
            R.id.item_log,
            logs
        )
}
}
