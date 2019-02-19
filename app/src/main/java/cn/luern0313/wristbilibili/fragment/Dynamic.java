package cn.luern0313.wristbilibili.fragment;

import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.util.LruCache;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

import cn.carbs.android.expandabletextview.library.ExpandableTextView;
import cn.luern0313.wristbilibili.R;
import cn.luern0313.wristbilibili.api.UserDynamic;
import cn.luern0313.wristbilibili.ui.ImgActivity;
import cn.luern0313.wristbilibili.ui.MainActivity;
import cn.luern0313.wristbilibili.ui.OtheruserActivity;
import cn.luern0313.wristbilibili.ui.ReplyActivity;
import cn.luern0313.wristbilibili.ui.VideodetailsActivity;
import jp.co.recruit_lifestyle.android.widget.WaveSwipeRefreshLayout;

/**
 * 被 luern0313 创建于 不知道什么时候.
 * 收藏的fragment
 * 畜生！你收藏了甚么！
 */

public class Dynamic extends Fragment
{
    Context ctx;

    UserDynamic userDynamic;
    ArrayList<Object> dynamicList;

    View rootLayout;
    ListView dyListView;
    WaveSwipeRefreshLayout waveSwipeRefreshLayout;
    View loadingView;
    mAdapter adapter;

    Handler handler = new Handler();
    Runnable runnableUi;
    Runnable runnableNoWeb;
    Runnable runnableNoWebH;
    Runnable runnableAddlist;
    Runnable runnableNodata;

    boolean isLoading = true;
    public static boolean isLogin = false;

