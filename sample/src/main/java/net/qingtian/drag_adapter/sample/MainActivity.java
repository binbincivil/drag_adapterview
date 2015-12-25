package net.qingtian.drag_adapter.sample;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import net.qingtian.drag_adapterview.DragGrid;
import net.qingtian.drag_adapterview.IDragGridAdapter;
import net.qingtian.drag_adapterview.sample.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by qingtian on 2015/12/22.
 * @blog http://blog.csdn.net/bingospunky
 */
public class MainActivity extends Activity {

    private static final String TAG = "qingtian";

    private DragGrid mDragGrid;

    private QTAdapter mAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        List<String> list = new ArrayList<String>();
        for (int i = 0; i < 250; i++) {
            list.add(String.valueOf(i));
        }

        mDragGrid = (DragGrid) findViewById(R.id.gv);
        mAdapter = new QTAdapter(list);
        mDragGrid.setAdapter(mAdapter);

    }

    class QTAdapter extends BaseAdapter implements IDragGridAdapter {

        @Override
        public int getFixedItemSize() {
            return 0;
        }

        @Override
        public int getColumnCount() {
            return 4;
        }

        @Override
        public void exchange(int dragPostion, int dropPostion) {
            String stringItem = getItem(dragPostion);
            if (dragPostion < dropPostion) {
                list.add(dropPostion + 1, stringItem);
                list.remove(dragPostion);
            } else {
                list.add(dropPostion, stringItem);
                list.remove(dragPostion + 1);
            }
            notifyDataSetChanged();
            for (int i = 0; i < list.size(); i++) {
                Log.e("TAG", "list.get(i):" + list.get(i));
            }
        }

        public int mDropItemPosition = -1;

        public void setDropItemPosition(int dropItemPosition) {
            mDropItemPosition = dropItemPosition;
        }

        boolean mShowDropItem = true;

        public void setShowDropItem(boolean show) {
            mShowDropItem = show;
            notifyDataSetChanged();
        }

        List<String> list;

        public QTAdapter(List<String> list) {
            this.list = list;
        }

        @Override
        public int getCount() {
            return list.size();
        }

        @Override
        public String getItem(int position) {
            return list.get(position);
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        /**
         * 由于在gridview里实现移动效果是使对item做了移动动画，所以这里不能对item进行复用
         *
         * @param position
         * @param convertView
         * @param parent
         * @return
         */
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            convertView = LayoutInflater.from(MainActivity.this).inflate(R.layout.item, parent, false);

            TextView tv = (TextView) convertView.findViewById(R.id.tv);

            tv.setText(getItem(position));

            if (!mShowDropItem && position == mDropItemPosition) {
                tv.setVisibility(View.INVISIBLE);
            } else {
                tv.setVisibility(View.VISIBLE);
            }

            return convertView;
        }

    }

}
