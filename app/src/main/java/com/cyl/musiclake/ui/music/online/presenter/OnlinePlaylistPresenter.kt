package com.cyl.musiclake.ui.music.online.presenter

import android.content.Context

import com.cyl.musicapi.BaseApiImpl
import com.cyl.musicapi.bean.ListItem
import com.cyl.musiclake.api.MusicUtils
import com.cyl.musiclake.api.PlaylistApiServiceImpl
import com.cyl.musiclake.api.baidu.BaiduApiServiceImpl
import com.cyl.musiclake.base.BasePresenter
import com.cyl.musiclake.bean.Music
import com.cyl.musiclake.bean.Playlist
import com.cyl.musiclake.net.ApiManager
import com.cyl.musiclake.net.RequestCallBack
import com.cyl.musiclake.ui.music.online.contract.OnlinePlaylistContract

import java.util.ArrayList

import javax.inject.Inject

import io.reactivex.Observer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers

/**
 * Created by D22434 on 2018/1/4.
 */

class OnlinePlaylistPresenter @Inject
constructor() : BasePresenter<OnlinePlaylistContract.View>(), OnlinePlaylistContract.Presenter {

    override fun loadBaiDuPlaylist() {
        BaiduApiServiceImpl.getOnlinePlaylist()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .compose(mView.bindToLife())
                .subscribe(object : Observer<List<Playlist>> {
                    override fun onSubscribe(d: Disposable) {
                        mView.showLoading()
                    }

                    override fun onNext(result: List<Playlist>) {
                        mView.showCharts(result)
                    }

                    override fun onError(e: Throwable) {
                        e.printStackTrace()
                        mView.hideLoading()
                        mView.showErrorInfo(e.message)
                    }

                    override fun onComplete() {
                        mView.hideLoading()
                    }
                })
    }

    override fun loadTopList() {
        val observable = PlaylistApiServiceImpl.getNeteaseRank(IntArray(22) { i -> i }, 3)
        ApiManager.request(observable, object : RequestCallBack<MutableList<Playlist>> {
            override fun success(result: MutableList<Playlist>) {
                mView?.showCharts(result)
            }

            override fun error(msg: String) {
                mView.hideLoading()
            }
        })
    }
}
