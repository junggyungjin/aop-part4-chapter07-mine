package fastcampus.aop.part4.chapter07_mine

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import fastcampus.aop.part4.chapter07_mine.data.models.PhotoResponse
import fastcampus.aop.part4.chapter07_mine.databinding.ItemPhotoBinding

class PhotoAdapter : RecyclerView.Adapter<PhotoAdapter.ViewHolder>() {

    var photos: List<PhotoResponse> = emptyList()

    var onClickPhoto: (PhotoResponse) -> Unit = {}

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            ItemPhotoBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(photos[position])
    }

    override fun getItemCount(): Int {
        return photos.size
    }

    inner class ViewHolder(
        private val binding: ItemPhotoBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            /**
             * 밑의 작업을 하면
             * 클릭 당시에 어댑터포지션에 있는 포토를 이벤트로 전달하게 됨
             * 그런 다음 메인액티비티로 넘어가서 이 이벤트 등록을 해서 처리해야함
             * 메인 액티비티 처리 부분을 2번이라고 주석 달겠음.
             */
            binding.root.setOnClickListener {
                onClickPhoto(photos[adapterPosition])
            }
        }

        fun bind(photo: PhotoResponse) {
            // photo.height / photo.width.toFloat() -> 비율값구하기
            val dimensionRatio = photo.height / photo.width.toFloat()

            /**
             * binding.root.resources.displayMetrics.widthPixels -> 스크린의 크기를 가져올수있음
             */

            val targetWidth = binding.root.resources.displayMetrics.widthPixels -
                    (binding.root.paddingStart + binding.root.paddingEnd)
            var targetHeight = (targetWidth * dimensionRatio).toInt()

            binding.contentsContainer.layoutParams = binding.contentsContainer.layoutParams.apply {
                height = targetHeight
            }

            Glide.with(binding.root)
                .load(photo.urls?.regular)
                .thumbnail(
                    Glide.with(binding.root)
                        .load(photo.urls?.thumb)
                        .transition(DrawableTransitionOptions.withCrossFade())
                )
                .override(targetWidth, targetHeight)
                .into(binding.photoImageView)

            Glide.with(binding.root)
                .load(photo.user?.profileImageUrls?.small)
                .placeholder(R.drawable.shape_profile_placeholder)
                .circleCrop()
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(binding.profileImageView)

            if (photo.user?.name.isNullOrBlank()) {
                binding.authorTextView.visibility = View.GONE
            } else {
                binding.authorTextView.visibility = View.VISIBLE
                binding.authorTextView.text = photo.user?.name
            }

            if (photo.description.isNullOrBlank()) {
                binding.descriptionTextView.visibility = View.GONE
            } else {
                binding.descriptionTextView.visibility = View.VISIBLE
                binding.descriptionTextView.text = photo.description
            }

        }
    }

}