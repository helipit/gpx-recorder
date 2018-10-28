package com.iboism.gpxrecorder.records.details

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.iboism.gpxrecorder.R
import com.iboism.gpxrecorder.model.GpxContent
import com.iboism.gpxrecorder.util.*
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.Consumer
import io.realm.Realm
import kotlinx.android.synthetic.main.fragment_gpx_content_viewer.*

class GpxDetailsFragment : Fragment() {
    private lateinit var detailsView: GpxDetailsView
    private var gpxId: Long? = null
    private var fileHelper: FileHelper? = null
    private val compositeDisposable = CompositeDisposable()

    private var gpxTitleConsumer: Consumer<in String> = Consumer {
        updateGpxTitle(it)
    }

    private val exportTouchConsumer = Consumer<Unit> {
        exportPressed()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        gpxId = arguments?.get(Keys.GpxId) as? Long
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        return inflater.inflate(R.layout.fragment_gpx_content_viewer, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Can't do anything if we don't have an Id and corresponding gpxContent //TODO handle invalid state
        val gpxId = gpxId ?: return
        val realm = Realm.getDefaultInstance()
        val gpxContent = GpxContent.withId(gpxId, realm) ?: return
        fileHelper = FileHelper(view.context)


        val distance = gpxContent.trackList.first()?.segments?.first()?.distance ?: 0f

        detailsView = GpxDetailsView(
                root = detail_root,
                titleText = gpxContent.title,
                distanceText = resources.getString(R.string.distance_km, distance),
                dateText = DateTimeFormatHelper.toReadableString(gpxContent.date),
                waypointsText = resources.getQuantityString(R.plurals.waypoint_count, gpxContent.waypointList.size, gpxContent.waypointList.size)
        )

        compositeDisposable.add(detailsView.gpxTitleObservable.subscribe(gpxTitleConsumer))
        compositeDisposable.add(detailsView.exportTouchObservable.subscribe(exportTouchConsumer))

        realm.close()

        map_view?.let {
            it.onCreate(savedInstanceState)
            val mapController = MapController(it.context, gpxId)
            it.viewTreeObserver.addOnGlobalLayoutListener(mapController)
            it.getMapAsync(mapController)
        }
    }

    private fun exportPressed() {
        val gpxId = gpxId ?: return

       detailsView.setButtonsExporting(true)
        fileHelper?.apply {
            shareGpxFile(gpxId).subscribe {
                detailsView.setButtonsExporting(false)
            }
        }
    }

    private fun updateGpxTitle(newTitle: String) {
        val realm = Realm.getDefaultInstance()
        realm.executeTransaction {itRealm ->
            gpxId?.let {
                GpxContent.withId(it, itRealm)?.title = newTitle
            }
        }
        realm.close()
    }

    // todo look into switching to MapFragment so I don't have to do these
    override fun onResume() {
        super.onResume()
        map_view?.onResume()
    }

    override fun onDestroy() {
        super.onDestroy()
        map_view?.onDestroy()
        compositeDisposable.dispose()
    }

    override fun onPause() {
        super.onPause()
        map_view?.onPause()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        map_view?.onLowMemory()
    }

    override fun onStart() {
        super.onStart()
        map_view?.onStart()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        map_view?.onSaveInstanceState(outState)
        super.onSaveInstanceState(outState)
    }

    companion object {
        fun newInstance(gpxId: Long): GpxDetailsFragment {
            val args = Bundle()
            args.putLong(Keys.GpxId, gpxId)

            val fragment = GpxDetailsFragment()
            fragment.arguments = args

            return fragment
        }
    }
}
