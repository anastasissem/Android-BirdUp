package com.example.birdup.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.birdup.R


class RecyclerAdapter (private var titles: MutableList<String>, private var details: MutableList<String>, private var percent: MutableList<String>, private var images: MutableList<Int>, val hideRecyclerView: () -> Unit) :
    RecyclerView.Adapter<RecyclerAdapter.ViewHolder>(){

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        val itemTitle: TextView = itemView.findViewById(R.id.saved_title)
        val itemDetails: TextView = itemView.findViewById(R.id.saved_details)
        val itemPercent: TextView = itemView.findViewById(R.id.prediction_percentage)
        val itemImage: ImageView  = itemView.findViewById(R.id.iv_image)

        init {
            itemView.setOnClickListener {

                val position: Int = adapterPosition

                val getInfo: MutableList<String> = mutableListOf()
                getInfo.add(position.toString())
                getInfo.add(details[position])
                getInfo.add(titles[position])
                getInfo.add(percent[position])

                // ADD POPUP WINDOW ASKING TO SAVE SAMPLE OR DISCARD
                ShowDialog(itemView, getInfo, onSave = hideRecyclerView)
//                {
//                    titles.clear()
//                    details.clear()
//                    percent.clear()
//                    images.clear()
//                    hideRecyclerView()
//                }
                .showDialog()

                }
            }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_layout, parent, false)
        return ViewHolder(v)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.itemTitle.text = titles[position]
        holder.itemDetails.text = details[position]
        holder.itemPercent.text = percent[position]
        holder.itemImage.setImageResource(images[position])
    }

    override fun getItemCount(): Int {
        return titles.size
    }
}