package net.qingtian.drag_adapterview;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.AttributeSet;
import android.view.ViewGroup;

public class DragGrid extends BaseDragGrid {

    public DragGrid(Context context) {
        super(context);
    }

    public DragGrid(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public DragGrid(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public Bitmap generateDragBitmap(ViewGroup dragViewGroup) {

        dragViewGroup.destroyDrawingCache();
        dragViewGroup.setDrawingCacheEnabled(true);
        Bitmap dragBitmap = Bitmap.createBitmap(dragViewGroup.getDrawingCache());
        dragViewGroup.setDrawingCacheEnabled(false);

        return dragBitmap;
    }
}