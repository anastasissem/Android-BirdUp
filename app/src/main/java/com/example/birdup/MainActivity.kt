package com.example.birdup

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.example.birdup.databinding.ActivityMainBinding
import com.example.birdup.ui.home.HomeFragment
import com.google.android.material.navigation.NavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.appBarMain.toolbar)

        // DISABLE + BUTTON UNTIL YOU HAVE USE FOR IT
//        binding.appBarMain.fab.setOnClickListener { view ->
//            Snackbar.make(view, "ADD FUN FACTS HERE", Snackbar.LENGTH_LONG)
//                .setAction("Action", null).show()
//        }
        val drawerLayout: DrawerLayout = binding.drawerLayout
        val navView: NavigationView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_home, R.id.nav_recordings, R.id.nav_about, R.id.nav_feedback,R.id.nav_rate
            ), drawerLayout
        )

        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        // Invoke e-mail clients to send feedback about app
        navView.menu.findItem(R.id.nav_feedback).setOnMenuItemClickListener {
            val intent = Intent(Intent.ACTION_SEND).apply{
                type = "message/rfc822"
                putExtra(Intent.EXTRA_EMAIL, arrayOf("anmessis@inf.uth.gr"))
                putExtra(Intent.EXTRA_SUBJECT, "BirdUp App Feedback")
            }
            try {
                startActivity(Intent.createChooser(intent, "Choose e-mail client"))
            } catch (e: ActivityNotFoundException){
                Toast.makeText(this, e.toString(), Toast.LENGTH_SHORT).show()
            }
            true
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }
}