@file:Suppress("DEPRECATION")

package info.kurozeropb.azurlane.fragments

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Environment
import android.text.Html
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.hendraanggrian.pikasso.into
import com.hendraanggrian.pikasso.picasso
import com.stfalcon.frescoimageviewer.ImageViewer
import info.kurozeropb.azurlane.API
import info.kurozeropb.azurlane.App
import info.kurozeropb.azurlane.R
import info.kurozeropb.azurlane.adapters.SkinRecyclerAdapter
import info.kurozeropb.azurlane.adapters.file
import info.kurozeropb.azurlane.helpers.GlideApp
import info.kurozeropb.azurlane.helpers.ItemDecoration
import info.kurozeropb.azurlane.responses.Ship
import kotlinx.android.synthetic.main.overlay.view.*
import kotlinx.android.synthetic.main.fragment_tab_general.view.*
import org.jetbrains.anko.backgroundColor
import org.jetbrains.anko.backgroundDrawable
import org.jetbrains.anko.sdk27.coroutines.onClick
import org.jetbrains.anko.support.v4.act
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.*

class GeneralInfo(val name: String, val ship: Ship) : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_tab_general, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val rainbow = GradientDrawable(
            GradientDrawable.Orientation.TL_BR, intArrayOf(
                ContextCompat.getColor(view.context, R.color.rainbow_yellow),
                ContextCompat.getColor(view.context, R.color.rainbow_green),
                ContextCompat.getColor(view.context, R.color.rainbow_blue),
                ContextCompat.getColor(view.context, R.color.rainbow_purple)
            )
        )

        val rarities = mapOf(
            "normal" to ContextCompat.getColor(view.context, R.color.normal),
            "rare" to ContextCompat.getColor(view.context, R.color.rare),
            "elite" to ContextCompat.getColor(view.context, R.color.elite),
            "super rare" to ContextCompat.getColor(view.context, R.color.super_rare),
            "priority" to ContextCompat.getColor(view.context, R.color.priority),
            "unreleased" to ContextCompat.getColor(view.context, R.color.unreleased)
        )

        if (ship.rarity != null) {
            val rarity = ship.rarity.toLowerCase(Locale.getDefault())
            val color = rarities[rarity]
            if (color != null) {
                view.main_image.backgroundColor = color
            } else {
                when (rarity) {
                    "decisive" -> view.main_image.backgroundDrawable = rainbow
                    "ultra rare" -> view.main_image.backgroundDrawable = rainbow
                }
            }
        }

        GlideApp.with(this)
            .load(ship.skins[0].image)
            .into(view.main_image)

        view.main_image.onClick {
            val overlay = View.inflate(view.context, R.layout.overlay, null)
            overlay.btn_share.onClick {
                picasso.load(ship.skins[0].image).into {
                    onFailed { e, _ ->
                        Snackbar.make(view, e.message ?: "Something went wrong", Snackbar.LENGTH_LONG).show()
                    }
                    onLoaded { bitmap, _ ->
                        val intent = Intent(Intent.ACTION_SEND)
                        intent.type = "image/png"

                        val bytes = ByteArrayOutputStream()
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, bytes)

                        val sdCard = Environment.getExternalStorageDirectory()
                        val dir = File(sdCard.absolutePath + "/AzurLane")
                        if (dir.exists().not())
                            dir.mkdirs()

                        file = File(dir, "${ship.skins[0].title?.toLowerCase(Locale.getDefault())?.replace(" ", "-")}.png")

                        try {
                            file.createNewFile()
                            val fo = FileOutputStream(file)
                            fo.write(bytes.toByteArray())
                            fo.flush()
                            fo.close()
                        } catch (e: IOException) {
                            val message = e.message ?: "Unable to save/share image"
                            Snackbar.make(view, message, Snackbar.LENGTH_LONG).show()
                        }

                        val uri = FileProvider.getUriForFile(view.context, view.context.applicationContext.packageName + ".ImageFileProvider", file)
                        intent.putExtra(Intent.EXTRA_STREAM, uri)

                        val parent = if (activity?.parent != null) {
                            activity?.parent
                        } else {
                            file.delete()
                            return@onLoaded
                        }
                        ActivityCompat.startActivityForResult(parent!!, Intent.createChooser(intent, "Share Image"), App.SHARE_IMAGE, null)

                        file.deleteOnExit()
                    }
                }
            }

            overlay.btn_save.onClick {
                API.downloadAndSave(
                    ship.skins[0].title?.toLowerCase(Locale.getDefault())?.replace(" ", "-") ?: "unkown",
                    ship.skins[0].image ?: "",
                    view
                )
            }

            ImageViewer.Builder(context, arrayOf(ship.skins[0].image))
                .setOverlayView(overlay)
                .show()
        }

        view.tv_name.text = Html.fromHtml(getString(R.string.name, "<b>${ship.nationalityShort ?: ""} ${ship.names.en ?: ""}<br/>(cn: ${ship.names.cn ?: ""}; jp: ${ship.names.jp ?: ""}; kr: ${ship.names.kr ?: ""})</b>"), Html.FROM_HTML_MODE_LEGACY)
        view.tv_construction_time.text = Html.fromHtml(getString(R.string.construction, "<b>${ship.buildTime ?: ""}</b>"), Html.FROM_HTML_MODE_LEGACY)
        view.tv_rarity.text = Html.fromHtml(getString(R.string.rarity, "<b>${ship.rarity ?: ""}</b>"), Html.FROM_HTML_MODE_LEGACY)
        view.tv_class.text = Html.fromHtml(getString(R.string.ship_class, "<b>${ship.`class` ?: ""}</b>"), Html.FROM_HTML_MODE_LEGACY)
        view.tv_nationality.text = Html.fromHtml(getString(R.string.nationality, "<b>${ship.nationality ?: ""}</b>"), Html.FROM_HTML_MODE_LEGACY)
        view.tv_classification.text = Html.fromHtml(getString(R.string.classification, "<b>${ship.hullType ?: ""}</b>"), Html.FROM_HTML_MODE_LEGACY)

        view.tv_artist.text = Html.fromHtml(getString(R.string.artist, "<a href=\"${ship.miscellaneous.artist?.link ?: ""}\"><b>${ship.miscellaneous.artist?.name ?: ""}</b></a>"), Html.FROM_HTML_MODE_LEGACY)
        view.tv_artist.movementMethod = LinkMovementMethod.getInstance()
        view.tv_web.text = Html.fromHtml(getString(R.string.web, "<a href=\"${ship.miscellaneous.web?.link ?: ""}\"><b>${ship.miscellaneous.web?.name ?: ""}</b></a>"), Html.FROM_HTML_MODE_LEGACY)
        view.tv_web.movementMethod = LinkMovementMethod.getInstance()
        view.tv_pixiv.text = Html.fromHtml(getString(R.string.pixiv, "<a href=\"${ship.miscellaneous.pixiv?.link ?: ""}\"><b>${ship.miscellaneous.pixiv?.name ?: ""}</b></a>"), Html.FROM_HTML_MODE_LEGACY)
        view.tv_pixiv.movementMethod = LinkMovementMethod.getInstance()
        view.tv_twitter.text = Html.fromHtml(getString(R.string.twitter, "<a href=\"${ship.miscellaneous.twitter?.link ?: ""}\"><b>${ship.miscellaneous.twitter?.name ?: ""}</b></a>"), Html.FROM_HTML_MODE_LEGACY)
        view.tv_twitter.movementMethod = LinkMovementMethod.getInstance()
        view.tv_voice_actress.text = Html.fromHtml(getString(R.string.voice_actress, "<a href=\"${ship.miscellaneous.voiceActress?.link ?: ""}\"><b>${ship.miscellaneous.voiceActress?.name ?: ""}</b></a>"), Html.FROM_HTML_MODE_LEGACY)
        view.tv_voice_actress.movementMethod = LinkMovementMethod.getInstance()

        view.rv_row.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        view.rv_row.addItemDecoration(ItemDecoration(80, ship.skins.size))

        val adapter = SkinRecyclerAdapter()
        val skins = ship.skins.subList(1, ship.skins.size)
        if (skins.isNullOrEmpty()) view.rv_row.visibility = View.GONE
        adapter.setImages(skins, act)
        view.rv_row.adapter = adapter
    }

}