    public View onCreateView(final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        ctx = getActivity();
        rootLayout = inflater.inflate(R.layout.fragment_dynamic, container, false);
        dyListView = rootLayout.findViewById(R.id.dy_listview);
        loadingView = inflater.inflate(R.layout.widget_dyloading, null);
        dyListView.addFooterView(loadingView);

        isLogin = MainActivity.sharedPreferences.contains("cookies");

        runnableUi = new Runnable()
        {
            @Override
            public void run()
            {
                rootLayout.findViewById(R.id.dy_noweb).setVisibility(View.GONE);
                rootLayout.findViewById(R.id.dy_nologin).setVisibility(View.GONE);
                rootLayout.findViewById(R.id.dy_nonthing).setVisibility(View.GONE);
                dyListView.setVisibility(View.VISIBLE);

                waveSwipeRefreshLayout.setRefreshing(false);
                adapter = new mAdapter(inflater, dynamicList);
                dyListView.setAdapter(adapter);
            }
        };

        runnableNoWeb = new Runnable()
        {
            @Override
            public void run()
            {
                waveSwipeRefreshLayout.setRefreshing(false);
                rootLayout.findViewById(R.id.dy_noweb).setVisibility(View.VISIBLE);
                rootLayout.findViewById(R.id.dy_nologin).setVisibility(View.GONE);
                rootLayout.findViewById(R.id.dy_nonthing).setVisibility(View.GONE);
                dyListView.setVisibility(View.GONE);
            }
        };

        runnableNoWebH = new Runnable()
        {
            @Override
            public void run()
            {
                ((TextView) loadingView.findViewById(R.id.dyload_text)).setText("好像没有网络...\n检查下网络？");
                loadingView.findViewById(R.id.dyload_button).setVisibility(View.VISIBLE);
                isLoading = false;
            }
        };

        runnableNodata = new Runnable()
        {
            @Override
            public void run()
            {
                rootLayout.findViewById(R.id.dy_noweb).setVisibility(View.GONE);
                rootLayout.findViewById(R.id.dy_nologin).setVisibility(View.VISIBLE);
                rootLayout.findViewById(R.id.dy_nonthing).setVisibility(View.GONE);
                dyListView.setVisibility(View.GONE);
                waveSwipeRefreshLayout.setRefreshing(false);
            }
        };

        runnableAddlist = new Runnable()
        {
            @Override
            public void run()
            {
                adapter.notifyDataSetChanged();
            }
        };

        waveSwipeRefreshLayout = rootLayout.findViewById(R.id.dy_swipe);
        waveSwipeRefreshLayout.setColorSchemeColors(Color.WHITE, Color.WHITE);
        waveSwipeRefreshLayout.setWaveColor(Color.argb(255, 250, 114, 152));
        waveSwipeRefreshLayout.setOnRefreshListener(new WaveSwipeRefreshLayout.OnRefreshListener()
        {
            @Override
            public void onRefresh()
            {
                handler.post(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        if(isLogin)
                        {
                            dyListView.setVisibility(View.GONE);
                            getDynamic();
                        }
                        else waveSwipeRefreshLayout.setRefreshing(false);
                    }
                });
            }
        });

        loadingView.findViewById(R.id.dyload_button).setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                ((TextView) loadingView.findViewById(R.id.dyload_text)).setText(" 加载中. . .");
                loadingView.findViewById(R.id.dyload_button).setVisibility(View.GONE);
                getMoreDynamic();
            }
        });

        dyListView.setOnScrollListener(new AbsListView.OnScrollListener()
        {
            @Override public void onScrollStateChanged(AbsListView view, int scrollState) {}

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount)
            {
                if(visibleItemCount + firstVisibleItem == totalItemCount && !isLoading && isLogin)
                {
                    isLoading = true;
                    getMoreDynamic();
                }
            }
        });

        if(isLogin)
        {
            waveSwipeRefreshLayout.setRefreshing(true);
            getDynamic();
        }
        else
        {
            rootLayout.findViewById(R.id.dy_noweb).setVisibility(View.GONE);
            rootLayout.findViewById(R.id.dy_nologin).setVisibility(View.VISIBLE);
            rootLayout.findViewById(R.id.dy_nonthing).setVisibility(View.GONE);
            dyListView.setVisibility(View.GONE);
        }

        return rootLayout;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, final Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == 0 && resultCode == 0)
        {
            new Thread(new Runnable()
            {
                @Override
                public void run()
                {
                    try
                    {
                        //oid和type都是传过去再传回来
                        //我王境泽传数据就是乱死！也不建多余的变量！（没有真香）
                        if(!data.getStringExtra("text").equals(""))
                        {
                            if(userDynamic.sendReply(data.getStringExtra("oid"), data.getStringExtra("type"), data.getStringExtra("text")))
                            {
                                Looper.prepare();
                                Toast.makeText(ctx, "发送成功！", Toast.LENGTH_SHORT).show();
                                Looper.loop();
                            }
                            else
                            {
                                Looper.prepare();
                                Toast.makeText(ctx, "发送失败，可能是短时间发送过多？", Toast.LENGTH_SHORT).show();
                                Looper.loop();
                            }
                        }
                    }
                    catch(IOException e)
                    {
                        e.printStackTrace();
                        Looper.prepare();
                        Toast.makeText(ctx, "发送失败，请检查网络？", Toast.LENGTH_SHORT).show();
                        Looper.loop();
                    }
                }
            }).start();
        }
    }

    void getDynamic()
    {
        new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    userDynamic = new UserDynamic(MainActivity.sharedPreferences.getString("cookies", ""),
                            MainActivity.sharedPreferences.getString("csrf", ""),
                            MainActivity.sharedPreferences.getString("mid", ""));
                    userDynamic.getDynamic();
                    dynamicList = userDynamic.getDynamicList();
                    if(dynamicList != null && dynamicList.size() != 0)
                    {
                        isLoading = false;
                        handler.post(runnableUi);
                    }
                    else
                    {
                        handler.post(runnableNodata);
                    }
                }
                catch (NullPointerException e)
                {
                    handler.post(runnableNodata);
                    e.printStackTrace();
                }
                catch (IOException e)
                {
                    handler.post(runnableNoWeb);
                    e.printStackTrace();
                }
            }
        }).start();
    }

    void getMoreDynamic()
    {
        new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    userDynamic.getHistoryDynamic();
                    dynamicList.addAll(userDynamic.getDynamicList());
                    isLoading = false;
                    handler.post(runnableAddlist);
                }
                catch (IOException e)
                {
                    handler.post(runnableNoWebH);
                    e.printStackTrace();
                }
            }
        }).start();
    }

    class mAdapter extends BaseAdapter
    {
        private LayoutInflater mInflater;

        private LruCache<String, BitmapDrawable> mImageCache;

        private ArrayList<Object> dyList;

        public mAdapter(LayoutInflater inflater, ArrayList<Object> dyList)
        {
            mInflater = inflater;
            this.dyList = dyList;

            int maxCache = (int) Runtime.getRuntime().maxMemory();
            int cacheSize = maxCache / 6;
            mImageCache = new LruCache<String, BitmapDrawable>(cacheSize)
            {
                @Override
                protected int sizeOf(String key, BitmapDrawable value)
                {
                    try
                    {
                        return value.getBitmap().getByteCount();
                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();
                    }
                    return 0;
                }
            };
        }

        @Override
        public int getCount()
        {
            return dyList.size();
        }

        @Override
        public Object getItem(int position)
        {
            return position;
        }

        @Override
        public long getItemId(int position)
        {
            return position;
        }

        @Override
        public int getViewTypeCount()
        {
            return 5;
        }

        @Override
        public int getItemViewType(int position)
        {
            if(dyList.get(position) instanceof UserDynamic.cardOriginalVideo) return 4;
            else if(dyList.get(position) instanceof UserDynamic.cardOriginalText) return 3;
            else if(dyList.get(position) instanceof UserDynamic.cardUnknow) return 2;
            else if(dyList.get(position) instanceof UserDynamic.cardShareVideo) return 1;
            else if(dyList.get(position) instanceof UserDynamic.cardShareText) return 0;
            return 2;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup viewGroup)
        {
            ViewHolderOriText viewHolderOriText = null;
            ViewHolderOriVid viewHolderOriVid = null;
            ViewHolderShaText viewHolderShaText = null;
            ViewHolderShaVid viewHolderShaVid = null;
            ViewHolderUnktyp viewHolderUnktyp = null;
            int type = getItemViewType(position);

            // 若无可重用的 view 则进行加载
            if(convertView == null)
            {
                switch (type)
                {
                    case 4:
                        //原创视频
                        convertView = mInflater.inflate(R.layout.item_news_original_video, null);
                        viewHolderOriVid = new ViewHolderOriVid();
                        convertView.setTag(viewHolderOriVid);

                        viewHolderOriVid.lay = convertView.findViewById(R.id.liov_lay);
                        viewHolderOriVid.head = convertView.findViewById(R.id.liov_head);
                        viewHolderOriVid.name = convertView.findViewById(R.id.liov_name);
                        viewHolderOriVid.time = convertView.findViewById(R.id.liov_time);
                        viewHolderOriVid.text = convertView.findViewById(R.id.liov_text);
                        viewHolderOriVid.img = convertView.findViewById(R.id.liov_share_img);
                        viewHolderOriVid.imgtext = convertView.findViewById(R.id.liov_textimg);
                        viewHolderOriVid.title = convertView.findViewById(R.id.liov_title);
                        viewHolderOriVid.likebu = convertView.findViewById(R.id.liov_likebu);
                        viewHolderOriVid.like = convertView.findViewById(R.id.liov_like);
                        break;

                    case 3:
                        //原创文字
                        convertView = mInflater.inflate(R.layout.item_news_original_text, null);
                        viewHolderOriText = new ViewHolderOriText();
                        convertView.setTag(viewHolderOriText);

                        viewHolderOriText.head = convertView.findViewById(R.id.liot_head);
                        viewHolderOriText.name = convertView.findViewById(R.id.liot_name);
                        viewHolderOriText.time = convertView.findViewById(R.id.liot_time);
                        viewHolderOriText.text = convertView.findViewById(R.id.liot_text);
                        viewHolderOriText.textimg = convertView.findViewById(R.id.liot_textimg);
                        viewHolderOriText.replybu = convertView.findViewById(R.id.liot_replybu);
                        viewHolderOriText.reply = convertView.findViewById(R.id.liot_reply);
                        viewHolderOriText.likebu = convertView.findViewById(R.id.liot_likebu);
                        viewHolderOriText.like = convertView.findViewById(R.id.liot_like);
                        break;

                    case 2:
                        //未知类型
                        convertView = mInflater.inflate(R.layout.item_news_unknowtype, null);
                        viewHolderUnktyp = new ViewHolderUnktyp();
                        convertView.setTag(viewHolderUnktyp);

                        viewHolderUnktyp.head = convertView.findViewById(R.id.liuk_head);
                        viewHolderUnktyp.name = convertView.findViewById(R.id.liuk_name);
                        viewHolderUnktyp.time = convertView.findViewById(R.id.liuk_time);
                        break;

                    case 1:
                        //转发视频
                        convertView = mInflater.inflate(R.layout.item_news_share_video, null);
                        viewHolderShaVid = new ViewHolderShaVid();
                        convertView.setTag(viewHolderShaVid);

                        viewHolderShaVid.head = convertView.findViewById(R.id.lisv_head);
                        viewHolderShaVid.name = convertView.findViewById(R.id.lisv_name);
                        viewHolderShaVid.time = convertView.findViewById(R.id.lisv_time);
                        viewHolderShaVid.text = convertView.findViewById(R.id.lisv_text);
                        viewHolderShaVid.slay = convertView.findViewById(R.id.lisv_share_lay);
                        viewHolderShaVid.shead = convertView.findViewById(R.id.lisv_share_head);
                        viewHolderShaVid.sname = convertView.findViewById(R.id.lisv_share_name);
                        viewHolderShaVid.simg = convertView.findViewById(R.id.lisv_share_img);
                        viewHolderShaVid.simgtext = convertView.findViewById(R.id.lisv_share_imgtext);
                        viewHolderShaVid.stitle = convertView.findViewById(R.id.lisv_share_text);
                        viewHolderShaVid.replybu = convertView.findViewById(R.id.lisv_replybu);
                        viewHolderShaVid.reply = convertView.findViewById(R.id.lisv_reply);
                        viewHolderShaVid.likebu = convertView.findViewById(R.id.lisv_likebu);
                        viewHolderShaVid.like = convertView.findViewById(R.id.lisv_like);
                        break;

                    case 0:
                        //转发文字
                        convertView = mInflater.inflate(R.layout.item_news_share_text, null);
                        viewHolderShaText = new ViewHolderShaText();
                        convertView.setTag(viewHolderShaText);

                        viewHolderShaText.head = convertView.findViewById(R.id.list_head);
                        viewHolderShaText.name = convertView.findViewById(R.id.list_name);
                        viewHolderShaText.time = convertView.findViewById(R.id.list_time);
                        viewHolderShaText.text = convertView.findViewById(R.id.list_text);
                        viewHolderShaText.shead = convertView.findViewById(R.id.list_share_head);
                        viewHolderShaText.sname = convertView.findViewById(R.id.list_share_name);
                        viewHolderShaText.stext = convertView.findViewById(R.id.list_share_text);
                        viewHolderShaText.stextimg = convertView.findViewById(R.id.list_share_textimg);
                        viewHolderShaText.replybu = convertView.findViewById(R.id.list_replybu);
                        viewHolderShaText.reply = convertView.findViewById(R.id.list_reply);
                        viewHolderShaText.likebu = convertView.findViewById(R.id.list_likebu);
                        viewHolderShaText.like = convertView.findViewById(R.id.list_like);
                        break;
                }
            }
            else
            {
                switch (type)
                {
                    case 4:
                        viewHolderOriVid = (ViewHolderOriVid) convertView.getTag();
                        break;
                    case 3:
                        viewHolderOriText = (ViewHolderOriText) convertView.getTag();
                        break;
                    case 2:
                        viewHolderUnktyp = (ViewHolderUnktyp) convertView.getTag();
                        break;
                    case 1:
                        viewHolderShaVid = (ViewHolderShaVid) convertView.getTag();
                        break;
                    case 0:
                        viewHolderShaText = (ViewHolderShaText) convertView.getTag();
                        break;
                }
            }

            if(type == 4) //原创视频
            {
                final UserDynamic.cardOriginalVideo dy = (UserDynamic.cardOriginalVideo) dyList.get(position);
                viewHolderOriVid.name.setText(Html.fromHtml("<b>" + dy.getOwnerName() + "</b>投稿了视频"));
                viewHolderOriVid.time.setText(dy.getDynamicTime());
                if(!dy.getDynamic().equals(""))
                {
                    viewHolderOriVid.text.setVisibility(View.VISIBLE);
                    viewHolderOriVid.text.setText(dy.getDynamic());
                }
                else viewHolderOriVid.text.setVisibility(View.GONE);
                viewHolderOriVid.imgtext.setText(dy.getVideoDuration() + "  " + dy.getVideoView() + "观看");
                viewHolderOriVid.title.setText(dy.getVideoTitle());
                viewHolderOriVid.like.setText(String.valueOf(dy.getBeLiked()));
                viewHolderOriVid.head.setImageResource(R.drawable.img_default_head);
                viewHolderOriVid.img.setImageResource(R.drawable.img_default_vid);

                viewHolderOriVid.head.setTag(dy.getOwnerHead());
                viewHolderOriVid.img.setTag(dy.getVideoImg());
                BitmapDrawable h = setImageFormWeb(dy.getOwnerHead());
                BitmapDrawable i = setImageFormWeb(dy.getVideoImg());
                if(h != null) viewHolderOriVid.head.setImageDrawable(h);
                if(i != null) viewHolderOriVid.img.setImageDrawable(i);

                viewHolderOriVid.lay.setOnClickListener(new View.OnClickListener()
                {
                    @Override
                    public void onClick(View v)
                    {
                        Intent intent = new Intent(ctx, VideodetailsActivity.class);
                        intent.putExtra("aid", dy.getVideoAid());
                        startActivity(intent);
                    }
                });

                viewHolderOriVid.head.setOnClickListener(new View.OnClickListener()
                {
                    @Override
                    public void onClick(View v)
                    {
                        Intent intent = new Intent(ctx, OtheruserActivity.class);
                        intent.putExtra("mid", dy.getOwnerUid());
                        startActivity(intent);
                    }
                });

            }
            else if(type == 3)// 原创文字
            {
                final UserDynamic.cardOriginalText dy = (UserDynamic.cardOriginalText) dyList.get(position);
                viewHolderOriText.name.setText(dy.getUserName());
                viewHolderOriText.time.setText(dy.getDynamicTime());
                viewHolderOriText.text.setText(dy.getDynamicText());
                if(!dy.getTextImgCount().equals("0"))
                {
                    viewHolderOriText.textimg.setVisibility(View.VISIBLE);
                    viewHolderOriText.textimg.setText("查看共" + dy.getTextImgCount() + "张图片");
                }
                else viewHolderOriText.textimg.setVisibility(View.GONE);
                viewHolderOriText.reply.setText(String.valueOf(dy.getBeReply()));
                viewHolderOriText.like.setText(String.valueOf(dy.getBeLiked()));
                viewHolderOriText.head.setImageResource(R.drawable.img_default_head);

                viewHolderOriText.head.setTag(dy.getUserHead());
                BitmapDrawable h = setImageFormWeb(dy.getUserHead());
                if(h != null) viewHolderOriText.head.setImageDrawable(h);

                viewHolderOriText.textimg.setOnClickListener(new View.OnClickListener()
                {
                    @Override
                    public void onClick(View v)
                    {
                        Intent intent = new Intent(ctx, ImgActivity.class);
                        intent.putExtra("imgUrl", dy.getImgsSrc());
                        startActivity(intent);
                    }
                });

                viewHolderOriText.head.setOnClickListener(new View.OnClickListener()
                {
                    @Override
                    public void onClick(View v)
                    {
                        Intent intent = new Intent(ctx, OtheruserActivity.class);
                        intent.putExtra("mid", dy.getUserUid());
                        startActivity(intent);
                    }
                });

                viewHolderOriText.replybu.setOnClickListener(new View.OnClickListener()
                {
                    @Override
                    public void onClick(View v)
                    {
                        Intent intent = new Intent(ctx, ReplyActivity.class);
                        intent.putExtra("oid", dy.getDynamicId());
                        intent.putExtra("type", dy.getReplyType());
                        startActivityForResult(intent, 0);
                    }
                });
            }
            else if(type == 2) //未知类型
            {
                final UserDynamic.cardUnknow dy = (UserDynamic.cardUnknow) dyList.get(position);
                viewHolderUnktyp.name.setText(dy.getOwnerName());
                viewHolderUnktyp.time.setText(dy.getDynamicTime());
                viewHolderUnktyp.head.setImageResource(R.drawable.img_default_head);

                if(dy.getOwnerHead() != null)
                {
                    viewHolderUnktyp.head.setTag(dy.getOwnerHead());
                    BitmapDrawable h = setImageFormWeb(dy.getOwnerHead());
                    if(h != null) viewHolderUnktyp.head.setImageDrawable(h);
                }

                viewHolderUnktyp.head.setOnClickListener(new View.OnClickListener()
                {
                    @Override
                    public void onClick(View v)
                    {
                        Intent intent = new Intent(ctx, OtheruserActivity.class);
                        intent.putExtra("mid", dy.getOwnerUid());
                        startActivity(intent);
                    }
                });
            }
            else if(type == 1) //转发视频
            {
                final UserDynamic.cardShareVideo dy = (UserDynamic.cardShareVideo) dyList.get(position);
                final UserDynamic.cardOriginalVideo sdy = (UserDynamic.cardOriginalVideo) userDynamic.getDynamicClass(dy.getOriginalVideo(), 1);
                viewHolderShaVid.name.setText(dy.getUserName());
                viewHolderShaVid.time.setText(dy.getDynamicTime());
                viewHolderShaVid.text.setText(dy.getDynamicText());
                viewHolderShaVid.sname.setText(sdy.getOwnerName());
                viewHolderShaVid.simgtext.setText(sdy.getVideoDuration() + "  " + sdy.getVideoView() + "观看");
                viewHolderShaVid.stitle.setText(sdy.getVideoTitle());
                viewHolderShaVid.reply.setText(String.valueOf(dy.getBeReply()));
                viewHolderShaVid.like.setText(String.valueOf(dy.getBeLiked()));
                viewHolderShaVid.head.setImageResource(R.drawable.img_default_head);
                viewHolderShaVid.shead.setImageResource(R.drawable.img_default_head);
                viewHolderShaVid.simg.setImageResource(R.drawable.img_default_vid);

                viewHolderShaVid.head.setTag(dy.getUserHead());
                viewHolderShaVid.shead.setTag(sdy.getOwnerHead());
                viewHolderShaVid.simg.setTag(sdy.getVideoImg());
                BitmapDrawable h = setImageFormWeb(dy.getUserHead());
                BitmapDrawable o = setImageFormWeb(sdy.getOwnerHead());
                BitmapDrawable i = setImageFormWeb(sdy.getVideoImg());
                if(h != null) viewHolderShaVid.head.setImageDrawable(h);
                if(o != null) viewHolderShaVid.shead.setImageDrawable(o);
                if(i != null) viewHolderShaVid.simg.setImageDrawable(i);

                viewHolderShaVid.slay.setOnClickListener(new View.OnClickListener()
                {
                    @Override
                    public void onClick(View v)
                    {
                        Intent intent = new Intent(ctx, VideodetailsActivity.class);
                        intent.putExtra("aid", sdy.getVideoAid());
                        startActivity(intent);
                    }
                });

                viewHolderShaVid.head.setOnClickListener(new View.OnClickListener()
                {
                    @Override
                    public void onClick(View v)
                    {
                        Intent intent = new Intent(ctx, OtheruserActivity.class);
                        intent.putExtra("mid", dy.getUserUid());
                        startActivity(intent);
                    }
                });

                convertView.findViewById(R.id.lisv_share_user).setOnClickListener(new View.OnClickListener()
                {
                    @Override
                    public void onClick(View v)
                    {
                        Intent intent = new Intent(ctx, OtheruserActivity.class);
                        intent.putExtra("mid", sdy.getOwnerUid());
                        startActivity(intent);
                    }
                });

                viewHolderShaVid.replybu.setOnClickListener(new View.OnClickListener()
                {
                    @Override
                    public void onClick(View v)
                    {
                        Intent intent = new Intent(ctx, ReplyActivity.class);
                        intent.putExtra("oid", dy.getDynamicId());
                        intent.putExtra("type", dy.getReplyType());
                        startActivityForResult(intent, 0);
                    }
                });
            }
            else if(type == 0) //转发文字
            {
                final UserDynamic.cardShareText dy = (UserDynamic.cardShareText) dyList.get(position);
                final UserDynamic.cardOriginalText sdy = (UserDynamic.cardOriginalText) userDynamic.getDynamicClass(dy.getOriginalText(), 2);
                viewHolderShaText.name.setText(dy.getUserName());
                viewHolderShaText.time.setText(dy.getDynamicTime());
                viewHolderShaText.text.setText(dy.getDynamicText());
                viewHolderShaText.sname.setText(sdy.getUserName());
                viewHolderShaText.stext.setText(sdy.getDynamicText());
                if(!sdy.getTextImgCount().equals("0"))
                {
                    viewHolderShaText.stextimg.setVisibility(View.VISIBLE);
                    viewHolderShaText.stextimg.setText("查看共" + sdy.getTextImgCount() + "张图片");
                }
                else viewHolderShaText.stextimg.setVisibility(View.GONE);
                viewHolderShaText.reply.setText(String.valueOf(dy.getBeReply()));
                viewHolderShaText.like.setText(String.valueOf(dy.getBeLiked()));
                viewHolderShaText.head.setImageResource(R.drawable.img_default_head);
                viewHolderShaText.shead.setImageResource(R.drawable.img_default_head);

                viewHolderShaText.head.setTag(dy.getUserHead());
                viewHolderShaText.shead.setTag(sdy.getUserHead());
                BitmapDrawable h = setImageFormWeb(dy.getUserHead());
                BitmapDrawable o = setImageFormWeb(sdy.getUserHead());
                if(h != null) viewHolderShaText.head.setImageDrawable(h);
                if(o != null) viewHolderShaText.shead.setImageDrawable(o);

                viewHolderShaText.stextimg.setOnClickListener(new View.OnClickListener()
                {
                    @Override
                    public void onClick(View v)
                    {
                        Intent intent = new Intent(ctx, ImgActivity.class);
                        intent.putExtra("imgUrl", sdy.getImgsSrc());
                        startActivity(intent);
                    }
                });

                viewHolderShaText.head.setOnClickListener(new View.OnClickListener()
                {
                    @Override
                    public void onClick(View v)
                    {
                        Intent intent = new Intent(ctx, OtheruserActivity.class);
                        intent.putExtra("mid", dy.getUserUid());
                        startActivity(intent);
                    }
                });

                convertView.findViewById(R.id.list_share_user).setOnClickListener(new View.OnClickListener()
                {
                    @Override
                    public void onClick(View v)
                    {
                        Intent intent = new Intent(ctx, OtheruserActivity.class);
                        intent.putExtra("mid", sdy.getUserUid());
                        startActivity(intent);
                    }
                });

                viewHolderShaText.replybu.setOnClickListener(new View.OnClickListener()
                {
                    @Override
                    public void onClick(View v)
                    {
                        Intent intent = new Intent(ctx, ReplyActivity.class);
                        intent.putExtra("oid", dy.getDynamicId());
                        intent.putExtra("type", dy.getReplyType());
                        startActivityForResult(intent, 0);
                    }
                });
            }
            return convertView;
        }

        BitmapDrawable setImageFormWeb(String url)
        {
            if(mImageCache.get(url) != null)
            {
                return mImageCache.get(url);
            }
            else
            {
                ImageTask it = new ImageTask();
                it.execute(url);
                return null;
            }
        }

        class ViewHolderOriVid
        {
            RelativeLayout lay;
            ImageView head;
            TextView name;
            TextView time;
            ExpandableTextView text;
            ImageView img;
            TextView imgtext;
            TextView title;
            ImageView likebu;
            TextView like;
        }

        class ViewHolderOriText
        {
            ImageView head;
            TextView name;
            TextView time;
            ExpandableTextView text;
            TextView textimg;
            LinearLayout replybu;
            TextView reply;
            ImageView likebu;
            TextView like;
        }

        class ViewHolderUnktyp
        {
            ImageView head;
            TextView name;
            TextView time;
        }

        class ViewHolderShaVid
        {
            ImageView head;
            TextView name;
            TextView time;
            ExpandableTextView text;
            RelativeLayout slay;
            ImageView shead;
            TextView sname;
            ImageView simg;
            TextView simgtext;
            TextView stitle;
            LinearLayout replybu;
            TextView reply;
            ImageView likebu;
            TextView like;
        }

        class ViewHolderShaText
        {
            ImageView head;
            TextView name;
            TextView time;
            ExpandableTextView text;
            ImageView shead;
            TextView sname;
            ExpandableTextView stext;
            TextView stextimg;
            LinearLayout replybu;
            TextView reply;
            ImageView likebu;
            TextView like;
        }

        class ImageTask extends AsyncTask<String, Void, BitmapDrawable>
        {
            private String imageUrl;

            @Override
            protected BitmapDrawable doInBackground(String... params)
            {
                try
                {
                    imageUrl = params[0];
                    Bitmap bitmap = null;
                    bitmap = downloadImage();
                    BitmapDrawable db = new BitmapDrawable(dyListView.getResources(), bitmap);
                    // 如果本地还没缓存该图片，就缓存
                    if(mImageCache.get(imageUrl) == null && bitmap != null)
                    {
                        mImageCache.put(imageUrl, db);
                    }
                    return db;
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected void onPostExecute(BitmapDrawable result)
            {
                // 通过Tag找到我们需要的ImageView，如果该ImageView所在的item已被移出页面，就会直接返回null
                ImageView iv = dyListView.findViewWithTag(imageUrl);
                if(iv != null && result != null)
                {
                    iv.setImageDrawable(result);
                }
            }

            /**
             * 获得需要压缩的比率
             *
             * @param options 需要传入已经BitmapFactory.decodeStream(is, null, options);
             * @return 返回压缩的比率，最小为1
             */
            public int getInSampleSize(BitmapFactory.Options options) {
                int inSampleSize = 1;
                int realWith = 170;
                int realHeight = 170;

                int outWidth = options.outWidth;
                int outHeight = options.outHeight;

                //获取比率最大的那个
                if (outWidth > realWith || outHeight > realHeight) {
                    int withRadio = Math.round(outWidth / realWith);
                    int heightRadio = Math.round(outHeight / realHeight);
                    inSampleSize = withRadio > heightRadio ? withRadio : heightRadio;
                }
                return inSampleSize;
            }

            /**
             * 根据输入流返回一个压缩的图片
             * @param input 图片的输入流
             * @return 压缩的图片
             */
            public Bitmap getCompressBitmap(InputStream input)
            {
                //因为InputStream要使用两次，但是使用一次就无效了，所以需要复制两个
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                try
                {
                    byte[] buffer = new byte[1024];
                    int len;
                    while ((len = input.read(buffer)) > -1)
                    {
                        baos.write(buffer, 0, len);
                    }
                    baos.flush();
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }

                //复制新的输入流
                InputStream is = new ByteArrayInputStream(baos.toByteArray());
                InputStream is2 = new ByteArrayInputStream(baos.toByteArray());

                //只是获取网络图片的大小，并没有真正获取图片
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                BitmapFactory.decodeStream(is, null, options);
                //获取图片并进行压缩
                options.inSampleSize = getInSampleSize(options);
                options.inJustDecodeBounds = false;
                return BitmapFactory.decodeStream(is2, null, options);
            }

            /**
             * 根据url从网络上下载图片
             *
             * @return 图片
             */
            private Bitmap downloadImage() throws IOException
            {
                HttpURLConnection con = null;
                Bitmap bitmap = null;
                URL url = new URL(imageUrl);
                con = (HttpURLConnection) url.openConnection();
                con.setConnectTimeout(5 * 1000);
                con.setReadTimeout(10 * 1000);
                bitmap = getCompressBitmap(con.getInputStream());
                if(con != null)
                {
                    con.disconnect();
                }
                return bitmap;
            }
        }

    }
}
