/*
 * Copyright (C) 2016, 程序亦非猿
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package me.yifeiyuan.climb.base;


import android.os.Bundle;
import android.support.annotation.CallSuper;
import android.support.annotation.Nullable;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;

import org.greenrobot.eventbus.Subscribe;

import butterknife.Bind;
import me.yifeiyuan.climb.R;
import me.yifeiyuan.climb.module.gank.ReturnTopEvent;
import me.yifeiyuan.climb.tools.bus.OldDriver;
import me.yifeiyuan.climb.ui.view.OpRecyclerView;
import rx.Subscriber;
import rx.Subscription;

/**
 * encapsulate refresh & loadmore
 */
public abstract class RefreshFragment<A extends RecyclerView.Adapter> extends BaseFragment implements SwipeRefreshLayout.OnRefreshListener, OpRecyclerView.OnLoadMoreListener {

    @Bind(R.id.rv)
    protected OpRecyclerView mRecyclerView;

    @Bind(R.id.refresh)
    protected SwipeRefreshLayout mSwipeRefreshLayout;

    @Nullable
    @Bind(R.id.fab)
    protected FloatingActionButton mFab;

    @Nullable
    @Bind(R.id.col)
    protected CoordinatorLayout mCol;

    /**
     * 页码
     */
    protected int mCurrPage = 1;


    @Override
    protected int getLayoutId() {
        return R.layout.base_refresh_fragment;
    }

    /**
     * 处理公用的view的初始化
     *
     * @param savedInstanceState
     */
    @CallSuper
    @Override
    protected void initView(@Nullable Bundle savedInstanceState) {

        OldDriver.getIns().register(this);
        mSwipeRefreshLayout.setColorSchemeResources(R.color.colorPrimary);
        mSwipeRefreshLayout.setOnRefreshListener(this);
        mRecyclerView.setOnLoadMoreListener(this);
        mRecyclerView.setLayoutManager(getLayoutManager());
        mRecyclerView.setAdapter(getAdapter());
    }

    @Override
    public void onRefresh() {
        requestData(true, true);
    }

    @Override
    public void onLoadMore() {
        requestData(false, false);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        OldDriver.getIns().unregister(this);
    }

    protected void setRefreshing(boolean refreshing) {
        mSwipeRefreshLayout.setRefreshing(refreshing);
    }

    protected void setLoadMoreComplete() {
        mRecyclerView.setLoadMoreComplete();
    }

    protected abstract A getAdapter();

    protected RecyclerView.LayoutManager getLayoutManager() {
        if (mRecyclerView.getLayoutManager() != null) {
            return mRecyclerView.getLayoutManager();
        }
        return new LinearLayoutManager(mActivity, LinearLayoutManager.VERTICAL, false);
    }

    @Override
    protected void requestData(boolean isForce, boolean isRefresh) {
        if (isRefresh) {
            mCurrPage = 1;
            setRefreshing(true);
        } else {
            mCurrPage++;
        }
        Log.d(TAG, "TAG" + TAG + "requestData() called with: " + "isForce = [" + isForce + "], isRefresh = [" + isRefresh + "]");
        addSubscription(onRequestData(isForce, isRefresh));
    }

    abstract protected Subscription onRequestData(boolean isForce, boolean isRefresh);

    /**
     * 默认处理处理
     *
     * @param <T>
     */
    protected class RefreshSubscriber<T> extends Subscriber<T> {

        /** 是否强制刷新 */
        boolean isForce;
        /** 是否是刷新 */
        boolean isRefresh;

        protected RefreshSubscriber(boolean isForce, boolean isRefresh) {
            this.isForce = isForce;
            this.isRefresh = isRefresh;
        }

        @Override
        public void onCompleted() {
            setRefreshing(false);
        }

        @Override
        public void onError(Throwable e) {
            setRefreshing(false);
            if (mCurrPage != 1) {
                setLoadMoreComplete();
            }
            Snackbar snackbar = Snackbar.make(mRootView, "ooops 出错啦!", Snackbar.LENGTH_LONG);
            snackbar.setAction("重试", v -> {
                mCurrPage--;
                requestData(isForce, isRefresh);
            });
            snackbar.show();
        }

        @Override
        public void onNext(T t) {

        }
    }

    @Subscribe
    public void onReturnTop(ReturnTopEvent event) {
        if (getUserVisibleHint() && getLayoutManager() instanceof LinearLayoutManager) {
            ((LinearLayoutManager) getLayoutManager()).scrollToPositionWithOffset(0, 0);
        }
    }
}
