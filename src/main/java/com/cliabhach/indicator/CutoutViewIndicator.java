package com.cliabhach.indicator;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.database.DataSetObserver;
import android.os.Build;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.util.SparseArrayCompat;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.LinearLayout;

import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

/**
 * There's a nice monospace line drawing in the javadoc for {@link #showOffsetIndicator(int, float)} that basically sums up
 * this ViewGroup.
 *
 * @author Philip Cohn-Cort (Fuzz)
 */
public class CutoutViewIndicator extends LinearLayout {

    private static final String TAG = CutoutViewIndicator.class.getSimpleName();

    /**
     * This holds onto the views that may be attached to this ViewGroup. It's initialised
     * with space for 5 values because practical experience says that space for 10 would
     * be excessive.
     * <p>
     *     This is kinda micro-optimising since it can expand automatically later.
     * </p>
     */
    @NonNull
    protected SparseArrayCompat<IndicatorViewHolder> holders = new SparseArrayCompat<>(5);

    @NonNull
    protected CutoutViewLayoutParams defaultChildParams;

    protected ViewPager viewPager;

    /**
     * {@link com.eccyan.widget.SpinningViewPager} reports positions as one greater than other ViewPagers. When this variable
     * is true, CutoutViewIndicator will correct for the discrepancy.
     */
    protected boolean usePositiveOffset;
    protected ViewPager.OnPageChangeListener pageChangeListener = new OnViewPagerChangeListener(this);

    protected DataSetObserver dataSetObserver = new DataSetObserver() {
        /**
         * This method is called when the entire data set has changed,
         * most likely through a call to {@link Cursor#requery()} on a {@link Cursor}.
         *
         * In our case, this is most commonly triggered by {@link android.support.v4.view.PagerAdapter#notifyDataSetChanged()}
         */
        @Override
        public void onChanged() {
            super.onChanged();
            int childCount = getChildCount();
            int pageCount = viewPager.getAdapter().getCount();

            CutoutViewLayoutParams[] params = new CutoutViewLayoutParams[Math.max(childCount, pageCount)];

            for (int i = 0; i < childCount; i++) {
                View child = getChildAt(i);
                ViewGroup.LayoutParams childParams = child.getLayoutParams();
                if (childParams != null) {
                    params[i] = CutoutViewLayoutParams.from(childParams);
                }
            }
            removeAllViews();

            for (int i = 0; i < pageCount; i++) {
                IndicatorViewHolder ivh = holders.get(i);
                // This is a cached view
                if (ivh == null || ivh.itemView.getParent() != null) {
                    // Current viewHolder is nonexistent or already in use elsewhere...create and add a new one!
                    addProgressChild(i, params[i]);
                } else {
                    // No need to make a new View, the existing one should do.
                    addView(ivh.itemView, i);
                }
            }

            Log.i(TAG, "onChanged: count=" + pageCount + ", child count=" + childCount);

            // Seriously. They called this the 'CurrentItem'. Can you believe it?
            int currentPageNumber = viewPager.getCurrentItem();
            // Anyway, we need to ensure that item is selected.
            pageChangeListener.onPageSelected(currentPageNumber);
        }

        /**
         * This method is called when the entire data becomes invalid,
         * most likely through a call to {@link Cursor#deactivate()} or {@link Cursor#close()} on a
         * {@link Cursor}.
         */
        @Override
        public void onInvalidated() {
            super.onInvalidated();
        }
    };

    public CutoutViewIndicator(Context context) {
        this(context, null);
    }

    public CutoutViewIndicator(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CutoutViewIndicator(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        defaultChildParams = new CutoutViewLayoutParams(WRAP_CONTENT, WRAP_CONTENT);
        init(context, attrs);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public CutoutViewIndicator(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        defaultChildParams = new CutoutViewLayoutParams(WRAP_CONTENT, WRAP_CONTENT);
        init(context, attrs);
    }

    protected void init(Context context, AttributeSet attrs) {
        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.CutoutViewIndicator);
            setIndicatorDrawableId(a.getResourceId(R.styleable.CutoutViewIndicator_rcv_drawable, 0));
            setInternalSpacing(a.getDimensionPixelOffset(R.styleable.CutoutViewIndicator_rcv_internal_margin, 0));

            // The superclass will have resolved orientation by now.
            if (getOrientation() == HORIZONTAL) {
                setPerpendicularLength(a.getDimensionPixelSize(R.styleable.CutoutViewIndicator_rcv_height, 0));
                setCellLength(a.getDimensionPixelOffset(R.styleable.CutoutViewIndicator_rcv_width, 0));
            } else {
                setPerpendicularLength(a.getDimensionPixelSize(R.styleable.CutoutViewIndicator_rcv_width, 0));
                setCellLength(a.getDimensionPixelOffset(R.styleable.CutoutViewIndicator_rcv_height, 0));
            }

            setCellBackgroundId(a.getResourceId(R.styleable.CutoutViewIndicator_rcv_drawable_unselected, 0));
            a.recycle();
        }
    }

