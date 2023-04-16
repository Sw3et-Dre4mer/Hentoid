package me.devsaki.hentoid.fragments.reader

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.bumptech.glide.load.Transformation
import com.bumptech.glide.load.resource.bitmap.CenterInside
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import me.devsaki.hentoid.R
import me.devsaki.hentoid.core.HentoidApp
import me.devsaki.hentoid.database.domains.Content
import me.devsaki.hentoid.databinding.IncludeReaderContentBottomPanelBinding
import me.devsaki.hentoid.util.ContentHelper
import me.devsaki.hentoid.util.ThemeHelper
import me.devsaki.hentoid.util.image.ImageHelper
import me.devsaki.hentoid.viewmodels.ReaderViewModel
import me.devsaki.hentoid.viewmodels.ViewModelFactory

class ReaderContentBottomSheetFragment : BottomSheetDialogFragment() {

    // Communication
    private lateinit var viewModel: ReaderViewModel

    // UI
    private var _binding: IncludeReaderContentBottomPanelBinding? = null
    private val binding get() = _binding!!
    private val stars: Array<ImageView?> = arrayOfNulls(5)

    // VARS
    private var currentRating = -1
    private val glideRequestOptions: RequestOptions

    init {
        val context: Context = HentoidApp.getInstance()
        val tintColor = ThemeHelper.getColor(context, R.color.light_gray)

        val bmp = BitmapFactory.decodeResource(context.resources, R.drawable.ic_hentoid_trans)
        val d: Drawable = BitmapDrawable(context.resources, ImageHelper.tintBitmap(bmp, tintColor))

        val centerInside: Transformation<Bitmap> = CenterInside()
        glideRequestOptions = RequestOptions()
            .optionalTransform(centerInside)
            .error(d)
    }


    override fun onAttach(context: Context) {
        super.onAttach(context)

        val vmFactory = ViewModelFactory(requireActivity().application)
        viewModel = ViewModelProvider(requireActivity(), vmFactory)[ReaderViewModel::class.java]
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = IncludeReaderContentBottomPanelBinding.inflate(inflater, container, false)

        stars[0] = binding.rating1
        stars[1] = binding.rating2
        stars[2] = binding.rating3
        stars[3] = binding.rating4
        stars[4] = binding.rating5

        binding.imgActionFavourite.setOnClickListener { onFavouriteClick() }
        for (i in 0..4) {
            stars[i]?.setOnClickListener { setRating(i + 1) }
        }

        return binding.root
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.getContent().observe(viewLifecycleOwner) { content ->
            this.onContentChanged(content)
        }
    }

    private fun onContentChanged(content: Content?) {
        if (null == content) return

        val thumbLocation = content.cover.usableUri
        if (thumbLocation.isEmpty()) {
            binding.ivCover.visibility = View.INVISIBLE
        } else {
            binding.ivCover.visibility = View.VISIBLE
            if (thumbLocation.startsWith("http")) {
                val glideUrl = ContentHelper.bindOnlineCover(content, thumbLocation)
                if (glideUrl != null) {
                    Glide.with(binding.ivCover)
                        .load(glideUrl)
                        .apply(glideRequestOptions)
                        .into(binding.ivCover)
                }
            } else Glide.with(binding.ivCover)
                .load(Uri.parse(thumbLocation))
                .apply(glideRequestOptions)
                .into(binding.ivCover)
        }
        binding.contentTitle.text = content.title
        binding.contentArtist.text = ContentHelper.formatArtistForDisplay(requireContext(), content)
        updateFavouriteDisplay(content.isFavourite)
        updateRatingDisplay(content.rating)
        val tagTxt = ContentHelper.formatTagsForDisplay(content)
        if (tagTxt.isEmpty()) {
            binding.contentTags.visibility = View.GONE
        } else {
            binding.contentTags.visibility = View.VISIBLE
            binding.contentTags.text = tagTxt
        }
    }


    private fun updateFavouriteDisplay(isFavourited: Boolean) {
        if (isFavourited) binding.imgActionFavourite.setImageResource(R.drawable.ic_fav_full) else binding.imgActionFavourite.setImageResource(
            R.drawable.ic_fav_empty
        )
    }

    private fun updateRatingDisplay(rating: Int) {
        currentRating = rating
        for (i in 5 downTo 1) stars[i - 1]?.setImageResource(if (i <= rating) R.drawable.ic_star_full else R.drawable.ic_star_empty)
    }

    private fun setRating(rating: Int) {
        val targetRating = if (currentRating == rating) 0 else rating
        viewModel.setContentRating(targetRating) { r: Int -> this.updateRatingDisplay(r) }
    }

    private fun onFavouriteClick() {
        viewModel.toggleContentFavourite { isFavourited: Boolean ->
            this.updateFavouriteDisplay(isFavourited)
        }
    }

    companion object {
        fun invoke(
            context: Context,
            fragmentManager: FragmentManager
        ) {
            val bottomSheetFragment = ReaderContentBottomSheetFragment()
            ThemeHelper.setStyle(
                context,
                bottomSheetFragment,
                DialogFragment.STYLE_NORMAL,
                R.style.Theme_Light_BottomSheetDialog
            )
            bottomSheetFragment.show(fragmentManager, "metaEditBottomSheetFragment")
        }
    }
}