package jp.juggler.subwaytooter

import android.view.View
import android.widget.HorizontalScrollView
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import jp.juggler.subwaytooter.actmain.ColumnStripLinearLayout
import jp.juggler.subwaytooter.view.MyDrawerLayout
import jp.juggler.subwaytooter.view.MyEditText
import jp.juggler.subwaytooter.view.MyNetworkImageView

class ActMainViews(
    val root: View,
    val drawerLayout: MyDrawerLayout,
    val llFormRoot: LinearLayout,
    val tvEmpty: TextView,
    val btnMenu: ImageButton,
    val svColumnStrip: HorizontalScrollView,
    val llColumnStrip: ColumnStripLinearLayout,
    val btnToot: ImageButton,
    val vBottomPadding: View,
    val vFooterDivider1: View,
    val vFooterDivider2: View,
)