    /**
     * Binds the special properties of {@link CutoutViewLayoutParams} to the
     * child in question.
     *
     * @param position    what position this child possesses in terms of {@link #getChildAt(int)}
     * @param child       a view that will soon be added to this CutoutViewIndicator
     */
    protected void bindChild(int position, View child) {
        CutoutViewLayoutParams lp;
        lp = CutoutViewLayoutParams.from(child.getLayoutParams());
        final int left, top;
        if (getOrientation() == HORIZONTAL) {
            lp.width = lp.cellLength;
            lp.height = lp.perpendicularLength;
            left = (position == 0) ? 0 : getInternalSpacing();
            top = 0;
        } else {
            lp.width = lp.perpendicularLength;
            lp.height = lp.cellLength;
            left = 0;
            top = (position == 0) ? 0 : getInternalSpacing();
        }
        lp.setMargins(left, top, 0, 0);
        lp.gravity = Gravity.CENTER;
    }

    /**
     * If the {@code lp} parameter is null, a new object will be
     * constructed with {@link #generateDefaultLayoutParams()}.
     *
     * @param position used as 'index' parameter to {@link #addView(View, int)}
     * @param lp parameters specifying what the child should look like
     */
    protected void addProgressChild(int position, @Nullable CutoutViewLayoutParams lp) {
        if (lp == null) {
            lp = generateDefaultLayoutParams();
        }

        ImageView child = new ImageView(getContext()); // inflater.inflate(R.layout.cell_layered, this, false);
        child.setScaleType(ImageView.ScaleType.MATRIX);
        child.setLayoutParams(lp);
        child.setBackgroundResource(lp.cellBackgroundId);
        child.setImageResource(lp.indicatorDrawableId);
        addView(child, position);
        holders.put(position, new LayeredImageViewHolder(child));
    }

    /**
     * The caller is responsible for ensuring the parameters are within bounds.
     * <p>
     * In the below line drawing of a horizontal CutoutViewIndicator, we have 4 cells (a.k.a. child views).
     * <br/>
     * ▓ is the indicator and ░ is the background color of each cell.
     * <br/>
     * Note that the percentageOffset is a percentage of each <i>cell</i> that is drawn.
     * This class does not require that each child view is the same length, but it probably
     * looks better that way.
     * </p>
     * <p>
     * <pre>
     *      position=0         position=1         position=2         position=3
     *     ┌──────────────┐   ┌──────────────┐   ┌──────────────┐   ┌──────────────┐
     *     │░░░░░░░░▓▓▓▓▓▓│   │▓▓▓▓▓▓▓▓▓░░░░░│   │░░░░░░░░░░░░░░│   │░░░░░░░░░░░░░░│
     *     │░░░░░░░░▓▓▓▓▓▓│   │▓▓▓▓▓▓▓▓▓░░░░░│   │░░░░░░░░░░░░░░│   │░░░░░░░░░░░░░░│
     *     └──────────────┘   └──────────────┘   └──────────────┘   └──────────────┘
     *      offset=8/14        offset=-8/14       offset=-22/14      offset=-36/14
     * </pre>
     * <p>
     * </p>
     *
     * @param position         corresponds to the view where an indicator should be shown. Must be less than {@link #getChildCount()}
     * @param percentageOffset how much of the indicator to draw (given as a value between -1 and 1). If out of range, no
     *                         indicator will be drawn
     */
    protected void showOffsetIndicator(int position, float percentageOffset) {
        View child = getChildAt(position);
        if (Math.abs(percentageOffset) < 1) {
            // We have something to draw
            if (child instanceof ImageView) {
                OffSetters.offsetImageBy((ImageView) child, getOrientation(), percentageOffset);
            }
        }
    }

    public void enablePositiveOffset(boolean usePositiveOffset) {
        this.usePositiveOffset = usePositiveOffset;
    }

    public void setCellBackgroundId(@DrawableRes int cellBackgroundId) {
        defaultChildParams.cellBackgroundId = cellBackgroundId;
    }

    public int getCellBackgroundId() {
        return defaultChildParams.cellBackgroundId;
    }

    public void setIndicatorDrawableId(@DrawableRes int indicatorDrawableId) {
        defaultChildParams.indicatorDrawableId = indicatorDrawableId;
    }

