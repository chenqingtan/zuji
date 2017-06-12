package com.wulee.administrator.zuji.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.text.TextUtils;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.coorchice.library.SuperTextView;
import com.jude.easyrecyclerview.EasyRecyclerView;
import com.wulee.administrator.zuji.R;
import com.wulee.administrator.zuji.adapter.StepRankingAdapter;
import com.wulee.administrator.zuji.base.BaseActivity;
import com.wulee.administrator.zuji.entity.StepInfo;
import com.wulee.administrator.zuji.utils.Pedometer;
import com.wulee.administrator.zuji.utils.SortList;
import com.wulee.administrator.zuji.widget.RecycleViewDivider;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import cn.bmob.v3.BmobQuery;
import cn.bmob.v3.exception.BmobException;
import cn.bmob.v3.listener.FindListener;


public class StepActivity extends BaseActivity{
    public static final String ACTION_ON_STEP_COUNT_CHANGE = "action_on_step_count_change";
    @InjectView(R.id.iv_back)
    ImageView ivBack;
    @InjectView(R.id.title)
    TextView title;
    @InjectView(R.id.tv_step_count)
    SuperTextView tvStepCount;
    @InjectView(R.id.tv_ranking)
    TextView tvRanking;
    @InjectView(R.id.tv_line)
    View tvLine;
    @InjectView(R.id.recyclerview)
    EasyRecyclerView recyclerview;
    @InjectView(R.id.swipeLayout)
    SwipeRefreshLayout swipeLayout;

    private Pedometer pedometer;
    private OnStepCountChangeReceiver mReceiver;

    private StepRankingAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_step);
        ButterKnife.inject(this);

        initData();
        addListener();

        mReceiver = new OnStepCountChangeReceiver();
        IntentFilter filter = new IntentFilter(ACTION_ON_STEP_COUNT_CHANGE);
        registerReceiver(mReceiver, filter);

        pedometer = new Pedometer(this);
    }


    private void initData() {
        title.setText("我的行走");

        mAdapter = new StepRankingAdapter(this,R.layout.step_rank_list_item,null);
        recyclerview.setLayoutManager(new LinearLayoutManager(this));
        recyclerview.addItemDecoration(new RecycleViewDivider(this,LinearLayoutManager.HORIZONTAL));
        recyclerview.setAdapter(mAdapter);

        queryStepRankList();
    }

    private void addListener() {
        swipeLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                queryStepRankList();
            }
        });
    }

    private void queryStepRankList() {
        BmobQuery<StepInfo> query = new BmobQuery<StepInfo>();
        query.include("personInfo");// 希望在查询计步信息的同时也把当前用户的信息查询出来
        query.findObjects(new FindListener<StepInfo>() {
            @Override
            public void done(List<StepInfo> dataList, BmobException e) {
                swipeLayout.setRefreshing(false);
                if(e == null){
                    if (null != dataList &&  dataList.size() > 0) {
                       //数据重复问题，暂未想到解决的好办法

                        mAdapter.setNewData(processReturnList(dataList));
                    }
                }else{
                    Toast.makeText(StepActivity.this,"查询失败"+e.getMessage()+","+e.getErrorCode(),Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private List<StepInfo>  processReturnList(List<StepInfo> dataList){
        SortList<StepInfo> msList = new SortList<StepInfo>();
        msList.sortByMethod(dataList, "getCount", true);
        Date now = new Date();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        String currdate = dateFormat.format( now );

        Iterator<StepInfo> iter = dataList.iterator();
        while(iter.hasNext()){
            StepInfo step = iter.next();
            if(!TextUtils.equals(currdate,step.getUpdatedAt().substring(0,10))){
                iter.remove();
            }
        }
        return dataList;
    }


    @Override
    protected void onResume() {
        super.onResume();
        pedometer.register();
    }


    @OnClick(R.id.iv_back)
    public void onViewClicked(View v) {
        switch (v.getId()) {
            case R.id.iv_back:
                finish();
                break;
        }
    }


    class OnStepCountChangeReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (TextUtils.equals(ACTION_ON_STEP_COUNT_CHANGE, intent.getAction())) {
                tvStepCount.setText(pedometer.getStepCount() + ""); // 支付宝步数统计就是依据了此原理
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        pedometer.unRegister();
    }
}