package com.player.streameraudio.presentation.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.media3.common.MediaItem
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.player.streameraudio.R
import com.player.streameraudio.presentation.ui.RecyclerViewInterface

class StreamersAdaptor(
    private val context: Context,
    private var list: List<MediaItem>,
    private val recyclerViewInterface: RecyclerViewInterface
) : RecyclerView.Adapter<StreamersAdaptor.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_CATEGORY = 0
        private const val VIEW_TYPE_STREAM = 1
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener {
        val streamerThumbnail: ImageView = itemView.findViewById(R.id.thumbnailImageView)
        val streamerUserName: TextView = itemView.findViewById(R.id.usernameTextView)
        val gameNameTextView: TextView = itemView.findViewById(R.id.gameNameTextView)

        // Stream-specific views (nullable - only in streamer_list_item)
        val streamerTitle: TextView? = itemView.findViewById(R.id.titleTextView)
        val streamerViewCount: TextView? = itemView.findViewById(R.id.viewerCountTextView)
        val viewerCountOverlay: LinearLayout? = itemView.findViewById(R.id.viewerCountOverlay)

        init {
            itemView.setOnClickListener(this)
        }

        override fun onClick(v: View?) {
            val pos = bindingAdapterPosition
            if (pos == RecyclerView.NO_POSITION) return
            recyclerViewInterface.onItemClick(pos)
        }
    }

    override fun getItemViewType(position: Int): Int {
        val item = list[position]
        // If item is browsable, it's a category/game
        // If item is playable, it's a stream
        return if (item.mediaMetadata.isBrowsable == true) {
            VIEW_TYPE_CATEGORY
        } else {
            VIEW_TYPE_STREAM
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layoutResId = when (viewType) {
            VIEW_TYPE_CATEGORY -> R.layout.category_list_item
            VIEW_TYPE_STREAM -> R.layout.streamer_list_item
            else -> R.layout.streamer_list_item
        }

        val view = LayoutInflater.from(context).inflate(layoutResId, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = list.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = list[position]
        val art = item.mediaMetadata.artworkUri

        // Load thumbnail (works for both layouts)
        if (art != null) {
            Glide.with(context)
                .load(art)
                .into(holder.streamerThumbnail)
        } else {
            holder.streamerThumbnail.setImageResource(android.R.drawable.ic_menu_gallery)
        }

        val itemType = getItemViewType(position)

        if (itemType == VIEW_TYPE_CATEGORY) {
            // Browsable item (Categories/Top Games)
            holder.streamerUserName.text = item.mediaMetadata.title
            holder.gameNameTextView.text = item.mediaMetadata.artist ?: item.mediaMetadata.subtitle ?: ""

        } else {
            // Playable stream item
            holder.streamerUserName.text = item.mediaMetadata.artist
            holder.streamerTitle?.text = item.mediaMetadata.title

            // Extract metadata from extras
            val extras = item.mediaMetadata.extras
            val viewerCount = extras?.getString("viewerCount")
            val gameName = extras?.getString("gameName")

            // Format and show viewer count overlay
            val formattedCount = viewerCount?.let {
                try {
                    val count = it.toLong()
                    when {
                        count >= 1000 -> {
                            val thousands = count / 1000.0
                            // Remove unnecessary decimals (e.g., 2.0K -> 2K, 2.6K stays)
                            if (thousands % 1.0 == 0.0) {
                                "${thousands.toInt()}K"
                            } else {
                                String.format("%.1fK", thousands)
                            }
                        }
                        else -> count.toString()
                    }
                } catch (e: Exception) {
                    null
                }
            }

            if (!formattedCount.isNullOrBlank()) {
                holder.streamerViewCount?.text = formattedCount
                holder.viewerCountOverlay?.visibility = View.VISIBLE
            } else {
                holder.viewerCountOverlay?.visibility = View.GONE
            }

            // Show game name
            holder.gameNameTextView.text = gameName ?: ""
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateData(newStreamersList: MutableList<MediaItem>) {
        list = newStreamersList
        notifyDataSetChanged()
    }

    fun getItemAtPosition(position: Int): MediaItem = list[position]
}