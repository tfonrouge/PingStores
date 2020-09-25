package com.fonrouge.pingstores

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.recyclerview_item_row.view.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.sql.Connection
import java.sql.ResultSet
import java.sql.Statement
import kotlin.system.measureTimeMillis

val adapterScope = CoroutineScope(Dispatchers.Default)

class RecyclerAdapter(private val storeItems: ArrayList<StoreItem>) :
    RecyclerView.Adapter<StoreItemHolder>() {

    var connectTimeout = 15

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): StoreItemHolder {
        val inflatedView = parent.inflate(R.layout.recyclerview_item_row, false)
        return StoreItemHolder(inflatedView, connectTimeout)
    }

    override fun onBindViewHolder(holder: StoreItemHolder, position: Int) {
        holder.bindStoreItem(storeItems[position])
    }

    override fun getItemCount() = storeItems.size

    fun addDataset(list: ArrayList<StoreItem>) {
        this.storeItems.clear()
        this.storeItems.addAll(list)
        notifyDataSetChanged()
    }
}

class StoreItemHolder(
    private val view: View,
    private val connectTimeout: Int,
) : RecyclerView.ViewHolder(view),
    View.OnClickListener {

    private var storeItem: StoreItem? = null

    init {
        view.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
/*
            val context = itemView.context
            val showPhotoIntent = Intent(context, PhotoActivity::class.java)
            showPhotoIntent.putExtra(PHOTO_KEY, photo)
            context.startActivity(showPhotoIntent)
*/
    }

    fun bindStoreItem(storeItem: StoreItem) {
        this.storeItem = storeItem
        view.hostName.text = storeItem.hostname
        view.port.text = storeItem.port.toString()
        view.imageAvailable.setImageResource(R.drawable.ic_hourglass_empty_24px)
        view.millis.text = ""

        adapterScope.launch(Dispatchers.Default) {
            val connection: Connection?
            var time = measureTimeMillis {
                connection = getSQLConnection(
                    storeItem.hostname,
                    storeItem.port,
                    storeItem.database,
                    storeItem.password,
                    connectTimeout
                )
            }
            if (connection != null) {
                val statement: Statement
                val resultSet: ResultSet
                val isAlive: Boolean
                time += measureTimeMillis {
                    statement = connection.createStatement()
                    resultSet = statement.executeQuery("SELECT TOP 1 Articulo FROM Articulos")
                    isAlive = resultSet.next()
                    resultSet.close()
                    statement.connection.close()
                }
                adapterScope.launch(Dispatchers.Main) {
                    view.imageAvailable.visibility = View.VISIBLE
                    view.millis.text = String.format("%d ms", time)
                    view.imageAvailable.setImageResource(if (isAlive) R.drawable.ic_check_24px else R.drawable.ic_error_outline_24px)
                }
            } else {
                adapterScope.launch(Dispatchers.Main) {
                    view.imageAvailable.visibility = View.VISIBLE
                    view.imageAvailable.setImageResource(R.drawable.ic_error_outline_24px)
                }
            }
        }
    }
}