    /**
     * This is the id of the drawable currently acting as indicator. If 0, no indicator will be shown.
     */
    public int getIndicatorDrawableId() {
        return defaultChildParams.indicatorDrawableId;
    }

    /**
     * This is the width of a cell when {@link #getOrientation() horizontal},
     * but the height of a cell when {@link #getOrientation() vertical}.
     * <p>
     *     All cells are the same proportions by default.
     * </p>
     *
     * @param cellLength any positive number of pixels
     * @see #setPerpendicularLength(int)
     */
    public void setCellLength(int cellLength) {
        defaultChildParams.cellLength = cellLength;
        requestLayout();
    }

    /**
     * This is the space between cells. It is not added as padding to either end of
     * the {@code CutoutViewIndicator}. This view does not draw anything in these
     * spaces (except the background, if present).
     *
     * @param internalSpacing any positive number of pixels
     */
    public void setInternalSpacing(int internalSpacing) {
        defaultChildParams.internalSpacing = internalSpacing;
        requestLayout();
    }

    /**
     * This is the height of a cell when {@link #getOrientation() horizontal},
     * but the width of a cell when {@link #getOrientation() vertical}.
     * <p>
     *     All cells are the same proportions by default.
     * </p>
     *
     * @param perpendicularLength any positive number of pixels
     * @see #setCellLength(int)
     */
    public void setPerpendicularLength(int perpendicularLength) {
        defaultChildParams.perpendicularLength = perpendicularLength;
        requestLayout();
    }

    /**
     * @see #setCellLength(int)
     *
     * @return current length of one cell in pixels
     */
    public int getCellLength() {
        return defaultChildParams.cellLength;
    }

    /**
     * @see #setInternalSpacing(int)
     *
     * @return current space between cells in pixels
     */
    public int getInternalSpacing() {
        return defaultChildParams.internalSpacing;
    }

    /**
     * @see #setPerpendicularLength(int)
     *
     * @return current perpendicular length of one cell in pixels
     */
    public int getPerpendicularLength() {
        return defaultChildParams.perpendicularLength;
    }

    /**
     * Call this after setting the other custom parameters ({@link #setIndicatorDrawableId(int)},
     * {@link #setCellLength(int)}, {@link #setInternalSpacing(int)}, {@link #setPerpendicularLength(int)},
     * {@link #setCellBackgroundId(int)})
     * to avoid redrawing or extra layout stuff.
     *
     * @param newPager the new ViewPager that this'll sync with. Pass null to disable.
     */
    public void setViewPager(@Nullable ViewPager newPager) {
        if (viewPager != null && viewPager.getAdapter() != null) {
            viewPager.removeOnPageChangeListener(pageChangeListener);
            viewPager.getAdapter().unregisterDataSetObserver(dataSetObserver);
        }
        viewPager = newPager;
        if (newPager != null && newPager.getAdapter() != null) {
            newPager.removeOnPageChangeListener(pageChangeListener);
            newPager.addOnPageChangeListener(pageChangeListener);
            newPager.getAdapter().registerDataSetObserver(dataSetObserver);
            dataSetObserver.onChanged();
            getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                        getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    } else {
                        getViewTreeObserver().removeGlobalOnLayoutListener(this);
                    }

                    ensureOnlyOneItemIsSelected();
                }
            });
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            // Note that the superclass calls layout on the child for us
            bindChild(i, child);
        }
        super.onLayout(changed, l, t, r, b);
        ensureOnlyOneItemIsSelected();
    }

    /**
     * Call this to hide the indicator on all views except for the one corresponding to the currently displaying item.
     */
    public void ensureOnlyOneItemIsSelected() {
        if (viewPager != null) {
            int current = viewPager.getCurrentItem();
            for (int i = 0; i < getChildCount(); i++) {
                if (i != current) {
                    IndicatorViewHolder child = getViewHolderAt(i);
                    if (child != null) {
                        // offset by 1 puts it just off-view (i.e. hiding it)
                        child.offsetImageBy(getOrientation(), 1);
                    }
                }
            }
        }
    }

    @Nullable
    private IndicatorViewHolder getViewHolderAt(int position) {
        return holders.get(position);
    }

    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
        return p instanceof CutoutViewLayoutParams;
    }

    @Override
    protected CutoutViewLayoutParams generateDefaultLayoutParams() {
        return new CutoutViewLayoutParams(defaultChildParams);
    }

    @Override
    public LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new CutoutViewLayoutParams(getContext(), attrs);
    }

    @Override
    protected LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
        return new CutoutViewLayoutParams(p);
    }
}
