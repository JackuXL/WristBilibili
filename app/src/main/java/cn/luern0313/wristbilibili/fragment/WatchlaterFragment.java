package cn.luern0313.wristbilibili.fragment;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import java.io.IOException;
import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import cn.luern0313.wristbilibili.R;
import cn.luern0313.wristbilibili.adapter.ListVideoAdapter;
import cn.luern0313.wristbilibili.api.WatchLaterApi;
import cn.luern0313.wristbilibili.models.ListVideoModel;
import cn.luern0313.wristbilibili.ui.VideoActivity;
import cn.luern0313.wristbilibili.util.ColorUtil;
import cn.luern0313.wristbilibili.util.DataProcessUtil;
import cn.luern0313.wristbilibili.util.ListViewTouchListener;
import cn.luern0313.wristbilibili.util.SharedPreferencesUtil;
import cn.luern0313.wristbilibili.util.ViewTouchListener;
import cn.luern0313.wristbilibili.widget.TitleView;
import jp.co.recruit_lifestyle.android.widget.WaveSwipeRefreshLayout;

/**
 * 被 luern0313 创建于 2019/8/31.
 * 稍后再看
 */

public class WatchlaterFragment extends Fragment
{
    private Context ctx;
    private View rootLayout;
    private ListView wlListView;
    private ListVideoAdapter.ListVideoAdapterListener listVideoAdapterListener;
    private WaveSwipeRefreshLayout waveSwipeRefreshLayout;
    private WatchLaterApi watchLaterApi;
    private TitleView.TitleViewListener titleViewListener;

    public static boolean isLogin;

    private final Handler handler = new Handler();
    private Runnable runnableUi, runnableNoWeb, runnableNoData;

    private ArrayList<ListVideoModel> watchLaterVideoArrayList;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        ctx = getActivity();

        rootLayout = inflater.inflate(R.layout.fragment_watchlater, container, false);
        wlListView = rootLayout.findViewById(R.id.wl_listview);
        waveSwipeRefreshLayout = rootLayout.findViewById(R.id.wl_swipe);
        waveSwipeRefreshLayout.setColorSchemeColors(Color.WHITE, Color.WHITE);
        waveSwipeRefreshLayout.setWaveColor(ColorUtil.getColor(R.attr.colorPrimary, ctx));
        waveSwipeRefreshLayout.setTopOffsetOfWave(getResources().getDimensionPixelSize(R.dimen.titleHeight));
        waveSwipeRefreshLayout.setOnRefreshListener(() -> handler.post(() -> {
            if(isLogin)
            {
                wlListView.setVisibility(View.GONE);
                getWatchLater();
            }
            else waveSwipeRefreshLayout.setRefreshing(false);
        }));

        listVideoAdapterListener = new ListVideoAdapter.ListVideoAdapterListener()
        {
            @Override
            public void onListVideoAdapterClick(int viewId, int position)
            {

            }

            @Override
            public void onListVideoAdapterLongClick(int viewId, final int position)
            {

            }
        };

        runnableUi = () -> {
            rootLayout.findViewById(R.id.wl_nologin).setVisibility(View.GONE);
            rootLayout.findViewById(R.id.wl_noweb).setVisibility(View.GONE);
            rootLayout.findViewById(R.id.wl_nonthing).setVisibility(View.GONE);
            wlListView.setAdapter(new ListVideoAdapter(inflater, watchLaterVideoArrayList, true, wlListView, listVideoAdapterListener));
            wlListView.setVisibility(View.VISIBLE);
            waveSwipeRefreshLayout.setRefreshing(false);
        };

        runnableNoWeb = () -> {
            waveSwipeRefreshLayout.setRefreshing(false);
            rootLayout.findViewById(R.id.wl_noweb).setVisibility(View.VISIBLE);
            rootLayout.findViewById(R.id.wl_nologin).setVisibility(View.GONE);
            rootLayout.findViewById(R.id.wl_nonthing).setVisibility(View.GONE);
        };

        runnableNoData = () -> {
            waveSwipeRefreshLayout.setRefreshing(false);
            rootLayout.findViewById(R.id.wl_nonthing).setVisibility(View.VISIBLE);
            rootLayout.findViewById(R.id.wl_noweb).setVisibility(View.GONE);
            rootLayout.findViewById(R.id.wl_nologin).setVisibility(View.GONE);
        };

        wlListView.setOnItemClickListener((parent, view, position, id) -> {
            Intent intent = new Intent(ctx, VideoActivity.class);
            intent.putExtra(VideoActivity.ARG_AID, watchLaterVideoArrayList.get(position).getAid());
            startActivity(intent);
        });
        wlListView.setOnTouchListener(new ViewTouchListener(wlListView, titleViewListener));

        isLogin = SharedPreferencesUtil.contains(SharedPreferencesUtil.cookies);
        if(isLogin)
        {
            waveSwipeRefreshLayout.setRefreshing(true);
            getWatchLater();
        }
        else
        {
            rootLayout.findViewById(R.id.wl_noweb).setVisibility(View.GONE);
            rootLayout.findViewById(R.id.wl_nologin).setVisibility(View.VISIBLE);
        }

        return rootLayout;
    }

    private void getWatchLater()
    {
        new Thread(() -> {
            try
            {
                watchLaterApi = new WatchLaterApi();
                watchLaterVideoArrayList = watchLaterApi.getWatchLater();
                if(watchLaterVideoArrayList != null && watchLaterVideoArrayList.size() != 0)
                {
                    handler.post(runnableUi);
                }
                else
                {
                    handler.post(runnableNoData);
                }
            }
            catch (NullPointerException e)
            {
                handler.post(runnableNoData);
                e.printStackTrace();
            }
            catch (IOException e)
            {
                handler.post(runnableNoWeb);
                e.printStackTrace();
            }
        }).start();
    }

    @Override
    public void onAttach(@NonNull Context context)
    {
        super.onAttach(context);
        if(context instanceof TitleView.TitleViewListener)
            titleViewListener = (TitleView.TitleViewListener) context;
    }
}
