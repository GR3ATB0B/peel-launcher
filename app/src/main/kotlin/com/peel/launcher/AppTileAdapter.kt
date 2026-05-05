package com.peel.launcher

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView

class AppTileAdapter(
    private val tiles: List<AppTile>,
    private val onTileClick: (AppTile) -> Unit,
) : RecyclerView.Adapter<AppTileAdapter.TileViewHolder>() {

    class TileViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tile: FrameLayout = view.findViewById(R.id.tile_root)
        val icon: ImageView = view.findViewById(R.id.tile_icon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TileViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_app_tile, parent, false)
        return TileViewHolder(view)
    }

    override fun onBindViewHolder(holder: TileViewHolder, position: Int) {
        val tile = tiles[position]
        holder.tile.setBackgroundResource(tile.backgroundRes)
        holder.icon.setImageResource(tile.iconRes)
        holder.icon.contentDescription = tile.label
        holder.tile.setOnClickListener { onTileClick(tile) }
    }

    override fun getItemCount(): Int = tiles.size
}
