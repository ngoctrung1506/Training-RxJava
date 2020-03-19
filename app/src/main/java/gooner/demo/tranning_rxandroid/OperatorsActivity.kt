package gooner.demo.tranning_rxandroid

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import gooner.demo.tranning_rxandroid.operator.combine.MergeExampleActivity
import gooner.demo.tranning_rxandroid.operator.create.SimpleExampleActivity
import gooner.demo.tranning_rxandroid.operator.filter.FilterExampleActivity
import gooner.demo.tranning_rxandroid.operator.filter.SkipExampleActivity
import gooner.demo.tranning_rxandroid.operator.transfrom.BufferExampleActivity
import gooner.demo.tranning_rxandroid.operator.transfrom.MapExampleActivity
import gooner.demo.tranning_rxandroid.operator.transfrom.SwitchMapExampleActivity

class OperatorsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_operators)
    }

    fun startSimpleActivity(view: View?) {
        startActivity(
            Intent(
                this@OperatorsActivity,
                SimpleExampleActivity::class.java
            )
        )
    }

    fun startMapActivity(view: View?) {
        startActivity(
            Intent(
                this@OperatorsActivity,
                MapExampleActivity::class.java
            )
        )
    }

    fun startSwitchMapActivity(view: View?) {
        startActivity(
            Intent(
                this@OperatorsActivity,
                SwitchMapExampleActivity::class.java
            )
        )
    }

    fun startFilterActivity(view: View?) {
        startActivity(
            Intent(
                this@OperatorsActivity,
                FilterExampleActivity::class.java
            )
        )
    }

    fun startSkipActivity(view: View?) {
        startActivity(
            Intent(
                this@OperatorsActivity,
                SkipExampleActivity::class.java
            )
        )
    }

    fun startBufferActivity(view: View?) {
        startActivity(
            Intent(
                this@OperatorsActivity,
                BufferExampleActivity::class.java
            )
        )
    }

    fun startMergeActivity(view: View?) {
        startActivity(
            Intent(
                this@OperatorsActivity,
                MergeExampleActivity::class.java
            )
        )
    }

}
