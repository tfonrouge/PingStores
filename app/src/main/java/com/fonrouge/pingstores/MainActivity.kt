package com.fonrouge.pingstores

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.jcraft.jsch.ChannelExec
import com.jcraft.jsch.JSch
import com.jcraft.jsch.Session
import kotlinx.android.synthetic.main.app_bar_main.*
import kotlinx.android.synthetic.main.content_main.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.*
import kotlin.collections.ArrayList
import kotlin.system.measureTimeMillis

class MainActivity : AppCompatActivity() {

    private lateinit var linearLayoutManager: LinearLayoutManager
    private val storeList = ArrayList<StoreItem>()
    private lateinit var adapter: RecyclerAdapter

    private val model: MainViewModel by viewModels()
    private var swipeRefreshing = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        swipeRefreshLayout.setOnRefreshListener {
            if (!swipeRefreshing) {
                swipeRefreshing = true
                model.getStoreItems(getServerCredentials())
                swipeRefreshing = false
                swipeRefreshLayout.isRefreshing = false
            }
        }

        linearLayoutManager = LinearLayoutManager(this)

        recyclerView.layoutManager = linearLayoutManager
        adapter = RecyclerAdapter(storeList)
        recyclerView.adapter = adapter

        model.getStoreItems(getServerCredentials()).observe(this, {
            adapter.addDataset(it)
        })

        setSupportActionBar(findViewById(R.id.toolbar))

        if (getServerCredentials().hostname.isEmpty()) {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            model.getStoreItems(getServerCredentials())
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
            }
        }
        return true
    }

    private fun getServerCredentials(): ServerCredentials {
        return ServerCredentials(
            PreferenceManager.getDefaultSharedPreferences(this).getString("hostname", "")!!,
            PreferenceManager.getDefaultSharedPreferences(this).getString("username", "")!!,
            PreferenceManager.getDefaultSharedPreferences(this).getString("password", "")!!,
            PreferenceManager.getDefaultSharedPreferences(this).getString("port", "22")!!.toInt(),
        )
    }

    class ServerCredentials(
        val hostname: String,
        val username: String,
        val password: String,
        val port: Int
    )

    class MainViewModel : ViewModel() {

        private val storeItems = MutableLiveData<ArrayList<StoreItem>>()

        fun getStoreItems(serverCredentials: ServerCredentials): MutableLiveData<ArrayList<StoreItem>> {
            loadStoreItems(serverCredentials)
            return storeItems
        }

        private fun loadStoreItems(serverCredentials: ServerCredentials) {
            viewModelScope.launch(Dispatchers.IO) {
                val time = measureTimeMillis {
                    val response = executeRemoteCommand(serverCredentials)
                    if (response != null) {
                        val list = parseToList(response)
                        list.sortBy { storeItem -> storeItem.hostname }
                        storeItems.postValue(list)
                    }
                }
                println("time elapsed: $time ms")
            }
        }

        private fun executeRemoteCommand(serverCredentials: ServerCredentials): String? {

            if (serverCredentials.hostname.isEmpty()) {
                return null
            }

            val jsch = JSch()
            val session: Session = jsch.getSession(
                serverCredentials.username,
                serverCredentials.hostname,
                serverCredentials.port
            )
            session.setPassword(serverCredentials.password)

            // Avoid asking for key confirmation
            val prop = Properties()
            prop["StrictHostKeyChecking"] = "no"
            session.setConfig(prop)

            try {
                session.connect()
            } catch (e: Exception) {
//                Toast.makeText(this, "Error: $e", Toast.LENGTH_LONG).show()
                return null
            }

            // SSH Channel
            val channelssh = session.openChannel("exec") as ChannelExec
            val baos = ByteArrayOutputStream()
            channelssh.outputStream = baos

            // Execute command
            channelssh.setCommand("/home/dulce/bin/getTiendas.sh")
            channelssh.connect()
            Thread.sleep(500)
            channelssh.disconnect()
            return baos.toString()
        }

        private fun buildStoreItem(id: String, password: String, database: String): StoreItem {
            val itm = id.split(",")
            val hostName = itm[0].trim()
            val port = if (itm.size > 1) itm[1].trim().toInt() else 1433
            return StoreItem(
                hostName.toLowerCase(Locale.ROOT),
                port,
                database,
                password
            )
        }

        private fun parseToList(response: String): ArrayList<StoreItem> {
            val list = ArrayList<StoreItem>()
            response.split("{", "}", "\n").forEach { s ->
                try {
                    val s1 = s.trim()
                    val obj = JSONObject("{$s1}")
                    if (obj.length() > 0) {
                        list.add(
                            buildStoreItem(
                                obj.getString("_id"),
                                obj.getString("pwd"),
                                obj.getString("database")
                            )
                        )
                        println(obj)
                    }
                } catch (e: java.lang.Exception) {

                }
            }
            return list
        }
    }
}

