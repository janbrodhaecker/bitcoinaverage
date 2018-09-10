package janbrodhaecker.de.bitcointracker

import android.content.DialogInterface
import android.graphics.Color
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.view.View
import android.widget.*
import io.reactivex.android.schedulers.AndroidSchedulers
import janbrodhaecker.de.bitcointracker.remote.`interface`.BitcoinAverageRemoteInterface
import janbrodhaecker.de.bitcointracker.remote.`interface`.BitcoinAverageCurrency
import janbrodhaecker.de.bitcointracker.remote.`interface`.BitcoinAverageHistoricalData
import lecho.lib.hellocharts.model.Axis
import lecho.lib.hellocharts.model.Line
import lecho.lib.hellocharts.model.LineChartData
import lecho.lib.hellocharts.model.PointValue
import lecho.lib.hellocharts.view.LineChartView
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList


class MainActivity : AppCompatActivity(), AdapterView.OnItemSelectedListener {

    lateinit var brmi: BitcoinAverageRemoteInterface

    lateinit var tvCurrentValue: TextView
    lateinit var tvPriceDayVal: TextView
    lateinit var tvPercentDayVal: TextView
    lateinit var tvLastUpdated: TextView
    lateinit var tvTodaysOpen: TextView
    lateinit var tvTodaysHigh: TextView
    lateinit var tvTodaysLow: TextView
    lateinit var tv24Average: TextView
    lateinit var tvGlobalVol: TextView

    lateinit var dpCurrency: Spinner

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initialize()
    }

    private fun initialize() {
        // Initialize remote Interface
        brmi = BitcoinAverageRemoteInterface()
        val subscribe = brmi.initialize().subscribe()

        showInitialDialog()

        // Initialize TextViews
        tvCurrentValue = findViewById(R.id.tvCurrentCourseVal)
        tvPriceDayVal = findViewById(R.id.tvPriceDayVal)
        tvPercentDayVal = findViewById(R.id.tvPercentDayVal)
        tvLastUpdated = findViewById(R.id.tvLastUpdateVal)
        tvTodaysOpen = findViewById(R.id.tvTodaysOpenVal)
        tvTodaysHigh = findViewById(R.id.tvTodaysHighVal)
        tvTodaysLow = findViewById(R.id.tvTodaysLowVal)
        tv24Average = findViewById(R.id.tv24AverageVal)
        tvGlobalVol = findViewById(R.id.tvGlobalValueVal)

        initializeCurrencyDropdown()
        initializeChart()
        initializeStateToast()

        brmi.subscribeToGeneralData()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ data ->
                    tvCurrentValue.text = formatCurrentValue(data.getCurrentValue())
                    tvPriceDayVal.text = formatCurrentPrice(data.getPriceDay())
                    tvPercentDayVal.text = formatPercentage(data.getPercentDay(), tvPercentDayVal)
                    tvLastUpdated.text = getFormattedTimestamp(data.getTimeStamp())
                    tvTodaysOpen.text = data.getTodaysOpen().toString()
                    tvTodaysHigh.text = data.getTodaysHigh().toString()
                    tvTodaysLow.text = data.getTodaysLow().toString()
                    tv24Average.text = data.getAverage24h().toString()
                    tvGlobalVol.text = "%.2f".format(data.getGlobalVolume())
        })
    }

    private fun showInitialDialog() {
        AlertDialog.Builder(this)
            .setMessage("Hi! \nThe initial setup will take up to 20seconds! Afterwards, the websocket will update the values every 15seconds.")
            .setPositiveButton("OK!", object: DialogInterface.OnClickListener {
                override fun onClick(dialog: DialogInterface?, which: Int) {
                    dialog?.cancel()
                }
            }).create().show()
    }

    private fun initializeStateToast() {
        brmi.getRemoteState()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({state ->
                    Toast.makeText(this, "Connection State changed to: ${state.name}",
                            Toast.LENGTH_SHORT).show()
        })
    }

    private fun formatCurrentValue(currentValue: Double): String {
        return "${currentValue.toString()}${brmi.getcurrentCurrency().getSymbol()}"
    }

    private fun formatCurrentPrice(currentPrice: Double): String {
        return "${brmi.getcurrentCurrency().getSymbol()} ${currentPrice.toString()}"
    }

    private fun formatPercentage(percentage: Double, tv: TextView): String {
        val indicator = if (percentage >= 0) "+" else "-"
        val color = if (percentage >= 0) Color.GREEN else Color.RED
        tv.setTextColor(color)
        return "($indicator ${percentage.toString()}%)"
    }

    private fun getFormattedTimestamp(timestamp: Long): String {
        val date = Date(timestamp * 1000)
        val format = if (is24HourFormat()) "HH:mm:ss" else "hh:mm:ss a"
        val dateFormat = SimpleDateFormat(format)
        return "${dateFormat.format(date)}"
    }

    private fun is24HourFormat(): Boolean {
        return android.text.format.DateFormat.is24HourFormat(applicationContext);
    }

    private fun initializeCurrencyDropdown() {
        dpCurrency = findViewById(R.id.dpCurrency)
        dpCurrency.onItemSelectedListener = this

        var currencyStringValues = ArrayList<String>()

        for (currency: BitcoinAverageCurrency in brmi.getAvailableCurrencies()) {
            currencyStringValues.add(currency.getText())
        }

        var dataAdapter = ArrayAdapter<String>(this, android.R.layout.simple_spinner_item,
                currencyStringValues)

        dpCurrency.adapter = dataAdapter
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {
        // Nothing to do so far ...
    }

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        brmi.setCurrency(brmi.getAvailableCurrencies().get(position))
    }

    private fun initializeChart() {

        var chartView: LineChartView = findViewById(R.id.chart)
        val entries = ArrayList<PointValue>()

        /*chartView.setOnLongClickListener(object: View.OnLongClickListener {
            override fun onLongClick(v: View?): Boolean {
                // I would like to use the long click, to allow the user to change the configuration of the chart
                return true
            }

        }) */

        var axisX = Axis()
        axisX.name = "Time"
        axisX.maxLabelChars = 3


        var axisY = Axis()
        axisY.name = "Average"

        brmi.getHistoricalData(BitcoinAverageRemoteInterface.ChartPeriod.daily).subscribe({ result: List<BitcoinAverageHistoricalData> ->
            for (historicalData in result) {
                entries.add(PointValue(historicalData.getTimestamp(), historicalData.getAverage()))
            }
            var line = Line(entries).setColor(Color.BLUE).setCubic(false).setHasPoints(false)
            var lines: ArrayList<Line> = ArrayList<Line>()
            lines.add(line)
            var lineChartData = LineChartData()
            lineChartData.axisXBottom = axisX
            lineChartData.axisYLeft = axisY
            lineChartData.setLines(lines)
            chartView.lineChartData = lineChartData

        })
    }
}
