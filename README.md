simple-charts (beta)
====================
A Simple Android Charting Library that uses Adapter pattern.
This library is currently in progress. Only line chart is available to use for now.

## Features
* Simple and Fast
* XML configuration/styling
* Scrubbing
* Customizable

## Preview
![](https://media.giphy.com/media/UzNAajOF5N5b7nbD5P/giphy.gif)

## Setup
1. Add Maven jitpack.io to repositories
```
repositories {
  maven {
    url "https://jitpack.io"
    //Optional - just to include this repository only
    //content {
    //    includeGroupByRegex "com\\.github.marungbukid.*"
    //}
  }
}
```
2. Add to dependencies
```
  implementation "com.github.marungbukid:simple-charts:x.y.z" // Version see tags
```

3. Define the View
```
<LinearLayout xmlns:app="http://schemas.android.com/apk/res-auto"
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:orientation="vertical">
     <com.marungbukid.charts.line.LineChartView
      android:id="@+id/lineChart"
      android:layout_width="match_parent"
      android:layout_height="188dp"
      android:paddingTop="@dimen/spacing_small"
      android:paddingBottom="@dimen/spacing_small"

      <!-- Define your own colors of your liking -->
      app:charts_lineColor="@color/secondaryColor"

      app:charts_scrubEnabled="true"
      app:charts_scrubLineColor="@color/black"
      app:charts_scrubLineWidth="0.5dp"

      app:charts_fillColor="@color/primary"
      app:charts_fillType="down"
      app:charts_hasPriceAxis="true"

      app:charts_topColorGradientFill="@color/secondaryColor"
      app:charts_bottomColorGradientFill="@android:color/transparent"

      app:charts_priceAxisDividerColor="@color/black"
      app:charts_priceAxisTextColor="@color/black"
      app:charts_priceAxisTextSize="12sp" />
</LinearLayout>
```

4. Define your adapter and chart entry class
```
// This class is available on ScrubListener
data class ChartEntry(
  val index: Long,
  val value: Double,
  val timestamp: Long,
  val label: String
) : LineChartEntry() {
  override fun getIndex(): Int {
    return index.toInt()
  }

  override fun getValue(): Float {
    return value.toFloat()
  }

  override fun getDateTime(): Long {
    return timestamp
  }
}


class ChartAdapter : BaseAdapter<LineChartEntry>() {
  var items = emptyList<LineChartEntry>()
  set(value) {
    field = value
    notifyDataSetChanged()
  }
  override fun getCount(): Int {
    return items.size
  }

  override fun getItem(index: Int): LineChartEntry {
    return items[index]
  }

  override fun getY(index: Int): Float {
    return getItem(index).value
  }
}
```

5. Implement in your activity/fragment
```
class ChartFragment : Fragment(), BaseChart.OnScrubListener {
  ...
  fun setupCharts() {
    val adapter = ChartsAdapter()
    
    lineChart.setAdapter(adapter)
    lineChart.scrubListener = this
    
    adapter.items = arrayListOf(
      ChartEntry(
          index = 1,
          value = 500,
          timestamp = Date().time,
          label = "Something"
      ),
      ChartEntry(
          index = 2,
          value = 600,
          timestamp = Date().time,
          label = "McDonald's"
      ),
      ChartEntry(
          index = 3,
          value = 700,
          timestamp = Date().time,
          label = "Jollibee"
      ),
      ChartEntry(
          index = 4,
          value = 800,
          timestamp = Date().time,
          label = "Samgyupsal"
      ),
    )
  }
  
  override fun onScrubbed(value: Any?) {
    (value as? ChartEntry)?.let {
        Log.d("SimpleCharts", value.toString())
    }
  }
}

```
