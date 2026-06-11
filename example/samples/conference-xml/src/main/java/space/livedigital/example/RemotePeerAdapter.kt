package space.livedigital.example

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView

internal class RemotePeerAdapter(
    private val layoutInflater: LayoutInflater
) : RecyclerView.Adapter<RecyclerView.ViewHolder>(), DiffUtilsUpdater<PeerWithMediaContentType> {

    private val list = mutableListOf<PeerWithMediaContentType>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return RemotePeerViewHolder(
            layoutInflater.inflate(
                R.layout.item_remote_peer,
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        (holder as? RemotePeerViewHolder)?.bind(list[position])
    }

    override fun onViewDetachedFromWindow(holder: RecyclerView.ViewHolder) {
        (holder as? RemotePeerViewHolder)?.unbind()
    }
    override fun getItemCount() = list.size

    override fun updateItemsWithDiffUtil(items: List<PeerWithMediaContentType>) {
        val diffUtil = PeerDiffUtils(list, items)
        val result = DiffUtil.calculateDiff(diffUtil)
        list.clear()
        list.addAll(items)
        result.dispatchUpdatesTo(this)
    }
}