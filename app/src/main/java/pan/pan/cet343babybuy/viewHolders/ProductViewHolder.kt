package pan.pan.cet343babybuy.viewHolders

import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import pan.pan.cet343babybuy.R

class ProductViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
    val productName: TextView = itemView.findViewById(R.id.tv_productName)
    val productCategory: TextView = itemView.findViewById(R.id.chip_category)
    val productPrice: TextView = itemView.findViewById(R.id.tv_productPrice)
    val productImage: ImageView = itemView.findViewById(R.id.iv_productImage)
    val addButton: Button = itemView.findViewById(R.id.tb_addNow)
    val deleteButton: Button = itemView.findViewById(R.id.tb_deleteNow)
}
