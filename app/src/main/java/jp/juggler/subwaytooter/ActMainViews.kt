package jp.juggler.subwaytooter

import android.view.View
import android.widget.HorizontalScrollView
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import jp.juggler.subwaytooter.actmain.ColumnStripLinearLayout
import jp.juggler.subwaytooter.actmain.TabletModeRecyclerView
import jp.juggler.subwaytooter.view.MyViewPager

class ActMainViews(
    val root: View,
    val viewPager: MyViewPager,
    val rvPager: TabletModeRecyclerView,
)
