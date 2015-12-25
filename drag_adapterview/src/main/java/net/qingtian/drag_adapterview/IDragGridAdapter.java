package net.qingtian.drag_adapterview;

/**
 * Created by qingtian on 2015/12/20.
 */
public interface IDragGridAdapter {
    /**
     * 前面的某几个item不可以改变位置
     * @return
     */
    public int getFixedItemSize();
    /**
     * 前列的数量
     * @return
     */
    public int getColumnCount();

    /**
     * 当gridview里出现拖动后，调用这个方法，改变数据的排序，刷新界面
     * @param dragPostion  起始拖拽的位置
     * @param dropPostion  结束拖拽的位置
     */
    public void exchange(int dragPostion, int dropPostion);

    /**
     * 设置当前鼠标拖动到那个item上了
     * @param dropItemPosition  鼠标所在的item的序号
     */
    public void setDropItemPosition(int dropItemPosition);

    /**
     * 设置鼠标所在的item是否显示
     * @param show
     */
    public void setShowDropItem(boolean show);
}
