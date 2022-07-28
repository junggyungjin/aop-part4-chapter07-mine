package fastcampus.aop.part4.chapter07_mine

import android.annotation.SuppressLint
import android.app.WallpaperManager
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.google.android.material.snackbar.Snackbar
import fastcampus.aop.part4.chapter07_mine.data.Repository
import fastcampus.aop.part4.chapter07_mine.data.models.PhotoResponse
import fastcampus.aop.part4.chapter07_mine.databinding.ActivityMainBinding
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.jar.Manifest

class MainActivity : AppCompatActivity() {

    private val scope = MainScope()
    private val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }
    private val photoAdapter by lazy { PhotoAdapter() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        initViews()
        bindViews()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            fetchRandomPhotos()
        }else {
            requestWriteStoragePermission()
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        val writeExternalStoragePermissionGranted =
            requestCode == REQUEST_WRITE_EXTERNAL_STORAGE_PERMISSION &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED

        if (writeExternalStoragePermissionGranted) {
            fetchRandomPhotos()
        }
    }

    private fun initViews() {
        binding.recyclerView.layoutManager = LinearLayoutManager(this, RecyclerView.VERTICAL, false)
        binding.recyclerView.adapter = photoAdapter
    }

    private fun bindViews() {
        binding.searchEditText.setOnEditorActionListener { editText, actionId, keyEvent ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {

                currentFocus?.let { view ->
                    val inputMethodManager =
                        getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                    // inputMethodManager?.hideSoftInputFromWindow() -> 검색(엔터)했을때 키보드 사라짐
                    inputMethodManager?.hideSoftInputFromWindow(view.windowToken, 0)

                    view.clearFocus()
                }
                fetchRandomPhotos(editText.text.toString())
            }

            true
        }

        binding.refreshLayout.setOnRefreshListener {
            fetchRandomPhotos(binding.searchEditText.text.toString())
        }

        /**
         * 안전한 캐스트 "as?"
        기본적으로 "as" 연산자는 대상 값을 as로 지정한 타입으로 캐스트 하며, 해당 타입으로 바꿀 수 없으면 ClassCastException이 발생하게 됩니다.
        이럴 경우 is 연산자를 통해 대상 값이 해당 타입으로 변환 가능한 타입인지 체크해야 하지만 코틀린에서는 훨씬 더 간결한 기능으로 "as?" 연산자를 제공합니다.
        "as?" 연산자는 어떤 값을 지정한 타입으로 캐스트를 하고, 만약 대상 타입으로 캐스트 할 수 없으면 null을 반환하게 됩니다.
         */
        // 2번(photoadapter에서 만든 이벤트 처리)
        (binding.recyclerView.adapter as? PhotoAdapter)?.onClickPhoto = { photo ->
            showDownloadPhotoConfirmationDialog(photo)
        }
    }

    private fun requestWriteStoragePermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE),
            REQUEST_WRITE_EXTERNAL_STORAGE_PERMISSION
        )
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun fetchRandomPhotos(query: String? = null) = scope.launch {
        try {
            Repository.getRandomPhotos(query)?.let { photos ->

                binding.errorDescriptionTextView.visibility = View.GONE
                (binding.recyclerView.adapter as? PhotoAdapter)?.apply {
                    this.photos = photos
                    notifyDataSetChanged()
                }
                binding.recyclerView.visibility = View.VISIBLE
            }
        } catch (e: Exception) {
            binding.recyclerView.visibility = View.INVISIBLE
            binding.errorDescriptionTextView.visibility = View.VISIBLE
        } finally {
            binding.shimmerLayout.visibility = View.GONE
            binding.refreshLayout.isRefreshing = false
        }


    }

    // 안드로이드에서 제공해주는 다이얼로그
    private fun showDownloadPhotoConfirmationDialog(photo: PhotoResponse) {
        AlertDialog.Builder(this)
            .setMessage("이 사진을 저장하시겠습니까?")
            .setPositiveButton("저장") { dialog, _ ->
                downloadPhoto(photo.urls?.full)
                dialog.dismiss()
            }
            .setNegativeButton("취소") { dialog, _ ->
                dialog.dismiss()
            }
            .create()
            .show()
    }

    private fun downloadPhoto(photoUrl: String?) {
        photoUrl ?: return

        Glide.with(this)
            .asBitmap()
            .load(photoUrl)
            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .into(
                object : CustomTarget<Bitmap>(SIZE_ORIGINAL, SIZE_ORIGINAL) {
                    override fun onResourceReady(
                        resource: Bitmap,
                        transition: Transition<in Bitmap>?
                    ) {
                        saveBitmapToMediaStore(resource)

                        val wallpaperManager = WallpaperManager.getInstance(this@MainActivity)

                        val snackbar = Snackbar.make(binding.root, "다운로드 완료", Snackbar.LENGTH_SHORT)

                        if (wallpaperManager.isWallpaperSupported
                            && (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && wallpaperManager.isSetWallpaperAllowed)) {
                            snackbar.setAction("배경 화면으로 저장") {
                                try {
                                    wallpaperManager.setBitmap(resource)
                                }catch (e: Exception) {
                                    Snackbar.make(binding.root, "배경화면 저장 실패", Snackbar.LENGTH_SHORT)
                                }
                            }
                            snackbar.duration = Snackbar.LENGTH_INDEFINITE
                        }
                        snackbar.show()
                    }

                    override fun onLoadStarted(placeholder: Drawable?) {
                        super.onLoadStarted(placeholder)
                        Snackbar.make(binding.root, "다운로드 중..", Snackbar.LENGTH_INDEFINITE).show()
                    }

                    // 어떤 처리도 하지 않을때
                    override fun onLoadCleared(placeholder: Drawable?) = Unit

                    override fun onLoadFailed(errorDrawable: Drawable?) {
                        super.onLoadFailed(errorDrawable)
                        Snackbar.make(binding.root, "다운로드 실패", Snackbar.LENGTH_SHORT).show()
                    }

                }
            )
    }


    // 저장하기 위한 로직
    private fun saveBitmapToMediaStore(bitmap: Bitmap) {
        var fileName = "${System.currentTimeMillis()}.jpg"
        val resolver = applicationContext.contentResolver
        val imageCollectionUri =
            //안드로이드 버전이 10이상이면
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Images.Media.getContentUri(
                    MediaStore.VOLUME_EXTERNAL_PRIMARY
                )
            } else {
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            }
        val imageDetails = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpg")

            // 내가 저장을 하는 동안에 즉,이게 0으로 바뀌기 전까지는 접근을 못함
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }

        //이미지 정보들을 삽입한다음 반환한다
        val imageUri = resolver.insert(imageCollectionUri, imageDetails)

        //imageUri가 null이면 저장을 하지 않고 리턴
        imageUri ?: return

        // 어떤 파일들을 쓸때 주로 씀 <-> 읽을 때는 반대로 InputStream
        // 코틀린에서 제공하는 use{}을 사용하면 블락이 다 끝나고 닫아주기 까지 한다.
        resolver.openOutputStream(imageUri).use { outputStream ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
        }

        // 저장 작업이 끝났으면 다시 pending을 0으로 바꿔줘야함
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            imageDetails.clear()
            imageDetails.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(imageUri, imageDetails, null, null)
        }

    }

    companion object {
        private const val REQUEST_WRITE_EXTERNAL_STORAGE_PERMISSION = 101
    }
}