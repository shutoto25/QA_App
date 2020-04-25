package jp.techacademy.mohri.shuto.qa_app

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity()
    , NavigationView.OnNavigationItemSelectedListener {
    /**
     * クラス名.
     */
    private val CLASS_NAME = "MainActivity"
    /**
     * ツールバー.
     */
    private lateinit var mToolbar: Toolbar
    /**
     * ジャンル.
     */
    private var mGenre = 0


    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "$CLASS_NAME.onCreate")

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mToolbar = findViewById(R.id.toolbar)
        setSupportActionBar(mToolbar)

        val fab = findViewById<FloatingActionButton>(R.id.faButton)
        fab.setOnClickListener { _ ->
            // ログイン済みのユーザを取得.
            val user = FirebaseAuth.getInstance().currentUser

            if (user == null) {
                // ログイン画面遷移.
                val intent = Intent(applicationContext, LoginActivity::class.java)
                startActivity(intent)
            } else {
                // 質問投稿画面遷移
                val intent = Intent(applicationContext, QuestionSendActivity::class.java)
                intent.putExtra("genre", mGenre)
                startActivity(intent)
            }
        }

        // ナビゲーションドロワーの設定
        val drawer = findViewById<DrawerLayout>(R.id.drawerLayout)
        val toggle =
            ActionBarDrawerToggle(this, drawer, mToolbar, R.string.app_name, R.string.app_name)
        drawer.addDrawerListener(toggle)
        toggle.syncState()

        val navigationView = findViewById<NavigationView>(R.id.navView)
        navigationView.setNavigationItemSelectedListener(this)
    }


    override fun onResume() {
        Log.d(TAG, "$CLASS_NAME.onResume")

        super.onResume()
        val navigationView = findViewById<NavigationView>(R.id.navView)

        // 趣味を既定の選択とする
        if(mGenre == 0) {
            onNavigationItemSelected(navigationView.menu.getItem(0))
        }
    }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        Log.d(TAG, "$CLASS_NAME.onCreateOptionsMenu")

        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        Log.d(TAG, "$CLASS_NAME.onOptionsItemSelected")

        val id = item.itemId

        if (id == R.id.action_settings) {
            val intent = Intent(applicationContext, SettingActivity::class.java)
            startActivity(intent)
            return true
        }
        return super.onOptionsItemSelected(item)
    }


    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        Log.d(TAG, "$CLASS_NAME.onNavigationItemSelected")

        when (item.itemId) {
            R.id.navHobby -> {
                mToolbar.title = getString(R.string.navigation_item_hobby)
                mGenre = 1
            }
            R.id.navLife -> {
                mToolbar.title = getString(R.string.navigation_item_life)
                mGenre = 2
            }
            R.id.navHealth -> {
                mToolbar.title = getString(R.string.navigation_item_health)
                mGenre = 3
            }
            R.id.navCompter -> {
                mToolbar.title = getString(R.string.navigation_item_computer)
                mGenre = 4
            }
        }
        // ドロワーを閉じる.
        val drawer = findViewById<DrawerLayout>(R.id.drawerLayout)
        drawer.closeDrawer(GravityCompat.START)
        return true
    }
}
