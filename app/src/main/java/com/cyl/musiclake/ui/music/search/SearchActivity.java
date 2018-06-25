package com.cyl.musiclake.ui.music.search;

import android.content.Intent;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.PopupMenu;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.cyl.musiclake.R;
import com.cyl.musiclake.api.AddPlaylistUtils;
import com.cyl.musiclake.base.BaseActivity;
import com.cyl.musiclake.common.Extras;
import com.cyl.musiclake.data.db.DaoLitepal;
import com.cyl.musiclake.data.db.Music;
import com.cyl.musiclake.db.SearchHistoryBean;
import com.cyl.musiclake.player.PlayManager;
import com.cyl.musiclake.ui.music.dialog.DownloadDialog;
import com.cyl.musiclake.ui.music.dialog.ShowDetailDialog;
import com.cyl.musiclake.ui.music.local.adapter.SongAdapter;
import com.cyl.musiclake.ui.music.online.activity.ArtistInfoActivity;
import com.cyl.musiclake.utils.LogUtil;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.OnClick;

/**
 * 作者：yonglong on 2016/9/15 12:32
 * 邮箱：643872807@qq.com
 * 版本：2.5
 */
public class SearchActivity extends BaseActivity<SearchPresenter> implements SearchContract.View {

    private static final String TAG = "SearchActivity";
    //搜索信息
    private String queryString;
    private SongAdapter mAdapter;
    private SearchSuggestionAdapter suggestionAdapter;

    @BindView(R.id.recyclerView)
    RecyclerView resultListRcv;
    @BindView(R.id.suggestions_list)
    RecyclerView suggestionsRcv;
    @BindView(R.id.suggestions_panel)
    View suggestionsPanel;
    @BindView(R.id.toolbar_search_edit_text)
    EditText searchEditText;
    @BindView(R.id.toolbar_search_container)
    View searchToolbarContainer;

    @OnClick(R.id.toolbar_search_clear)
    void clearQuery() {
        queryString = "";
        searchEditText.setText("");
    }

    private List<Music> searchResults = new ArrayList<>();
    private List<SearchHistoryBean> suggestions = new ArrayList<>();

    private int mCurrentCounter = 10;
    private int limit = 10;
    private int mOffset = 0;


    @Override
    protected int getLayoutResID() {
        return R.layout.acitvity_search;
    }

    @Override
    protected void initView() {
        suggestionsPanel.setVisibility(View.GONE);
        showSearchOnStart();
    }

    private void showSearchOnStart() {
        searchEditText.setText(queryString);

        if (TextUtils.isEmpty(queryString) || TextUtils.isEmpty(searchEditText.getText())) {
            searchToolbarContainer.setTranslationX(100);
            searchToolbarContainer.setAlpha(0f);
            searchToolbarContainer.setVisibility(View.VISIBLE);
            searchToolbarContainer.animate().translationX(0).alpha(1f).setDuration(200).setInterpolator(new DecelerateInterpolator()).start();
        } else {
            searchToolbarContainer.setTranslationX(0);
            searchToolbarContainer.setAlpha(1f);
            searchToolbarContainer.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void initData() {
        mAdapter = new SongAdapter(searchResults);
        mAdapter.setEnableLoadMore(true);
        //初始化列表
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        resultListRcv.setLayoutManager(layoutManager);
        resultListRcv.setAdapter(mAdapter);
        mAdapter.bindToRecyclerView(resultListRcv);
    }

    @Override
    protected void initInjector() {
        mActivityComponent.inject(this);
    }

    @SuppressWarnings({"unchecked", "varargs"})
    @Override
    protected void listener() {
        mAdapter.setOnItemClickListener((adapter, view, position) -> {
            Music music = searchResults.get(position);
            LogUtil.e(TAG, music.toString());
            PlayManager.playOnline(music);
        });
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                String newText = searchEditText.getText().toString();
                mPresenter.getSuggestions(newText);
            }
        });

        searchEditText.setOnEditorActionListener((v, actionId, event) -> {
            LogUtil.d(TAG, "onEditorAction() called with: v = [" + v + "], actionId = [" + actionId + "], event = [" + event + "]");
            if (event != null && (event.getKeyCode() == KeyEvent.KEYCODE_ENTER || event.getAction() == EditorInfo.IME_ACTION_SEARCH)) {
                search(searchEditText.getText().toString());
                return true;
            }
            return false;
        });

        mAdapter.setOnItemChildClickListener((adapter, view, position) -> {
            Music music = searchResults.get(position);
            PopupMenu popupMenu = new PopupMenu(this, view);
            popupMenu.setOnMenuItemClickListener(item -> {
                switch (item.getItemId()) {
                    case R.id.popup_song_detail:
                        ShowDetailDialog.newInstance(music)
                                .show(getSupportFragmentManager(), TAG);
                        break;
                    case R.id.popup_song_goto_artist:
                        LogUtil.e(TAG, music.toString());
                        Intent intent = new Intent(this, ArtistInfoActivity.class);
                        intent.putExtra(Extras.TING_UID, music);
                        startActivity(intent);
                        break;
                    case R.id.popup_add_playlist:
                        AddPlaylistUtils.INSTANCE.getPlaylist(this, music);
                        break;
                    case R.id.popup_song_download:
                        DownloadDialog.newInstance(music)
                                .show(getSupportFragmentManager(), getLocalClassName());
                        break;
                }
                return true;
            });
            popupMenu.inflate(R.menu.popup_song_online);
            popupMenu.show();
        });
        mAdapter.setOnLoadMoreListener(() -> resultListRcv.postDelayed(() -> {
            if (mCurrentCounter == 0) {
                //数据全部加载完毕
                mAdapter.loadMoreEnd();
            } else {
                mOffset++;
                //成功获取更多数据
                mPresenter.search(queryString, filter, limit, mOffset);
            }
        }, 1000), resultListRcv);


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_search, menu);
        restoreFilterChecked(menu, filterItemCheckedId);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
            case R.id.menu_filter_all:
            case R.id.menu_filter_qq:
            case R.id.menu_filter_xiami:
            case R.id.menu_filter_netease:
                changeFilter(item, getFilterFromMenuId(item.getItemId()));
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    SearchEngine.Filter filter = SearchEngine.Filter.ANY;

    private void changeFilter(MenuItem item, SearchEngine.Filter filter) {
        this.filter = filter;
        this.filterItemCheckedId = item.getItemId();
        item.setChecked(true);

        if (!TextUtils.isEmpty(queryString)) {
            search(queryString);
        }
    }


    private void restoreFilterChecked(Menu menu, int itemId) {
        if (itemId != -1) {
            MenuItem item = menu.findItem(itemId);
            if (item == null) return;

            item.setChecked(true);
            filter = getFilterFromMenuId(itemId);
        }
    }


    int filterItemCheckedId = -1;

    private SearchEngine.Filter getFilterFromMenuId(int itemId) {
        switch (itemId) {
            case R.id.menu_filter_qq:
                return SearchEngine.Filter.QQ;
            case R.id.menu_filter_xiami:
                return SearchEngine.Filter.XIAMI;
            case R.id.menu_filter_netease:
                return SearchEngine.Filter.NETEASE;
            case R.id.menu_filter_all:
            default:
                return SearchEngine.Filter.ANY;
        }
    }

    private void search(String query) {
        if (query.length() > 0) {
            mOffset = 0;
            searchResults.clear();
            queryString = query;
            suggestionsPanel.setVisibility(View.GONE);
            mPresenter.saveQueryInfo(query);
            mPresenter.search(query, filter, limit, mOffset);
        }
    }


    @Override
    public void showLoading() {
        super.showLoading();
    }

    @Override
    public void hideLoading() {
        super.hideLoading();
    }

    @Override
    public void showSearchResult(List<Music> list) {
        if (list.size() > 0) {
            suggestionsPanel.setVisibility(View.GONE);
        } else {
            suggestionsPanel.setVisibility(View.VISIBLE);
        }
        searchResults.addAll(list);
        mAdapter.setNewData(searchResults);
        mAdapter.loadMoreComplete();
        mCurrentCounter = mAdapter.getData().size();
        LogUtil.e("search", mCurrentCounter + "--" + mCurrentCounter + "--" + mOffset);
    }

    @Override
    public void showSearchSuggestion(List<SearchHistoryBean> result) {
        suggestionsPanel.setVisibility(View.VISIBLE);
        if (suggestionAdapter == null) {
            suggestionAdapter = new SearchSuggestionAdapter(suggestions);
            suggestionsRcv.setLayoutManager(new LinearLayoutManager(this));
            suggestionsRcv.setAdapter(suggestionAdapter);
            suggestionAdapter.bindToRecyclerView(suggestionsRcv);
            suggestionAdapter.setOnItemLongClickListener((adapter, view, position) -> {
                return false;
            });
            suggestionAdapter.setOnItemClickListener((adapter, view, position) -> {
            });
            suggestionAdapter.setOnItemChildClickListener((adapter, view, position) -> {
                if (view.getId() == R.id.history_search) {
                    searchEditText.setText(suggestions.get(position).getTitle());
                } else if (view.getId() == R.id.deleteView) {
                    DaoLitepal.INSTANCE.deleteSearchInfo(suggestions.get(position).getTitle());
                    suggestionAdapter.remove(position);
                }
            });
        } else {
            suggestions = result;
            suggestionsPanel.setVisibility(View.VISIBLE);
            suggestionAdapter.setNewData(result);
            suggestionsPanel.setVisibility(View.GONE);
        }
    }

    @Override
    public void showEmptyView() {
        mAdapter.setEmptyView(R.layout.view_song_empty);
    }

}
